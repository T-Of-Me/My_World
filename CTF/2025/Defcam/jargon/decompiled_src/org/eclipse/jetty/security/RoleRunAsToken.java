/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.security;

import org.eclipse.jetty.security.RunAsToken;

public class RoleRunAsToken
implements RunAsToken {
    private final String _runAsRole;

    public RoleRunAsToken(String runAsRole) {
        this._runAsRole = runAsRole;
    }

    public String getRunAsRole() {
        return this._runAsRole;
    }

    public String toString() {
        return "RoleRunAsToken(" + this._runAsRole + ")";
    }
}

