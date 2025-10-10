/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executor;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.ManagedSelector;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.component.Dumpable;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.thread.Scheduler;

public abstract class SelectorManager
extends ContainerLifeCycle
implements Dumpable {
    public static final int DEFAULT_CONNECT_TIMEOUT = 15000;
    protected static final Logger LOG = Log.getLogger(SelectorManager.class);
    private final Executor executor;
    private final Scheduler scheduler;
    private final ManagedSelector[] _selectors;
    private long _connectTimeout = 15000L;
    private long _selectorIndex;

    protected SelectorManager(Executor executor, Scheduler scheduler) {
        this(executor, scheduler, (Runtime.getRuntime().availableProcessors() + 1) / 2);
    }

    protected SelectorManager(Executor executor, Scheduler scheduler, int selectors) {
        if (selectors <= 0) {
            throw new IllegalArgumentException("No selectors");
        }
        this.executor = executor;
        this.scheduler = scheduler;
        this._selectors = new ManagedSelector[selectors];
    }

    public Executor getExecutor() {
        return this.executor;
    }

    public Scheduler getScheduler() {
        return this.scheduler;
    }

    public long getConnectTimeout() {
        return this._connectTimeout;
    }

    public void setConnectTimeout(long milliseconds) {
        this._connectTimeout = milliseconds;
    }

    protected void execute(Runnable task) {
        this.executor.execute(task);
    }

    public int getSelectorCount() {
        return this._selectors.length;
    }

    private ManagedSelector chooseSelector(SelectableChannel channel) {
        ManagedSelector candidate1 = null;
        if (channel != null) {
            try {
                byte[] addr;
                SocketAddress remote;
                if (channel instanceof SocketChannel && (remote = ((SocketChannel)channel).getRemoteAddress()) instanceof InetSocketAddress && (addr = ((InetSocketAddress)remote).getAddress().getAddress()) != null) {
                    int s = addr[addr.length - 1] & 0xFF;
                    candidate1 = this._selectors[s % this.getSelectorCount()];
                }
            }
            catch (IOException x) {
                LOG.ignore(x);
            }
        }
        long s = this._selectorIndex++;
        int index = (int)(s % (long)this.getSelectorCount());
        ManagedSelector candidate2 = this._selectors[index];
        if (candidate1 == null || candidate1.size() >= candidate2.size() * 2) {
            return candidate2;
        }
        return candidate1;
    }

    public void connect(SelectableChannel channel, Object attachment) {
        ManagedSelector set;
        ManagedSelector managedSelector = set = this.chooseSelector(channel);
        managedSelector.getClass();
        set.submit(managedSelector.new ManagedSelector.Connect(channel, attachment));
    }

    public void accept(SelectableChannel channel) {
        this.accept(channel, null);
    }

    public void accept(SelectableChannel channel, Object attachment) {
        ManagedSelector selector;
        ManagedSelector managedSelector = selector = this.chooseSelector(channel);
        managedSelector.getClass();
        selector.submit(managedSelector.new ManagedSelector.Accept(channel, attachment));
    }

    public void acceptor(SelectableChannel server) {
        ManagedSelector selector;
        ManagedSelector managedSelector = selector = this.chooseSelector(null);
        managedSelector.getClass();
        selector.submit(managedSelector.new ManagedSelector.Acceptor(server));
    }

    protected void accepted(SelectableChannel channel) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void doStart() throws Exception {
        for (int i = 0; i < this._selectors.length; ++i) {
            ManagedSelector selector;
            this._selectors[i] = selector = this.newSelector(i);
            this.addBean(selector);
        }
        super.doStart();
    }

    protected ManagedSelector newSelector(int id) {
        return new ManagedSelector(this, id);
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        for (ManagedSelector selector : this._selectors) {
            this.removeBean(selector);
        }
    }

    protected void endPointOpened(EndPoint endpoint) {
    }

    protected void endPointClosed(EndPoint endpoint) {
    }

    public void connectionOpened(Connection connection) {
        try {
            connection.onOpen();
        }
        catch (Throwable x) {
            if (this.isRunning()) {
                LOG.warn("Exception while notifying connection " + connection, x);
            } else {
                LOG.debug("Exception while notifying connection " + connection, x);
            }
            throw x;
        }
    }

    public void connectionClosed(Connection connection) {
        try {
            connection.onClose();
        }
        catch (Throwable x) {
            LOG.debug("Exception while notifying connection " + connection, x);
        }
    }

    protected boolean doFinishConnect(SelectableChannel channel) throws IOException {
        return ((SocketChannel)channel).finishConnect();
    }

    protected boolean isConnectionPending(SelectableChannel channel) {
        return ((SocketChannel)channel).isConnectionPending();
    }

    protected SelectableChannel doAccept(SelectableChannel server) throws IOException {
        return ((ServerSocketChannel)server).accept();
    }

    protected void connectionFailed(SelectableChannel channel, Throwable ex, Object attachment) {
        LOG.warn(String.format("%s - %s", channel, attachment), ex);
    }

    protected Selector newSelector() throws IOException {
        return Selector.open();
    }

    protected abstract EndPoint newEndPoint(SelectableChannel var1, ManagedSelector var2, SelectionKey var3) throws IOException;

    public abstract Connection newConnection(SelectableChannel var1, EndPoint var2, Object var3) throws IOException;
}

