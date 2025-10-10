/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.io;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executor;
import javax.net.ssl.SSLEngine;
import org.eclipse.jetty.io.AbstractConnection;
import org.eclipse.jetty.io.ClientConnectionFactory;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.RuntimeIOException;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public abstract class NegotiatingClientConnection
extends AbstractConnection {
    private static final Logger LOG = Log.getLogger(NegotiatingClientConnection.class);
    private final SSLEngine engine;
    private final ClientConnectionFactory connectionFactory;
    private final Map<String, Object> context;
    private volatile boolean completed;

    protected NegotiatingClientConnection(EndPoint endp, Executor executor, SSLEngine sslEngine, ClientConnectionFactory connectionFactory, Map<String, Object> context) {
        super(endp, executor);
        this.engine = sslEngine;
        this.connectionFactory = connectionFactory;
        this.context = context;
    }

    protected SSLEngine getSSLEngine() {
        return this.engine;
    }

    protected void completed() {
        this.completed = true;
    }

    @Override
    public void onOpen() {
        super.onOpen();
        try {
            this.getEndPoint().flush(BufferUtil.EMPTY_BUFFER);
            if (this.completed) {
                this.replaceConnection();
            } else {
                this.fillInterested();
            }
        }
        catch (IOException x) {
            this.close();
            throw new RuntimeIOException(x);
        }
    }

    @Override
    public void onFillable() {
        int filled;
        do {
            if ((filled = this.fill()) != 0 || this.completed) continue;
            this.fillInterested();
        } while (filled > 0 && !this.completed);
        if (this.completed) {
            this.replaceConnection();
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

    private void replaceConnection() {
        EndPoint endPoint = this.getEndPoint();
        try {
            endPoint.upgrade(this.connectionFactory.newConnection(endPoint, this.context));
        }
        catch (Throwable x) {
            LOG.debug(x);
            this.close();
        }
    }

    @Override
    public void close() {
        this.getEndPoint().shutdownOutput();
        super.close();
    }
}

