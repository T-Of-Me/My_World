/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.handler;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.util.IncludeExcludeSet;
import org.eclipse.jetty.util.InetAddressSet;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class InetAccessHandler
extends HandlerWrapper {
    private static final Logger LOG = Log.getLogger(InetAccessHandler.class);
    private final IncludeExcludeSet<String, InetAddress> _set = new IncludeExcludeSet(InetAddressSet.class);

    public void include(String pattern) {
        this._set.include(pattern);
    }

    public void include(String ... patterns) {
        this._set.include((String[])patterns);
    }

    public void exclude(String pattern) {
        this._set.exclude(pattern);
    }

    public void exclude(String ... patterns) {
        this._set.exclude((String[])patterns);
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        InetSocketAddress address;
        EndPoint endp;
        HttpChannel channel = baseRequest.getHttpChannel();
        if (channel != null && (endp = channel.getEndPoint()) != null && (address = endp.getRemoteAddress()) != null && !this.isAllowed(address.getAddress(), request)) {
            response.sendError(403);
            baseRequest.setHandled(true);
            return;
        }
        this.getHandler().handle(target, baseRequest, request, response);
    }

    protected boolean isAllowed(InetAddress address, HttpServletRequest request) {
        boolean allowed = this._set.test(address);
        if (LOG.isDebugEnabled()) {
            LOG.debug("{} {} {} for {}", this, allowed ? "allowed" : "denied", address, request);
        }
        return allowed;
    }

    @Override
    public void dump(Appendable out, String indent) throws IOException {
        this.dumpBeans(out, indent, this._set.getIncluded(), this._set.getExcluded());
    }
}

