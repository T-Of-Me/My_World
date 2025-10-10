/*
 * Decompiled with CFR 0.152.
 */
package javax.servlet.http;

import java.util.EventListener;
import javax.servlet.http.HttpSessionBindingEvent;

public interface HttpSessionAttributeListener
extends EventListener {
    public void attributeAdded(HttpSessionBindingEvent var1);

    public void attributeRemoved(HttpSessionBindingEvent var1);

    public void attributeReplaced(HttpSessionBindingEvent var1);
}

