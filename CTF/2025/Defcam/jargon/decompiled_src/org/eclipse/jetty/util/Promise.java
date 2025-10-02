/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.eclipse.jetty.util.log.Log;

public interface Promise<C> {
    default public void succeeded(C result) {
    }

    default public void failed(Throwable x) {
    }

    public static <T> Promise<T> from(final CompletableFuture<? super T> completable) {
        if (completable instanceof Promise) {
            return (Promise)((Object)completable);
        }
        return new Promise<T>(){

            @Override
            public void succeeded(T result) {
                completable.complete(result);
            }

            @Override
            public void failed(Throwable x) {
                completable.completeExceptionally(x);
            }
        };
    }

    public static class Wrapper<W>
    implements Promise<W> {
        private final Promise<W> promise;

        public Wrapper(Promise<W> promise) {
            this.promise = Objects.requireNonNull(promise);
        }

        @Override
        public void succeeded(W result) {
            this.promise.succeeded(result);
        }

        @Override
        public void failed(Throwable x) {
            this.promise.failed(x);
        }

        public Promise<W> getPromise() {
            return this.promise;
        }

        public Promise<W> unwrap() {
            Promise<W> result = this.promise;
            while (result instanceof Wrapper) {
                result = ((Wrapper)result).unwrap();
            }
            return result;
        }
    }

    public static class Completable<S>
    extends CompletableFuture<S>
    implements Promise<S> {
        @Override
        public void succeeded(S result) {
            this.complete(result);
        }

        @Override
        public void failed(Throwable x) {
            this.completeExceptionally(x);
        }
    }

    public static class Adapter<U>
    implements Promise<U> {
        @Override
        public void failed(Throwable x) {
            Log.getLogger(this.getClass()).warn(x);
        }
    }
}

