/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.security;

import java.security.Principal;
import javax.security.auth.Subject;
import org.eclipse.jetty.security.DefaultUserIdentity;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.RoleRunAsToken;
import org.eclipse.jetty.security.RunAsToken;
import org.eclipse.jetty.server.UserIdentity;

public class DefaultIdentityService
implements IdentityService {
    @Override
    public Object associate(UserIdentity user) {
        return null;
    }

    @Override
    public void disassociate(Object previous) {
    }

    @Override
    public Object setRunAs(UserIdentity user, RunAsToken token) {
        return token;
    }

    @Override
    public void unsetRunAs(Object lastToken) {
    }

    @Override
    public RunAsToken newRunAsToken(String runAsName) {
        return new RoleRunAsToken(runAsName);
    }

    @Override
    public UserIdentity getSystemUserIdentity() {
        return null;
    }

    @Override
    public UserIdentity newUserIdentity(Subject subject, Principal userPrincipal, String[] roles) {
        return new DefaultUserIdentity(subject, userPrincipal, roles);
    }
}

