/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.thread;

import java.io.Closeable;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public interface Invocable {
    public static final ThreadLocal<Boolean> __nonBlocking = new ThreadLocal<Boolean>(){

        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

    public static boolean isNonBlockingInvocation() {
        return __nonBlocking.get();
    }

    public static void invokeNonBlocking(Runnable task) {
        Boolean was_non_blocking = __nonBlocking.get();
        try {
            __nonBlocking.set(Boolean.TRUE);
            task.run();
        }
        finally {
            __nonBlocking.set(was_non_blocking);
        }
    }

    public static void invokePreferNonBlocking(Runnable task) {
        switch (Invocable.getInvocationType(task)) {
            case BLOCKING: 
            case NON_BLOCKING: {
                task.run();
                break;
            }
            case EITHER: {
                Invocable.invokeNonBlocking(task);
            }
        }
    }

    public static void invokePreferred(Runnable task, InvocationType preferredInvocationType) {
        switch (Invocable.getInvocationType(task)) {
            case BLOCKING: 
            case NON_BLOCKING: {
                task.run();
                break;
            }
            case EITHER: {
                if (Invocable.getInvocationType(task) == InvocationType.EITHER && preferredInvocationType == InvocationType.NON_BLOCKING) {
                    Invocable.invokeNonBlocking(task);
                    break;
                }
                task.run();
            }
        }
    }

    public static Runnable asPreferred(Runnable task, InvocationType preferredInvocationType) {
        switch (Invocable.getInvocationType(task)) {
            case BLOCKING: 
            case NON_BLOCKING: {
                break;
            }
            case EITHER: {
                if (preferredInvocationType != InvocationType.NON_BLOCKING) break;
                return () -> Invocable.invokeNonBlocking(task);
            }
        }
        return task;
    }

    public static InvocationType getInvocationType(Object o) {
        if (o instanceof Invocable) {
            return ((Invocable)o).getInvocationType();
        }
        return InvocationType.BLOCKING;
    }

    default public InvocationType getInvocationType() {
        return InvocationType.BLOCKING;
    }

    public static class InvocableExecutor
    implements Executor {
        private static final Logger LOG = Log.getLogger(InvocableExecutor.class);
        private final Executor _executor;
        private final InvocationType _preferredInvocationForExecute;
        private final InvocationType _preferredInvocationForInvoke;

        public InvocableExecutor(Executor executor, InvocationType preferred) {
            this(executor, preferred, preferred);
        }

        public InvocableExecutor(Executor executor, InvocationType preferredInvocationForExecute, InvocationType preferredInvocationForIvoke) {
            this._executor = executor;
            this._preferredInvocationForExecute = preferredInvocationForExecute;
            this._preferredInvocationForInvoke = preferredInvocationForIvoke;
        }

        public InvocationType getPreferredInvocationType() {
            return this._preferredInvocationForInvoke;
        }

        public void invoke(Runnable task) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("{} invoke  {}", this, task);
            }
            Invocable.invokePreferred(task, this._preferredInvocationForInvoke);
            if (LOG.isDebugEnabled()) {
                LOG.debug("{} invoked {}", this, task);
            }
        }

        @Override
        public void execute(Runnable task) {
            this.tryExecute(task, this._preferredInvocationForExecute);
        }

        public void execute(Runnable task, InvocationType preferred) {
            this.tryExecute(task, preferred);
        }

        public boolean tryExecute(Runnable task) {
            return this.tryExecute(task, this._preferredInvocationForExecute);
        }

        public boolean tryExecute(Runnable task, InvocationType preferred) {
            try {
                this._executor.execute(Invocable.asPreferred(task, preferred));
                return true;
            }
            catch (RejectedExecutionException e) {
                LOG.debug(e);
                LOG.warn("Rejected execution of {}", task);
                try {
                    if (task instanceof Closeable) {
                        ((Closeable)((Object)task)).close();
                    }
                }
                catch (Exception x) {
                    e.addSuppressed(x);
                    LOG.warn(e);
                }
                return false;
            }
        }
    }

    public static enum InvocationType {
        BLOCKING,
        NON_BLOCKING,
        EITHER;

    }
}

