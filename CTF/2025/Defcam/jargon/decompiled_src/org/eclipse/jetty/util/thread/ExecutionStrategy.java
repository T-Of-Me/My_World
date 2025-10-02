/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.thread;

public interface ExecutionStrategy {
    public void dispatch();

    public void produce();

    public static interface Producer {
        public Runnable produce();
    }
}

