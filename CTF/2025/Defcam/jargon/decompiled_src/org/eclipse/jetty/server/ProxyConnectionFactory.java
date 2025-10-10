/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.ReadPendingException;
import java.nio.channels.WritePendingException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import org.eclipse.jetty.io.AbstractConnection;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.AbstractConnectionFactory;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.util.AttributesMap;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.TypeUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class ProxyConnectionFactory
extends AbstractConnectionFactory {
    public static final String TLS_VERSION = "TLS_VERSION";
    private static final Logger LOG = Log.getLogger(ProxyConnectionFactory.class);
    private final String _next;
    private int _maxProxyHeader = 1024;
    private static final byte[] MAGIC = new byte[]{13, 10, 13, 10, 0, 13, 10, 81, 85, 73, 84, 10};

    public ProxyConnectionFactory() {
        super("proxy");
        this._next = null;
    }

    public ProxyConnectionFactory(String nextProtocol) {
        super("proxy");
        this._next = nextProtocol;
    }

    public int getMaxProxyHeader() {
        return this._maxProxyHeader;
    }

    public void setMaxProxyHeader(int maxProxyHeader) {
        this._maxProxyHeader = maxProxyHeader;
    }

    @Override
    public Connection newConnection(Connector connector, EndPoint endp) {
        String next = this._next;
        if (next == null) {
            Iterator<String> i = connector.getProtocols().iterator();
            while (i.hasNext()) {
                String p = i.next();
                if (!this.getProtocol().equalsIgnoreCase(p)) continue;
                next = i.next();
                break;
            }
        }
        return new ProxyProtocolV1orV2Connection(endp, connector, next);
    }

    public static class ProxyEndPoint
    extends AttributesMap
    implements EndPoint {
        private final EndPoint _endp;
        private final InetSocketAddress _remote;
        private final InetSocketAddress _local;

        public ProxyEndPoint(EndPoint endp, InetSocketAddress remote, InetSocketAddress local) {
            this._endp = endp;
            this._remote = remote;
            this._local = local;
        }

        @Override
        public boolean isOptimizedForDirectBuffers() {
            return this._endp.isOptimizedForDirectBuffers();
        }

        @Override
        public InetSocketAddress getLocalAddress() {
            return this._local;
        }

        @Override
        public InetSocketAddress getRemoteAddress() {
            return this._remote;
        }

        @Override
        public boolean isOpen() {
            return this._endp.isOpen();
        }

        @Override
        public long getCreatedTimeStamp() {
            return this._endp.getCreatedTimeStamp();
        }

        @Override
        public void shutdownOutput() {
            this._endp.shutdownOutput();
        }

        @Override
        public boolean isOutputShutdown() {
            return this._endp.isOutputShutdown();
        }

        @Override
        public boolean isInputShutdown() {
            return this._endp.isInputShutdown();
        }

        @Override
        public void close() {
            this._endp.close();
        }

        @Override
        public int fill(ByteBuffer buffer) throws IOException {
            return this._endp.fill(buffer);
        }

        @Override
        public boolean flush(ByteBuffer ... buffer) throws IOException {
            return this._endp.flush(buffer);
        }

        @Override
        public Object getTransport() {
            return this._endp.getTransport();
        }

        @Override
        public long getIdleTimeout() {
            return this._endp.getIdleTimeout();
        }

        @Override
        public void setIdleTimeout(long idleTimeout) {
            this._endp.setIdleTimeout(idleTimeout);
        }

        @Override
        public void fillInterested(Callback callback) throws ReadPendingException {
            this._endp.fillInterested(callback);
        }

        @Override
        public boolean tryFillInterested(Callback callback) {
            return this._endp.tryFillInterested(callback);
        }

        @Override
        public boolean isFillInterested() {
            return this._endp.isFillInterested();
        }

        @Override
        public void write(Callback callback, ByteBuffer ... buffers) throws WritePendingException {
            this._endp.write(callback, buffers);
        }

        @Override
        public Connection getConnection() {
            return this._endp.getConnection();
        }

        @Override
        public void setConnection(Connection connection) {
            this._endp.setConnection(connection);
        }

        @Override
        public void onOpen() {
            this._endp.onOpen();
        }

        @Override
        public void onClose() {
            this._endp.onClose();
        }

        @Override
        public void upgrade(Connection newConnection) {
            this._endp.upgrade(newConnection);
        }
    }

    public class ProxyProtocolV2Connection
    extends AbstractConnection {
        private final Connector _connector;
        private final String _next;
        private final boolean _local;
        private final Family _family;
        private final Transport _transport;
        private final int _length;
        private final ByteBuffer _buffer;

        protected ProxyProtocolV2Connection(EndPoint endp, Connector connector, String next, ByteBuffer buffer) throws IOException {
            super(endp, connector.getExecutor());
            this._connector = connector;
            this._next = next;
            if (buffer.remaining() != 16) {
                throw new IllegalStateException();
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("PROXYv2 header {} for {}", BufferUtil.toHexSummary(buffer), this);
            }
            for (int i = 0; i < MAGIC.length; ++i) {
                if (buffer.get() == MAGIC[i]) continue;
                throw new IOException("Bad PROXY protocol v2 signature");
            }
            int versionAndCommand = 0xFF & buffer.get();
            if ((versionAndCommand & 0xF0) != 32) {
                throw new IOException("Bad PROXY protocol v2 version");
            }
            this._local = (versionAndCommand & 0xF) == 0;
            int transportAndFamily = 0xFF & buffer.get();
            switch (transportAndFamily >> 4) {
                case 0: {
                    this._family = Family.UNSPEC;
                    break;
                }
                case 1: {
                    this._family = Family.INET;
                    break;
                }
                case 2: {
                    this._family = Family.INET6;
                    break;
                }
                case 3: {
                    this._family = Family.UNIX;
                    break;
                }
                default: {
                    throw new IOException("Bad PROXY protocol v2 family");
                }
            }
            switch (0xF & transportAndFamily) {
                case 0: {
                    this._transport = Transport.UNSPEC;
                    break;
                }
                case 1: {
                    this._transport = Transport.STREAM;
                    break;
                }
                case 2: {
                    this._transport = Transport.DGRAM;
                    break;
                }
                default: {
                    throw new IOException("Bad PROXY protocol v2 family");
                }
            }
            this._length = buffer.getChar();
            if (!(this._local || this._family != Family.UNSPEC && this._family != Family.UNIX && this._transport == Transport.STREAM)) {
                throw new IOException(String.format("Unsupported PROXY protocol v2 mode 0x%x,0x%x", versionAndCommand, transportAndFamily));
            }
            if (this._length > ProxyConnectionFactory.this._maxProxyHeader) {
                throw new IOException(String.format("Unsupported PROXY protocol v2 mode 0x%x,0x%x,0x%x", versionAndCommand, transportAndFamily, this._length));
            }
            this._buffer = this._length > 0 ? BufferUtil.allocate(this._length) : BufferUtil.EMPTY_BUFFER;
        }

        @Override
        public void onOpen() {
            super.onOpen();
            if (this._buffer.remaining() == this._length) {
                this.next();
            } else {
                this.fillInterested();
            }
        }

        @Override
        public void onFillable() {
            try {
                while (this._buffer.remaining() < this._length) {
                    int fill = this.getEndPoint().fill(this._buffer);
                    if (fill < 0) {
                        this.getEndPoint().shutdownOutput();
                        return;
                    }
                    if (fill != 0) continue;
                    this.fillInterested();
                    return;
                }
            }
            catch (Throwable x) {
                LOG.warn("PROXY error for " + this.getEndPoint(), x);
                this.close();
                return;
            }
            this.next();
        }

        private void next() {
            ConnectionFactory connectionFactory;
            if (LOG.isDebugEnabled()) {
                LOG.debug("PROXYv2 next {} from {} for {}", this._next, BufferUtil.toHexSummary(this._buffer), this);
            }
            if ((connectionFactory = this._connector.getConnectionFactory(this._next)) == null) {
                LOG.info("Next protocol '{}' for {}", this._next, this.getEndPoint());
                this.close();
                return;
            }
            EndPoint endPoint = this.getEndPoint();
            if (!this._local) {
                try {
                    char dp;
                    char sp;
                    InetAddress dst;
                    InetAddress src;
                    switch (this._family) {
                        case INET: {
                            byte[] addr = new byte[4];
                            this._buffer.get(addr);
                            src = Inet4Address.getByAddress(addr);
                            this._buffer.get(addr);
                            dst = Inet4Address.getByAddress(addr);
                            sp = this._buffer.getChar();
                            dp = this._buffer.getChar();
                            break;
                        }
                        case INET6: {
                            byte[] addr = new byte[16];
                            this._buffer.get(addr);
                            src = Inet6Address.getByAddress(addr);
                            this._buffer.get(addr);
                            dst = Inet6Address.getByAddress(addr);
                            sp = this._buffer.getChar();
                            dp = this._buffer.getChar();
                            break;
                        }
                        default: {
                            throw new IllegalStateException();
                        }
                    }
                    InetSocketAddress remote = new InetSocketAddress(src, (int)sp);
                    InetSocketAddress local = new InetSocketAddress(dst, (int)dp);
                    ProxyEndPoint proxyEndPoint = new ProxyEndPoint(endPoint, remote, local);
                    endPoint = proxyEndPoint;
                    while (this._buffer.hasRemaining()) {
                        int type = 0xFF & this._buffer.get();
                        short length = this._buffer.getShort();
                        byte[] value = new byte[length];
                        this._buffer.get(value);
                        if (LOG.isDebugEnabled()) {
                            LOG.debug(String.format("T=%x L=%d V=%s for %s", type, (int)length, TypeUtil.toHexString(value), this), new Object[0]);
                        }
                        switch (type) {
                            case 1: {
                                break;
                            }
                            case 2: {
                                break;
                            }
                            case 32: {
                                int i = 0;
                                int client = 0xFF & value[i++];
                                int verify = (0xFF & value[i++]) << 24 + (0xFF & value[i++]) << 16 + (0xFF & value[i++]) << 8 + (0xFF & value[i++]);
                                while (i < value.length) {
                                    int ssl_type = 0xFF & value[i++];
                                    int ssl_length = (0xFF & value[i++]) * 256 + (0xFF & value[i++]);
                                    byte[] ssl_val = new byte[ssl_length];
                                    System.arraycopy(value, i, ssl_val, 0, ssl_length);
                                    i += ssl_length;
                                    switch (ssl_type) {
                                        case 33: {
                                            String version = new String(ssl_val, 0, ssl_length, StandardCharsets.ISO_8859_1);
                                            if (client != 1) break;
                                            proxyEndPoint.setAttribute(ProxyConnectionFactory.TLS_VERSION, version);
                                            break;
                                        }
                                    }
                                }
                                break;
                            }
                            case 33: {
                                break;
                            }
                            case 34: {
                                break;
                            }
                            case 48: {
                                break;
                            }
                        }
                    }
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("{} {}", this.getEndPoint(), proxyEndPoint.toString());
                    }
                }
                catch (Exception e) {
                    LOG.warn(e);
                }
            }
            Connection newConnection = connectionFactory.newConnection(this._connector, endPoint);
            endPoint.upgrade(newConnection);
        }
    }

    static enum Transport {
        UNSPEC,
        STREAM,
        DGRAM;

    }

    static enum Family {
        UNSPEC,
        INET,
        INET6,
        UNIX;

    }

    public static class ProxyProtocolV1Connection
    extends AbstractConnection {
        private final int[] __size = new int[]{29, 23, 21, 13, 5, 3, 1};
        private final Connector _connector;
        private final String _next;
        private final StringBuilder _builder = new StringBuilder();
        private final String[] _field = new String[6];
        private int _fields;
        private int _length;

        protected ProxyProtocolV1Connection(EndPoint endp, Connector connector, String next, ByteBuffer buffer) {
            super(endp, connector.getExecutor());
            this._connector = connector;
            this._next = next;
            this._length = buffer.remaining();
            this.parse(buffer);
        }

        @Override
        public void onOpen() {
            super.onOpen();
            this.fillInterested();
        }

        private boolean parse(ByteBuffer buffer) {
            while (buffer.hasRemaining()) {
                byte b = buffer.get();
                if (this._fields < 6) {
                    if (b == 32 || b == 13 && this._fields == 5) {
                        this._field[this._fields++] = this._builder.toString();
                        this._builder.setLength(0);
                        continue;
                    }
                    if (b < 32) {
                        LOG.warn("Bad character {} for {}", b & 0xFF, this.getEndPoint());
                        this.close();
                        return false;
                    }
                    this._builder.append((char)b);
                    continue;
                }
                if (b == 10) {
                    this._fields = 7;
                    return true;
                }
                LOG.warn("Bad CRLF for {}", this.getEndPoint());
                this.close();
                return false;
            }
            return true;
        }

        @Override
        public void onFillable() {
            try {
                Buffer buffer = null;
                while (this._fields < 7) {
                    int size = Math.max(1, this.__size[this._fields] - this._builder.length());
                    if (buffer == null || buffer.capacity() != size) {
                        buffer = BufferUtil.allocate(size);
                    } else {
                        BufferUtil.clear((ByteBuffer)buffer);
                    }
                    int fill = this.getEndPoint().fill((ByteBuffer)buffer);
                    if (fill < 0) {
                        this.getEndPoint().shutdownOutput();
                        return;
                    }
                    if (fill == 0) {
                        this.fillInterested();
                        return;
                    }
                    this._length += fill;
                    if (this._length >= 108) {
                        LOG.warn("PROXY line too long {} for {}", this._length, this.getEndPoint());
                        this.close();
                        return;
                    }
                    if (this.parse((ByteBuffer)buffer)) continue;
                    return;
                }
                if (!"PROXY".equals(this._field[0])) {
                    LOG.warn("Not PROXY protocol for {}", this.getEndPoint());
                    this.close();
                    return;
                }
                InetSocketAddress remote = new InetSocketAddress(this._field[2], Integer.parseInt(this._field[4]));
                InetSocketAddress local = new InetSocketAddress(this._field[3], Integer.parseInt(this._field[5]));
                ConnectionFactory connectionFactory = this._connector.getConnectionFactory(this._next);
                if (connectionFactory == null) {
                    LOG.warn("No Next protocol '{}' for {}", this._next, this.getEndPoint());
                    this.close();
                    return;
                }
                if (LOG.isDebugEnabled()) {
                    LOG.warn("Next protocol '{}' for {} r={} l={}", this._next, this.getEndPoint(), remote, local);
                }
                ProxyEndPoint endPoint = new ProxyEndPoint(this.getEndPoint(), remote, local);
                Connection newConnection = connectionFactory.newConnection(this._connector, endPoint);
                endPoint.upgrade(newConnection);
            }
            catch (Throwable x) {
                LOG.warn("PROXY error for " + this.getEndPoint(), x);
                this.close();
            }
        }
    }

    public class ProxyProtocolV1orV2Connection
    extends AbstractConnection {
        private final Connector _connector;
        private final String _next;
        private ByteBuffer _buffer;

        protected ProxyProtocolV1orV2Connection(EndPoint endp, Connector connector, String next) {
            super(endp, connector.getExecutor());
            this._buffer = BufferUtil.allocate(16);
            this._connector = connector;
            this._next = next;
        }

        @Override
        public void onOpen() {
            super.onOpen();
            this.fillInterested();
        }

        @Override
        public void onFillable() {
            try {
                while (BufferUtil.space(this._buffer) > 0) {
                    int fill = this.getEndPoint().fill(this._buffer);
                    if (fill < 0) {
                        this.getEndPoint().shutdownOutput();
                        return;
                    }
                    if (fill != 0) continue;
                    this.fillInterested();
                    return;
                }
                switch (this._buffer.get(0)) {
                    case 80: {
                        ProxyProtocolV1Connection v1 = new ProxyProtocolV1Connection(this.getEndPoint(), this._connector, this._next, this._buffer);
                        this.getEndPoint().upgrade(v1);
                        return;
                    }
                    case 13: {
                        ProxyProtocolV2Connection v2 = new ProxyProtocolV2Connection(this.getEndPoint(), this._connector, this._next, this._buffer);
                        this.getEndPoint().upgrade(v2);
                        return;
                    }
                }
                LOG.warn("Not PROXY protocol for {}", this.getEndPoint());
                this.close();
            }
            catch (Throwable x) {
                LOG.warn("PROXY error for " + this.getEndPoint(), x);
                this.close();
            }
        }
    }
}

