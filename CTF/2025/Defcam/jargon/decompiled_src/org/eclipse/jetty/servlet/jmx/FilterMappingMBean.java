/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.eclipse.jetty.jmx.ObjectMBean
 */
package org.eclipse.jetty.servlet.jmx;

import org.eclipse.jetty.jmx.ObjectMBean;
import org.eclipse.jetty.servlet.FilterMapping;

public class FilterMappingMBean
extends ObjectMBean {
    public FilterMappingMBean(Object managedObject) {
        super(managedObject);
    }

    public String getObjectNameBasis() {
        FilterMapping mapping;
        String name;
        if (this._managed != null && this._managed instanceof FilterMapping && (name = (mapping = (FilterMapping)this._managed).getFilterName()) != null) {
            return name;
        }
        return super.getObjectNameBasis();
    }
}

