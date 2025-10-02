/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util;

import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.jetty.util.Callback;

public class CountingCallback
extends Callback.Nested {
    private final AtomicInteger count;

    public CountingCallback(Callback callback, int count) {
        super(callback);
        this.count = new AtomicInteger(count);
    }

    @Override
    public void succeeded() {
        int current;
        do {
            if ((current = this.count.get()) != 0) continue;
            return;
        } while (!this.count.compareAndSet(current, current - 1));
        if (current == 1) {
            super.succeeded();
        }
    }

    @Override
    public void failed(Throwable failure) {
        int current;
        do {
            if ((current = this.count.get()) != 0) continue;
            return;
        } while (!this.count.compareAndSet(current, 0));
        super.failed(failure);
    }

    public String toString() {
        return String.format("%s@%x", this.getClass().getSimpleName(), this.hashCode());
    }
}

