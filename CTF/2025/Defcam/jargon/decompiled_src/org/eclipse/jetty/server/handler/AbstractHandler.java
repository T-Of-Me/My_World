/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.handler;

import java.io.IOException;
import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

@ManagedObject(value="Jetty Handler")
public abstract class AbstractHandler
extends ContainerLifeCycle
implements Handler {
    private static final Logger LOG = Log.getLogger(AbstractHandler.class);
    private Server _server;

    @Override
    public abstract void handle(String var1, Request var2, HttpServletRequest var3, HttpServletResponse var4) throws IOException, ServletException;

    protected void doError(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        Object o = request.getAttribute("javax.servlet.error.status_code");
        int code = o instanceof Integer ? (Integer)o : (o != null ? Integer.valueOf(o.toString()) : 500);
        o = request.getAttribute("javax.servlet.error.message");
        String reason = o != null ? o.toString() : null;
        response.sendError(code, reason);
    }

    @Override
    protected void doStart() throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("starting {}", this);
        }
        if (this._server == null) {
            LOG.warn("No Server set for {}", this);
        }
        super.doStart();
    }

    @Override
    protected void doStop() throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("stopping {}", this);
        }
        super.doStop();
    }

    @Override
    public void setServer(Server server) {
        if (this._server == server) {
            return;
        }
        if (this.isStarted()) {
            throw new IllegalStateException("STARTED");
        }
        this._server = server;
    }

    @Override
    public Server getServer() {
        return this._server;
    }

    @Override
    public void destroy() {
        if (!this.isStopped()) {
            throw new IllegalStateException("!STOPPED");
        }
        super.destroy();
    }

    @Override
    public void dumpThis(Appendable out) throws IOException {
        out.append(this.toString()).append(" - ").append(this.getState()).append('\n');
    }

    public static abstract class ErrorDispatchHandler
    extends AbstractHandler {
        @Override
        public final void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            if (baseRequest.getDispatcherType() == DispatcherType.ERROR) {
                this.doError(target, baseRequest, request, response);
            } else {
                this.doNonErrorHandle(target, baseRequest, request, response);
            }
        }

        protected abstract void doNonErrorHandle(String var1, Request var2, HttpServletRequest var3, HttpServletResponse var4) throws IOException, ServletException;
    }
}

