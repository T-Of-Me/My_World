/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import javax.servlet.DispatcherType;
import org.eclipse.jetty.http.BadMessageException;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpGenerator;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.ChannelEndPoint;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.QuietException;
import org.eclipse.jetty.io.RuntimeIOException;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpChannelState;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpInput;
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.HttpTransport;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.RequestLogCollection;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.SharedBlockingCallback;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.thread.Scheduler;

public class HttpChannel
implements Runnable,
HttpOutput.Interceptor {
    private static final Logger LOG = Log.getLogger(HttpChannel.class);
    private final AtomicBoolean _committed = new AtomicBoolean();
    private final AtomicLong _requests = new AtomicLong();
    private final Connector _connector;
    private final Executor _executor;
    private final HttpConfiguration _configuration;
    private final EndPoint _endPoint;
    private final HttpTransport _transport;
    private final HttpChannelState _state;
    private final Request _request;
    private final Response _response;
    private MetaData.Response _committedMetaData;
    private RequestLog _requestLog;
    private long _oldIdleTimeout;
    private long _written;

    public HttpChannel(Connector connector, HttpConfiguration configuration, EndPoint endPoint, HttpTransport transport) {
        this._connector = connector;
        this._configuration = configuration;
        this._endPoint = endPoint;
        this._transport = transport;
        this._state = new HttpChannelState(this);
        this._request = new Request(this, this.newHttpInput(this._state));
        this._response = new Response(this, this.newHttpOutput());
        this._executor = connector == null ? null : connector.getServer().getThreadPool();
        RequestLog requestLog = this._requestLog = connector == null ? null : connector.getServer().getRequestLog();
        if (LOG.isDebugEnabled()) {
            LOG.debug("new {} -> {},{},{}", this, this._endPoint, this._endPoint.getConnection(), this._state);
        }
    }

    protected HttpInput newHttpInput(HttpChannelState state) {
        return new HttpInput(state);
    }

    protected HttpOutput newHttpOutput() {
        return new HttpOutput(this);
    }

    public HttpChannelState getState() {
        return this._state;
    }

    public long getBytesWritten() {
        return this._written;
    }

    public long getRequests() {
        return this._requests.get();
    }

    public Connector getConnector() {
        return this._connector;
    }

    public HttpTransport getHttpTransport() {
        return this._transport;
    }

    public RequestLog getRequestLog() {
        return this._requestLog;
    }

    public void setRequestLog(RequestLog requestLog) {
        this._requestLog = requestLog;
    }

    public void addRequestLog(RequestLog requestLog) {
        if (this._requestLog == null) {
            this._requestLog = requestLog;
        } else if (this._requestLog instanceof RequestLogCollection) {
            ((RequestLogCollection)this._requestLog).add(requestLog);
        } else {
            this._requestLog = new RequestLogCollection(this._requestLog, requestLog);
        }
    }

    public MetaData.Response getCommittedMetaData() {
        return this._committedMetaData;
    }

    public long getIdleTimeout() {
        return this._endPoint.getIdleTimeout();
    }

    public void setIdleTimeout(long timeoutMs) {
        this._endPoint.setIdleTimeout(timeoutMs);
    }

    public ByteBufferPool getByteBufferPool() {
        return this._connector.getByteBufferPool();
    }

    public HttpConfiguration getHttpConfiguration() {
        return this._configuration;
    }

    @Override
    public boolean isOptimizedForDirectBuffers() {
        return this.getHttpTransport().isOptimizedForDirectBuffers();
    }

    public Server getServer() {
        return this._connector.getServer();
    }

    public Request getRequest() {
        return this._request;
    }

    public Response getResponse() {
        return this._response;
    }

    public EndPoint getEndPoint() {
        return this._endPoint;
    }

    public InetSocketAddress getLocalAddress() {
        return this._endPoint.getLocalAddress();
    }

    public InetSocketAddress getRemoteAddress() {
        return this._endPoint.getRemoteAddress();
    }

    public void continue100(int available) throws IOException {
        throw new UnsupportedOperationException();
    }

    public void recycle() {
        this._committed.set(false);
        this._request.recycle();
        this._response.recycle();
        this._committedMetaData = null;
        this._requestLog = this._connector == null ? null : this._connector.getServer().getRequestLog();
        this._written = 0L;
    }

    public void asyncReadFillInterested() {
    }

    @Override
    public void run() {
        this.handle();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean handle() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("{} handle {} ", this, this._request.getHttpURI());
        }
        HttpChannelState.Action action = this._state.handling();
        block24: while (!this.getServer().isStopped()) {
            try {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("{} action {}", new Object[]{this, action});
                }
                switch (action) {
                    case TERMINATED: 
                    case WAIT: {
                        break block24;
                    }
                    case DISPATCH: {
                        if (!this._request.hasMetaData()) {
                            throw new IllegalStateException("state=" + this._state);
                        }
                        this._request.setHandled(false);
                        this._response.getHttpOutput().reopen();
                        try {
                            this._request.setDispatcherType(DispatcherType.REQUEST);
                            List<HttpConfiguration.Customizer> customizers = this._configuration.getCustomizers();
                            if (!customizers.isEmpty()) {
                                for (HttpConfiguration.Customizer customizer : customizers) {
                                    customizer.customize(this.getConnector(), this._configuration, this._request);
                                    if (!this._request.isHandled()) continue;
                                    break;
                                }
                            }
                            if (!this._request.isHandled()) {
                                this.getServer().handle(this);
                            }
                            break;
                        }
                        finally {
                            this._request.setDispatcherType(null);
                        }
                    }
                    case ASYNC_DISPATCH: {
                        this._request.setHandled(false);
                        this._response.getHttpOutput().reopen();
                        try {
                            this._request.setDispatcherType(DispatcherType.ASYNC);
                            this.getServer().handleAsync(this);
                            break;
                        }
                        finally {
                            this._request.setDispatcherType(null);
                        }
                    }
                    case ERROR_DISPATCH: {
                        try {
                            this._response.reset(true);
                            Integer icode = (Integer)this._request.getAttribute("javax.servlet.error.status_code");
                            int code = icode != null ? icode : 500;
                            this._response.setStatus(code);
                            this._request.setAttribute("javax.servlet.error.status_code", code);
                            this._request.setHandled(false);
                            this._response.getHttpOutput().reopen();
                            try {
                                this._request.setDispatcherType(DispatcherType.ERROR);
                                this.getServer().handle(this);
                                break;
                            }
                            finally {
                                this._request.setDispatcherType(null);
                            }
                        }
                        catch (Throwable x) {
                            Throwable failure;
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Could not perform ERROR dispatch, aborting", x);
                            }
                            if ((failure = (Throwable)this._request.getAttribute("javax.servlet.error.exception")) == null) {
                                this.minimalErrorResponse(x);
                                break;
                            }
                            failure.addSuppressed(x);
                            this.minimalErrorResponse(failure);
                            break;
                        }
                    }
                    case ASYNC_ERROR: {
                        throw this._state.getAsyncContextEvent().getThrowable();
                    }
                    case READ_PRODUCE: {
                        this._request.getHttpInput().produceContent();
                        break;
                    }
                    case READ_CALLBACK: {
                        ContextHandler handler = this._state.getContextHandler();
                        if (handler != null) {
                            handler.handle(this._request, this._request.getHttpInput());
                            break;
                        }
                        this._request.getHttpInput().run();
                        break;
                    }
                    case WRITE_CALLBACK: {
                        ContextHandler handler = this._state.getContextHandler();
                        if (handler != null) {
                            handler.handle(this._request, this._response.getHttpOutput());
                            break;
                        }
                        this._response.getHttpOutput().run();
                        break;
                    }
                    case COMPLETE: {
                        if (!this._response.isCommitted() && !this._request.isHandled()) {
                            this._response.sendError(404);
                        } else {
                            boolean hasContent;
                            int status = this._response.getStatus();
                            boolean bl = hasContent = !this._request.isHead() && (!HttpMethod.CONNECT.is(this._request.getMethod()) || status != 200) && !HttpStatus.isInformational(status) && status != 204 && status != 304;
                            if (hasContent && !this._response.isContentComplete(this._response.getHttpOutput().getWritten())) {
                                if (this.isCommitted()) {
                                    this._transport.abort(new IOException("insufficient content written"));
                                } else {
                                    this._response.sendError(500, "insufficient content written");
                                }
                            }
                        }
                        this._response.closeOutput();
                        this._request.setHandled(true);
                        this._state.onComplete();
                        this.onCompleted();
                        break block24;
                    }
                    default: {
                        throw new IllegalStateException("state=" + this._state);
                    }
                }
            }
            catch (Throwable failure) {
                if ("org.eclipse.jetty.continuation.ContinuationThrowable".equals(failure.getClass().getName())) {
                    LOG.ignore(failure);
                }
                this.handleException(failure);
            }
            action = this._state.unhandle();
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("{} handle exit, result {}", new Object[]{this, action});
        }
        boolean suspended = action == HttpChannelState.Action.WAIT;
        return !suspended;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    protected void sendError(int code, String reason) {
        try {
            this._response.sendError(code, reason);
        }
        catch (Throwable x) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Could not send error " + code + " " + reason, x);
            }
        }
        finally {
            this._state.errorComplete();
        }
    }

    protected void handleException(Throwable failure) {
        if (failure instanceof RuntimeIOException) {
            failure = failure.getCause();
        }
        if (failure instanceof QuietException || !this.getServer().isRunning()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(this._request.getRequestURI(), failure);
            }
        } else if (failure instanceof BadMessageException) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(this._request.getRequestURI(), failure);
            } else {
                LOG.warn("{} {}", this._request.getRequestURI(), failure);
            }
        } else {
            LOG.warn(this._request.getRequestURI(), failure);
        }
        try {
            this._state.onError(failure);
        }
        catch (Throwable e) {
            failure.addSuppressed(e);
            LOG.warn("ERROR dispatch failed", failure);
            this.minimalErrorResponse(failure);
        }
    }

    private void minimalErrorResponse(Throwable failure) {
        try {
            Integer code = (Integer)this._request.getAttribute("javax.servlet.error.status_code");
            this._response.reset(true);
            this._response.setStatus(code == null ? 500 : code);
            this._response.flushBuffer();
        }
        catch (Throwable x) {
            failure.addSuppressed(x);
            this._transport.abort(failure);
        }
    }

    public boolean isExpecting100Continue() {
        return false;
    }

    public boolean isExpecting102Processing() {
        return false;
    }

    public String toString() {
        return String.format("%s@%x{r=%s,c=%b,a=%s,uri=%s}", new Object[]{this.getClass().getSimpleName(), this.hashCode(), this._requests, this._committed.get(), this._state.getState(), this._request.getHttpURI()});
    }

    public void onRequest(MetaData.Request request) {
        this._requests.incrementAndGet();
        this._request.setTimeStamp(System.currentTimeMillis());
        HttpFields fields = this._response.getHttpFields();
        if (this._configuration.getSendDateHeader() && !fields.contains(HttpHeader.DATE)) {
            fields.put(this._connector.getServer().getDateField());
        }
        long idleTO = this._configuration.getIdleTimeout();
        this._oldIdleTimeout = this.getIdleTimeout();
        if (idleTO >= 0L && this._oldIdleTimeout != idleTO) {
            this.setIdleTimeout(idleTO);
        }
        this._request.setMetaData(request);
        if (LOG.isDebugEnabled()) {
            LOG.debug("REQUEST for {} on {}{}{} {} {}{}{}", new Object[]{request.getURIString(), this, System.lineSeparator(), request.getMethod(), request.getURIString(), request.getHttpVersion(), System.lineSeparator(), request.getFields()});
        }
    }

    public boolean onContent(HttpInput.Content content) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("{} onContent {}", this, content);
        }
        return this._request.getHttpInput().addContent(content);
    }

    public boolean onContentComplete() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("{} onContentComplete", this);
        }
        return false;
    }

    public void onTrailers(HttpFields trailers) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("{} onTrailers {}", this, trailers);
        }
        this._request.setTrailers(trailers);
    }

    public boolean onRequestComplete() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("{} onRequestComplete", this);
        }
        return this._request.getHttpInput().eof();
    }

    public void onCompleted() {
        long idleTO;
        if (LOG.isDebugEnabled()) {
            LOG.debug("COMPLETE for {} written={}", this.getRequest().getRequestURI(), this.getBytesWritten());
        }
        if (this._requestLog != null) {
            this._requestLog.log(this._request, this._response);
        }
        if ((idleTO = this._configuration.getIdleTimeout()) >= 0L && this.getIdleTimeout() != this._oldIdleTimeout) {
            this.setIdleTimeout(this._oldIdleTimeout);
        }
        this._transport.onCompleted();
    }

    public boolean onEarlyEOF() {
        return this._request.getHttpInput().earlyEOF();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void onBadMessage(int status, String reason) {
        HttpChannelState.Action action;
        if (status < 400 || status > 599) {
            status = 400;
        }
        try {
            action = this._state.handling();
        }
        catch (IllegalStateException e) {
            this.abort(e);
            throw new BadMessageException(status, reason);
        }
        try {
            if (action == HttpChannelState.Action.DISPATCH) {
                ByteBuffer content = null;
                HttpFields fields = new HttpFields();
                ErrorHandler handler = this.getServer().getBean(ErrorHandler.class);
                if (handler != null) {
                    content = handler.badMessageError(status, reason, fields);
                }
                this.sendResponse(new MetaData.Response(HttpVersion.HTTP_1_1, status, reason, fields, BufferUtil.length(content)), content, true);
            }
        }
        catch (IOException e) {
            LOG.debug(e);
        }
        finally {
            if (this._state.unhandle() != HttpChannelState.Action.COMPLETE) {
                throw new IllegalStateException();
            }
            this._state.onComplete();
            this.onCompleted();
        }
    }

    protected boolean sendResponse(MetaData.Response info, ByteBuffer content, boolean complete, Callback callback) {
        boolean committing = this._committed.compareAndSet(false, true);
        if (LOG.isDebugEnabled()) {
            LOG.debug("sendResponse info={} content={} complete={} committing={} callback={}", info, BufferUtil.toDetailString(content), complete, committing, callback);
        }
        if (committing) {
            if (info == null) {
                info = this._response.newResponseMetaData();
            }
            this.commit(info);
            int status = info.getStatus();
            CommitCallback committed = status < 200 && status >= 100 ? new Commit100Callback(callback) : new CommitCallback(callback);
            this._transport.send(info, this._request.isHead(), content, complete, committed);
        } else if (info == null) {
            this._transport.send(null, this._request.isHead(), content, complete, callback);
        } else {
            callback.failed(new IllegalStateException("committed"));
        }
        return committing;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    protected boolean sendResponse(MetaData.Response info, ByteBuffer content, boolean complete) throws IOException {
        try (SharedBlockingCallback.Blocker blocker = this._response.getHttpOutput().acquireWriteBlockingCallback();){
            boolean committing = this.sendResponse(info, content, complete, blocker);
            blocker.block();
            boolean bl = committing;
            return bl;
        }
        catch (Throwable failure) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(failure);
            }
            this.abort(failure);
            throw failure;
        }
    }

    protected void commit(MetaData.Response info) {
        this._committedMetaData = info;
        if (LOG.isDebugEnabled()) {
            LOG.debug("COMMIT for {} on {}{}{} {} {}{}{}", new Object[]{this.getRequest().getRequestURI(), this, System.lineSeparator(), info.getStatus(), info.getReason(), info.getHttpVersion(), System.lineSeparator(), info.getFields()});
        }
    }

    public boolean isCommitted() {
        return this._committed.get();
    }

    @Override
    public void write(ByteBuffer content, boolean complete, Callback callback) {
        this._written += (long)BufferUtil.length(content);
        this.sendResponse(null, content, complete, callback);
    }

    @Override
    public void resetBuffer() {
        if (this.isCommitted()) {
            throw new IllegalStateException("Committed");
        }
    }

    @Override
    public HttpOutput.Interceptor getNextInterceptor() {
        return null;
    }

    protected void execute(Runnable task) {
        this._executor.execute(task);
    }

    public Scheduler getScheduler() {
        return this._connector.getScheduler();
    }

    public boolean useDirectBuffers() {
        return this.getEndPoint() instanceof ChannelEndPoint;
    }

    public void abort(Throwable failure) {
        this._transport.abort(failure);
    }

    private class Commit100Callback
    extends CommitCallback {
        private Commit100Callback(Callback callback) {
            super(callback);
        }

        @Override
        public void succeeded() {
            if (HttpChannel.this._committed.compareAndSet(true, false)) {
                super.succeeded();
            } else {
                super.failed(new IllegalStateException());
            }
        }
    }

    private class CommitCallback
    extends Callback.Nested {
        private CommitCallback(Callback callback) {
            super(callback);
        }

        @Override
        public void failed(final Throwable x) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Commit failed", x);
            }
            if (x instanceof BadMessageException) {
                HttpChannel.this._transport.send(HttpGenerator.RESPONSE_500_INFO, false, null, true, new Callback.Nested(this){

                    @Override
                    public void succeeded() {
                        super.failed(x);
                        HttpChannel.this._response.getHttpOutput().closed();
                    }

                    @Override
                    public void failed(Throwable th) {
                        HttpChannel.this._transport.abort(x);
                        super.failed(x);
                    }
                });
            } else {
                HttpChannel.this._transport.abort(x);
                super.failed(x);
            }
        }
    }
}

