/*
 * Decompiled with CFR 0.152.
 */
package javax.servlet;

import java.util.EventListener;
import javax.servlet.ServletRequestAttributeEvent;

public interface ServletRequestAttributeListener
extends EventListener {
    public void attributeAdded(ServletRequestAttributeEvent var1);

    public void attributeRemoved(ServletRequestAttributeEvent var1);

    public void attributeReplaced(ServletRequestAttributeEvent var1);
}

