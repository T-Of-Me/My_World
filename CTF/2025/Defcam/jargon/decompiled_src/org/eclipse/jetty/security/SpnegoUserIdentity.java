/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.security;

import java.security.Principal;
import java.util.List;
import javax.security.auth.Subject;
import org.eclipse.jetty.server.UserIdentity;

public class SpnegoUserIdentity
implements UserIdentity {
    private Subject _subject;
    private Principal _principal;
    private List<String> _roles;

    public SpnegoUserIdentity(Subject subject, Principal principal, List<String> roles) {
        this._subject = subject;
        this._principal = principal;
        this._roles = roles;
    }

    @Override
    public Subject getSubject() {
        return this._subject;
    }

    @Override
    public Principal getUserPrincipal() {
        return this._principal;
    }

    @Override
    public boolean isUserInRole(String role, UserIdentity.Scope scope) {
        return this._roles.contains(role);
    }
}

