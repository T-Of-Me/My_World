/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.io;

import java.io.Closeable;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.SelectorManager;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.component.Dumpable;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.thread.ExecutionStrategy;
import org.eclipse.jetty.util.thread.Invocable;
import org.eclipse.jetty.util.thread.Locker;
import org.eclipse.jetty.util.thread.Scheduler;
import org.eclipse.jetty.util.thread.strategy.EatWhatYouKill;

public class ManagedSelector
extends ContainerLifeCycle
implements Dumpable {
    private static final Logger LOG = Log.getLogger(ManagedSelector.class);
    private final Locker _locker = new Locker();
    private boolean _selecting = false;
    private final Queue<Runnable> _actions = new ArrayDeque<Runnable>();
    private final SelectorManager _selectorManager;
    private final int _id;
    private final ExecutionStrategy _strategy;
    private Selector _selector;

    public ManagedSelector(SelectorManager selectorManager, int id) {
        this._selectorManager = selectorManager;
        this._id = id;
        SelectorProducer producer = new SelectorProducer();
        Executor executor = selectorManager.getExecutor();
        this._strategy = new EatWhatYouKill(producer, executor);
        this.addBean(this._strategy);
        this.setStopTimeout(5000L);
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        this._selector = this._selectorManager.newSelector();
        this._selectorManager.execute(this._strategy::produce);
    }

    public int size() {
        Selector s = this._selector;
        if (s == null) {
            return 0;
        }
        return s.keys().size();
    }

    @Override
    protected void doStop() throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Stopping {}", this);
        }
        CloseEndPoints close_endps = new CloseEndPoints();
        this.submit(close_endps);
        close_endps.await(this.getStopTimeout());
        CloseSelector close_selector = new CloseSelector();
        this.submit(close_selector);
        close_selector.await(this.getStopTimeout());
        super.doStop();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Stopped {}", this);
        }
    }

    public void submit(Runnable change) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Queued change {} on {}", change, this);
        }
        Selector selector = null;
        try (Locker.Lock lock = this._locker.lock();){
            this._actions.offer(change);
            if (this._selecting) {
                selector = this._selector;
                this._selecting = false;
            }
        }
        if (selector != null) {
            selector.wakeup();
        }
    }

    private Runnable processConnect(SelectionKey key, final Connect connect) {
        SelectableChannel channel = key.channel();
        try {
            key.attach(connect.attachment);
            boolean connected = this._selectorManager.doFinishConnect(channel);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Connected {} {}", connected, channel);
            }
            if (connected) {
                if (connect.timeout.cancel()) {
                    key.interestOps(0);
                    return new CreateEndPoint(channel, key){

                        @Override
                        protected void failed(Throwable failure) {
                            super.failed(failure);
                            connect.failed(failure);
                        }
                    };
                }
                throw new SocketTimeoutException("Concurrent Connect Timeout");
            }
            throw new ConnectException();
        }
        catch (Throwable x) {
            connect.failed(x);
            return null;
        }
    }

    private void processAccept(SelectionKey key) {
        SelectableChannel server = key.channel();
        SelectableChannel channel = null;
        try {
            while ((channel = this._selectorManager.doAccept(server)) != null) {
                this._selectorManager.accepted(channel);
            }
        }
        catch (Throwable x) {
            this.closeNoExceptions(channel);
            LOG.warn("Accept failed for channel " + channel, x);
        }
    }

    private void closeNoExceptions(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        }
        catch (Throwable x) {
            LOG.ignore(x);
        }
    }

    private EndPoint createEndPoint(SelectableChannel channel, SelectionKey selectionKey) throws IOException {
        EndPoint endPoint = this._selectorManager.newEndPoint(channel, this, selectionKey);
        endPoint.onOpen();
        this._selectorManager.endPointOpened(endPoint);
        Connection connection = this._selectorManager.newConnection(channel, endPoint, selectionKey.attachment());
        endPoint.setConnection(connection);
        selectionKey.attach(endPoint);
        this._selectorManager.connectionOpened(connection);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Created {}", endPoint);
        }
        return endPoint;
    }

    public void destroyEndPoint(EndPoint endPoint) {
        Connection connection = endPoint.getConnection();
        this.submit(() -> {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Destroyed {}", endPoint);
            }
            if (connection != null) {
                this._selectorManager.connectionClosed(connection);
            }
            this._selectorManager.endPointClosed(endPoint);
        });
    }

    @Override
    public String dump() {
        return ContainerLifeCycle.dump(this);
    }

    @Override
    public void dump(Appendable out, String indent) throws IOException {
        out.append(String.valueOf(this)).append(" id=").append(String.valueOf(this._id)).append(System.lineSeparator());
        Selector selector = this._selector;
        if (selector != null && selector.isOpen()) {
            ArrayList dump = new ArrayList(selector.keys().size() * 2);
            DumpKeys dumpKeys = new DumpKeys(dump);
            this.submit(dumpKeys);
            dumpKeys.await(5L, TimeUnit.SECONDS);
            ContainerLifeCycle.dump(out, indent, dump);
        }
    }

    public String toString() {
        Selector selector = this._selector;
        return String.format("%s id=%s keys=%d selected=%d", super.toString(), this._id, selector != null && selector.isOpen() ? selector.keys().size() : -1, selector != null && selector.isOpen() ? selector.selectedKeys().size() : -1);
    }

    public Selector getSelector() {
        return this._selector;
    }

    private class CloseSelector
    extends NonBlockingAction {
        private CountDownLatch _latch;

        private CloseSelector() {
            this._latch = new CountDownLatch(1);
        }

        @Override
        public void run() {
            Selector selector = ManagedSelector.this._selector;
            ManagedSelector.this._selector = null;
            ManagedSelector.this.closeNoExceptions(selector);
            this._latch.countDown();
        }

        public boolean await(long timeout) {
            try {
                return this._latch.await(timeout, TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException x) {
                return false;
            }
        }
    }

    private class EndPointCloser
    implements Runnable {
        private final EndPoint _endPoint;
        private final CountDownLatch _latch;

        private EndPointCloser(EndPoint endPoint, CountDownLatch latch) {
            this._endPoint = endPoint;
            this._latch = latch;
        }

        @Override
        public void run() {
            ManagedSelector.this.closeNoExceptions(this._endPoint.getConnection());
            this._latch.countDown();
        }
    }

    private class CloseEndPoints
    extends NonBlockingAction {
        private final CountDownLatch _latch;
        private CountDownLatch _allClosed;

        private CloseEndPoints() {
            this._latch = new CountDownLatch(1);
        }

        @Override
        public void run() {
            ArrayList<EndPoint> end_points = new ArrayList<EndPoint>();
            for (SelectionKey key : ManagedSelector.this._selector.keys()) {
                Object attachment;
                if (!key.isValid() || !((attachment = key.attachment()) instanceof EndPoint)) continue;
                end_points.add((EndPoint)attachment);
            }
            int size = end_points.size();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Closing {} endPoints on {}", size, ManagedSelector.this);
            }
            this._allClosed = new CountDownLatch(size);
            this._latch.countDown();
            for (EndPoint endp : end_points) {
                ManagedSelector.this.submit(new EndPointCloser(endp, this._allClosed));
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Closed {} endPoints on {}", size, ManagedSelector.this);
            }
        }

        public boolean await(long timeout) {
            try {
                return this._latch.await(timeout, TimeUnit.MILLISECONDS) && this._allClosed.await(timeout, TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException x) {
                return false;
            }
        }
    }

    private class ConnectTimeout
    extends NonBlockingAction {
        private final Connect connect;

        private ConnectTimeout(Connect connect) {
            this.connect = connect;
        }

        @Override
        public void run() {
            SelectableChannel channel = this.connect.channel;
            if (ManagedSelector.this._selectorManager.isConnectionPending(channel)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Channel {} timed out while connecting, closing it", channel);
                }
                this.connect.failed(new SocketTimeoutException("Connect Timeout"));
            }
        }
    }

    class Connect
    extends NonBlockingAction {
        private final AtomicBoolean failed;
        private final SelectableChannel channel;
        private final Object attachment;
        private final Scheduler.Task timeout;

        Connect(SelectableChannel channel, Object attachment) {
            this.failed = new AtomicBoolean();
            this.channel = channel;
            this.attachment = attachment;
            this.timeout = ManagedSelector.this._selectorManager.getScheduler().schedule(new ConnectTimeout(this), ManagedSelector.this._selectorManager.getConnectTimeout(), TimeUnit.MILLISECONDS);
        }

        @Override
        public void run() {
            try {
                this.channel.register(ManagedSelector.this._selector, 8, this);
            }
            catch (Throwable x) {
                this.failed(x);
            }
        }

        private void failed(Throwable failure) {
            if (this.failed.compareAndSet(false, true)) {
                this.timeout.cancel();
                ManagedSelector.this.closeNoExceptions(this.channel);
                ManagedSelector.this._selectorManager.connectionFailed(this.channel, failure, this.attachment);
            }
        }
    }

    private class CreateEndPoint
    implements Runnable,
    Closeable {
        private final SelectableChannel channel;
        private final SelectionKey key;

        public CreateEndPoint(SelectableChannel channel, SelectionKey key) {
            this.channel = channel;
            this.key = key;
        }

        @Override
        public void run() {
            try {
                ManagedSelector.this.createEndPoint(this.channel, this.key);
            }
            catch (Throwable x) {
                LOG.debug(x);
                this.failed(x);
            }
        }

        @Override
        public void close() {
            LOG.debug("closed creation of {}", this.channel);
            ManagedSelector.this.closeNoExceptions(this.channel);
        }

        protected void failed(Throwable failure) {
            ManagedSelector.this.closeNoExceptions(this.channel);
            LOG.debug(failure);
        }
    }

    class Accept
    extends NonBlockingAction
    implements Closeable {
        private final SelectableChannel channel;
        private final Object attachment;

        Accept(SelectableChannel channel, Object attachment) {
            this.channel = channel;
            this.attachment = attachment;
        }

        @Override
        public void close() {
            LOG.debug("closed accept of {}", this.channel);
            ManagedSelector.this.closeNoExceptions(this.channel);
        }

        @Override
        public void run() {
            try {
                SelectionKey key = this.channel.register(ManagedSelector.this._selector, 0, this.attachment);
                ManagedSelector.this.submit(new CreateEndPoint(this.channel, key));
            }
            catch (Throwable x) {
                ManagedSelector.this.closeNoExceptions(this.channel);
                LOG.debug(x);
            }
        }
    }

    class Acceptor
    extends NonBlockingAction {
        private final SelectableChannel _channel;

        public Acceptor(SelectableChannel channel) {
            this._channel = channel;
        }

        @Override
        public void run() {
            try {
                SelectionKey key = this._channel.register(ManagedSelector.this._selector, 16, "Acceptor");
                if (LOG.isDebugEnabled()) {
                    LOG.debug("{} acceptor={}", this, key);
                }
            }
            catch (Throwable x) {
                ManagedSelector.this.closeNoExceptions(this._channel);
                LOG.warn(x);
            }
        }
    }

    private class DumpKeys
    implements Runnable {
        private final CountDownLatch latch = new CountDownLatch(1);
        private final List<Object> _dumps;

        private DumpKeys(List<Object> dumps) {
            this._dumps = dumps;
        }

        @Override
        public void run() {
            Selector selector = ManagedSelector.this._selector;
            if (selector != null && selector.isOpen()) {
                Set<SelectionKey> keys = selector.keys();
                this._dumps.add(selector + " keys=" + keys.size());
                for (SelectionKey key : keys) {
                    try {
                        this._dumps.add(String.format("SelectionKey@%x{i=%d}->%s", key.hashCode(), key.interestOps(), key.attachment()));
                    }
                    catch (Throwable x) {
                        LOG.ignore(x);
                    }
                }
            }
            this.latch.countDown();
        }

        public boolean await(long timeout, TimeUnit unit) {
            try {
                return this.latch.await(timeout, unit);
            }
            catch (InterruptedException x) {
                return false;
            }
        }
    }

    private static abstract class NonBlockingAction
    implements Runnable,
    Invocable {
        private NonBlockingAction() {
        }

        @Override
        public final Invocable.InvocationType getInvocationType() {
            return Invocable.InvocationType.NON_BLOCKING;
        }
    }

    private class SelectorProducer
    implements ExecutionStrategy.Producer {
        private Set<SelectionKey> _keys = Collections.emptySet();
        private Iterator<SelectionKey> _cursor = Collections.emptyIterator();

        private SelectorProducer() {
        }

        @Override
        public Runnable produce() {
            do {
                Runnable task;
                if ((task = this.processSelected()) != null) {
                    return task;
                }
                Runnable action = this.nextAction();
                if (action != null) {
                    return action;
                }
                this.update();
            } while (this.select());
            return null;
        }

        private Runnable nextAction() {
            while (true) {
                Runnable action;
                try (Locker.Lock lock = ManagedSelector.this._locker.lock();){
                    action = (Runnable)ManagedSelector.this._actions.poll();
                    if (action == null) {
                        ManagedSelector.this._selecting = true;
                        Runnable runnable = null;
                        return runnable;
                    }
                }
                if (Invocable.getInvocationType(action) == Invocable.InvocationType.BLOCKING) {
                    return action;
                }
                try {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Running action {}", action);
                    }
                    action.run();
                    continue;
                }
                catch (Throwable x) {
                    LOG.debug("Could not run action " + action, x);
                    continue;
                }
                break;
            }
        }

        private boolean select() {
            block17: {
                try {
                    Selector selector = ManagedSelector.this._selector;
                    if (selector == null || !selector.isOpen()) break block17;
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Selector loop waiting on select", new Object[0]);
                    }
                    int selected = selector.select();
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Selector loop woken up from select, {}/{} selected", selected, selector.keys().size());
                    }
                    try (Locker.Lock lock = ManagedSelector.this._locker.lock();){
                        ManagedSelector.this._selecting = false;
                    }
                    this._keys = selector.selectedKeys();
                    this._cursor = this._keys.iterator();
                    return true;
                }
                catch (Throwable x) {
                    ManagedSelector.this.closeNoExceptions(ManagedSelector.this._selector);
                    if (ManagedSelector.this.isRunning()) {
                        LOG.warn(x);
                    }
                    LOG.debug(x);
                }
            }
            return false;
        }

        private Runnable processSelected() {
            while (this._cursor.hasNext()) {
                Object attachment;
                SelectionKey key = this._cursor.next();
                if (key.isValid()) {
                    attachment = key.attachment();
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("selected {} {} ", key, attachment);
                    }
                    try {
                        Runnable task;
                        if (attachment instanceof Selectable) {
                            task = ((Selectable)attachment).onSelected();
                            if (task == null) continue;
                            return task;
                        }
                        if (key.isConnectable()) {
                            task = ManagedSelector.this.processConnect(key, (Connect)attachment);
                            if (task == null) continue;
                            return task;
                        }
                        if (key.isAcceptable()) {
                            ManagedSelector.this.processAccept(key);
                            continue;
                        }
                        throw new IllegalStateException("key=" + key + ", att=" + attachment + ", iOps=" + key.interestOps() + ", rOps=" + key.readyOps());
                    }
                    catch (CancelledKeyException x) {
                        LOG.debug("Ignoring cancelled key for channel {}", key.channel());
                        if (!(attachment instanceof EndPoint)) continue;
                        ManagedSelector.this.closeNoExceptions((EndPoint)attachment);
                        continue;
                    }
                    catch (Throwable x) {
                        LOG.warn("Could not process key for channel " + key.channel(), x);
                        if (!(attachment instanceof EndPoint)) continue;
                        ManagedSelector.this.closeNoExceptions((EndPoint)attachment);
                        continue;
                    }
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Selector loop ignoring invalid key for channel {}", key.channel());
                }
                if (!((attachment = key.attachment()) instanceof EndPoint)) continue;
                ManagedSelector.this.closeNoExceptions((EndPoint)attachment);
            }
            return null;
        }

        private void update() {
            for (SelectionKey key : this._keys) {
                this.updateKey(key);
            }
            this._keys.clear();
        }

        private void updateKey(SelectionKey key) {
            Object attachment = key.attachment();
            if (attachment instanceof Selectable) {
                ((Selectable)attachment).updateKey();
            }
        }
    }

    public static interface Selectable {
        public Runnable onSelected();

        public void updateKey();
    }
}

