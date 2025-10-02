/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.handler;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.URIUtil;

public class SecuredRedirectHandler
extends AbstractHandler {
    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        HttpChannel channel = baseRequest.getHttpChannel();
        if (baseRequest.isSecure() || channel == null) {
            return;
        }
        HttpConfiguration httpConfig = channel.getHttpConfiguration();
        if (httpConfig == null) {
            response.sendError(403, "No http configuration available");
            return;
        }
        if (httpConfig.getSecurePort() > 0) {
            String scheme = httpConfig.getSecureScheme();
            int port = httpConfig.getSecurePort();
            String url = URIUtil.newURI(scheme, baseRequest.getServerName(), port, baseRequest.getRequestURI(), baseRequest.getQueryString());
            response.setContentLength(0);
            response.sendRedirect(url);
        } else {
            response.sendError(403, "Not Secure");
        }
        baseRequest.setHandled(true);
    }
}

