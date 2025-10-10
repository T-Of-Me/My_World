/*
 * Decompiled with CFR 0.152.
 */
package javax.servlet.http;

import java.util.EventListener;
import javax.servlet.http.HttpSessionBindingEvent;

public interface HttpSessionBindingListener
extends EventListener {
    public void valueBound(HttpSessionBindingEvent var1);

    public void valueUnbound(HttpSessionBindingEvent var1);
}

