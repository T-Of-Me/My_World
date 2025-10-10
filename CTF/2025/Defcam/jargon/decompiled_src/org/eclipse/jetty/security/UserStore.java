/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.security.auth.Subject;
import org.eclipse.jetty.security.AbstractLoginService;
import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.security.Credential;

public class UserStore
extends AbstractLifeCycle {
    private IdentityService _identityService = new DefaultIdentityService();
    private final Map<String, UserIdentity> _knownUserIdentities = new ConcurrentHashMap<String, UserIdentity>();

    public void addUser(String username, Credential credential, String[] roles) {
        AbstractLoginService.UserPrincipal userPrincipal = new AbstractLoginService.UserPrincipal(username, credential);
        Subject subject = new Subject();
        subject.getPrincipals().add(userPrincipal);
        subject.getPrivateCredentials().add(credential);
        if (roles != null) {
            for (String role : roles) {
                subject.getPrincipals().add(new AbstractLoginService.RolePrincipal(role));
            }
        }
        subject.setReadOnly();
        this._knownUserIdentities.put(username, this._identityService.newUserIdentity(subject, userPrincipal, roles));
    }

    public void removeUser(String username) {
        this._knownUserIdentities.remove(username);
    }

    public UserIdentity getUserIdentity(String userName) {
        return this._knownUserIdentities.get(userName);
    }

    public IdentityService getIdentityService() {
        return this._identityService;
    }

    public Map<String, UserIdentity> getKnownUserIdentities() {
        return this._knownUserIdentities;
    }
}

