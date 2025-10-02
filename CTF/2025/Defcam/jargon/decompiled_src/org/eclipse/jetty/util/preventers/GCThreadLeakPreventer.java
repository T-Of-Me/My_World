/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.preventers;

import java.lang.reflect.Method;
import org.eclipse.jetty.util.preventers.AbstractLeakPreventer;

public class GCThreadLeakPreventer
extends AbstractLeakPreventer {
    @Override
    public void prevent(ClassLoader loader) {
        try {
            Class<?> clazz = Class.forName("sun.misc.GC");
            Method requestLatency = clazz.getMethod("requestLatency", Long.TYPE);
            requestLatency.invoke(null, 0x7FFFFFFFFFFFFFFEL);
        }
        catch (ClassNotFoundException e) {
            LOG.ignore(e);
        }
        catch (Exception e) {
            LOG.warn(e);
        }
    }
}

