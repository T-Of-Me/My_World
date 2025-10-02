/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.annotation.ManagedOperation;
import org.eclipse.jetty.util.component.Destroyable;
import org.eclipse.jetty.util.component.LifeCycle;

@ManagedObject(value="Jetty Handler")
public interface Handler
extends LifeCycle,
Destroyable {
    public void handle(String var1, Request var2, HttpServletRequest var3, HttpServletResponse var4) throws IOException, ServletException;

    public void setServer(Server var1);

    @ManagedAttribute(value="the jetty server for this handler", readonly=true)
    public Server getServer();

    @Override
    @ManagedOperation(value="destroy associated resources", impact="ACTION")
    public void destroy();
}

