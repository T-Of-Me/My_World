/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.thread;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.thread.Scheduler;

public class Sweeper
extends AbstractLifeCycle
implements Runnable {
    private static final Logger LOG = Log.getLogger(Sweeper.class);
    private final AtomicReference<List<Sweepable>> items = new AtomicReference();
    private final AtomicReference<Scheduler.Task> task = new AtomicReference();
    private final Scheduler scheduler;
    private final long period;

    public Sweeper(Scheduler scheduler, long period) {
        this.scheduler = scheduler;
        this.period = period;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        this.items.set(new CopyOnWriteArrayList());
        this.activate();
    }

    @Override
    protected void doStop() throws Exception {
        this.deactivate();
        this.items.set(null);
        super.doStop();
    }

    public int getSize() {
        List<Sweepable> refs = this.items.get();
        return refs == null ? 0 : refs.size();
    }

    public boolean offer(Sweepable sweepable) {
        List<Sweepable> refs = this.items.get();
        if (refs == null) {
            return false;
        }
        refs.add(sweepable);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Resource offered {}", sweepable);
        }
        return true;
    }

    public boolean remove(Sweepable sweepable) {
        List<Sweepable> refs = this.items.get();
        return refs != null && refs.remove(sweepable);
    }

    @Override
    public void run() {
        List<Sweepable> refs = this.items.get();
        if (refs == null) {
            return;
        }
        for (Sweepable sweepable : refs) {
            try {
                if (!sweepable.sweep()) continue;
                refs.remove(sweepable);
                if (!LOG.isDebugEnabled()) continue;
                LOG.debug("Resource swept {}", sweepable);
            }
            catch (Throwable x) {
                LOG.info("Exception while sweeping " + sweepable, x);
            }
        }
        this.activate();
    }

    private void activate() {
        if (this.isRunning()) {
            Scheduler.Task t = this.scheduler.schedule(this, this.period, TimeUnit.MILLISECONDS);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Scheduled in {} ms sweep task {}", this.period, t);
            }
            this.task.set(t);
        } else if (LOG.isDebugEnabled()) {
            LOG.debug("Skipping sweep task scheduling", new Object[0]);
        }
    }

    private void deactivate() {
        Scheduler.Task t = this.task.getAndSet(null);
        if (t != null) {
            boolean cancelled = t.cancel();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Cancelled ({}) sweep task {}", cancelled, t);
            }
        }
    }

    public static interface Sweepable {
        public boolean sweep();
    }
}

