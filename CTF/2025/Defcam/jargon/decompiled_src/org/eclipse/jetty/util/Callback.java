/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util;

import java.util.concurrent.CompletableFuture;
import org.eclipse.jetty.util.thread.Invocable;

public interface Callback
extends Invocable {
    public static final Callback NOOP = new Callback(){};

    default public void succeeded() {
    }

    default public void failed(Throwable x) {
    }

    public static Callback from(CompletableFuture<?> completable) {
        return Callback.from(completable, Invocable.InvocationType.NON_BLOCKING);
    }

    public static Callback from(final CompletableFuture<?> completable, final Invocable.InvocationType invocation) {
        if (completable instanceof Callback) {
            return (Callback)((Object)completable);
        }
        return new Callback(){

            @Override
            public void succeeded() {
                completable.complete(null);
            }

            @Override
            public void failed(Throwable x) {
                completable.completeExceptionally(x);
            }

            @Override
            public Invocable.InvocationType getInvocationType() {
                return invocation;
            }
        };
    }

    public static class Completable
    extends CompletableFuture<Void>
    implements Callback {
        private final Invocable.InvocationType invocation;

        public Completable() {
            this(Invocable.InvocationType.NON_BLOCKING);
        }

        public Completable(Invocable.InvocationType invocation) {
            this.invocation = invocation;
        }

        @Override
        public void succeeded() {
            this.complete(null);
        }

        @Override
        public void failed(Throwable x) {
            this.completeExceptionally(x);
        }

        @Override
        public Invocable.InvocationType getInvocationType() {
            return this.invocation;
        }
    }

    public static class Nested
    implements Callback {
        private final Callback callback;

        public Nested(Callback callback) {
            this.callback = callback;
        }

        public Nested(Nested nested) {
            this.callback = nested.callback;
        }

        public Callback getCallback() {
            return this.callback;
        }

        @Override
        public void succeeded() {
            this.callback.succeeded();
        }

        @Override
        public void failed(Throwable x) {
            this.callback.failed(x);
        }

        @Override
        public Invocable.InvocationType getInvocationType() {
            return this.callback.getInvocationType();
        }
    }
}

