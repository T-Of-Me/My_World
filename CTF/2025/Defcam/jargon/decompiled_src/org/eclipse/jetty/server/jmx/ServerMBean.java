/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.eclipse.jetty.jmx.ObjectMBean
 */
package org.eclipse.jetty.server.jmx;

import org.eclipse.jetty.jmx.ObjectMBean;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;

@ManagedObject(value="MBean Wrapper for Server")
public class ServerMBean
extends ObjectMBean {
    private final long startupTime = System.currentTimeMillis();
    private final Server server;

    public ServerMBean(Object managedObject) {
        super(managedObject);
        this.server = (Server)managedObject;
    }

    @ManagedAttribute(value="contexts on this server")
    public Handler[] getContexts() {
        return this.server.getChildHandlersByClass(ContextHandler.class);
    }

    @ManagedAttribute(value="the startup time since January 1st, 1970 (in ms)")
    public long getStartupTime() {
        return this.startupTime;
    }
}

