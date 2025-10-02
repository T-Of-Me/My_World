/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.handler.jmx;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.jmx.AbstractHandlerMBean;
import org.eclipse.jetty.util.Attributes;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.annotation.ManagedOperation;
import org.eclipse.jetty.util.annotation.Name;

@ManagedObject(value="ContextHandler mbean wrapper")
public class ContextHandlerMBean
extends AbstractHandlerMBean {
    public ContextHandlerMBean(Object managedObject) {
        super(managedObject);
    }

    @ManagedAttribute(value="Map of context attributes")
    public Map<String, Object> getContextAttributes() {
        HashMap<String, Object> map = new HashMap<String, Object>();
        Attributes attrs = ((ContextHandler)this._managed).getAttributes();
        Enumeration<String> en = attrs.getAttributeNames();
        while (en.hasMoreElements()) {
            String name = en.nextElement();
            Object value = attrs.getAttribute(name);
            map.put(name, value);
        }
        return map;
    }

    @ManagedOperation(value="Set context attribute", impact="ACTION")
    public void setContextAttribute(@Name(value="name", description="attribute name") String name, @Name(value="value", description="attribute value") Object value) {
        Attributes attrs = ((ContextHandler)this._managed).getAttributes();
        attrs.setAttribute(name, value);
    }

    @ManagedOperation(value="Set context attribute", impact="ACTION")
    public void setContextAttribute(@Name(value="name", description="attribute name") String name, @Name(value="value", description="attribute value") String value) {
        Attributes attrs = ((ContextHandler)this._managed).getAttributes();
        attrs.setAttribute(name, value);
    }

    @ManagedOperation(value="Remove context attribute", impact="ACTION")
    public void removeContextAttribute(@Name(value="name", description="attribute name") String name) {
        Attributes attrs = ((ContextHandler)this._managed).getAttributes();
        attrs.removeAttribute(name);
    }
}

