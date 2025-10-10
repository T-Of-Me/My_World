/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.ConstantThrowable;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.thread.Invocable;

public class SharedBlockingCallback {
    static final Logger LOG = Log.getLogger(SharedBlockingCallback.class);
    private static Throwable IDLE = new ConstantThrowable("IDLE");
    private static Throwable SUCCEEDED = new ConstantThrowable("SUCCEEDED");
    private static Throwable FAILED = new ConstantThrowable("FAILED");
    private final ReentrantLock _lock = new ReentrantLock();
    private final Condition _idle = this._lock.newCondition();
    private final Condition _complete = this._lock.newCondition();
    private Blocker _blocker = new Blocker();

    protected long getIdleTimeout() {
        return -1L;
    }

    public Blocker acquire() throws IOException {
        long idle = this.getIdleTimeout();
        this._lock.lock();
        try {
            while (this._blocker._state != IDLE) {
                if (idle > 0L && idle < 0x3FFFFFFFFFFFFFFFL) {
                    if (this._idle.await(idle * 2L, TimeUnit.MILLISECONDS)) continue;
                    throw new IOException(new TimeoutException());
                }
                this._idle.await();
            }
            this._blocker._state = null;
            Blocker blocker = this._blocker;
            return blocker;
        }
        catch (InterruptedException x) {
            throw new InterruptedIOException();
        }
        finally {
            this._lock.unlock();
        }
    }

    protected void notComplete(Blocker blocker) {
        LOG.warn("Blocker not complete {}", blocker);
        if (LOG.isDebugEnabled()) {
            LOG.debug(new Throwable());
        }
    }

    private static class BlockerTimeoutException
    extends TimeoutException {
        private BlockerTimeoutException() {
        }
    }

    public class Blocker
    implements Callback,
    Closeable {
        private Throwable _state = SharedBlockingCallback.access$100();

        protected Blocker() {
        }

        @Override
        public Invocable.InvocationType getInvocationType() {
            return Invocable.InvocationType.NON_BLOCKING;
        }

        @Override
        public void succeeded() {
            block4: {
                SharedBlockingCallback.this._lock.lock();
                try {
                    if (this._state == null) {
                        this._state = SUCCEEDED;
                        SharedBlockingCallback.this._complete.signalAll();
                        break block4;
                    }
                    throw new IllegalStateException(this._state);
                }
                finally {
                    SharedBlockingCallback.this._lock.unlock();
                }
            }
        }

        @Override
        public void failed(Throwable cause) {
            block5: {
                SharedBlockingCallback.this._lock.lock();
                try {
                    if (this._state == null) {
                        this._state = cause == null ? FAILED : (cause instanceof BlockerTimeoutException ? new IOException(cause) : cause);
                        SharedBlockingCallback.this._complete.signalAll();
                        break block5;
                    }
                    if (this._state instanceof BlockerTimeoutException) {
                        break block5;
                    }
                    throw new IllegalStateException(this._state);
                }
                finally {
                    SharedBlockingCallback.this._lock.unlock();
                }
            }
        }

        public void block() throws IOException {
            long idle = SharedBlockingCallback.this.getIdleTimeout();
            SharedBlockingCallback.this._lock.lock();
            try {
                while (this._state == null) {
                    if (idle > 0L) {
                        long excess = Math.min(idle / 2L, 1000L);
                        if (SharedBlockingCallback.this._complete.await(idle + excess, TimeUnit.MILLISECONDS)) continue;
                        this._state = new BlockerTimeoutException();
                        continue;
                    }
                    SharedBlockingCallback.this._complete.await();
                }
                if (this._state == SUCCEEDED) {
                    return;
                }
                try {
                    if (this._state == IDLE) {
                        throw new IllegalStateException("IDLE");
                    }
                    if (this._state instanceof IOException) {
                        throw (IOException)this._state;
                    }
                    if (this._state instanceof CancellationException) {
                        throw (CancellationException)this._state;
                    }
                    if (this._state instanceof RuntimeException) {
                        throw (RuntimeException)this._state;
                    }
                    if (this._state instanceof Error) {
                        throw (Error)this._state;
                    }
                    throw new IOException(this._state);
                }
                catch (InterruptedException e) {
                    throw new InterruptedIOException();
                }
            }
            finally {
                SharedBlockingCallback.this._lock.unlock();
            }
        }

        @Override
        public void close() {
            SharedBlockingCallback.this._lock.lock();
            try {
                if (this._state == IDLE) {
                    throw new IllegalStateException("IDLE");
                }
                if (this._state == null) {
                    SharedBlockingCallback.this.notComplete(this);
                }
            }
            finally {
                try {
                    if (this._state instanceof BlockerTimeoutException) {
                        SharedBlockingCallback.this._blocker = new Blocker();
                    } else {
                        this._state = IDLE;
                    }
                    SharedBlockingCallback.this._idle.signalAll();
                    SharedBlockingCallback.this._complete.signalAll();
                }
                finally {
                    SharedBlockingCallback.this._lock.unlock();
                }
            }
        }

        public String toString() {
            SharedBlockingCallback.this._lock.lock();
            try {
                String string = String.format("%s@%x{%s}", Blocker.class.getSimpleName(), this.hashCode(), this._state);
                return string;
            }
            finally {
                SharedBlockingCallback.this._lock.unlock();
            }
        }
    }
}

