/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util;

public interface Decorator {
    public <T> T decorate(T var1);

    public void destroy(Object var1);
}

