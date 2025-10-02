/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.preventers;

import java.sql.DriverManager;
import org.eclipse.jetty.util.preventers.AbstractLeakPreventer;

public class DriverManagerLeakPreventer
extends AbstractLeakPreventer {
    @Override
    public void prevent(ClassLoader loader) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Pinning DriverManager classloader with " + loader, new Object[0]);
        }
        DriverManager.getDrivers();
    }
}

