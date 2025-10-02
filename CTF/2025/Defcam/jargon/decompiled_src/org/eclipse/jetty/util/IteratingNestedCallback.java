/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util;

import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.IteratingCallback;
import org.eclipse.jetty.util.thread.Invocable;

public abstract class IteratingNestedCallback
extends IteratingCallback {
    final Callback _callback;

    public IteratingNestedCallback(Callback callback) {
        this._callback = callback;
    }

    @Override
    public Invocable.InvocationType getInvocationType() {
        return this._callback.getInvocationType();
    }

    @Override
    protected void onCompleteSuccess() {
        this._callback.succeeded();
    }

    @Override
    protected void onCompleteFailure(Throwable x) {
        this._callback.failed(x);
    }

    @Override
    public String toString() {
        return String.format("%s@%x", this.getClass().getSimpleName(), this.hashCode());
    }
}

