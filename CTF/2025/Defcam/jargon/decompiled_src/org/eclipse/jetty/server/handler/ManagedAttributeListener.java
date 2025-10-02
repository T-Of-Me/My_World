/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.handler;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class ManagedAttributeListener
implements ServletContextListener,
ServletContextAttributeListener {
    private static final Logger LOG = Log.getLogger(ManagedAttributeListener.class);
    final Set<String> _managedAttributes = new HashSet<String>();
    final ContextHandler _context;

    public ManagedAttributeListener(ContextHandler context, String ... managedAttributes) {
        this._context = context;
        for (String attr : managedAttributes) {
            this._managedAttributes.add(attr);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("managedAttributes {}", this._managedAttributes);
        }
    }

    @Override
    public void attributeReplaced(ServletContextAttributeEvent event) {
        if (this._managedAttributes.contains(event.getName())) {
            this.updateBean(event.getName(), event.getValue(), event.getServletContext().getAttribute(event.getName()));
        }
    }

    @Override
    public void attributeRemoved(ServletContextAttributeEvent event) {
        if (this._managedAttributes.contains(event.getName())) {
            this.updateBean(event.getName(), event.getValue(), null);
        }
    }

    @Override
    public void attributeAdded(ServletContextAttributeEvent event) {
        if (this._managedAttributes.contains(event.getName())) {
            this.updateBean(event.getName(), null, event.getValue());
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent event) {
        Enumeration<String> e = event.getServletContext().getAttributeNames();
        while (e.hasMoreElements()) {
            String name = e.nextElement();
            if (!this._managedAttributes.contains(name)) continue;
            this.updateBean(name, null, event.getServletContext().getAttribute(name));
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        Enumeration<String> e = this._context.getServletContext().getAttributeNames();
        while (e.hasMoreElements()) {
            String name = e.nextElement();
            if (!this._managedAttributes.contains(name)) continue;
            this.updateBean(name, event.getServletContext().getAttribute(name), null);
        }
    }

    protected void updateBean(String name, Object oldBean, Object newBean) {
        LOG.info("update {} {}->{} on {}", name, oldBean, newBean, this._context);
        if (LOG.isDebugEnabled()) {
            LOG.debug("update {} {}->{} on {}", name, oldBean, newBean, this._context);
        }
        this._context.updateBean(oldBean, newBean, false);
    }
}

