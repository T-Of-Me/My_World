/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server;

import java.security.Principal;
import java.util.Map;
import javax.security.auth.Subject;

public interface UserIdentity {
    public static final UserIdentity UNAUTHENTICATED_IDENTITY = new UnauthenticatedUserIdentity(){

        @Override
        public Subject getSubject() {
            return null;
        }

        @Override
        public Principal getUserPrincipal() {
            return null;
        }

        @Override
        public boolean isUserInRole(String role, Scope scope) {
            return false;
        }

        public String toString() {
            return "UNAUTHENTICATED";
        }
    };

    public Subject getSubject();

    public Principal getUserPrincipal();

    public boolean isUserInRole(String var1, Scope var2);

    public static interface UnauthenticatedUserIdentity
    extends UserIdentity {
    }

    public static interface Scope {
        public String getContextPath();

        public String getName();

        public Map<String, String> getRoleRefMap();
    }
}

