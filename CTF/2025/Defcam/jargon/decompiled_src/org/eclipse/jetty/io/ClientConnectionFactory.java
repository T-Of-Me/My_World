/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.io;

import java.io.IOException;
import java.util.Map;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.util.component.ContainerLifeCycle;

public interface ClientConnectionFactory {
    public static final String CONNECTOR_CONTEXT_KEY = "client.connector";

    public Connection newConnection(EndPoint var1, Map<String, Object> var2) throws IOException;

    default public Connection customize(Connection connection, Map<String, Object> context) {
        ContainerLifeCycle connector = (ContainerLifeCycle)context.get(CONNECTOR_CONTEXT_KEY);
        connector.getBeans(Connection.Listener.class).forEach(connection::addListener);
        return connection;
    }
}

