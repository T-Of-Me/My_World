/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.security;

import java.io.Serializable;
import java.util.Arrays;

public class Constraint
implements Cloneable,
Serializable {
    public static final String __BASIC_AUTH = "BASIC";
    public static final String __FORM_AUTH = "FORM";
    public static final String __DIGEST_AUTH = "DIGEST";
    public static final String __CERT_AUTH = "CLIENT_CERT";
    public static final String __CERT_AUTH2 = "CLIENT-CERT";
    public static final String __SPNEGO_AUTH = "SPNEGO";
    public static final String __NEGOTIATE_AUTH = "NEGOTIATE";
    public static final int DC_UNSET = -1;
    public static final int DC_NONE = 0;
    public static final int DC_INTEGRAL = 1;
    public static final int DC_CONFIDENTIAL = 2;
    public static final int DC_FORBIDDEN = 3;
    public static final String NONE = "NONE";
    public static final String ANY_ROLE = "*";
    public static final String ANY_AUTH = "**";
    private String _name;
    private String[] _roles;
    private int _dataConstraint = -1;
    private boolean _anyRole = false;
    private boolean _anyAuth = false;
    private boolean _authenticate = false;

    public static boolean validateMethod(String method) {
        if (method == null) {
            return false;
        }
        return (method = method.trim()).equals(__FORM_AUTH) || method.equals(__BASIC_AUTH) || method.equals(__DIGEST_AUTH) || method.equals(__CERT_AUTH) || method.equals(__CERT_AUTH2) || method.equals(__SPNEGO_AUTH) || method.equals(__NEGOTIATE_AUTH);
    }

    public Constraint() {
    }

    public Constraint(String name, String role) {
        this.setName(name);
        this.setRoles(new String[]{role});
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public void setName(String name) {
        this._name = name;
    }

    public String getName() {
        return this._name;
    }

    public void setRoles(String[] roles) {
        this._roles = roles;
        this._anyRole = false;
        this._anyAuth = false;
        if (roles != null) {
            int i = roles.length;
            while (i-- > 0) {
                this._anyRole |= ANY_ROLE.equals(roles[i]);
                this._anyAuth |= ANY_AUTH.equals(roles[i]);
            }
        }
    }

    public boolean isAnyRole() {
        return this._anyRole;
    }

    public boolean isAnyAuth() {
        return this._anyAuth;
    }

    public String[] getRoles() {
        return this._roles;
    }

    public boolean hasRole(String role) {
        if (this._anyRole) {
            return true;
        }
        if (this._roles != null) {
            int i = this._roles.length;
            while (i-- > 0) {
                if (!role.equals(this._roles[i])) continue;
                return true;
            }
        }
        return false;
    }

    public void setAuthenticate(boolean authenticate) {
        this._authenticate = authenticate;
    }

    public boolean getAuthenticate() {
        return this._authenticate;
    }

    public boolean isForbidden() {
        return this._authenticate && !this._anyRole && (this._roles == null || this._roles.length == 0);
    }

    public void setDataConstraint(int c) {
        if (c < 0 || c > 2) {
            throw new IllegalArgumentException("Constraint out of range");
        }
        this._dataConstraint = c;
    }

    public int getDataConstraint() {
        return this._dataConstraint;
    }

    public boolean hasDataConstraint() {
        return this._dataConstraint >= 0;
    }

    public String toString() {
        return "SC{" + this._name + "," + (this._anyRole ? ANY_ROLE : (this._roles == null ? "-" : Arrays.asList(this._roles).toString())) + "," + (this._dataConstraint == -1 ? "DC_UNSET}" : (this._dataConstraint == 0 ? "NONE}" : (this._dataConstraint == 1 ? "INTEGRAL}" : "CONFIDENTIAL}")));
    }
}

