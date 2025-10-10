/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.preventers;

import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public abstract class AbstractLeakPreventer
extends AbstractLifeCycle {
    protected static final Logger LOG = Log.getLogger(AbstractLeakPreventer.class);

    public abstract void prevent(ClassLoader var1);

    @Override
    protected void doStart() throws Exception {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            this.prevent(this.getClass().getClassLoader());
            super.doStart();
        }
        finally {
            Thread.currentThread().setContextClassLoader(loader);
        }
    }
}

