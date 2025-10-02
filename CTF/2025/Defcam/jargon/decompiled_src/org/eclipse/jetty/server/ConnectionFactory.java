/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server;

import java.util.List;
import org.eclipse.jetty.http.BadMessageException;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.Connector;

public interface ConnectionFactory {
    public String getProtocol();

    public List<String> getProtocols();

    public Connection newConnection(Connector var1, EndPoint var2);

    public static interface Upgrading
    extends ConnectionFactory {
        public Connection upgradeConnection(Connector var1, EndPoint var2, MetaData.Request var3, HttpFields var4) throws BadMessageException;
    }
}

