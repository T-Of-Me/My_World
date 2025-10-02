/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.thread;

import java.util.concurrent.Executor;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;

@ManagedObject(value="Pool of Threads")
public interface ThreadPool
extends Executor {
    public void join() throws InterruptedException;

    @ManagedAttribute(value="number of threads in pool")
    public int getThreads();

    @ManagedAttribute(value="number of idle threads in pool")
    public int getIdleThreads();

    @ManagedAttribute(value="indicates the pool is low on available threads")
    public boolean isLowOnThreads();

    public static interface SizedThreadPool
    extends ThreadPool {
        public int getMinThreads();

        public int getMaxThreads();

        public void setMinThreads(int var1);

        public void setMaxThreads(int var1);
    }
}

