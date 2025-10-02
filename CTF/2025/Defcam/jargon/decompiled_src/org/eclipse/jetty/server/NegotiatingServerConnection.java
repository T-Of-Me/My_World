/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server;

import java.io.IOException;
import java.util.List;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import org.eclipse.jetty.io.AbstractConnection;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public abstract class NegotiatingServerConnection
extends AbstractConnection {
    private static final Logger LOG = Log.getLogger(NegotiatingServerConnection.class);
    private final Connector connector;
    private final SSLEngine engine;
    private final List<String> protocols;
    private final String defaultProtocol;
    private String protocol;

    protected NegotiatingServerConnection(Connector connector, EndPoint endPoint, SSLEngine engine, List<String> protocols, String defaultProtocol) {
        super(endPoint, connector.getExecutor());
        this.connector = connector;
        this.protocols = protocols;
        this.defaultProtocol = defaultProtocol;
        this.engine = engine;
    }

    protected List<String> getProtocols() {
        return this.protocols;
    }

    protected String getDefaultProtocol() {
        return this.defaultProtocol;
    }

    protected Connector getConnector() {
        return this.connector;
    }

    protected SSLEngine getSSLEngine() {
        return this.engine;
    }

    protected String getProtocol() {
        return this.protocol;
    }

    protected void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @Override
    public void onOpen() {
        super.onOpen();
        this.fillInterested();
    }

    @Override
    public void onFillable() {
        int filled = this.fill();
        if (filled == 0) {
            if (this.protocol == null) {
                if (this.engine.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("{} could not negotiate protocol, SSLEngine: {}", this, this.engine);
                    }
                    this.close();
                } else {
                    this.fillInterested();
                }
            } else {
                ConnectionFactory connectionFactory = this.connector.getConnectionFactory(this.protocol);
                if (connectionFactory == null) {
                    LOG.info("{} application selected protocol '{}', but no correspondent {} has been configured", this, this.protocol, ConnectionFactory.class.getName());
                    this.close();
                } else {
                    EndPoint endPoint = this.getEndPoint();
                    Connection newConnection = connectionFactory.newConnection(this.connector, endPoint);
                    endPoint.upgrade(newConnection);
                }
            }
        } else if (filled < 0) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("{} detected close on client side", this);
            }
            this.close();
        } else {
            throw new IllegalStateException();
        }
    }

    private int fill() {
        try {
            return this.getEndPoint().fill(BufferUtil.EMPTY_BUFFER);
        }
        catch (IOException x) {
            LOG.debug(x);
            this.close();
            return -1;
        }
    }

    @Override
    public void close() {
        this.getEndPoint().shutdownOutput();
        super.close();
    }

    public static interface CipherDiscriminator {
        public boolean isAcceptable(String var1, String var2, String var3);
    }
}

