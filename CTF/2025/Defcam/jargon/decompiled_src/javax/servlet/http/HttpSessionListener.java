/*
 * Decompiled with CFR 0.152.
 */
package javax.servlet.http;

import java.util.EventListener;
import javax.servlet.http.HttpSessionEvent;

public interface HttpSessionListener
extends EventListener {
    public void sessionCreated(HttpSessionEvent var1);

    public void sessionDestroyed(HttpSessionEvent var1);
}

