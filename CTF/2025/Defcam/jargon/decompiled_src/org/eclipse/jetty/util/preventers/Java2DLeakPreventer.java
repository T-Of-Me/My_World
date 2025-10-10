/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.preventers;

import org.eclipse.jetty.util.preventers.AbstractLeakPreventer;

public class Java2DLeakPreventer
extends AbstractLeakPreventer {
    @Override
    public void prevent(ClassLoader loader) {
        try {
            Class.forName("sun.java2d.Disposer", true, loader);
        }
        catch (ClassNotFoundException e) {
            LOG.ignore(e);
        }
    }
}

