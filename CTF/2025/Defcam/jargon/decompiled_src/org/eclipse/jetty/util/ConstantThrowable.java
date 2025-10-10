/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util;

public class ConstantThrowable
extends Throwable {
    public ConstantThrowable() {
        this((String)null);
    }

    public ConstantThrowable(String name) {
        super(name, null, false, false);
    }

    @Override
    public String toString() {
        return String.valueOf(this.getMessage());
    }
}

