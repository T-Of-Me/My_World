/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.security;

import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Server;

public interface Authenticator {
    public void setConfiguration(AuthConfiguration var1);

    public String getAuthMethod();

    public void prepareRequest(ServletRequest var1);

    public Authentication validateRequest(ServletRequest var1, ServletResponse var2, boolean var3) throws ServerAuthException;

    public boolean secureResponse(ServletRequest var1, ServletResponse var2, boolean var3, Authentication.User var4) throws ServerAuthException;

    public static interface Factory {
        public Authenticator getAuthenticator(Server var1, ServletContext var2, AuthConfiguration var3, IdentityService var4, LoginService var5);
    }

    public static interface AuthConfiguration {
        public String getAuthMethod();

        public String getRealmName();

        public String getInitParameter(String var1);

        public Set<String> getInitParameterNames();

        public LoginService getLoginService();

        public IdentityService getIdentityService();

        public boolean isSessionRenewedOnAuthentication();
    }
}

