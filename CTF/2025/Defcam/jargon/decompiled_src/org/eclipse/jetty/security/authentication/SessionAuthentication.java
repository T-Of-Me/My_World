/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.security.authentication;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionEvent;
import org.eclipse.jetty.security.AbstractUserAuthentication;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class SessionAuthentication
extends AbstractUserAuthentication
implements Serializable,
HttpSessionActivationListener,
HttpSessionBindingListener {
    private static final Logger LOG = Log.getLogger(SessionAuthentication.class);
    private static final long serialVersionUID = -4643200685888258706L;
    public static final String __J_AUTHENTICATED = "org.eclipse.jetty.security.UserIdentity";
    private final String _name;
    private final Object _credentials;
    private transient HttpSession _session;

    public SessionAuthentication(String method, UserIdentity userIdentity, Object credentials) {
        super(method, userIdentity);
        this._name = userIdentity.getUserPrincipal().getName();
        this._credentials = credentials;
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        SecurityHandler security = SecurityHandler.getCurrentSecurityHandler();
        if (security == null) {
            throw new IllegalStateException("!SecurityHandler");
        }
        LoginService login_service = security.getLoginService();
        if (login_service == null) {
            throw new IllegalStateException("!LoginService");
        }
        this._userIdentity = login_service.login(this._name, this._credentials, null);
        LOG.debug("Deserialized and relogged in {}", this);
    }

    @Override
    public void logout() {
        if (this._session != null && this._session.getAttribute(__J_AUTHENTICATED) != null) {
            this._session.removeAttribute(__J_AUTHENTICATED);
        }
        this.doLogout();
    }

    private void doLogout() {
        SecurityHandler security = SecurityHandler.getCurrentSecurityHandler();
        if (security != null) {
            security.logout(this);
        }
        if (this._session != null) {
            this._session.removeAttribute("org.eclipse.jetty.security.sessionCreatedSecure");
        }
    }

    public String toString() {
        return String.format("%s@%x{%s,%s}", this.getClass().getSimpleName(), this.hashCode(), this._session == null ? "-" : this._session.getId(), this._userIdentity);
    }

    @Override
    public void sessionWillPassivate(HttpSessionEvent se) {
    }

    @Override
    public void sessionDidActivate(HttpSessionEvent se) {
        if (this._session == null) {
            this._session = se.getSession();
        }
    }

    @Override
    public void valueBound(HttpSessionBindingEvent event) {
        if (this._session == null) {
            this._session = event.getSession();
        }
    }

    @Override
    public void valueUnbound(HttpSessionBindingEvent event) {
        this.doLogout();
    }
}

