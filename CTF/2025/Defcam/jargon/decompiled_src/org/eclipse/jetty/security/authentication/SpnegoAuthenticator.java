/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.security.authentication;

import java.io.IOException;
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
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class SpnegoAuthenticator
extends LoginAuthenticator {
    private static final Logger LOG = Log.getLogger(SpnegoAuthenticator.class);
    private String _authMethod = "SPNEGO";

    public SpnegoAuthenticator() {
    }

    public SpnegoAuthenticator(String authMethod) {
        this._authMethod = authMethod;
    }

    @Override
    public String getAuthMethod() {
        return this._authMethod;
    }

    @Override
    public Authentication validateRequest(ServletRequest request, ServletResponse response, boolean mandatory) throws ServerAuthException {
        String spnegoToken;
        UserIdentity user;
        HttpServletRequest req = (HttpServletRequest)request;
        HttpServletResponse res = (HttpServletResponse)response;
        String header = req.getHeader(HttpHeader.AUTHORIZATION.asString());
        if (!mandatory) {
            return new DeferredAuthentication(this);
        }
        if (header == null) {
            try {
                if (DeferredAuthentication.isDeferred(res)) {
                    return Authentication.UNAUTHENTICATED;
                }
                LOG.debug("SpengoAuthenticator: sending challenge", new Object[0]);
                res.setHeader(HttpHeader.WWW_AUTHENTICATE.asString(), HttpHeader.NEGOTIATE.asString());
                res.sendError(401);
                return Authentication.SEND_CONTINUE;
            }
            catch (IOException ioe) {
                throw new ServerAuthException(ioe);
            }
        }
        if (header != null && header.startsWith(HttpHeader.NEGOTIATE.asString()) && (user = this.login(null, spnegoToken = header.substring(10), request)) != null) {
            return new UserAuthentication(this.getAuthMethod(), user);
        }
        return Authentication.UNAUTHENTICATED;
    }

    @Override
    public boolean secureResponse(ServletRequest request, ServletResponse response, boolean mandatory, Authentication.User validatedUser) throws ServerAuthException {
        return true;
    }
}

