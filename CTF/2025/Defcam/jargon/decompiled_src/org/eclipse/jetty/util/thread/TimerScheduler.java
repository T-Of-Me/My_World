/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.thread;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.thread.Scheduler;

public class TimerScheduler
extends AbstractLifeCycle
implements Scheduler,
Runnable {
    private static final Logger LOG = Log.getLogger(TimerScheduler.class);
    private final String _name;
    private final boolean _daemon;
    private Timer _timer;

    public TimerScheduler() {
        this(null, false);
    }

    public TimerScheduler(String name, boolean daemon) {
        this._name = name;
        this._daemon = daemon;
    }

    @Override
    protected void doStart() throws Exception {
        this._timer = this._name == null ? new Timer() : new Timer(this._name, this._daemon);
        this.run();
        super.doStart();
    }

    @Override
    protected void doStop() throws Exception {
        this._timer.cancel();
        super.doStop();
        this._timer = null;
    }

    @Override
    public Scheduler.Task schedule(Runnable task, long delay, TimeUnit units) {
        Timer timer = this._timer;
        if (timer == null) {
            throw new RejectedExecutionException("STOPPED: " + this);
        }
        SimpleTask t = new SimpleTask(task);
        timer.schedule((TimerTask)t, units.toMillis(delay));
        return t;
    }

    @Override
    public void run() {
        Timer timer = this._timer;
        if (timer != null) {
            timer.purge();
            this.schedule(this, 1L, TimeUnit.SECONDS);
        }
    }

    private static class SimpleTask
    extends TimerTask
    implements Scheduler.Task {
        private final Runnable _task;

        private SimpleTask(Runnable runnable) {
            this._task = runnable;
        }

        @Override
        public void run() {
            try {
                this._task.run();
            }
            catch (Throwable x) {
                LOG.warn("Exception while executing task " + this._task, x);
            }
        }

        public String toString() {
            return String.format("%s.%s@%x", TimerScheduler.class.getSimpleName(), SimpleTask.class.getSimpleName(), this.hashCode());
        }
    }
}

