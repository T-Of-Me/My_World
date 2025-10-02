/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.component.LifeCycle;

@ManagedObject(value="Handler of Multiple Handlers")
public interface HandlerContainer
extends LifeCycle {
    @ManagedAttribute(value="handlers in this container")
    public Handler[] getHandlers();

    @ManagedAttribute(value="all contained handlers")
    public Handler[] getChildHandlers();

    public Handler[] getChildHandlersByClass(Class<?> var1);

    public <T extends Handler> T getChildHandlerByClass(Class<T> var1);
}

