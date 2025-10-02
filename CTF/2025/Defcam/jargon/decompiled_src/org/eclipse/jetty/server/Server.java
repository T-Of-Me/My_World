/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.DateGenerator;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpGenerator;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.http.PreEncodedHttpField;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.AsyncContextEvent;
import org.eclipse.jetty.server.ClassLoaderDump;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpChannelState;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SessionIdManager;
import org.eclipse.jetty.server.ShutdownMonitor;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.util.Attributes;
import org.eclipse.jetty.util.AttributesMap;
import org.eclipse.jetty.util.Jetty;
import org.eclipse.jetty.util.MultiException;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.Uptime;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.annotation.Name;
import org.eclipse.jetty.util.component.Graceful;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.thread.Locker;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ShutdownThread;
import org.eclipse.jetty.util.thread.ThreadPool;

@ManagedObject(value="Jetty HTTP Servlet server")
public class Server
extends HandlerWrapper
implements Attributes {
    private static final Logger LOG = Log.getLogger(Server.class);
    private final AttributesMap _attributes = new AttributesMap();
    private final ThreadPool _threadPool;
    private final List<Connector> _connectors = new CopyOnWriteArrayList<Connector>();
    private SessionIdManager _sessionIdManager;
    private boolean _stopAtShutdown;
    private boolean _dumpAfterStart = false;
    private boolean _dumpBeforeStop = false;
    private ErrorHandler _errorHandler;
    private RequestLog _requestLog;
    private final Locker _dateLocker = new Locker();
    private volatile DateField _dateField;

    public Server() {
        this((ThreadPool)null);
    }

    public Server(@Name(value="port") int port) {
        this((ThreadPool)null);
        ServerConnector connector = new ServerConnector(this);
        connector.setPort(port);
        this.setConnectors(new Connector[]{connector});
    }

    public Server(@Name(value="address") InetSocketAddress addr) {
        this((ThreadPool)null);
        ServerConnector connector = new ServerConnector(this);
        connector.setHost(addr.getHostName());
        connector.setPort(addr.getPort());
        this.setConnectors(new Connector[]{connector});
    }

    public Server(@Name(value="threadpool") ThreadPool pool) {
        this._threadPool = pool != null ? pool : new QueuedThreadPool();
        this.addBean(this._threadPool);
        this.setServer(this);
    }

    public RequestLog getRequestLog() {
        return this._requestLog;
    }

    public ErrorHandler getErrorHandler() {
        return this._errorHandler;
    }

    public void setRequestLog(RequestLog requestLog) {
        this.updateBean(this._requestLog, requestLog);
        this._requestLog = requestLog;
    }

    public void setErrorHandler(ErrorHandler errorHandler) {
        if (errorHandler instanceof ErrorHandler.ErrorPageMapper) {
            throw new IllegalArgumentException("ErrorPageMapper is applicable only to ContextHandler");
        }
        this.updateBean(this._errorHandler, errorHandler);
        this._errorHandler = errorHandler;
        if (errorHandler != null) {
            errorHandler.setServer(this);
        }
    }

    @ManagedAttribute(value="version of this server")
    public static String getVersion() {
        return Jetty.VERSION;
    }

    public boolean getStopAtShutdown() {
        return this._stopAtShutdown;
    }

    @Override
    public void setStopTimeout(long stopTimeout) {
        super.setStopTimeout(stopTimeout);
    }

    public void setStopAtShutdown(boolean stop) {
        if (stop) {
            if (!this._stopAtShutdown && this.isStarted()) {
                ShutdownThread.register(this);
            }
        } else {
            ShutdownThread.deregister(this);
        }
        this._stopAtShutdown = stop;
    }

    @ManagedAttribute(value="connectors for this server", readonly=true)
    public Connector[] getConnectors() {
        ArrayList<Connector> connectors = new ArrayList<Connector>(this._connectors);
        return connectors.toArray(new Connector[connectors.size()]);
    }

    public void addConnector(Connector connector) {
        if (connector.getServer() != this) {
            throw new IllegalArgumentException("Connector " + connector + " cannot be shared among server " + connector.getServer() + " and server " + this);
        }
        if (this._connectors.add(connector)) {
            this.addBean(connector);
        }
    }

    public void removeConnector(Connector connector) {
        if (this._connectors.remove(connector)) {
            this.removeBean(connector);
        }
    }

    public void setConnectors(Connector[] connectors) {
        if (connectors != null) {
            for (Connector connector : connectors) {
                if (connector.getServer() == this) continue;
                throw new IllegalArgumentException("Connector " + connector + " cannot be shared among server " + connector.getServer() + " and server " + this);
            }
        }
        Object[] oldConnectors = this.getConnectors();
        this.updateBeans(oldConnectors, connectors);
        this._connectors.removeAll(Arrays.asList(oldConnectors));
        if (connectors != null) {
            this._connectors.addAll(Arrays.asList(connectors));
        }
    }

    @ManagedAttribute(value="the server thread pool")
    public ThreadPool getThreadPool() {
        return this._threadPool;
    }

    @ManagedAttribute(value="dump state to stderr after start")
    public boolean isDumpAfterStart() {
        return this._dumpAfterStart;
    }

    public void setDumpAfterStart(boolean dumpAfterStart) {
        this._dumpAfterStart = dumpAfterStart;
    }

    @ManagedAttribute(value="dump state to stderr before stop")
    public boolean isDumpBeforeStop() {
        return this._dumpBeforeStop;
    }

    public void setDumpBeforeStop(boolean dumpBeforeStop) {
        this._dumpBeforeStop = dumpBeforeStop;
    }

    public HttpField getDateField() {
        long now = System.currentTimeMillis();
        long seconds = now / 1000L;
        DateField df = this._dateField;
        if (df == null || df._seconds != seconds) {
            try (Locker.Lock lock = this._dateLocker.lock();){
                df = this._dateField;
                if (df == null || df._seconds != seconds) {
                    PreEncodedHttpField field = new PreEncodedHttpField(HttpHeader.DATE, DateGenerator.formatDate(now));
                    this._dateField = new DateField(seconds, field);
                    PreEncodedHttpField preEncodedHttpField = field;
                    return preEncodedHttpField;
                }
            }
        }
        return df._dateField;
    }

    @Override
    protected void doStart() throws Exception {
        if (this._errorHandler == null) {
            this._errorHandler = this.getBean(ErrorHandler.class);
        }
        if (this._errorHandler == null) {
            this.setErrorHandler(new ErrorHandler());
        }
        if (this._errorHandler instanceof ErrorHandler.ErrorPageMapper) {
            LOG.warn("ErrorPageMapper not supported for Server level Error Handling", new Object[0]);
        }
        if (this.getStopAtShutdown()) {
            ShutdownThread.register(this);
        }
        ShutdownMonitor.register(this);
        ShutdownMonitor.getInstance().start();
        LOG.info("jetty-" + Server.getVersion(), new Object[0]);
        if (!Jetty.STABLE) {
            LOG.warn("THIS IS NOT A STABLE RELEASE! DO NOT USE IN PRODUCTION!", new Object[0]);
            LOG.warn("Download a stable release from http://download.eclipse.org/jetty/", new Object[0]);
        }
        HttpGenerator.setJettyVersion(HttpConfiguration.SERVER_VERSION);
        ThreadPool.SizedThreadPool pool = this.getBean(ThreadPool.SizedThreadPool.class);
        int max = pool == null ? -1 : pool.getMaxThreads();
        int selectors = 0;
        int acceptors = 0;
        for (Connector connector : this._connectors) {
            if (!(connector instanceof AbstractConnector)) continue;
            AbstractConnector abstractConnector = (AbstractConnector)connector;
            Executor connectorExecutor = connector.getExecutor();
            if (connectorExecutor != pool) continue;
            acceptors += abstractConnector.getAcceptors();
            if (!(connector instanceof ServerConnector)) continue;
            selectors += 2 * ((ServerConnector)connector).getSelectorManager().getSelectorCount();
        }
        int needed = 1 + selectors + acceptors;
        if (max > 0 && needed > max) {
            throw new IllegalStateException(String.format("Insufficient threads: max=%d < needed(acceptors=%d + selectors=%d + request=1)", max, acceptors, selectors));
        }
        MultiException mex = new MultiException();
        try {
            super.doStart();
        }
        catch (Throwable e) {
            mex.add(e);
        }
        for (Connector connector : this._connectors) {
            try {
                connector.start();
            }
            catch (Throwable e) {
                mex.add(e);
            }
        }
        if (this.isDumpAfterStart()) {
            this.dumpStdErr();
        }
        mex.ifExceptionThrow();
        LOG.info(String.format("Started @%dms", Uptime.getUptime()), new Object[0]);
    }

    @Override
    protected void start(LifeCycle l) throws Exception {
        if (!(l instanceof Connector)) {
            super.start(l);
        }
    }

    @Override
    protected void doStop() throws Exception {
        Handler[] gracefuls;
        if (this.isDumpBeforeStop()) {
            this.dumpStdErr();
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("doStop {}", this);
        }
        MultiException mex = new MultiException();
        ArrayList<Future<Void>> futures = new ArrayList<Future<Void>>();
        for (Connector connector : this._connectors) {
            futures.add(connector.shutdown());
        }
        for (Handler handler : gracefuls = this.getChildHandlersByClass(Graceful.class)) {
            futures.add(((Graceful)((Object)handler)).shutdown());
        }
        long l = this.getStopTimeout();
        if (l > 0L) {
            long stop_by = System.currentTimeMillis() + l;
            if (LOG.isDebugEnabled()) {
                LOG.debug("Graceful shutdown {} by ", this, new Date(stop_by));
            }
            for (Future future : futures) {
                try {
                    if (future.isDone()) continue;
                    future.get(Math.max(1L, stop_by - System.currentTimeMillis()), TimeUnit.MILLISECONDS);
                }
                catch (Exception e) {
                    mex.add(e);
                }
            }
        }
        for (Future future : futures) {
            if (future.isDone()) continue;
            future.cancel(true);
        }
        for (Connector connector : this._connectors) {
            try {
                connector.stop();
            }
            catch (Throwable e) {
                mex.add(e);
            }
        }
        try {
            super.doStop();
        }
        catch (Throwable e) {
            mex.add(e);
        }
        if (this.getStopAtShutdown()) {
            ShutdownThread.deregister(this);
        }
        ShutdownMonitor.deregister(this);
        mex.ifExceptionThrow();
    }

    public void handle(HttpChannel channel) throws IOException, ServletException {
        String target = channel.getRequest().getPathInfo();
        Request request = channel.getRequest();
        Response response = channel.getResponse();
        if (LOG.isDebugEnabled()) {
            LOG.debug("{} {} {} on {}", new Object[]{request.getDispatcherType(), request.getMethod(), target, channel});
        }
        if (HttpMethod.OPTIONS.is(request.getMethod()) || "*".equals(target)) {
            if (!HttpMethod.OPTIONS.is(request.getMethod())) {
                response.sendError(400);
            }
            this.handleOptions(request, response);
            if (!request.isHandled()) {
                this.handle(target, request, request, response);
            }
        } else {
            this.handle(target, request, request, response);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("handled={} async={} committed={} on {}", request.isHandled(), request.isAsyncStarted(), response.isCommitted(), channel);
        }
    }

    protected void handleOptions(Request request, Response response) throws IOException {
    }

    public void handleAsync(HttpChannel channel) throws IOException, ServletException {
        HttpChannelState state = channel.getRequest().getHttpChannelState();
        AsyncContextEvent event = state.getAsyncContextEvent();
        Request baseRequest = channel.getRequest();
        String path = event.getPath();
        if (path != null) {
            ServletContext context = event.getServletContext();
            String query = baseRequest.getQueryString();
            baseRequest.setURIPathQuery(URIUtil.addEncodedPaths(context == null ? null : URIUtil.encodePath(context.getContextPath()), path));
            HttpURI uri = baseRequest.getHttpURI();
            baseRequest.setPathInfo(uri.getDecodedPath());
            if (uri.getQuery() != null) {
                baseRequest.mergeQueryParameters(query, uri.getQuery(), true);
            }
        }
        String target = baseRequest.getPathInfo();
        HttpServletRequest request = (HttpServletRequest)event.getSuppliedRequest();
        HttpServletResponse response = (HttpServletResponse)event.getSuppliedResponse();
        if (LOG.isDebugEnabled()) {
            LOG.debug("{} {} {} on {}", new Object[]{request.getDispatcherType(), request.getMethod(), target, channel});
        }
        this.handle(target, baseRequest, request, response);
        if (LOG.isDebugEnabled()) {
            LOG.debug("handledAsync={} async={} committed={} on {}", channel.getRequest().isHandled(), request.isAsyncStarted(), response.isCommitted(), channel);
        }
    }

    public void join() throws InterruptedException {
        this.getThreadPool().join();
    }

    public SessionIdManager getSessionIdManager() {
        return this._sessionIdManager;
    }

    public void setSessionIdManager(SessionIdManager sessionIdManager) {
        this.updateBean(this._sessionIdManager, sessionIdManager);
        this._sessionIdManager = sessionIdManager;
    }

    @Override
    public void clearAttributes() {
        Enumeration<String> names = this._attributes.getAttributeNames();
        while (names.hasMoreElements()) {
            this.removeBean(this._attributes.getAttribute(names.nextElement()));
        }
        this._attributes.clearAttributes();
    }

    @Override
    public Object getAttribute(String name) {
        return this._attributes.getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return AttributesMap.getAttributeNamesCopy(this._attributes);
    }

    @Override
    public void removeAttribute(String name) {
        Object bean = this._attributes.getAttribute(name);
        if (bean != null) {
            this.removeBean(bean);
        }
        this._attributes.removeAttribute(name);
    }

    @Override
    public void setAttribute(String name, Object attribute) {
        Object old = this._attributes.getAttribute(name);
        this.updateBean(old, attribute);
        this._attributes.setAttribute(name, attribute);
    }

    public URI getURI() {
        NetworkConnector connector = null;
        for (Connector c : this._connectors) {
            if (!(c instanceof NetworkConnector)) continue;
            connector = (NetworkConnector)c;
            break;
        }
        if (connector == null) {
            return null;
        }
        ContextHandler context = this.getChildHandlerByClass(ContextHandler.class);
        try {
            String path;
            String protocol = connector.getDefaultConnectionFactory().getProtocol();
            String scheme = "http";
            if (protocol.startsWith("SSL-") || protocol.equals("SSL")) {
                scheme = "https";
            }
            String host = connector.getHost();
            if (context != null && context.getVirtualHosts() != null && context.getVirtualHosts().length > 0) {
                host = context.getVirtualHosts()[0];
            }
            if (host == null) {
                host = InetAddress.getLocalHost().getHostAddress();
            }
            String string = path = context == null ? null : context.getContextPath();
            if (path == null) {
                path = "/";
            }
            return new URI(scheme, null, host, connector.getLocalPort(), path, null, null);
        }
        catch (Exception e) {
            LOG.warn(e);
            return null;
        }
    }

    public String toString() {
        return this.getClass().getName() + "@" + Integer.toHexString(this.hashCode());
    }

    @Override
    public void dump(Appendable out, String indent) throws IOException {
        this.dumpBeans(out, indent, Collections.singleton(new ClassLoaderDump(this.getClass().getClassLoader())));
    }

    public static void main(String ... args) throws Exception {
        System.err.println(Server.getVersion());
    }

    private static class DateField {
        final long _seconds;
        final HttpField _dateField;

        public DateField(long seconds, HttpField dateField) {
            this._seconds = seconds;
            this._dateField = dateField;
        }
    }
}

