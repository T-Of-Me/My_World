/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.handler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.http.pathmap.PathSpecSet;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.IncludeExclude;
import org.eclipse.jetty.util.IteratingCallback;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class BufferedResponseHandler
extends HandlerWrapper {
    static final Logger LOG = Log.getLogger(BufferedResponseHandler.class);
    private final IncludeExclude<String> _methods = new IncludeExclude();
    private final IncludeExclude<String> _paths = new IncludeExclude(PathSpecSet.class);
    private final IncludeExclude<String> _mimeTypes = new IncludeExclude();

    public BufferedResponseHandler() {
        this._methods.include(HttpMethod.GET.asString());
        for (String type : MimeTypes.getKnownMimeTypes()) {
            if (!type.startsWith("image/") && !type.startsWith("audio/") && !type.startsWith("video/")) continue;
            this._mimeTypes.exclude(type);
        }
        LOG.debug("{} mime types {}", this, this._mimeTypes);
    }

    public IncludeExclude<String> getMethodIncludeExclude() {
        return this._methods;
    }

    public IncludeExclude<String> getPathIncludeExclude() {
        return this._paths;
    }

    public IncludeExclude<String> getMimeIncludeExclude() {
        return this._mimeTypes;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String mimeType;
        ServletContext context = baseRequest.getServletContext();
        String path = context == null ? baseRequest.getRequestURI() : URIUtil.addPaths(baseRequest.getServletPath(), baseRequest.getPathInfo());
        LOG.debug("{} handle {} in {}", this, baseRequest, context);
        HttpOutput out = baseRequest.getResponse().getHttpOutput();
        for (HttpOutput.Interceptor interceptor = out.getInterceptor(); interceptor != null; interceptor = interceptor.getNextInterceptor()) {
            if (!(interceptor instanceof BufferedInterceptor)) continue;
            LOG.debug("{} already intercepting {}", this, request);
            this._handler.handle(target, baseRequest, request, response);
            return;
        }
        if (!this._methods.test(baseRequest.getMethod())) {
            LOG.debug("{} excluded by method {}", this, request);
            this._handler.handle(target, baseRequest, request, response);
            return;
        }
        if (!this.isPathBufferable(path)) {
            LOG.debug("{} excluded by path {}", this, request);
            this._handler.handle(target, baseRequest, request, response);
            return;
        }
        String string = mimeType = context == null ? MimeTypes.getDefaultMimeByExtension(path) : context.getMimeType(path);
        if (mimeType != null && !this.isMimeTypeBufferable(mimeType = MimeTypes.getContentTypeWithoutCharset(mimeType))) {
            LOG.debug("{} excluded by path suffix mime type {}", this, request);
            this._handler.handle(target, baseRequest, request, response);
            return;
        }
        out.setInterceptor(new BufferedInterceptor(baseRequest.getHttpChannel(), out.getInterceptor()));
        if (this._handler != null) {
            this._handler.handle(target, baseRequest, request, response);
        }
    }

    protected boolean isMimeTypeBufferable(String mimetype) {
        return this._mimeTypes.test(mimetype);
    }

    protected boolean isPathBufferable(String requestURI) {
        if (requestURI == null) {
            return true;
        }
        return this._paths.test(requestURI);
    }

    private class BufferedInterceptor
    implements HttpOutput.Interceptor {
        final HttpOutput.Interceptor _next;
        final HttpChannel _channel;
        final Queue<ByteBuffer> _buffers = new ConcurrentLinkedQueue<ByteBuffer>();
        Boolean _aggregating;
        ByteBuffer _aggregate;

        public BufferedInterceptor(HttpChannel httpChannel, HttpOutput.Interceptor interceptor) {
            this._next = interceptor;
            this._channel = httpChannel;
        }

        @Override
        public void resetBuffer() {
            this._buffers.clear();
            this._aggregating = null;
            this._aggregate = null;
        }

        @Override
        public void write(ByteBuffer content, boolean last, Callback callback) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("{} write last={} {}", this, last, BufferUtil.toDetailString(content));
            }
            if (this._aggregating == null) {
                Response response = this._channel.getResponse();
                int sc = response.getStatus();
                if (sc > 0 && (sc < 200 || sc == 204 || sc == 205 || sc >= 300)) {
                    this._aggregating = Boolean.FALSE;
                } else {
                    String ct = response.getContentType();
                    if (ct == null) {
                        this._aggregating = Boolean.TRUE;
                    } else {
                        ct = MimeTypes.getContentTypeWithoutCharset(ct);
                        this._aggregating = BufferedResponseHandler.this.isMimeTypeBufferable(StringUtil.asciiToLowerCase(ct));
                    }
                }
            }
            if (!this._aggregating.booleanValue()) {
                this.getNextInterceptor().write(content, last, callback);
                return;
            }
            if (last) {
                if (BufferUtil.length(content) > 0) {
                    this._buffers.add(content);
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug("{} committing {}", this, this._buffers.size());
                }
                this.commit(this._buffers, callback);
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("{} aggregating", this);
                }
                while (BufferUtil.hasContent(content)) {
                    if (BufferUtil.space(this._aggregate) == 0) {
                        int size = Math.max(this._channel.getHttpConfiguration().getOutputBufferSize(), BufferUtil.length(content));
                        this._aggregate = BufferUtil.allocate(size);
                        this._buffers.add(this._aggregate);
                    }
                    BufferUtil.append(this._aggregate, content);
                }
                callback.succeeded();
            }
        }

        @Override
        public HttpOutput.Interceptor getNextInterceptor() {
            return this._next;
        }

        @Override
        public boolean isOptimizedForDirectBuffers() {
            return false;
        }

        protected void commit(Queue<ByteBuffer> buffers, final Callback callback) {
            if (this._buffers.size() == 0) {
                this.getNextInterceptor().write(BufferUtil.EMPTY_BUFFER, true, callback);
            } else if (this._buffers.size() == 1) {
                this.getNextInterceptor().write(this._buffers.remove(), true, callback);
            } else {
                IteratingCallback icb = new IteratingCallback(){

                    @Override
                    protected IteratingCallback.Action process() throws Exception {
                        ByteBuffer buffer = BufferedInterceptor.this._buffers.poll();
                        if (buffer == null) {
                            return IteratingCallback.Action.SUCCEEDED;
                        }
                        BufferedInterceptor.this.getNextInterceptor().write(buffer, BufferedInterceptor.this._buffers.isEmpty(), this);
                        return IteratingCallback.Action.SCHEDULED;
                    }

                    @Override
                    protected void onCompleteSuccess() {
                        callback.succeeded();
                    }

                    @Override
                    protected void onCompleteFailure(Throwable cause) {
                        callback.failed(cause);
                    }
                };
                icb.iterate();
            }
        }
    }
}

