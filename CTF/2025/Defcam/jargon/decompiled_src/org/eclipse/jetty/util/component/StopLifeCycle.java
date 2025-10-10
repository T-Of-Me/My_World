/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.component;

import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class StopLifeCycle
extends AbstractLifeCycle
implements LifeCycle.Listener {
    private static final Logger LOG = Log.getLogger(StopLifeCycle.class);
    private final LifeCycle _lifecycle;

    public StopLifeCycle(LifeCycle lifecycle) {
        this._lifecycle = lifecycle;
        this.addLifeCycleListener(this);
    }

    @Override
    public void lifeCycleStarting(LifeCycle lifecycle) {
    }

    @Override
    public void lifeCycleStarted(LifeCycle lifecycle) {
        try {
            this._lifecycle.stop();
        }
        catch (Exception e) {
            LOG.warn(e);
        }
    }

    @Override
    public void lifeCycleFailure(LifeCycle lifecycle, Throwable cause) {
    }

    @Override
    public void lifeCycleStopping(LifeCycle lifecycle) {
    }

    @Override
    public void lifeCycleStopped(LifeCycle lifecycle) {
    }
}

