/*
 * Decompiled with CFR 0.152.
 */
package javax.servlet.http;

import java.util.EventListener;
import javax.servlet.http.HttpSessionEvent;

public interface HttpSessionIdListener
extends EventListener {
    public void sessionIdChanged(HttpSessionEvent var1, String var2);
}

