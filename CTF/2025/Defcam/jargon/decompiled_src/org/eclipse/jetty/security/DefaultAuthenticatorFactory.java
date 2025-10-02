/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.security;

import javax.servlet.ServletContext;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.security.authentication.ClientCertAuthenticator;
import org.eclipse.jetty.security.authentication.DigestAuthenticator;
import org.eclipse.jetty.security.authentication.FormAuthenticator;
import org.eclipse.jetty.security.authentication.LoginAuthenticator;
import org.eclipse.jetty.security.authentication.SpnegoAuthenticator;
import org.eclipse.jetty.server.Server;

public class DefaultAuthenticatorFactory
implements Authenticator.Factory {
    LoginService _loginService;

    @Override
    public Authenticator getAuthenticator(Server server, ServletContext context, Authenticator.AuthConfiguration configuration, IdentityService identityService, LoginService loginService) {
        String auth = configuration.getAuthMethod();
        LoginAuthenticator authenticator = null;
        if (auth == null || "BASIC".equalsIgnoreCase(auth)) {
            authenticator = new BasicAuthenticator();
        } else if ("DIGEST".equalsIgnoreCase(auth)) {
            authenticator = new DigestAuthenticator();
        } else if ("FORM".equalsIgnoreCase(auth)) {
            authenticator = new FormAuthenticator();
        } else if ("SPNEGO".equalsIgnoreCase(auth)) {
            authenticator = new SpnegoAuthenticator();
        } else if ("NEGOTIATE".equalsIgnoreCase(auth)) {
            authenticator = new SpnegoAuthenticator("NEGOTIATE");
        }
        if ("CLIENT_CERT".equalsIgnoreCase(auth) || "CLIENT-CERT".equalsIgnoreCase(auth)) {
            authenticator = new ClientCertAuthenticator();
        }
        return authenticator;
    }

    public LoginService getLoginService() {
        return this._loginService;
    }

    public void setLoginService(LoginService loginService) {
        this._loginService = loginService;
    }
}

