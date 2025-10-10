/*
 * Decompiled with CFR 0.152.
 */
package javax.servlet;

import java.util.EventListener;
import javax.servlet.ServletRequestEvent;

public interface ServletRequestListener
extends EventListener {
    public void requestDestroyed(ServletRequestEvent var1);

    public void requestInitialized(ServletRequestEvent var1);
}

