/*
 * Decompiled with CFR 0.152.
 */
package javax.servlet;

import java.util.EventListener;
import javax.servlet.ServletContextEvent;

public interface ServletContextListener
extends EventListener {
    public void contextInitialized(ServletContextEvent var1);

    public void contextDestroyed(ServletContextEvent var1);
}

