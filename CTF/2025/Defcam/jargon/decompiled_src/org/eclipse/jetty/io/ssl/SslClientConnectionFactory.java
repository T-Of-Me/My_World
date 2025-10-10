/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.io.ssl;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executor;
import javax.net.ssl.SSLEngine;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.ClientConnectionFactory;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.ssl.SslConnection;
import org.eclipse.jetty.io.ssl.SslHandshakeListener;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class SslClientConnectionFactory
implements ClientConnectionFactory {
    public static final String SSL_CONTEXT_FACTORY_CONTEXT_KEY = "ssl.context.factory";
    public static final String SSL_PEER_HOST_CONTEXT_KEY = "ssl.peer.host";
    public static final String SSL_PEER_PORT_CONTEXT_KEY = "ssl.peer.port";
    public static final String SSL_ENGINE_CONTEXT_KEY = "ssl.engine";
    private final SslContextFactory sslContextFactory;
    private final ByteBufferPool byteBufferPool;
    private final Executor executor;
    private final ClientConnectionFactory connectionFactory;
    private boolean allowMissingCloseMessage = true;

    public SslClientConnectionFactory(SslContextFactory sslContextFactory, ByteBufferPool byteBufferPool, Executor executor, ClientConnectionFactory connectionFactory) {
        this.sslContextFactory = sslContextFactory;
        this.byteBufferPool = byteBufferPool;
        this.executor = executor;
        this.connectionFactory = connectionFactory;
    }

    public boolean isAllowMissingCloseMessage() {
        return this.allowMissingCloseMessage;
    }

    public void setAllowMissingCloseMessage(boolean allowMissingCloseMessage) {
        this.allowMissingCloseMessage = allowMissingCloseMessage;
    }

    @Override
    public Connection newConnection(EndPoint endPoint, Map<String, Object> context) throws IOException {
        String host = (String)context.get(SSL_PEER_HOST_CONTEXT_KEY);
        int port = (Integer)context.get(SSL_PEER_PORT_CONTEXT_KEY);
        SSLEngine engine = this.sslContextFactory.newSSLEngine(host, port);
        engine.setUseClientMode(true);
        context.put(SSL_ENGINE_CONTEXT_KEY, engine);
        SslConnection sslConnection = this.newSslConnection(this.byteBufferPool, this.executor, endPoint, engine);
        endPoint.setConnection(sslConnection);
        SslConnection.DecryptedEndPoint appEndPoint = sslConnection.getDecryptedEndPoint();
        appEndPoint.setConnection(this.connectionFactory.newConnection(appEndPoint, context));
        this.customize(sslConnection, context);
        return sslConnection;
    }

    protected SslConnection newSslConnection(ByteBufferPool byteBufferPool, Executor executor, EndPoint endPoint, SSLEngine engine) {
        return new SslConnection(byteBufferPool, executor, endPoint, engine);
    }

    @Override
    public Connection customize(Connection connection, Map<String, Object> context) {
        if (connection instanceof SslConnection) {
            SslConnection sslConnection = (SslConnection)connection;
            sslConnection.setRenegotiationAllowed(this.sslContextFactory.isRenegotiationAllowed());
            sslConnection.setRenegotiationLimit(this.sslContextFactory.getRenegotiationLimit());
            sslConnection.setAllowMissingCloseMessage(this.isAllowMissingCloseMessage());
            ContainerLifeCycle connector = (ContainerLifeCycle)context.get("client.connector");
            connector.getBeans(SslHandshakeListener.class).forEach(sslConnection::addHandshakeListener);
        }
        return ClientConnectionFactory.super.customize(connection, context);
    }
}

