/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.ServletRequestHttpWrapper;
import org.eclipse.jetty.server.ServletResponseHttpWrapper;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.Attributes;
import org.eclipse.jetty.util.MultiMap;

public class Dispatcher
implements RequestDispatcher {
    public static final String __ERROR_DISPATCH = "org.eclipse.jetty.server.Dispatcher.ERROR";
    public static final String __INCLUDE_PREFIX = "javax.servlet.include.";
    public static final String __FORWARD_PREFIX = "javax.servlet.forward.";
    private final ContextHandler _contextHandler;
    private final HttpURI _uri;
    private final String _pathInContext;
    private final String _named;

    public Dispatcher(ContextHandler contextHandler, HttpURI uri, String pathInContext) {
        this._contextHandler = contextHandler;
        this._uri = uri;
        this._pathInContext = pathInContext;
        this._named = null;
    }

    public Dispatcher(ContextHandler contextHandler, String name) throws IllegalStateException {
        this._contextHandler = contextHandler;
        this._uri = null;
        this._pathInContext = null;
        this._named = name;
    }

    @Override
    public void forward(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        this.forward(request, response, DispatcherType.FORWARD);
    }

    public void error(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        try {
            request.setAttribute(__ERROR_DISPATCH, Boolean.TRUE);
            this.forward(request, response, DispatcherType.ERROR);
        }
        finally {
            request.setAttribute(__ERROR_DISPATCH, null);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void include(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        Request baseRequest = Request.getBaseRequest(request);
        if (!(request instanceof HttpServletRequest)) {
            request = new ServletRequestHttpWrapper(request);
        }
        if (!(response instanceof HttpServletResponse)) {
            response = new ServletResponseHttpWrapper(response);
        }
        DispatcherType old_type = baseRequest.getDispatcherType();
        Attributes old_attr = baseRequest.getAttributes();
        MultiMap<String> old_query_params = baseRequest.getQueryParameters();
        try {
            baseRequest.setDispatcherType(DispatcherType.INCLUDE);
            baseRequest.getResponse().include();
            if (this._named != null) {
                this._contextHandler.handle(this._named, baseRequest, (HttpServletRequest)request, (HttpServletResponse)response);
            } else {
                IncludeAttributes attr = new IncludeAttributes(old_attr);
                attr._requestURI = this._uri.getPath();
                attr._contextPath = this._contextHandler.getContextPath();
                attr._servletPath = null;
                attr._pathInfo = this._pathInContext;
                attr._query = this._uri.getQuery();
                if (attr._query != null) {
                    baseRequest.mergeQueryParameters(baseRequest.getQueryString(), attr._query, false);
                }
                baseRequest.setAttributes(attr);
                this._contextHandler.handle(this._pathInContext, baseRequest, (HttpServletRequest)request, (HttpServletResponse)response);
            }
        }
        finally {
            baseRequest.setAttributes(old_attr);
            baseRequest.getResponse().included();
            baseRequest.setQueryParameters(old_query_params);
            baseRequest.resetParameters();
            baseRequest.setDispatcherType(old_type);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    protected void forward(ServletRequest request, ServletResponse response, DispatcherType dispatch) throws ServletException, IOException {
        Request baseRequest = Request.getBaseRequest(request);
        Response base_response = baseRequest.getResponse();
        base_response.resetForForward();
        if (!(request instanceof HttpServletRequest)) {
            request = new ServletRequestHttpWrapper(request);
        }
        if (!(response instanceof HttpServletResponse)) {
            response = new ServletResponseHttpWrapper(response);
        }
        HttpURI old_uri = baseRequest.getHttpURI();
        String old_context_path = baseRequest.getContextPath();
        String old_servlet_path = baseRequest.getServletPath();
        String old_path_info = baseRequest.getPathInfo();
        MultiMap<String> old_query_params = baseRequest.getQueryParameters();
        Attributes old_attr = baseRequest.getAttributes();
        DispatcherType old_type = baseRequest.getDispatcherType();
        try {
            baseRequest.setDispatcherType(dispatch);
            if (this._named != null) {
                this._contextHandler.handle(this._named, baseRequest, (HttpServletRequest)request, (HttpServletResponse)response);
            } else {
                ForwardAttributes attr = new ForwardAttributes(old_attr);
                if (old_attr.getAttribute("javax.servlet.forward.request_uri") != null) {
                    attr._pathInfo = (String)old_attr.getAttribute("javax.servlet.forward.path_info");
                    attr._query = (String)old_attr.getAttribute("javax.servlet.forward.query_string");
                    attr._requestURI = (String)old_attr.getAttribute("javax.servlet.forward.request_uri");
                    attr._contextPath = (String)old_attr.getAttribute("javax.servlet.forward.context_path");
                    attr._servletPath = (String)old_attr.getAttribute("javax.servlet.forward.servlet_path");
                } else {
                    attr._pathInfo = old_path_info;
                    attr._query = old_uri.getQuery();
                    attr._requestURI = old_uri.getPath();
                    attr._contextPath = old_context_path;
                    attr._servletPath = old_servlet_path;
                }
                HttpURI uri = new HttpURI(old_uri.getScheme(), old_uri.getHost(), old_uri.getPort(), this._uri.getPath(), this._uri.getParam(), this._uri.getQuery(), this._uri.getFragment());
                baseRequest.setHttpURI(uri);
                baseRequest.setContextPath(this._contextHandler.getContextPath());
                baseRequest.setServletPath(null);
                baseRequest.setPathInfo(this._pathInContext);
                if (this._uri.getQuery() != null || old_uri.getQuery() != null) {
                    baseRequest.mergeQueryParameters(old_uri.getQuery(), this._uri.getQuery(), true);
                }
                baseRequest.setAttributes(attr);
                this._contextHandler.handle(this._pathInContext, baseRequest, (HttpServletRequest)request, (HttpServletResponse)response);
                if (!baseRequest.getHttpChannelState().isAsync()) {
                    this.commitResponse(response, baseRequest);
                }
            }
        }
        finally {
            baseRequest.setHttpURI(old_uri);
            baseRequest.setContextPath(old_context_path);
            baseRequest.setServletPath(old_servlet_path);
            baseRequest.setPathInfo(old_path_info);
            baseRequest.setQueryParameters(old_query_params);
            baseRequest.resetParameters();
            baseRequest.setAttributes(old_attr);
            baseRequest.setDispatcherType(old_type);
        }
    }

    public String toString() {
        return String.format("Dispatcher@0x%x{%s,%s}", this.hashCode(), this._named, this._uri);
    }

    private void commitResponse(ServletResponse response, Request baseRequest) throws IOException, ServletException {
        if (baseRequest.getResponse().isWriting()) {
            try {
                response.getWriter().close();
            }
            catch (IllegalStateException e1) {
                try {
                    response.getOutputStream().close();
                }
                catch (IllegalStateException e2) {
                    ServletException servletException = new ServletException("Unable to commit the response", e2);
                    servletException.addSuppressed(e1);
                    throw servletException;
                }
            }
        } else {
            try {
                response.getOutputStream().close();
            }
            catch (IllegalStateException e1) {
                try {
                    response.getWriter().close();
                }
                catch (IllegalStateException e2) {
                    ServletException servletException = new ServletException("Unable to commit the response", e2);
                    servletException.addSuppressed(e1);
                    throw servletException;
                }
            }
        }
    }

    private class IncludeAttributes
    implements Attributes {
        final Attributes _attr;
        String _requestURI;
        String _contextPath;
        String _servletPath;
        String _pathInfo;
        String _query;

        IncludeAttributes(Attributes attributes) {
            this._attr = attributes;
        }

        @Override
        public Object getAttribute(String key) {
            if (Dispatcher.this._named == null) {
                if (key.equals("javax.servlet.include.path_info")) {
                    return this._pathInfo;
                }
                if (key.equals("javax.servlet.include.servlet_path")) {
                    return this._servletPath;
                }
                if (key.equals("javax.servlet.include.context_path")) {
                    return this._contextPath;
                }
                if (key.equals("javax.servlet.include.query_string")) {
                    return this._query;
                }
                if (key.equals("javax.servlet.include.request_uri")) {
                    return this._requestURI;
                }
            } else if (key.startsWith(Dispatcher.__INCLUDE_PREFIX)) {
                return null;
            }
            return this._attr.getAttribute(key);
        }

        @Override
        public Enumeration<String> getAttributeNames() {
            HashSet<String> set = new HashSet<String>();
            Enumeration<String> e = this._attr.getAttributeNames();
            while (e.hasMoreElements()) {
                String name = e.nextElement();
                if (name.startsWith(Dispatcher.__INCLUDE_PREFIX)) continue;
                set.add(name);
            }
            if (Dispatcher.this._named == null) {
                if (this._pathInfo != null) {
                    set.add("javax.servlet.include.path_info");
                } else {
                    set.remove("javax.servlet.include.path_info");
                }
                set.add("javax.servlet.include.request_uri");
                set.add("javax.servlet.include.servlet_path");
                set.add("javax.servlet.include.context_path");
                if (this._query != null) {
                    set.add("javax.servlet.include.query_string");
                } else {
                    set.remove("javax.servlet.include.query_string");
                }
            }
            return Collections.enumeration(set);
        }

        @Override
        public void setAttribute(String key, Object value) {
            if (Dispatcher.this._named == null && key.startsWith("javax.servlet.")) {
                if (key.equals("javax.servlet.include.path_info")) {
                    this._pathInfo = (String)value;
                } else if (key.equals("javax.servlet.include.request_uri")) {
                    this._requestURI = (String)value;
                } else if (key.equals("javax.servlet.include.servlet_path")) {
                    this._servletPath = (String)value;
                } else if (key.equals("javax.servlet.include.context_path")) {
                    this._contextPath = (String)value;
                } else if (key.equals("javax.servlet.include.query_string")) {
                    this._query = (String)value;
                } else if (value == null) {
                    this._attr.removeAttribute(key);
                } else {
                    this._attr.setAttribute(key, value);
                }
            } else if (value == null) {
                this._attr.removeAttribute(key);
            } else {
                this._attr.setAttribute(key, value);
            }
        }

        public String toString() {
            return "INCLUDE+" + this._attr.toString();
        }

        @Override
        public void clearAttributes() {
            throw new IllegalStateException();
        }

        @Override
        public void removeAttribute(String name) {
            this.setAttribute(name, null);
        }
    }

    private class ForwardAttributes
    implements Attributes {
        final Attributes _attr;
        String _requestURI;
        String _contextPath;
        String _servletPath;
        String _pathInfo;
        String _query;

        ForwardAttributes(Attributes attributes) {
            this._attr = attributes;
        }

        @Override
        public Object getAttribute(String key) {
            if (Dispatcher.this._named == null) {
                if (key.equals("javax.servlet.forward.path_info")) {
                    return this._pathInfo;
                }
                if (key.equals("javax.servlet.forward.request_uri")) {
                    return this._requestURI;
                }
                if (key.equals("javax.servlet.forward.servlet_path")) {
                    return this._servletPath;
                }
                if (key.equals("javax.servlet.forward.context_path")) {
                    return this._contextPath;
                }
                if (key.equals("javax.servlet.forward.query_string")) {
                    return this._query;
                }
            }
            if (key.startsWith(Dispatcher.__INCLUDE_PREFIX)) {
                return null;
            }
            return this._attr.getAttribute(key);
        }

        @Override
        public Enumeration<String> getAttributeNames() {
            HashSet<String> set = new HashSet<String>();
            Enumeration<String> e = this._attr.getAttributeNames();
            while (e.hasMoreElements()) {
                String name = e.nextElement();
                if (name.startsWith(Dispatcher.__INCLUDE_PREFIX) || name.startsWith(Dispatcher.__FORWARD_PREFIX)) continue;
                set.add(name);
            }
            if (Dispatcher.this._named == null) {
                if (this._pathInfo != null) {
                    set.add("javax.servlet.forward.path_info");
                } else {
                    set.remove("javax.servlet.forward.path_info");
                }
                set.add("javax.servlet.forward.request_uri");
                set.add("javax.servlet.forward.servlet_path");
                set.add("javax.servlet.forward.context_path");
                if (this._query != null) {
                    set.add("javax.servlet.forward.query_string");
                } else {
                    set.remove("javax.servlet.forward.query_string");
                }
            }
            return Collections.enumeration(set);
        }

        @Override
        public void setAttribute(String key, Object value) {
            if (Dispatcher.this._named == null && key.startsWith("javax.servlet.")) {
                if (key.equals("javax.servlet.forward.path_info")) {
                    this._pathInfo = (String)value;
                } else if (key.equals("javax.servlet.forward.request_uri")) {
                    this._requestURI = (String)value;
                } else if (key.equals("javax.servlet.forward.servlet_path")) {
                    this._servletPath = (String)value;
                } else if (key.equals("javax.servlet.forward.context_path")) {
                    this._contextPath = (String)value;
                } else if (key.equals("javax.servlet.forward.query_string")) {
                    this._query = (String)value;
                } else if (value == null) {
                    this._attr.removeAttribute(key);
                } else {
                    this._attr.setAttribute(key, value);
                }
            } else if (value == null) {
                this._attr.removeAttribute(key);
            } else {
                this._attr.setAttribute(key, value);
            }
        }

        public String toString() {
            return "FORWARD+" + this._attr.toString();
        }

        @Override
        public void clearAttributes() {
            throw new IllegalStateException();
        }

        @Override
        public void removeAttribute(String name) {
            this.setAttribute(name, null);
        }
    }
}

