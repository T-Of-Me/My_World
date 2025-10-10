/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util;

import java.util.Enumeration;

public interface Attributes {
    public void removeAttribute(String var1);

    public void setAttribute(String var1, Object var2);

    public Object getAttribute(String var1);

    public Enumeration<String> getAttributeNames();

    public void clearAttributes();
}

