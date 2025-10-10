/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.component.Graceful;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.thread.Scheduler;

@ManagedObject(value="Connector Interface")
public interface Connector
extends LifeCycle,
Graceful {
    public Server getServer();

    public Executor getExecutor();

    public Scheduler getScheduler();

    public ByteBufferPool getByteBufferPool();

    public ConnectionFactory getConnectionFactory(String var1);

    public <T> T getConnectionFactory(Class<T> var1);

    public ConnectionFactory getDefaultConnectionFactory();

    public Collection<ConnectionFactory> getConnectionFactories();

    public List<String> getProtocols();

    @ManagedAttribute(value="maximum time a connection can be idle before being closed (in ms)")
    public long getIdleTimeout();

    public Object getTransport();

    public Collection<EndPoint> getConnectedEndPoints();

    public String getName();
}

