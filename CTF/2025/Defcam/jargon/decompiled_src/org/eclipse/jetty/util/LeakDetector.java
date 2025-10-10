/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class LeakDetector<T>
extends AbstractLifeCycle
implements Runnable {
    private static final Logger LOG = Log.getLogger(LeakDetector.class);
    private final ReferenceQueue<T> queue = new ReferenceQueue();
    private final ConcurrentMap<String, LeakInfo> resources = new ConcurrentHashMap<String, LeakInfo>();
    private Thread thread;

    public boolean acquired(T resource) {
        String id = this.id(resource);
        LeakInfo info = this.resources.putIfAbsent(id, new LeakInfo(resource, id));
        return info == null;
    }

    public boolean released(T resource) {
        String id = this.id(resource);
        LeakInfo info = (LeakInfo)this.resources.remove(id);
        return info != null;
    }

    public String id(T resource) {
        return String.valueOf(System.identityHashCode(resource));
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        this.thread = new Thread((Runnable)this, this.getClass().getSimpleName());
        this.thread.setDaemon(true);
        this.thread.start();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        this.thread.interrupt();
    }

    @Override
    public void run() {
        try {
            while (this.isRunning()) {
                LeakInfo leakInfo = (LeakInfo)this.queue.remove();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Resource GC'ed: {}", leakInfo);
                }
                if (this.resources.remove(leakInfo.id) == null) continue;
                this.leaked(leakInfo);
            }
        }
        catch (InterruptedException interruptedException) {
            // empty catch block
        }
    }

    protected void leaked(LeakInfo leakInfo) {
        LOG.warn("Resource leaked: " + leakInfo.description, leakInfo.stackFrames);
    }

    public class LeakInfo
    extends PhantomReference<T> {
        private final String id;
        private final String description;
        private final Throwable stackFrames;

        private LeakInfo(T referent, String id) {
            super(referent, LeakDetector.this.queue);
            this.id = id;
            this.description = referent.toString();
            this.stackFrames = new Throwable();
        }

        public String getResourceDescription() {
            return this.description;
        }

        public Throwable getStackFrames() {
            return this.stackFrames;
        }

        public String toString() {
            return this.description;
        }
    }
}

