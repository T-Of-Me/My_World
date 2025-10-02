/*
 * Decompiled with CFR 0.152.
 */
package javax.servlet;

import java.util.EventListener;
import javax.servlet.ServletContextAttributeEvent;

public interface ServletContextAttributeListener
extends EventListener {
    public void attributeAdded(ServletContextAttributeEvent var1);

    public void attributeRemoved(ServletContextAttributeEvent var1);

    public void attributeReplaced(ServletContextAttributeEvent var1);
}

