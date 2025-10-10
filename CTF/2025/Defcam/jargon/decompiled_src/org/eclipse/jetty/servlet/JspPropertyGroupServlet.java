/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.servlet;

import java.io.IOException;
import java.util.Locale;
import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.resource.Resource;

public class JspPropertyGroupServlet
extends GenericServlet {
    private static final long serialVersionUID = 3681783214726776945L;
    public static final String NAME = "__org.eclipse.jetty.servlet.JspPropertyGroupServlet__";
    private final ServletHandler _servletHandler;
    private final ContextHandler _contextHandler;
    private ServletHolder _dftServlet;
    private ServletHolder _jspServlet;
    private boolean _starJspMapped;

    public JspPropertyGroupServlet(ContextHandler context, ServletHandler servletHandler) {
        this._contextHandler = context;
        this._servletHandler = servletHandler;
    }

    @Override
    public void init() throws ServletException {
        String jsp_name = "jsp";
        ServletMapping servlet_mapping = this._servletHandler.getServletMapping("*.jsp");
        if (servlet_mapping != null) {
            ServletMapping[] mappings;
            this._starJspMapped = true;
            for (ServletMapping m : mappings = this._servletHandler.getServletMappings()) {
                String[] paths = m.getPathSpecs();
                if (paths == null) continue;
                for (String path : paths) {
                    if (!"*.jsp".equals(path) || NAME.equals(m.getServletName())) continue;
                    servlet_mapping = m;
                }
            }
            jsp_name = servlet_mapping.getServletName();
        }
        this._jspServlet = this._servletHandler.getServlet(jsp_name);
        String dft_name = "default";
        ServletMapping default_mapping = this._servletHandler.getServletMapping("/");
        if (default_mapping != null) {
            dft_name = default_mapping.getServletName();
        }
        this._dftServlet = this._servletHandler.getServlet(dft_name);
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        String pathInContext;
        HttpServletRequest request = null;
        if (!(req instanceof HttpServletRequest)) {
            throw new ServletException("Request not HttpServletRequest");
        }
        request = (HttpServletRequest)req;
        String servletPath = null;
        String pathInfo = null;
        if (request.getAttribute("javax.servlet.include.request_uri") != null) {
            servletPath = (String)request.getAttribute("javax.servlet.include.servlet_path");
            pathInfo = (String)request.getAttribute("javax.servlet.include.path_info");
            if (servletPath == null) {
                servletPath = request.getServletPath();
                pathInfo = request.getPathInfo();
            }
        } else {
            servletPath = request.getServletPath();
            pathInfo = request.getPathInfo();
        }
        if ((pathInContext = URIUtil.addPaths(servletPath, pathInfo)).endsWith("/")) {
            this._dftServlet.getServlet().service(req, res);
        } else if (this._starJspMapped && pathInContext.toLowerCase(Locale.ENGLISH).endsWith(".jsp")) {
            this._jspServlet.getServlet().service(req, res);
        } else {
            Resource resource = this._contextHandler.getResource(pathInContext);
            if (resource != null && resource.isDirectory()) {
                this._dftServlet.getServlet().service(req, res);
            } else {
                this._jspServlet.getServlet().service(req, res);
            }
        }
    }
}

