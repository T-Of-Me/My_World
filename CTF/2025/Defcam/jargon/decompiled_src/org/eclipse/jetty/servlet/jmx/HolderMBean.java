/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.eclipse.jetty.jmx.ObjectMBean
 */
package org.eclipse.jetty.servlet.jmx;

import org.eclipse.jetty.jmx.ObjectMBean;
import org.eclipse.jetty.servlet.Holder;

public class HolderMBean
extends ObjectMBean {
    public HolderMBean(Object managedObject) {
        super(managedObject);
    }

    public String getObjectNameBasis() {
        Holder holder;
        String name;
        if (this._managed != null && this._managed instanceof Holder && (name = (holder = (Holder)this._managed).getName()) != null) {
            return name;
        }
        return super.getObjectNameBasis();
    }
}

