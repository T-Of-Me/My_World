/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.component;

import java.io.FileWriter;
import java.io.Writer;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class FileNoticeLifeCycleListener
implements LifeCycle.Listener {
    private static final Logger LOG = Log.getLogger(FileNoticeLifeCycleListener.class);
    private final String _filename;

    public FileNoticeLifeCycleListener(String filename) {
        this._filename = filename;
    }

    private void writeState(String action, LifeCycle lifecycle) {
        try (FileWriter out = new FileWriter(this._filename, true);){
            ((Writer)out).append(action).append(" ").append(lifecycle.toString()).append("\n");
        }
        catch (Exception e) {
            LOG.warn(e);
        }
    }

    @Override
    public void lifeCycleStarting(LifeCycle event) {
        this.writeState("STARTING", event);
    }

    @Override
    public void lifeCycleStarted(LifeCycle event) {
        this.writeState("STARTED", event);
    }

    @Override
    public void lifeCycleFailure(LifeCycle event, Throwable cause) {
        this.writeState("FAILED", event);
    }

    @Override
    public void lifeCycleStopping(LifeCycle event) {
        this.writeState("STOPPING", event);
    }

    @Override
    public void lifeCycleStopped(LifeCycle event) {
        this.writeState("STOPPED", event);
    }
}

