/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.handler;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HostPortHttpField;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.QuotedCSV;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.util.IncludeExcludeSet;
import org.eclipse.jetty.util.InetAddressSet;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedOperation;
import org.eclipse.jetty.util.annotation.Name;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.thread.Locker;

public class ThreadLimitHandler
extends HandlerWrapper {
    private static final Logger LOG = Log.getLogger(ThreadLimitHandler.class);
    private static final String REMOTE = "o.e.j.s.h.TLH.REMOTE";
    private static final String PERMIT = "o.e.j.s.h.TLH.PASS";
    private final boolean _rfc7239;
    private final String _forwardedHeader;
    private final IncludeExcludeSet<String, InetAddress> _includeExcludeSet = new IncludeExcludeSet(InetAddressSet.class);
    private final ConcurrentMap<String, Remote> _remotes = new ConcurrentHashMap<String, Remote>();
    private volatile boolean _enabled;
    private int _threadLimit = 10;

    public ThreadLimitHandler() {
        this(null, false);
    }

    public ThreadLimitHandler(@Name(value="forwardedHeader") String forwardedHeader) {
        this(forwardedHeader, HttpHeader.FORWARDED.is(forwardedHeader));
    }

    public ThreadLimitHandler(@Name(value="forwardedHeader") String forwardedHeader, @Name(value="rfc7239") boolean rfc7239) {
        this._rfc7239 = rfc7239;
        this._forwardedHeader = forwardedHeader;
        this._enabled = true;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        LOG.info(String.format("ThreadLimitHandler enable=%b limit=%d include=%s", this._enabled, this._threadLimit, this._includeExcludeSet), new Object[0]);
    }

    @ManagedAttribute(value="true if this handler is enabled")
    public boolean isEnabled() {
        return this._enabled;
    }

    public void setEnabled(boolean enabled) {
        this._enabled = enabled;
        LOG.info(String.format("ThreadLimitHandler enable=%b limit=%d include=%s", this._enabled, this._threadLimit, this._includeExcludeSet), new Object[0]);
    }

    @ManagedAttribute(value="The maximum threads that can be dispatched per remote IP")
    public int getThreadLimit() {
        return this._threadLimit;
    }

    public void setThreadLimit(int threadLimit) {
        if (threadLimit <= 0) {
            throw new IllegalArgumentException("limit must be >0");
        }
        this._threadLimit = threadLimit;
    }

    @ManagedOperation(value="Include IP in thread limits")
    public void include(String inetAddressPattern) {
        this._includeExcludeSet.include(inetAddressPattern);
    }

    @ManagedOperation(value="Exclude IP from thread limits")
    public void exclude(String inetAddressPattern) {
        this._includeExcludeSet.exclude(inetAddressPattern);
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!this._enabled) {
            super.handle(target, baseRequest, request, response);
        } else {
            Remote remote = this.getRemote(baseRequest);
            if (remote == null) {
                super.handle(target, baseRequest, request, response);
            } else {
                try (Closeable permit = (Closeable)baseRequest.getAttribute(PERMIT);){
                    if (permit != null) {
                        baseRequest.removeAttribute(PERMIT);
                    } else {
                        CompletableFuture<Closeable> future_permit = remote.acquire();
                        if (future_permit.isDone()) {
                            permit = future_permit.get();
                        } else {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Threadlimited {} {}", remote, target);
                            }
                            AsyncContext async = baseRequest.startAsync();
                            async.setTimeout(0L);
                            future_permit.thenAccept(c -> {
                                baseRequest.setAttribute(PERMIT, c);
                                async.dispatch();
                            });
                            return;
                        }
                    }
                    super.handle(target, baseRequest, request, response);
                }
            }
        }
    }

    protected int getThreadLimit(String ip) {
        if (!this._includeExcludeSet.isEmpty()) {
            try {
                if (!this._includeExcludeSet.test(InetAddress.getByName(ip))) {
                    LOG.debug("excluded {}", ip);
                    return 0;
                }
            }
            catch (Exception e) {
                LOG.ignore(e);
            }
        }
        return this._threadLimit;
    }

    protected Remote getRemote(Request baseRequest) {
        Remote r;
        Remote remote = (Remote)baseRequest.getAttribute(REMOTE);
        if (remote != null) {
            return remote;
        }
        String ip = this.getRemoteIP(baseRequest);
        LOG.debug("ip={}", ip);
        if (ip == null) {
            return null;
        }
        int limit = this.getThreadLimit(ip);
        if (limit <= 0) {
            return null;
        }
        remote = (Remote)this._remotes.get(ip);
        if (remote == null && (remote = this._remotes.putIfAbsent(ip, r = new Remote(ip, limit))) == null) {
            remote = r;
        }
        baseRequest.setAttribute(REMOTE, remote);
        return remote;
    }

    protected String getRemoteIP(Request baseRequest) {
        InetSocketAddress inet_addr;
        if (this._forwardedHeader != null && !this._forwardedHeader.isEmpty()) {
            String remote;
            String string = remote = this._rfc7239 ? this.getForwarded(baseRequest) : this.getXForwardedFor(baseRequest);
            if (remote != null && !remote.isEmpty()) {
                return remote;
            }
        }
        if ((inet_addr = baseRequest.getHttpChannel().getRemoteAddress()) != null && inet_addr.getAddress() != null) {
            return inet_addr.getAddress().getHostAddress();
        }
        return null;
    }

    private String getForwarded(Request request) {
        RFC7239 rfc7239 = new RFC7239();
        HttpFields httpFields = request.getHttpFields();
        for (HttpField field : httpFields) {
            if (!this._forwardedHeader.equalsIgnoreCase(field.getName())) continue;
            rfc7239.addValue(field.getValue());
        }
        if (rfc7239.getFor() != null) {
            return new HostPortHttpField(rfc7239.getFor()).getHost();
        }
        return null;
    }

    private String getXForwardedFor(Request request) {
        String forwarded_for = null;
        HttpFields httpFields = request.getHttpFields();
        for (HttpField field : httpFields) {
            if (!this._forwardedHeader.equalsIgnoreCase(field.getName())) continue;
            forwarded_for = field.getValue();
        }
        if (forwarded_for == null || forwarded_for.isEmpty()) {
            return null;
        }
        int comma = forwarded_for.lastIndexOf(44);
        return comma >= 0 ? forwarded_for.substring(comma + 1).trim() : forwarded_for;
    }

    private final class RFC7239
    extends QuotedCSV {
        String _for;

        private RFC7239() {
            super(false, new String[0]);
        }

        String getFor() {
            return this._for;
        }

        @Override
        protected void parsedParam(StringBuffer buffer, int valueLength, int paramName, int paramValue) {
            String name;
            if (valueLength == 0 && paramValue > paramName && "for".equalsIgnoreCase(name = StringUtil.asciiToLowerCase(buffer.substring(paramName, paramValue - 1)))) {
                String value = buffer.substring(paramValue);
                this._for = "unknown".equalsIgnoreCase(value) ? null : value;
            }
        }
    }

    private final class Remote
    implements Closeable {
        private final String _ip;
        private final int _limit;
        private final Locker _locker = new Locker();
        private int _permits;
        private Deque<CompletableFuture<Closeable>> _queue = new ArrayDeque<CompletableFuture<Closeable>>();
        private final CompletableFuture<Closeable> _permitted = CompletableFuture.completedFuture(this);

        public Remote(String ip, int limit) {
            this._ip = ip;
            this._limit = limit;
        }

        public CompletableFuture<Closeable> acquire() {
            try (Locker.Lock lock = this._locker.lock();){
                if (this._permits < this._limit) {
                    ++this._permits;
                    CompletableFuture<Closeable> completableFuture = this._permitted;
                    return completableFuture;
                }
                CompletableFuture<Closeable> pass = new CompletableFuture<Closeable>();
                this._queue.addLast(pass);
                CompletableFuture<Closeable> completableFuture = pass;
                return completableFuture;
            }
        }

        @Override
        public void close() throws IOException {
            block13: {
                try (Locker.Lock lock = this._locker.lock();){
                    CompletableFuture<Closeable> permit;
                    --this._permits;
                    do {
                        if ((permit = this._queue.pollFirst()) != null) continue;
                        break block13;
                    } while (!permit.complete(this));
                    ++this._permits;
                }
            }
        }

        public String toString() {
            try (Locker.Lock lock = this._locker.lock();){
                String string = String.format("R[ip=%s,p=%d,l=%d,q=%d]", this._ip, this._permits, this._limit, this._queue.size());
                return string;
            }
        }
    }
}

