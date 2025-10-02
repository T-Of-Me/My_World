/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.security;

public enum UserDataConstraint {
    None,
    Integral,
    Confidential;


    public static UserDataConstraint get(int dataConstraint) {
        if (dataConstraint < -1 || dataConstraint > 2) {
            throw new IllegalArgumentException("Expected -1, 0, 1, or 2, not: " + dataConstraint);
        }
        if (dataConstraint == -1) {
            return None;
        }
        return UserDataConstraint.values()[dataConstraint];
    }

    public UserDataConstraint combine(UserDataConstraint other) {
        if (this.compareTo(other) < 0) {
            return this;
        }
        return other;
    }
}

