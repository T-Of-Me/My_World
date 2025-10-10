/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpParser;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.io.ByteArrayEndPoint;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.server.AbstractConnectionFactory;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.ByteArrayOutputStream2;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.Scheduler;

public class LocalConnector
extends AbstractConnector {
    private final BlockingQueue<LocalEndPoint> _connects = new LinkedBlockingQueue<LocalEndPoint>();

    public LocalConnector(Server server, Executor executor, Scheduler scheduler, ByteBufferPool pool, int acceptors, ConnectionFactory ... factories) {
        super(server, executor, scheduler, pool, acceptors, factories);
        this.setIdleTimeout(30000L);
    }

    public LocalConnector(Server server) {
        this(server, null, null, null, -1, new HttpConnectionFactory());
    }

    public LocalConnector(Server server, SslContextFactory sslContextFactory) {
        this(server, null, null, null, -1, AbstractConnectionFactory.getFactories(sslContextFactory, new HttpConnectionFactory()));
    }

    public LocalConnector(Server server, ConnectionFactory connectionFactory) {
        this(server, null, null, null, -1, connectionFactory);
    }

    public LocalConnector(Server server, ConnectionFactory connectionFactory, SslContextFactory sslContextFactory) {
        this(server, null, null, null, -1, AbstractConnectionFactory.getFactories(sslContextFactory, connectionFactory));
    }

    @Override
    public Object getTransport() {
        return this;
    }

    @Deprecated
    public String getResponses(String requests) throws Exception {
        return this.getResponses(requests, 5L, TimeUnit.SECONDS);
    }

    @Deprecated
    public String getResponses(String requests, long idleFor, TimeUnit units) throws Exception {
        ByteBuffer result = this.getResponses(BufferUtil.toBuffer(requests, StandardCharsets.UTF_8), idleFor, units);
        return result == null ? null : BufferUtil.toString(result, StandardCharsets.UTF_8);
    }

    @Deprecated
    public ByteBuffer getResponses(ByteBuffer requestsBuffer) throws Exception {
        return this.getResponses(requestsBuffer, 5L, TimeUnit.SECONDS);
    }

    @Deprecated
    public ByteBuffer getResponses(ByteBuffer requestsBuffer, long idleFor, TimeUnit units) throws Exception {
        if (this.LOG.isDebugEnabled()) {
            this.LOG.debug("requests {}", BufferUtil.toUTF8String(requestsBuffer));
        }
        LocalEndPoint endp = this.executeRequest(requestsBuffer);
        endp.waitUntilClosedOrIdleFor(idleFor, units);
        ByteBuffer responses = endp.takeOutput();
        if (endp.isOutputShutdown()) {
            endp.close();
        }
        if (this.LOG.isDebugEnabled()) {
            this.LOG.debug("responses {}", BufferUtil.toUTF8String(responses));
        }
        return responses;
    }

    public LocalEndPoint executeRequest(String rawRequest) {
        return this.executeRequest(BufferUtil.toBuffer(rawRequest, StandardCharsets.UTF_8));
    }

    private LocalEndPoint executeRequest(ByteBuffer rawRequest) {
        if (!this.isStarted()) {
            throw new IllegalStateException("!STARTED");
        }
        LocalEndPoint endp = new LocalEndPoint();
        endp.addInput(rawRequest);
        this._connects.add(endp);
        return endp;
    }

    public LocalEndPoint connect() {
        LocalEndPoint endp = new LocalEndPoint();
        this._connects.add(endp);
        return endp;
    }

    @Override
    protected void accept(int acceptorID) throws IOException, InterruptedException {
        if (this.LOG.isDebugEnabled()) {
            this.LOG.debug("accepting {}", acceptorID);
        }
        LocalEndPoint endPoint = this._connects.take();
        endPoint.onOpen();
        this.onEndPointOpened(endPoint);
        Connection connection = this.getDefaultConnectionFactory().newConnection(this, endPoint);
        endPoint.setConnection(connection);
        connection.onOpen();
    }

    public ByteBuffer getResponse(ByteBuffer requestsBuffer) throws Exception {
        return this.getResponse(requestsBuffer, false, 10L, TimeUnit.SECONDS);
    }

    public ByteBuffer getResponse(ByteBuffer requestBuffer, long time, TimeUnit unit) throws Exception {
        boolean head = BufferUtil.toString(requestBuffer).toLowerCase().startsWith("head ");
        if (this.LOG.isDebugEnabled()) {
            this.LOG.debug("requests {}", BufferUtil.toUTF8String(requestBuffer));
        }
        LocalEndPoint endp = this.executeRequest(requestBuffer);
        return endp.waitForResponse(head, time, unit);
    }

    public ByteBuffer getResponse(ByteBuffer requestBuffer, boolean head, long time, TimeUnit unit) throws Exception {
        if (this.LOG.isDebugEnabled()) {
            this.LOG.debug("requests {}", BufferUtil.toUTF8String(requestBuffer));
        }
        LocalEndPoint endp = this.executeRequest(requestBuffer);
        return endp.waitForResponse(head, time, unit);
    }

    public String getResponse(String rawRequest) throws Exception {
        return this.getResponse(rawRequest, false, 30L, TimeUnit.SECONDS);
    }

    public String getResponse(String rawRequest, long time, TimeUnit unit) throws Exception {
        boolean head = rawRequest.toLowerCase().startsWith("head ");
        ByteBuffer requestsBuffer = BufferUtil.toBuffer(rawRequest, StandardCharsets.ISO_8859_1);
        if (this.LOG.isDebugEnabled()) {
            this.LOG.debug("request {}", BufferUtil.toUTF8String(requestsBuffer));
        }
        LocalEndPoint endp = this.executeRequest(requestsBuffer);
        return BufferUtil.toString(endp.waitForResponse(head, time, unit), StandardCharsets.ISO_8859_1);
    }

    public String getResponse(String rawRequest, boolean head, long time, TimeUnit unit) throws Exception {
        ByteBuffer requestsBuffer = BufferUtil.toBuffer(rawRequest, StandardCharsets.ISO_8859_1);
        if (this.LOG.isDebugEnabled()) {
            this.LOG.debug("request {}", BufferUtil.toUTF8String(requestsBuffer));
        }
        LocalEndPoint endp = this.executeRequest(requestsBuffer);
        return BufferUtil.toString(endp.waitForResponse(head, time, unit), StandardCharsets.ISO_8859_1);
    }

    public class LocalEndPoint
    extends ByteArrayEndPoint {
        private final CountDownLatch _closed;
        private ByteBuffer _responseData;

        public LocalEndPoint() {
            super(LocalConnector.this.getScheduler(), LocalConnector.this.getIdleTimeout());
            this._closed = new CountDownLatch(1);
            this.setGrowOutput(true);
        }

        @Override
        protected void execute(Runnable task) {
            LocalConnector.this.getExecutor().execute(task);
        }

        @Override
        public void onClose() {
            this.getConnection().onClose();
            LocalConnector.this.onEndPointClosed(this);
            super.onClose();
            this._closed.countDown();
        }

        @Override
        public void doShutdownOutput() {
            super.shutdownOutput();
            this.close();
        }

        public void waitUntilClosed() {
            while (this.isOpen()) {
                try {
                    if (this._closed.await(10L, TimeUnit.SECONDS)) continue;
                    break;
                }
                catch (Exception e) {
                    LocalConnector.this.LOG.warn(e);
                }
            }
        }

        public void waitUntilClosedOrIdleFor(long idleFor, TimeUnit units) {
            Thread.yield();
            int size = this.getOutput().remaining();
            while (this.isOpen()) {
                try {
                    if (this._closed.await(idleFor, units)) continue;
                    if (size == this.getOutput().remaining()) {
                        if (LocalConnector.this.LOG.isDebugEnabled()) {
                            LocalConnector.this.LOG.debug("idle for {} {}", new Object[]{idleFor, units});
                        }
                        return;
                    }
                    size = this.getOutput().remaining();
                }
                catch (Exception e) {
                    LocalConnector.this.LOG.warn(e);
                }
            }
        }

        public String getResponse() throws Exception {
            return this.getResponse(false, 30L, TimeUnit.SECONDS);
        }

        public String getResponse(boolean head, long time, TimeUnit unit) throws Exception {
            ByteBuffer response = this.waitForResponse(head, time, unit);
            if (response != null) {
                return BufferUtil.toString(response);
            }
            return null;
        }

        /*
         * Unable to fully structure code
         * Enabled aggressive block sorting
         * Enabled unnecessary exception pruning
         * Enabled aggressive exception aggregation
         */
        public ByteBuffer waitForResponse(boolean head, long time, TimeUnit unit) throws Exception {
            handler = new HttpParser.ResponseHandler(){

                @Override
                public void parsedHeader(HttpField field) {
                }

                @Override
                public boolean contentComplete() {
                    return false;
                }

                @Override
                public boolean messageComplete() {
                    return true;
                }

                @Override
                public boolean headerComplete() {
                    return false;
                }

                @Override
                public int getHeaderCacheSize() {
                    return 0;
                }

                @Override
                public void earlyEOF() {
                }

                @Override
                public boolean content(ByteBuffer item) {
                    return false;
                }

                @Override
                public void badMessage(int status, String reason) {
                }

                @Override
                public boolean startResponse(HttpVersion version, int status, String reason) {
                    return false;
                }
            };
            parser = new HttpParser(handler);
            parser.setHeadResponse(head);
            bout = new ByteArrayOutputStream2();
            var8_7 = null;
            try {
                block26: {
                    block15: while (true) {
                        if (BufferUtil.hasContent(this._responseData)) {
                            chunk = this._responseData;
                        } else {
                            chunk = this.waitForOutput(time, unit);
                            if (BufferUtil.isEmpty(chunk) && (!this.isOpen() || this.isOutputShutdown())) {
                                parser.atEOF();
                                parser.parseNext(BufferUtil.EMPTY_BUFFER);
                                break block26;
                            }
                        }
                        do {
                            if (!BufferUtil.hasContent(chunk)) continue block15;
                            pos = chunk.position();
                            complete = parser.parseNext(chunk);
                            if (chunk.position() == pos) {
                                if (!BufferUtil.isEmpty(chunk)) ** break;
                                continue block15;
                                var12_14 = null;
                                return var12_14;
                            }
                            bout.write(chunk.array(), chunk.arrayOffset() + pos, chunk.position() - pos);
                        } while (!complete);
                        break;
                    }
                    if (BufferUtil.hasContent(chunk)) {
                        this._responseData = chunk;
                    }
                }
                if (bout.getCount() == 0 && this.isOutputShutdown()) {
                    var9_8 = null;
                    return var9_8;
                }
                var9_8 = ByteBuffer.wrap(bout.getBuf(), 0, bout.getCount());
                return var9_8;
            }
            catch (Throwable var9_9) {
                var8_7 = var9_9;
                throw var9_9;
            }
            finally {
                if (bout != null) {
                    if (var8_7 != null) {
                        try {
                            bout.close();
                        }
                        catch (Throwable var13_15) {
                            var8_7.addSuppressed(var13_15);
                        }
                    } else {
                        bout.close();
                    }
                }
            }
        }
    }
}

