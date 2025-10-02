/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server;

import java.util.Objects;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Request;

public class HostHeaderCustomizer
implements HttpConfiguration.Customizer {
    private final String serverName;
    private final int serverPort;

    public HostHeaderCustomizer(String serverName) {
        this(serverName, 0);
    }

    public HostHeaderCustomizer(String serverName, int serverPort) {
        this.serverName = Objects.requireNonNull(serverName);
        this.serverPort = serverPort;
    }

    @Override
    public void customize(Connector connector, HttpConfiguration channelConfig, Request request) {
        if (request.getHeader("Host") == null) {
            request.setAuthority(this.serverName, this.serverPort);
        }
    }
}

