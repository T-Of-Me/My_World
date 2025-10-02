/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.thread;

import java.io.Closeable;

public class ThreadClassLoaderScope
implements Closeable {
    private final ClassLoader old = Thread.currentThread().getContextClassLoader();
    private final ClassLoader scopedClassLoader;

    public ThreadClassLoaderScope(ClassLoader cl) {
        this.scopedClassLoader = cl;
        Thread.currentThread().setContextClassLoader(this.scopedClassLoader);
    }

    @Override
    public void close() {
        Thread.currentThread().setContextClassLoader(this.old);
    }

    public ClassLoader getScopedClassLoader() {
        return this.scopedClassLoader;
    }
}

