/*
 * Decompiled with CFR 0.152.
 */
package javax.servlet.http;

import javax.servlet.http.WebConnection;

public interface HttpUpgradeHandler {
    public void init(WebConnection var1);

    public void destroy();
}

