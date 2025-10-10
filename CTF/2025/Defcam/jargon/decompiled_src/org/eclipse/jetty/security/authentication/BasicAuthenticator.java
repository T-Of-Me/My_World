/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.security.authentication;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.security.authentication.DeferredAuthentication;
import org.eclipse.jetty.security.authentication.LoginAuthenticator;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.B64Code;

public class BasicAuthenticator
extends LoginAuthenticator {
    @Override
    public String getAuthMethod() {
        return "BASIC";
    }

    @Override
    public Authentication validateRequest(ServletRequest req, ServletResponse res, boolean mandatory) throws ServerAuthException {
        HttpServletRequest request = (HttpServletRequest)req;
        HttpServletResponse response = (HttpServletResponse)res;
        String credentials = request.getHeader(HttpHeader.AUTHORIZATION.asString());
        try {
            String method;
            int space;
            if (!mandatory) {
                return new DeferredAuthentication(this);
            }
            if (credentials != null && (space = credentials.indexOf(32)) > 0 && "basic".equalsIgnoreCase(method = credentials.substring(0, space))) {
                String password;
                String username;
                UserIdentity user;
                credentials = credentials.substring(space + 1);
                int i = (credentials = B64Code.decode(credentials, StandardCharsets.ISO_8859_1)).indexOf(58);
                if (i > 0 && (user = this.login(username = credentials.substring(0, i), password = credentials.substring(i + 1), request)) != null) {
                    return new UserAuthentication(this.getAuthMethod(), user);
                }
            }
            if (DeferredAuthentication.isDeferred(response)) {
                return Authentication.UNAUTHENTICATED;
            }
            response.setHeader(HttpHeader.WWW_AUTHENTICATE.asString(), "basic realm=\"" + this._loginService.getName() + '\"');
            response.sendError(401);
            return Authentication.SEND_CONTINUE;
        }
        catch (IOException e) {
            throw new ServerAuthException(e);
        }
    }

    @Override
    public boolean secureResponse(ServletRequest req, ServletResponse res, boolean mandatory, Authentication.User validatedUser) throws ServerAuthException {
        return true;
    }
}

