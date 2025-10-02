/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.io;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadPendingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.thread.Invocable;

public abstract class FillInterest {
    private static final Logger LOG = Log.getLogger(FillInterest.class);
    private final AtomicReference<Callback> _interested = new AtomicReference<Object>(null);
    private Throwable _lastSet;

    protected FillInterest() {
    }

    public void register(Callback callback) throws ReadPendingException {
        if (!this.tryRegister(callback)) {
            LOG.warn("Read pending for {} prevented {}", this._interested, callback);
            if (LOG.isDebugEnabled()) {
                LOG.warn("callback set at ", this._lastSet);
            }
            throw new ReadPendingException();
        }
    }

    public boolean tryRegister(Callback callback) {
        if (callback == null) {
            throw new IllegalArgumentException();
        }
        if (!this._interested.compareAndSet(null, callback)) {
            return false;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("{} register {}", this, callback);
            this._lastSet = new Throwable(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()) + ":" + Thread.currentThread().getName());
        }
        try {
            this.needsFillInterest();
        }
        catch (Throwable e) {
            this.onFail(e);
        }
        return true;
    }

    public void fillable() {
        Callback callback = this._interested.get();
        if (LOG.isDebugEnabled()) {
            LOG.debug("{} fillable {}", this, callback);
        }
        if (callback != null && this._interested.compareAndSet(callback, null)) {
            callback.succeeded();
        } else if (LOG.isDebugEnabled()) {
            LOG.debug("{} lost race {}", this, callback);
        }
    }

    public boolean isInterested() {
        return this._interested.get() != null;
    }

    public Invocable.InvocationType getCallbackInvocationType() {
        Callback callback = this._interested.get();
        return Invocable.getInvocationType(callback);
    }

    public boolean onFail(Throwable cause) {
        Callback callback = this._interested.get();
        if (callback != null && this._interested.compareAndSet(callback, null)) {
            callback.failed(cause);
            return true;
        }
        return false;
    }

    public void onClose() {
        Callback callback = this._interested.get();
        if (LOG.isDebugEnabled()) {
            LOG.debug("{} onClose {}", this, callback);
        }
        if (callback != null && this._interested.compareAndSet(callback, null)) {
            callback.failed(new ClosedChannelException());
        }
    }

    public String toString() {
        return String.format("FillInterest@%x{%b,%s}", this.hashCode(), this._interested.get() != null, this._interested.get());
    }

    public String toStateString() {
        return this._interested.get() == null ? "-" : "FI";
    }

    protected abstract void needsFillInterest() throws IOException;
}

