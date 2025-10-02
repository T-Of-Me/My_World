/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.security;

import java.io.Serializable;
import java.security.Principal;
import javax.security.auth.Subject;
import javax.servlet.ServletRequest;
import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.security.Credential;

public abstract class AbstractLoginService
extends AbstractLifeCycle
implements LoginService {
    private static final Logger LOG = Log.getLogger(AbstractLoginService.class);
    protected IdentityService _identityService = new DefaultIdentityService();
    protected String _name;
    protected boolean _fullValidate = false;

    protected abstract String[] loadRoleInfo(UserPrincipal var1);

    protected abstract UserPrincipal loadUserInfo(String var1);

    @Override
    public String getName() {
        return this._name;
    }

    @Override
    public void setIdentityService(IdentityService identityService) {
        if (this.isRunning()) {
            throw new IllegalStateException("Running");
        }
        this._identityService = identityService;
    }

    public void setName(String name) {
        if (this.isRunning()) {
            throw new IllegalStateException("Running");
        }
        this._name = name;
    }

    public String toString() {
        return this.getClass().getSimpleName() + "[" + this._name + "]";
    }

    @Override
    public UserIdentity login(String username, Object credentials, ServletRequest request) {
        if (username == null) {
            return null;
        }
        UserPrincipal userPrincipal = this.loadUserInfo(username);
        if (userPrincipal != null && userPrincipal.authenticate(credentials)) {
            String[] roles = this.loadRoleInfo(userPrincipal);
            Subject subject = new Subject();
            subject.getPrincipals().add(userPrincipal);
            subject.getPrivateCredentials().add(userPrincipal._credential);
            if (roles != null) {
                for (String role : roles) {
                    subject.getPrincipals().add(new RolePrincipal(role));
                }
            }
            subject.setReadOnly();
            return this._identityService.newUserIdentity(subject, userPrincipal, roles);
        }
        return null;
    }

    @Override
    public boolean validate(UserIdentity user) {
        if (!this.isFullValidate()) {
            return true;
        }
        UserPrincipal fresh = this.loadUserInfo(user.getUserPrincipal().getName());
        if (fresh == null) {
            return false;
        }
        if (user.getUserPrincipal() instanceof UserPrincipal) {
            System.err.println("VALIDATING user " + fresh.getName());
            return fresh.authenticate(((UserPrincipal)user.getUserPrincipal())._credential);
        }
        throw new IllegalStateException("UserPrincipal not KnownUser");
    }

    @Override
    public IdentityService getIdentityService() {
        return this._identityService;
    }

    @Override
    public void logout(UserIdentity user) {
    }

    public boolean isFullValidate() {
        return this._fullValidate;
    }

    public void setFullValidate(boolean fullValidate) {
        this._fullValidate = fullValidate;
    }

    public static class UserPrincipal
    implements Principal,
    Serializable {
        private static final long serialVersionUID = -6226920753748399662L;
        private final String _name;
        private final Credential _credential;

        public UserPrincipal(String name, Credential credential) {
            this._name = name;
            this._credential = credential;
        }

        public boolean authenticate(Object credentials) {
            return this._credential != null && this._credential.check(credentials);
        }

        public boolean authenticate(Credential c) {
            return this._credential != null && c != null && this._credential.equals(c);
        }

        @Override
        public String getName() {
            return this._name;
        }

        @Override
        public String toString() {
            return this._name;
        }
    }

    public static class RolePrincipal
    implements Principal,
    Serializable {
        private static final long serialVersionUID = 2998397924051854402L;
        private final String _roleName;

        public RolePrincipal(String name) {
            this._roleName = name;
        }

        @Override
        public String getName() {
            return this._roleName;
        }
    }
}

