/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.security.authentication;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.server.session.Session;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public abstract class LoginAuthenticator
implements Authenticator {
    private static final Logger LOG = Log.getLogger(LoginAuthenticator.class);
    protected LoginService _loginService;
    protected IdentityService _identityService;
    private boolean _renewSession;

    protected LoginAuthenticator() {
    }

    @Override
    public void prepareRequest(ServletRequest request) {
    }

    public UserIdentity login(String username, Object password, ServletRequest request) {
        UserIdentity user = this._loginService.login(username, password, request);
        if (user != null) {
            this.renewSession((HttpServletRequest)request, request instanceof Request ? ((Request)request).getResponse() : null);
            return user;
        }
        return null;
    }

    @Override
    public void setConfiguration(Authenticator.AuthConfiguration configuration) {
        this._loginService = configuration.getLoginService();
        if (this._loginService == null) {
            throw new IllegalStateException("No LoginService for " + this + " in " + configuration);
        }
        this._identityService = configuration.getIdentityService();
        if (this._identityService == null) {
            throw new IllegalStateException("No IdentityService for " + this + " in " + configuration);
        }
        this._renewSession = configuration.isSessionRenewedOnAuthentication();
    }

    public LoginService getLoginService() {
        return this._loginService;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    protected HttpSession renewSession(HttpServletRequest request, HttpServletResponse response) {
        HttpSession httpSession = request.getSession(false);
        if (this._renewSession && httpSession != null) {
            HttpSession httpSession2 = httpSession;
            synchronized (httpSession2) {
                if (httpSession.getAttribute("org.eclipse.jetty.security.sessionCreatedSecure") != Boolean.TRUE) {
                    if (httpSession instanceof Session) {
                        Session s = (Session)httpSession;
                        String oldId = s.getId();
                        s.renewId(request);
                        s.setAttribute("org.eclipse.jetty.security.sessionCreatedSecure", Boolean.TRUE);
                        if (s.isIdChanged() && response != null && response instanceof Response) {
                            ((Response)response).addCookie(s.getSessionHandler().getSessionCookie(s, request.getContextPath(), request.isSecure()));
                        }
                        LOG.debug("renew {}->{}", oldId, s.getId());
                    } else {
                        LOG.warn("Unable to renew session " + httpSession, new Object[0]);
                    }
                    return httpSession;
                }
            }
        }
        return httpSession;
    }
}

