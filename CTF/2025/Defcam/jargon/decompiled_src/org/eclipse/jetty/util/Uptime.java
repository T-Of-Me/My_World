/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Uptime {
    public static final int NOIMPL = -1;
    private static final Uptime INSTANCE = new Uptime();
    private Impl impl;

    public static Uptime getInstance() {
        return INSTANCE;
    }

    private Uptime() {
        try {
            this.impl = new DefaultImpl();
        }
        catch (UnsupportedOperationException e) {
            System.err.printf("Defaulting Uptime to NOIMPL due to (%s) %s%n", e.getClass().getName(), e.getMessage());
            this.impl = null;
        }
    }

    public Impl getImpl() {
        return this.impl;
    }

    public void setImpl(Impl impl) {
        this.impl = impl;
    }

    public static long getUptime() {
        Uptime u = Uptime.getInstance();
        if (u == null || u.impl == null) {
            return -1L;
        }
        return u.impl.getUptime();
    }

    public static class DefaultImpl
    implements Impl {
        public Object mxBean;
        public Method uptimeMethod;

        public DefaultImpl() {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            try {
                Class<?> mgmtFactory = Class.forName("java.lang.management.ManagementFactory", true, cl);
                Class<?> runtimeClass = Class.forName("java.lang.management.RuntimeMXBean", true, cl);
                Class[] noparams = new Class[]{};
                Method mxBeanMethod = mgmtFactory.getMethod("getRuntimeMXBean", noparams);
                if (mxBeanMethod == null) {
                    throw new UnsupportedOperationException("method getRuntimeMXBean() not found");
                }
                this.mxBean = mxBeanMethod.invoke(mgmtFactory, new Object[0]);
                if (this.mxBean == null) {
                    throw new UnsupportedOperationException("getRuntimeMXBean() method returned null");
                }
                this.uptimeMethod = runtimeClass.getMethod("getUptime", noparams);
                if (this.mxBean == null) {
                    throw new UnsupportedOperationException("method getUptime() not found");
                }
            }
            catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException | NoClassDefFoundError | NoSuchMethodException | SecurityException | InvocationTargetException e) {
                throw new UnsupportedOperationException("Implementation not available in this environment", e);
            }
        }

        @Override
        public long getUptime() {
            try {
                return (Long)this.uptimeMethod.invoke(this.mxBean, new Object[0]);
            }
            catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                return -1L;
            }
        }
    }

    public static interface Impl {
        public long getUptime();
    }
}

