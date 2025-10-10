/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.handler;

import java.io.IOException;
import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;

public class AsyncDelayHandler
extends HandlerWrapper {
    public static final String AHW_ATTR = "o.e.j.s.h.AsyncHandlerWrapper";

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!this.isStarted() || this._handler == null) {
            return;
        }
        DispatcherType ctype = baseRequest.getDispatcherType();
        DispatcherType dtype = (DispatcherType)((Object)baseRequest.getAttribute(AHW_ATTR));
        Object async_context_path = null;
        Object async_path_info = null;
        Object async_query_string = null;
        Object async_request_uri = null;
        Object async_servlet_path = null;
        boolean restart = false;
        if (dtype != null) {
            baseRequest.setAttribute(AHW_ATTR, null);
            baseRequest.setDispatcherType(dtype);
            restart = true;
            async_context_path = baseRequest.getAttribute("javax.servlet.async.context_path");
            baseRequest.setAttribute("javax.servlet.async.context_path", null);
            async_path_info = baseRequest.getAttribute("javax.servlet.async.path_info");
            baseRequest.setAttribute("javax.servlet.async.path_info", null);
            async_query_string = baseRequest.getAttribute("javax.servlet.async.query_string");
            baseRequest.setAttribute("javax.servlet.async.query_string", null);
            async_request_uri = baseRequest.getAttribute("javax.servlet.async.request_uri");
            baseRequest.setAttribute("javax.servlet.async.request_uri", null);
            async_servlet_path = baseRequest.getAttribute("javax.servlet.async.servlet_path");
            baseRequest.setAttribute("javax.servlet.async.servlet_path", null);
        }
        if (!this.startHandling(baseRequest, restart)) {
            AsyncContext context = baseRequest.startAsync();
            baseRequest.setAttribute(AHW_ATTR, (Object)ctype);
            this.delayHandling(baseRequest, context);
            return;
        }
        try {
            this._handler.handle(target, baseRequest, request, response);
            if (restart) {
                baseRequest.setDispatcherType(ctype);
                baseRequest.setAttribute("javax.servlet.async.context_path", async_context_path);
                baseRequest.setAttribute("javax.servlet.async.path_info", async_path_info);
                baseRequest.setAttribute("javax.servlet.async.query_string", async_query_string);
                baseRequest.setAttribute("javax.servlet.async.request_uri", async_request_uri);
                baseRequest.setAttribute("javax.servlet.async.servlet_path", async_servlet_path);
            }
            this.endHandling(baseRequest);
        }
        catch (Throwable throwable) {
            if (restart) {
                baseRequest.setDispatcherType(ctype);
                baseRequest.setAttribute("javax.servlet.async.context_path", async_context_path);
                baseRequest.setAttribute("javax.servlet.async.path_info", async_path_info);
                baseRequest.setAttribute("javax.servlet.async.query_string", async_query_string);
                baseRequest.setAttribute("javax.servlet.async.request_uri", async_request_uri);
                baseRequest.setAttribute("javax.servlet.async.servlet_path", async_servlet_path);
            }
            this.endHandling(baseRequest);
            throw throwable;
        }
    }

    protected boolean startHandling(Request request, boolean restart) {
        return true;
    }

    protected void delayHandling(Request request, AsyncContext context) {
        context.dispatch();
    }

    protected void endHandling(Request request) {
    }
}

