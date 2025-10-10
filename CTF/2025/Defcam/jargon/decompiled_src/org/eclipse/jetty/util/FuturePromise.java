/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util;

import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.jetty.util.ConstantThrowable;
import org.eclipse.jetty.util.Promise;

public class FuturePromise<C>
implements Future<C>,
Promise<C> {
    private static Throwable COMPLETED = new ConstantThrowable();
    private final AtomicBoolean _done = new AtomicBoolean(false);
    private final CountDownLatch _latch = new CountDownLatch(1);
    private Throwable _cause;
    private C _result;

    public FuturePromise() {
    }

    public FuturePromise(C result) {
        this._cause = COMPLETED;
        this._result = result;
        this._done.set(true);
        this._latch.countDown();
    }

    public FuturePromise(C ctx, Throwable failed) {
        this._result = ctx;
        this._cause = failed;
        this._done.set(true);
        this._latch.countDown();
    }

    @Override
    public void succeeded(C result) {
        if (this._done.compareAndSet(false, true)) {
            this._result = result;
            this._cause = COMPLETED;
            this._latch.countDown();
        }
    }

    @Override
    public void failed(Throwable cause) {
        if (this._done.compareAndSet(false, true)) {
            this._cause = cause;
            this._latch.countDown();
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (this._done.compareAndSet(false, true)) {
            this._result = null;
            this._cause = new CancellationException();
            this._latch.countDown();
            return true;
        }
        return false;
    }

    @Override
    public boolean isCancelled() {
        if (this._done.get()) {
            try {
                this._latch.await();
            }
            catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return this._cause instanceof CancellationException;
        }
        return false;
    }

    @Override
    public boolean isDone() {
        return this._done.get() && this._latch.getCount() == 0L;
    }

    @Override
    public C get() throws InterruptedException, ExecutionException {
        this._latch.await();
        if (this._cause == COMPLETED) {
            return this._result;
        }
        if (this._cause instanceof CancellationException) {
            throw (CancellationException)new CancellationException().initCause(this._cause);
        }
        throw new ExecutionException(this._cause);
    }

    @Override
    public C get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (!this._latch.await(timeout, unit)) {
            throw new TimeoutException();
        }
        if (this._cause == COMPLETED) {
            return this._result;
        }
        if (this._cause instanceof TimeoutException) {
            throw (TimeoutException)this._cause;
        }
        if (this._cause instanceof CancellationException) {
            throw (CancellationException)new CancellationException().initCause(this._cause);
        }
        throw new ExecutionException(this._cause);
    }

    public static void rethrow(ExecutionException e) throws IOException {
        Throwable cause = e.getCause();
        if (cause instanceof IOException) {
            throw (IOException)cause;
        }
        if (cause instanceof Error) {
            throw (Error)cause;
        }
        if (cause instanceof RuntimeException) {
            throw (RuntimeException)cause;
        }
        throw new RuntimeException(cause);
    }

    public String toString() {
        return String.format("FutureCallback@%x{%b,%b,%s}", this.hashCode(), this._done.get(), this._cause == COMPLETED, this._result);
    }
}

