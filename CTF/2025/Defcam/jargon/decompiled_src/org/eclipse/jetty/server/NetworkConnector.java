/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server;

import java.io.Closeable;
import java.io.IOException;
import org.eclipse.jetty.server.Connector;

public interface NetworkConnector
extends Connector,
Closeable {
    public void open() throws IOException;

    @Override
    public void close();

    public boolean isOpen();

    public String getHost();

    public int getPort();

    public int getLocalPort();
}

