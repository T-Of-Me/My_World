/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.handler;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;

public abstract class ScopedHandler
extends HandlerWrapper {
    private static final ThreadLocal<ScopedHandler> __outerScope = new ThreadLocal();
    protected ScopedHandler _outerScope;
    protected ScopedHandler _nextScope;

    @Override
    protected void doStart() throws Exception {
        try {
            this._outerScope = __outerScope.get();
            if (this._outerScope == null) {
                __outerScope.set(this);
            }
            super.doStart();
            this._nextScope = this.getChildHandlerByClass(ScopedHandler.class);
        }
        finally {
            if (this._outerScope == null) {
                __outerScope.set(null);
            }
        }
    }

    @Override
    public final void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (this.isStarted()) {
            if (this._outerScope == null) {
                this.doScope(target, baseRequest, request, response);
            } else {
                this.doHandle(target, baseRequest, request, response);
            }
        }
    }

    public void doScope(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        this.nextScope(target, baseRequest, request, response);
    }

    public final void nextScope(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (this._nextScope != null) {
            this._nextScope.doScope(target, baseRequest, request, response);
        } else if (this._outerScope != null) {
            this._outerScope.doHandle(target, baseRequest, request, response);
        } else {
            this.doHandle(target, baseRequest, request, response);
        }
    }

    public abstract void doHandle(String var1, Request var2, HttpServletRequest var3, HttpServletResponse var4) throws IOException, ServletException;

    public final void nextHandle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (this._nextScope != null && this._nextScope == this._handler) {
            this._nextScope.doHandle(target, baseRequest, request, response);
        } else if (this._handler != null) {
            super.handle(target, baseRequest, request, response);
        }
    }
}

