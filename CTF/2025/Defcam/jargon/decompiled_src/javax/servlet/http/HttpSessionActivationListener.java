/*
 * Decompiled with CFR 0.152.
 */
package javax.servlet.http;

import java.util.EventListener;
import javax.servlet.http.HttpSessionEvent;

public interface HttpSessionActivationListener
extends EventListener {
    public void sessionWillPassivate(HttpSessionEvent var1);

    public void sessionDidActivate(HttpSessionEvent var1);
}

