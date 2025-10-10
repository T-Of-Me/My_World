/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.security;

import org.eclipse.jetty.security.AbstractUserAuthentication;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.server.UserIdentity;

public class UserAuthentication
extends AbstractUserAuthentication {
    public UserAuthentication(String method, UserIdentity userIdentity) {
        super(method, userIdentity);
    }

    public String toString() {
        return "{User," + this.getAuthMethod() + "," + this._userIdentity + "}";
    }

    @Override
    public void logout() {
        SecurityHandler security = SecurityHandler.getCurrentSecurityHandler();
        if (security != null) {
            security.logout(this);
        }
    }
}

