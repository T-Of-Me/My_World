/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.component;

import java.util.concurrent.CopyOnWriteArrayList;
import org.eclipse.jetty.util.Uptime;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

@ManagedObject(value="Abstract Implementation of LifeCycle")
public abstract class AbstractLifeCycle
implements LifeCycle {
    private static final Logger LOG = Log.getLogger(AbstractLifeCycle.class);
    public static final String STOPPED = "STOPPED";
    public static final String FAILED = "FAILED";
    public static final String STARTING = "STARTING";
    public static final String STARTED = "STARTED";
    public static final String STOPPING = "STOPPING";
    public static final String RUNNING = "RUNNING";
    private final CopyOnWriteArrayList<LifeCycle.Listener> _listeners = new CopyOnWriteArrayList();
    private final Object _lock = new Object();
    private final int __FAILED = -1;
    private final int __STOPPED = 0;
    private final int __STARTING = 1;
    private final int __STARTED = 2;
    private final int __STOPPING = 3;
    private volatile int _state = 0;
    private long _stopTimeout = 30000L;
    public static final LifeCycle.Listener STOP_ON_FAILURE = new AbstractLifeCycleListener(){

        @Override
        public void lifeCycleFailure(LifeCycle lifecycle, Throwable cause) {
            try {
                lifecycle.stop();
            }
            catch (Exception e) {
                cause.addSuppressed(e);
            }
        }
    };

    protected void doStart() throws Exception {
    }

    protected void doStop() throws Exception {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public final void start() throws Exception {
        Object object = this._lock;
        synchronized (object) {
            try {
                if (this._state == 2 || this._state == 1) {
                    return;
                }
                this.setStarting();
                this.doStart();
                this.setStarted();
            }
            catch (Throwable e) {
                this.setFailed(e);
                throw e;
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public final void stop() throws Exception {
        Object object = this._lock;
        synchronized (object) {
            try {
                if (this._state == 3 || this._state == 0) {
                    return;
                }
                this.setStopping();
                this.doStop();
                this.setStopped();
            }
            catch (Throwable e) {
                this.setFailed(e);
                throw e;
            }
        }
    }

    @Override
    public boolean isRunning() {
        int state = this._state;
        return state == 2 || state == 1;
    }

    @Override
    public boolean isStarted() {
        return this._state == 2;
    }

    @Override
    public boolean isStarting() {
        return this._state == 1;
    }

    @Override
    public boolean isStopping() {
        return this._state == 3;
    }

    @Override
    public boolean isStopped() {
        return this._state == 0;
    }

    @Override
    public boolean isFailed() {
        return this._state == -1;
    }

    @Override
    public void addLifeCycleListener(LifeCycle.Listener listener) {
        this._listeners.add(listener);
    }

    @Override
    public void removeLifeCycleListener(LifeCycle.Listener listener) {
        this._listeners.remove(listener);
    }

    @ManagedAttribute(value="Lifecycle State for this instance", readonly=true)
    public String getState() {
        switch (this._state) {
            case -1: {
                return FAILED;
            }
            case 1: {
                return STARTING;
            }
            case 2: {
                return STARTED;
            }
            case 3: {
                return STOPPING;
            }
            case 0: {
                return STOPPED;
            }
        }
        return null;
    }

    public static String getState(LifeCycle lc) {
        if (lc.isStarting()) {
            return STARTING;
        }
        if (lc.isStarted()) {
            return STARTED;
        }
        if (lc.isStopping()) {
            return STOPPING;
        }
        if (lc.isStopped()) {
            return STOPPED;
        }
        return FAILED;
    }

    private void setStarted() {
        this._state = 2;
        if (LOG.isDebugEnabled()) {
            LOG.debug("STARTED @{}ms {}", Uptime.getUptime(), this);
        }
        for (LifeCycle.Listener listener : this._listeners) {
            listener.lifeCycleStarted(this);
        }
    }

    private void setStarting() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("starting {}", this);
        }
        this._state = 1;
        for (LifeCycle.Listener listener : this._listeners) {
            listener.lifeCycleStarting(this);
        }
    }

    private void setStopping() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("stopping {}", this);
        }
        this._state = 3;
        for (LifeCycle.Listener listener : this._listeners) {
            listener.lifeCycleStopping(this);
        }
    }

    private void setStopped() {
        this._state = 0;
        if (LOG.isDebugEnabled()) {
            LOG.debug("{} {}", STOPPED, this);
        }
        for (LifeCycle.Listener listener : this._listeners) {
            listener.lifeCycleStopped(this);
        }
    }

    private void setFailed(Throwable th) {
        this._state = -1;
        if (LOG.isDebugEnabled()) {
            LOG.warn("FAILED " + this + ": " + th, th);
        }
        for (LifeCycle.Listener listener : this._listeners) {
            listener.lifeCycleFailure(this, th);
        }
    }

    @ManagedAttribute(value="The stop timeout in milliseconds")
    public long getStopTimeout() {
        return this._stopTimeout;
    }

    public void setStopTimeout(long stopTimeout) {
        this._stopTimeout = stopTimeout;
    }

    public static abstract class AbstractLifeCycleListener
    implements LifeCycle.Listener {
        @Override
        public void lifeCycleFailure(LifeCycle event, Throwable cause) {
        }

        @Override
        public void lifeCycleStarted(LifeCycle event) {
        }

        @Override
        public void lifeCycleStarting(LifeCycle event) {
        }

        @Override
        public void lifeCycleStopped(LifeCycle event) {
        }

        @Override
        public void lifeCycleStopping(LifeCycle event) {
        }
    }
}

