/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.io.AbstractConnection;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.ssl.SslConnection;
import org.eclipse.jetty.io.ssl.SslHandshakeListener;
import org.eclipse.jetty.server.AbstractConnectionFactory;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.util.annotation.Name;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class SslConnectionFactory
extends AbstractConnectionFactory {
    private final SslContextFactory _sslContextFactory;
    private final String _nextProtocol;

    public SslConnectionFactory() {
        this(HttpVersion.HTTP_1_1.asString());
    }

    public SslConnectionFactory(@Name(value="next") String nextProtocol) {
        this((SslContextFactory)null, nextProtocol);
    }

    public SslConnectionFactory(@Name(value="sslContextFactory") SslContextFactory factory, @Name(value="next") String nextProtocol) {
        super("SSL");
        this._sslContextFactory = factory == null ? new SslContextFactory() : factory;
        this._nextProtocol = nextProtocol;
        this.addBean(this._sslContextFactory);
    }

    public SslContextFactory getSslContextFactory() {
        return this._sslContextFactory;
    }

    public String getNextProtocol() {
        return this._nextProtocol;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        SSLEngine engine = this._sslContextFactory.newSSLEngine();
        engine.setUseClientMode(false);
        SSLSession session = engine.getSession();
        if (session.getPacketBufferSize() > this.getInputBufferSize()) {
            this.setInputBufferSize(session.getPacketBufferSize());
        }
    }

    @Override
    public Connection newConnection(Connector connector, EndPoint endPoint) {
        SSLEngine engine = this._sslContextFactory.newSSLEngine(endPoint.getRemoteAddress());
        engine.setUseClientMode(false);
        SslConnection sslConnection = this.newSslConnection(connector, endPoint, engine);
        sslConnection.setRenegotiationAllowed(this._sslContextFactory.isRenegotiationAllowed());
        sslConnection.setRenegotiationLimit(this._sslContextFactory.getRenegotiationLimit());
        this.configure(sslConnection, connector, endPoint);
        ConnectionFactory next = connector.getConnectionFactory(this._nextProtocol);
        SslConnection.DecryptedEndPoint decryptedEndPoint = sslConnection.getDecryptedEndPoint();
        Connection connection = next.newConnection(connector, decryptedEndPoint);
        decryptedEndPoint.setConnection(connection);
        return sslConnection;
    }

    protected SslConnection newSslConnection(Connector connector, EndPoint endPoint, SSLEngine engine) {
        return new SslConnection(connector.getByteBufferPool(), connector.getExecutor(), endPoint, engine);
    }

    @Override
    protected AbstractConnection configure(AbstractConnection connection, Connector connector, EndPoint endPoint) {
        if (connection instanceof SslConnection) {
            SslConnection sslConnection = (SslConnection)connection;
            if (connector instanceof ContainerLifeCycle) {
                ContainerLifeCycle container = (ContainerLifeCycle)((Object)connector);
                container.getBeans(SslHandshakeListener.class).forEach(sslConnection::addHandshakeListener);
            }
            this.getBeans(SslHandshakeListener.class).forEach(sslConnection::addHandshakeListener);
        }
        return super.configure(connection, connector, endPoint);
    }

    @Override
    public String toString() {
        return String.format("%s@%x{%s->%s}", this.getClass().getSimpleName(), this.hashCode(), this.getProtocol(), this._nextProtocol);
    }
}

