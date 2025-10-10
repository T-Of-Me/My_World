/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.component;

import java.util.EventListener;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.annotation.ManagedOperation;

@ManagedObject(value="Lifecycle Interface for startable components")
public interface LifeCycle {
    @ManagedOperation(value="Starts the instance", impact="ACTION")
    public void start() throws Exception;

    @ManagedOperation(value="Stops the instance", impact="ACTION")
    public void stop() throws Exception;

    public boolean isRunning();

    public boolean isStarted();

    public boolean isStarting();

    public boolean isStopping();

    public boolean isStopped();

    public boolean isFailed();

    public void addLifeCycleListener(Listener var1);

    public void removeLifeCycleListener(Listener var1);

    public static void start(Object object) {
        if (object instanceof LifeCycle) {
            try {
                ((LifeCycle)object).start();
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void stop(Object object) {
        if (object instanceof LifeCycle) {
            try {
                ((LifeCycle)object).stop();
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static interface Listener
    extends EventListener {
        public void lifeCycleStarting(LifeCycle var1);

        public void lifeCycleStarted(LifeCycle var1);

        public void lifeCycleFailure(LifeCycle var1, Throwable var2);

        public void lifeCycleStopping(LifeCycle var1);

        public void lifeCycleStopped(LifeCycle var1);
    }
}

