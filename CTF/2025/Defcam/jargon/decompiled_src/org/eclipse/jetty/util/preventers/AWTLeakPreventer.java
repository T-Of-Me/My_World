/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.preventers;

import java.awt.Toolkit;
import org.eclipse.jetty.util.preventers.AbstractLeakPreventer;

public class AWTLeakPreventer
extends AbstractLeakPreventer {
    @Override
    public void prevent(ClassLoader loader) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Pinning classloader for java.awt.EventQueue using " + loader, new Object[0]);
        }
        Toolkit.getDefaultToolkit();
    }
}

