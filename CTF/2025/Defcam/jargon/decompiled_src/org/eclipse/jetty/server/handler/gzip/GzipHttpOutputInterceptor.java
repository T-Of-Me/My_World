/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.handler.gzip;

import java.nio.ByteBuffer;
import java.nio.channels.WritePendingException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import org.eclipse.jetty.http.CompressedContentFormat;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.http.PreEncodedHttpField;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.gzip.GzipFactory;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.IteratingCallback;
import org.eclipse.jetty.util.IteratingNestedCallback;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class GzipHttpOutputInterceptor
implements HttpOutput.Interceptor {
    public static Logger LOG = Log.getLogger(GzipHttpOutputInterceptor.class);
    private static final byte[] GZIP_HEADER = new byte[]{31, -117, 8, 0, 0, 0, 0, 0, 0, 0};
    public static final HttpField VARY_ACCEPT_ENCODING_USER_AGENT = new PreEncodedHttpField(HttpHeader.VARY, (Object)((Object)HttpHeader.ACCEPT_ENCODING) + ", " + (Object)((Object)HttpHeader.USER_AGENT));
    public static final HttpField VARY_ACCEPT_ENCODING = new PreEncodedHttpField(HttpHeader.VARY, HttpHeader.ACCEPT_ENCODING.asString());
    private final AtomicReference<GZState> _state = new AtomicReference<GZState>(GZState.MIGHT_COMPRESS);
    private final CRC32 _crc = new CRC32();
    private final GzipFactory _factory;
    private final HttpOutput.Interceptor _interceptor;
    private final HttpChannel _channel;
    private final HttpField _vary;
    private final int _bufferSize;
    private final boolean _syncFlush;
    private Deflater _deflater;
    private ByteBuffer _buffer;

    public GzipHttpOutputInterceptor(GzipFactory factory, HttpChannel channel, HttpOutput.Interceptor next, boolean syncFlush) {
        this(factory, VARY_ACCEPT_ENCODING_USER_AGENT, channel.getHttpConfiguration().getOutputBufferSize(), channel, next, syncFlush);
    }

    public GzipHttpOutputInterceptor(GzipFactory factory, HttpField vary, HttpChannel channel, HttpOutput.Interceptor next, boolean syncFlush) {
        this(factory, vary, channel.getHttpConfiguration().getOutputBufferSize(), channel, next, syncFlush);
    }

    public GzipHttpOutputInterceptor(GzipFactory factory, HttpField vary, int bufferSize, HttpChannel channel, HttpOutput.Interceptor next, boolean syncFlush) {
        this._factory = factory;
        this._channel = channel;
        this._interceptor = next;
        this._vary = vary;
        this._bufferSize = bufferSize;
        this._syncFlush = syncFlush;
    }

    @Override
    public HttpOutput.Interceptor getNextInterceptor() {
        return this._interceptor;
    }

    @Override
    public boolean isOptimizedForDirectBuffers() {
        return false;
    }

    @Override
    public void write(ByteBuffer content, boolean complete, Callback callback) {
        switch (this._state.get()) {
            case MIGHT_COMPRESS: {
                this.commit(content, complete, callback);
                break;
            }
            case NOT_COMPRESSING: {
                this._interceptor.write(content, complete, callback);
                return;
            }
            case COMMITTING: {
                callback.failed(new WritePendingException());
                break;
            }
            case COMPRESSING: {
                this.gzip(content, complete, callback);
                break;
            }
            default: {
                callback.failed(new IllegalStateException("state=" + (Object)((Object)this._state.get())));
            }
        }
    }

    private void addTrailer() {
        int i = this._buffer.limit();
        this._buffer.limit(i + 8);
        int v = (int)this._crc.getValue();
        this._buffer.put(i++, (byte)(v & 0xFF));
        this._buffer.put(i++, (byte)(v >>> 8 & 0xFF));
        this._buffer.put(i++, (byte)(v >>> 16 & 0xFF));
        this._buffer.put(i++, (byte)(v >>> 24 & 0xFF));
        v = this._deflater.getTotalIn();
        this._buffer.put(i++, (byte)(v & 0xFF));
        this._buffer.put(i++, (byte)(v >>> 8 & 0xFF));
        this._buffer.put(i++, (byte)(v >>> 16 & 0xFF));
        this._buffer.put(i++, (byte)(v >>> 24 & 0xFF));
    }

    private void gzip(ByteBuffer content, boolean complete, Callback callback) {
        if (content.hasRemaining() || complete) {
            new GzipBufferCB(content, complete, callback).iterate();
        } else {
            callback.succeeded();
        }
    }

    protected void commit(ByteBuffer content, boolean complete, Callback callback) {
        Response response = this._channel.getResponse();
        int sc = response.getStatus();
        if (sc > 0 && (sc < 200 || sc == 204 || sc == 205 || sc >= 300)) {
            LOG.debug("{} exclude by status {}", this, sc);
            this.noCompression();
            if (sc == 304) {
                String response_etag_gzip;
                String request_etags = (String)this._channel.getRequest().getAttribute("o.e.j.s.h.gzip.GzipHandler.etag");
                String response_etag = response.getHttpFields().get(HttpHeader.ETAG);
                if (request_etags != null && response_etag != null && request_etags.contains(response_etag_gzip = this.etagGzip(response_etag))) {
                    response.getHttpFields().put(HttpHeader.ETAG, response_etag_gzip);
                }
            }
            this._interceptor.write(content, complete, callback);
            return;
        }
        String ct = response.getContentType();
        if (ct != null && !this._factory.isMimeTypeGzipable(StringUtil.asciiToLowerCase(ct = MimeTypes.getContentTypeWithoutCharset(ct)))) {
            LOG.debug("{} exclude by mimeType {}", this, ct);
            this.noCompression();
            this._interceptor.write(content, complete, callback);
            return;
        }
        HttpFields fields = response.getHttpFields();
        String ce = fields.get(HttpHeader.CONTENT_ENCODING);
        if (ce != null) {
            LOG.debug("{} exclude by content-encoding {}", this, ce);
            this.noCompression();
            this._interceptor.write(content, complete, callback);
            return;
        }
        if (this._state.compareAndSet(GZState.MIGHT_COMPRESS, GZState.COMMITTING)) {
            long content_length;
            if (this._vary != null) {
                if (fields.contains(HttpHeader.VARY)) {
                    fields.addCSV(HttpHeader.VARY, this._vary.getValues());
                } else {
                    fields.add(this._vary);
                }
            }
            if ((content_length = response.getContentLength()) < 0L && complete) {
                content_length = content.remaining();
            }
            this._deflater = this._factory.getDeflater(this._channel.getRequest(), content_length);
            if (this._deflater == null) {
                LOG.debug("{} exclude no deflater", this);
                this._state.set(GZState.NOT_COMPRESSING);
                this._interceptor.write(content, complete, callback);
                return;
            }
            fields.put(CompressedContentFormat.GZIP._contentEncoding);
            this._crc.reset();
            this._buffer = this._channel.getByteBufferPool().acquire(this._bufferSize, false);
            BufferUtil.fill(this._buffer, GZIP_HEADER, 0, GZIP_HEADER.length);
            response.setContentLength(-1);
            String etag = fields.get(HttpHeader.ETAG);
            if (etag != null) {
                fields.put(HttpHeader.ETAG, this.etagGzip(etag));
            }
            LOG.debug("{} compressing {}", this, this._deflater);
            this._state.set(GZState.COMPRESSING);
            this.gzip(content, complete, callback);
        } else {
            callback.failed(new WritePendingException());
        }
    }

    private String etagGzip(String etag) {
        int end = etag.length() - 1;
        return etag.charAt(end) == '\"' ? etag.substring(0, end) + CompressedContentFormat.GZIP._etag + '\"' : etag + CompressedContentFormat.GZIP._etag;
    }

    public void noCompression() {
        block4: while (true) {
            switch (this._state.get()) {
                case NOT_COMPRESSING: {
                    return;
                }
                case MIGHT_COMPRESS: {
                    if (!this._state.compareAndSet(GZState.MIGHT_COMPRESS, GZState.NOT_COMPRESSING)) continue block4;
                    return;
                }
            }
            break;
        }
        throw new IllegalStateException(this._state.get().toString());
    }

    public void noCompressionIfPossible() {
        block4: while (true) {
            switch (this._state.get()) {
                case NOT_COMPRESSING: 
                case COMPRESSING: {
                    return;
                }
                case MIGHT_COMPRESS: {
                    if (!this._state.compareAndSet(GZState.MIGHT_COMPRESS, GZState.NOT_COMPRESSING)) continue block4;
                    return;
                }
            }
            break;
        }
        throw new IllegalStateException(this._state.get().toString());
    }

    public boolean mightCompress() {
        return this._state.get() == GZState.MIGHT_COMPRESS;
    }

    private class GzipBufferCB
    extends IteratingNestedCallback {
        private ByteBuffer _copy;
        private final ByteBuffer _content;
        private final boolean _last;

        public GzipBufferCB(ByteBuffer content, boolean complete, Callback callback) {
            super(callback);
            this._content = content;
            this._last = complete;
        }

        @Override
        protected IteratingCallback.Action process() throws Exception {
            boolean finished;
            if (GzipHttpOutputInterceptor.this._deflater == null) {
                return IteratingCallback.Action.SUCCEEDED;
            }
            if (GzipHttpOutputInterceptor.this._deflater.needsInput()) {
                if (BufferUtil.isEmpty(this._content)) {
                    if (GzipHttpOutputInterceptor.this._deflater.finished()) {
                        GzipHttpOutputInterceptor.this._factory.recycle(GzipHttpOutputInterceptor.this._deflater);
                        GzipHttpOutputInterceptor.this._deflater = null;
                        GzipHttpOutputInterceptor.this._channel.getByteBufferPool().release(GzipHttpOutputInterceptor.this._buffer);
                        GzipHttpOutputInterceptor.this._buffer = null;
                        if (this._copy != null) {
                            GzipHttpOutputInterceptor.this._channel.getByteBufferPool().release(this._copy);
                            this._copy = null;
                        }
                        return IteratingCallback.Action.SUCCEEDED;
                    }
                    if (!this._last) {
                        return IteratingCallback.Action.SUCCEEDED;
                    }
                    GzipHttpOutputInterceptor.this._deflater.finish();
                } else if (this._content.hasArray()) {
                    byte[] array = this._content.array();
                    int off = this._content.arrayOffset() + this._content.position();
                    int len = this._content.remaining();
                    BufferUtil.clear(this._content);
                    GzipHttpOutputInterceptor.this._crc.update(array, off, len);
                    GzipHttpOutputInterceptor.this._deflater.setInput(array, off, len);
                    if (this._last) {
                        GzipHttpOutputInterceptor.this._deflater.finish();
                    }
                } else {
                    if (this._copy == null) {
                        this._copy = GzipHttpOutputInterceptor.this._channel.getByteBufferPool().acquire(GzipHttpOutputInterceptor.this._bufferSize, false);
                    }
                    BufferUtil.clearToFill(this._copy);
                    int took = BufferUtil.put(this._content, this._copy);
                    BufferUtil.flipToFlush(this._copy, 0);
                    if (took == 0) {
                        throw new IllegalStateException();
                    }
                    byte[] array = this._copy.array();
                    int off = this._copy.arrayOffset() + this._copy.position();
                    int len = this._copy.remaining();
                    GzipHttpOutputInterceptor.this._crc.update(array, off, len);
                    GzipHttpOutputInterceptor.this._deflater.setInput(array, off, len);
                    if (this._last && BufferUtil.isEmpty(this._content)) {
                        GzipHttpOutputInterceptor.this._deflater.finish();
                    }
                }
            }
            BufferUtil.compact(GzipHttpOutputInterceptor.this._buffer);
            int off = GzipHttpOutputInterceptor.this._buffer.arrayOffset() + GzipHttpOutputInterceptor.this._buffer.limit();
            int len = GzipHttpOutputInterceptor.this._buffer.capacity() - GzipHttpOutputInterceptor.this._buffer.limit() - (this._last ? 8 : 0);
            if (len > 0) {
                int produced = GzipHttpOutputInterceptor.this._deflater.deflate(GzipHttpOutputInterceptor.this._buffer.array(), off, len, GzipHttpOutputInterceptor.this._syncFlush ? 2 : 0);
                GzipHttpOutputInterceptor.this._buffer.limit(GzipHttpOutputInterceptor.this._buffer.limit() + produced);
            }
            if (finished = GzipHttpOutputInterceptor.this._deflater.finished()) {
                GzipHttpOutputInterceptor.this.addTrailer();
            }
            GzipHttpOutputInterceptor.this._interceptor.write(GzipHttpOutputInterceptor.this._buffer, finished, this);
            return IteratingCallback.Action.SCHEDULED;
        }
    }

    private static enum GZState {
        MIGHT_COMPRESS,
        NOT_COMPRESSING,
        COMMITTING,
        COMPRESSING,
        FINISHED;

    }
}

