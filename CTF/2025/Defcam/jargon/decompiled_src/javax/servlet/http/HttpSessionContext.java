/*
 * Decompiled with CFR 0.152.
 */
package javax.servlet.http;

import java.util.Enumeration;
import javax.servlet.http.HttpSession;

public interface HttpSessionContext {
    public HttpSession getSession(String var1);

    public Enumeration<String> getIds();
}

