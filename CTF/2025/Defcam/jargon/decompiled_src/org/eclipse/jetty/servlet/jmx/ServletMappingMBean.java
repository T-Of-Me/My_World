/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.eclipse.jetty.jmx.ObjectMBean
 */
package org.eclipse.jetty.servlet.jmx;

import org.eclipse.jetty.jmx.ObjectMBean;
import org.eclipse.jetty.servlet.ServletMapping;

public class ServletMappingMBean
extends ObjectMBean {
    public ServletMappingMBean(Object managedObject) {
        super(managedObject);
    }

    public String getObjectNameBasis() {
        ServletMapping mapping;
        String name;
        if (this._managed != null && this._managed instanceof ServletMapping && (name = (mapping = (ServletMapping)this._managed).getServletName()) != null) {
            return name;
        }
        return super.getObjectNameBasis();
    }
}

