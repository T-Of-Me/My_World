/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.session;

import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.session.SessionHandler;

public class SessionContext {
    public static final String NULL_VHOST = "0.0.0.0";
    private ContextHandler.Context _context;
    private SessionHandler _sessionHandler;
    private String _workerName;
    private String _canonicalContextPath;
    private String _vhost;

    public SessionContext(String workerName, ContextHandler.Context context) {
        if (context != null) {
            this._sessionHandler = context.getContextHandler().getChildHandlerByClass(SessionHandler.class);
        }
        this._workerName = workerName;
        this._context = context;
        this._canonicalContextPath = this.canonicalizeContextPath(this._context);
        this._vhost = this.canonicalizeVHost(this._context);
    }

    public String getWorkerName() {
        return this._workerName;
    }

    public SessionHandler getSessionHandler() {
        return this._sessionHandler;
    }

    public ContextHandler.Context getContext() {
        return this._context;
    }

    public String getCanonicalContextPath() {
        return this._canonicalContextPath;
    }

    public String getVhost() {
        return this._vhost;
    }

    public String toString() {
        return this._workerName + "_" + this._canonicalContextPath + "_" + this._vhost;
    }

    public void run(Runnable r) {
        if (this._context != null) {
            this._context.getContextHandler().handle(r);
        } else {
            r.run();
        }
    }

    private String canonicalizeContextPath(ContextHandler.Context context) {
        if (context == null) {
            return "";
        }
        return this.canonicalize(context.getContextPath());
    }

    private String canonicalizeVHost(ContextHandler.Context context) {
        String vhost = NULL_VHOST;
        if (context == null) {
            return vhost;
        }
        String[] vhosts = context.getContextHandler().getVirtualHosts();
        if (vhosts == null || vhosts.length == 0 || vhosts[0] == null) {
            return vhost;
        }
        return vhosts[0];
    }

    private String canonicalize(String path) {
        if (path == null) {
            return "";
        }
        return path.replace('/', '_').replace('.', '_').replace('\\', '_');
    }
}

