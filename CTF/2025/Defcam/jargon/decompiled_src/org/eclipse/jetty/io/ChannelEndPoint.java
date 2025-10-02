/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.io;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.SelectionKey;
import org.eclipse.jetty.io.AbstractEndPoint;
import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.io.ManagedSelector;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.thread.Invocable;
import org.eclipse.jetty.util.thread.Locker;
import org.eclipse.jetty.util.thread.Scheduler;

public abstract class ChannelEndPoint
extends AbstractEndPoint
implements ManagedSelector.Selectable {
    private static final Logger LOG = Log.getLogger(ChannelEndPoint.class);
    private final Locker _locker = new Locker();
    private final ByteChannel _channel;
    private final GatheringByteChannel _gather;
    protected final ManagedSelector _selector;
    protected final SelectionKey _key;
    private boolean _updatePending;
    protected int _currentInterestOps;
    protected int _desiredInterestOps;
    private final Runnable _runUpdateKey = new RunnableTask("runUpdateKey"){

        @Override
        public Invocable.InvocationType getInvocationType() {
            return Invocable.InvocationType.NON_BLOCKING;
        }

        @Override
        public void run() {
            ChannelEndPoint.this.updateKey();
        }
    };
    private final Runnable _runFillable = new RunnableCloseable("runFillable"){

        @Override
        public Invocable.InvocationType getInvocationType() {
            return ChannelEndPoint.this.getFillInterest().getCallbackInvocationType();
        }

        @Override
        public void run() {
            ChannelEndPoint.this.getFillInterest().fillable();
        }
    };
    private final Runnable _runCompleteWrite = new RunnableCloseable("runCompleteWrite"){

        @Override
        public Invocable.InvocationType getInvocationType() {
            return ChannelEndPoint.this.getWriteFlusher().getCallbackInvocationType();
        }

        @Override
        public void run() {
            ChannelEndPoint.this.getWriteFlusher().completeWrite();
        }

        @Override
        public String toString() {
            return String.format("CEP:%s:%s:%s->%s", new Object[]{ChannelEndPoint.this, this._operation, this.getInvocationType(), ChannelEndPoint.this.getWriteFlusher()});
        }
    };
    private final Runnable _runCompleteWriteFillable = new RunnableCloseable("runCompleteWriteFillable"){

        @Override
        public Invocable.InvocationType getInvocationType() {
            Invocable.InvocationType flushT;
            Invocable.InvocationType fillT = ChannelEndPoint.this.getFillInterest().getCallbackInvocationType();
            if (fillT == (flushT = ChannelEndPoint.this.getWriteFlusher().getCallbackInvocationType())) {
                return fillT;
            }
            if (fillT == Invocable.InvocationType.EITHER && flushT == Invocable.InvocationType.NON_BLOCKING) {
                return Invocable.InvocationType.EITHER;
            }
            if (fillT == Invocable.InvocationType.NON_BLOCKING && flushT == Invocable.InvocationType.EITHER) {
                return Invocable.InvocationType.EITHER;
            }
            return Invocable.InvocationType.BLOCKING;
        }

        @Override
        public void run() {
            ChannelEndPoint.this.getWriteFlusher().completeWrite();
            ChannelEndPoint.this.getFillInterest().fillable();
        }
    };

    public ChannelEndPoint(ByteChannel channel, ManagedSelector selector, SelectionKey key, Scheduler scheduler) {
        super(scheduler);
        this._channel = channel;
        this._selector = selector;
        this._key = key;
        this._gather = channel instanceof GatheringByteChannel ? (GatheringByteChannel)((Object)channel) : null;
    }

    @Override
    public boolean isOptimizedForDirectBuffers() {
        return true;
    }

    @Override
    public boolean isOpen() {
        return this._channel.isOpen();
    }

    @Override
    public void doClose() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("doClose {}", this);
        }
        try {
            this._channel.close();
        }
        catch (IOException e) {
            LOG.debug(e);
        }
        finally {
            super.doClose();
        }
    }

    @Override
    public void onClose() {
        try {
            super.onClose();
        }
        finally {
            if (this._selector != null) {
                this._selector.destroyEndPoint(this);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public int fill(ByteBuffer buffer) throws IOException {
        if (this.isInputShutdown()) {
            return -1;
        }
        int pos = BufferUtil.flipToFill(buffer);
        try {
            int filled = this._channel.read(buffer);
            if (LOG.isDebugEnabled()) {
                LOG.debug("filled {} {}", filled, this);
            }
            if (filled > 0) {
                this.notIdle();
            } else if (filled == -1) {
                this.shutdownInput();
            }
            int n = filled;
            return n;
        }
        catch (IOException e) {
            LOG.debug(e);
            this.shutdownInput();
            int n = -1;
            return n;
        }
        finally {
            BufferUtil.flipToFlush(buffer, pos);
        }
    }

    @Override
    public boolean flush(ByteBuffer ... buffers) throws IOException {
        long flushed = 0L;
        try {
            if (buffers.length == 1) {
                flushed = this._channel.write(buffers[0]);
            } else if (this._gather != null && buffers.length > 1) {
                flushed = this._gather.write(buffers, 0, buffers.length);
            } else {
                for (ByteBuffer b : buffers) {
                    if (!b.hasRemaining()) continue;
                    int l = this._channel.write(b);
                    if (l > 0) {
                        flushed += (long)l;
                    }
                    if (b.hasRemaining()) break;
                }
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("flushed {} {}", flushed, this);
            }
        }
        catch (IOException e) {
            throw new EofException(e);
        }
        if (flushed > 0L) {
            this.notIdle();
        }
        for (ByteBuffer b : buffers) {
            if (BufferUtil.isEmpty(b)) continue;
            return false;
        }
        return true;
    }

    public ByteChannel getChannel() {
        return this._channel;
    }

    @Override
    public Object getTransport() {
        return this._channel;
    }

    @Override
    protected void needsFillInterest() {
        this.changeInterests(1);
    }

    @Override
    protected void onIncompleteFlush() {
        this.changeInterests(4);
    }

    @Override
    public Runnable onSelected() {
        Runnable task;
        boolean flushable;
        int newInterestOps;
        int oldInterestOps;
        int readyOps = this._key.readyOps();
        try (Locker.Lock lock = this._locker.lock();){
            this._updatePending = true;
            oldInterestOps = this._desiredInterestOps;
            this._desiredInterestOps = newInterestOps = oldInterestOps & ~readyOps;
        }
        boolean fillable = (readyOps & 1) != 0;
        boolean bl = flushable = (readyOps & 4) != 0;
        if (LOG.isDebugEnabled()) {
            LOG.debug("onSelected {}->{} r={} w={} for {}", oldInterestOps, newInterestOps, fillable, flushable, this);
        }
        Runnable runnable = fillable ? (flushable ? this._runCompleteWriteFillable : this._runFillable) : (task = flushable ? this._runCompleteWrite : null);
        if (LOG.isDebugEnabled()) {
            LOG.debug("task {}", task);
        }
        return task;
    }

    @Override
    public void updateKey() {
        try {
            int newInterestOps;
            int oldInterestOps;
            try (Locker.Lock lock = this._locker.lock();){
                this._updatePending = false;
                oldInterestOps = this._currentInterestOps;
                newInterestOps = this._desiredInterestOps;
                if (oldInterestOps != newInterestOps) {
                    this._currentInterestOps = newInterestOps;
                    this._key.interestOps(newInterestOps);
                }
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Key interests updated {} -> {} on {}", oldInterestOps, newInterestOps, this);
            }
        }
        catch (CancelledKeyException x) {
            LOG.debug("Ignoring key update for concurrently closed channel {}", this);
            this.close();
        }
        catch (Throwable x) {
            LOG.warn("Ignoring key update for " + this, x);
            this.close();
        }
    }

    private void changeInterests(int operation) {
        int newInterestOps;
        int oldInterestOps;
        boolean pending;
        try (Locker.Lock lock = this._locker.lock();){
            pending = this._updatePending;
            oldInterestOps = this._desiredInterestOps;
            newInterestOps = oldInterestOps | operation;
            if (newInterestOps != oldInterestOps) {
                this._desiredInterestOps = newInterestOps;
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("changeInterests p={} {}->{} for {}", pending, oldInterestOps, newInterestOps, this);
        }
        if (!pending && this._selector != null) {
            this._selector.submit(this._runUpdateKey);
        }
    }

    @Override
    public String toEndPointString() {
        try {
            boolean valid = this._key != null && this._key.isValid();
            int keyInterests = valid ? this._key.interestOps() : -1;
            int keyReadiness = valid ? this._key.readyOps() : -1;
            return String.format("%s{io=%d/%d,kio=%d,kro=%d}", super.toEndPointString(), this._currentInterestOps, this._desiredInterestOps, keyInterests, keyReadiness);
        }
        catch (Throwable x) {
            return String.format("%s{io=%s,kio=-2,kro=-2}", super.toString(), this._desiredInterestOps);
        }
    }

    private abstract class RunnableCloseable
    extends RunnableTask
    implements Closeable {
        protected RunnableCloseable(String op) {
            super(op);
        }

        @Override
        public void close() {
            try {
                ChannelEndPoint.this.close();
            }
            catch (Throwable x) {
                LOG.warn(x);
            }
        }
    }

    private abstract class RunnableTask
    implements Runnable,
    Invocable {
        final String _operation;

        protected RunnableTask(String op) {
            this._operation = op;
        }

        public String toString() {
            return String.format("CEP:%s:%s:%s", new Object[]{ChannelEndPoint.this, this._operation, this.getInvocationType()});
        }
    }
}

