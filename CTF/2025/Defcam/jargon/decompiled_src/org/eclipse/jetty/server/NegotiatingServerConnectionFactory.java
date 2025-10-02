/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.net.ssl.SSLEngine;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.io.AbstractConnection;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.ssl.SslConnection;
import org.eclipse.jetty.server.AbstractConnectionFactory;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.SslConnectionFactory;

public abstract class NegotiatingServerConnectionFactory
extends AbstractConnectionFactory {
    private final List<String> negotiatedProtocols = new ArrayList<String>();
    private String defaultProtocol;

    public static void checkProtocolNegotiationAvailable() {
        try {
            String javaVersion = System.getProperty("java.version");
            String alpnClassName = "org.eclipse.jetty.alpn.ALPN";
            if (javaVersion.startsWith("1.")) {
                Class<?> klass = ClassLoader.getSystemClassLoader().loadClass(alpnClassName);
                if (klass.getClassLoader() != null) {
                    throw new IllegalStateException(alpnClassName + " must be on JVM boot classpath");
                }
            } else {
                NegotiatingServerConnectionFactory.class.getClassLoader().loadClass(alpnClassName);
            }
        }
        catch (ClassNotFoundException x) {
            throw new IllegalStateException("No ALPN classes available");
        }
    }

    public NegotiatingServerConnectionFactory(String protocol, String ... negotiatedProtocols) {
        super(protocol);
        if (negotiatedProtocols != null) {
            for (String p : negotiatedProtocols) {
                if ((p = p.trim()).isEmpty()) continue;
                this.negotiatedProtocols.add(p.trim());
            }
        }
    }

    public String getDefaultProtocol() {
        return this.defaultProtocol;
    }

    public void setDefaultProtocol(String defaultProtocol) {
        String dft = defaultProtocol == null ? "" : defaultProtocol.trim();
        this.defaultProtocol = dft.isEmpty() ? null : dft;
    }

    public List<String> getNegotiatedProtocols() {
        return this.negotiatedProtocols;
    }

    @Override
    public Connection newConnection(Connector connector, EndPoint endPoint) {
        String dft;
        List<String> negotiated = this.negotiatedProtocols;
        if (negotiated.isEmpty()) {
            negotiated = connector.getProtocols().stream().filter(p -> {
                ConnectionFactory f = connector.getConnectionFactory((String)p);
                return !(f instanceof SslConnectionFactory) && !(f instanceof NegotiatingServerConnectionFactory);
            }).collect(Collectors.toList());
        }
        if ((dft = this.defaultProtocol) == null && !negotiated.isEmpty()) {
            dft = negotiated.contains(HttpVersion.HTTP_1_1.asString()) ? HttpVersion.HTTP_1_1.asString() : negotiated.get(0);
        }
        SSLEngine engine = null;
        EndPoint ep = endPoint;
        while (engine == null && ep != null) {
            if (ep instanceof SslConnection.DecryptedEndPoint) {
                engine = ((SslConnection.DecryptedEndPoint)ep).getSslConnection().getSSLEngine();
                continue;
            }
            ep = null;
        }
        return this.configure(this.newServerConnection(connector, endPoint, engine, negotiated, dft), connector, endPoint);
    }

    protected abstract AbstractConnection newServerConnection(Connector var1, EndPoint var2, SSLEngine var3, List<String> var4, String var5);

    @Override
    public String toString() {
        return String.format("%s@%x{%s,%s,%s}", this.getClass().getSimpleName(), this.hashCode(), this.getProtocols(), this.getDefaultProtocol(), this.getNegotiatedProtocols());
    }
}

