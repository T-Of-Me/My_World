/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util;

import java.security.AccessController;
import java.security.PrivilegedAction;

public class MemoryUtils {
    private static final int cacheLineBytes;

    private MemoryUtils() {
    }

    public static int getCacheLineBytes() {
        return cacheLineBytes;
    }

    public static int getIntegersPerCacheLine() {
        return MemoryUtils.getCacheLineBytes() >> 2;
    }

    public static int getLongsPerCacheLine() {
        return MemoryUtils.getCacheLineBytes() >> 3;
    }

    static {
        int defaultValue = 64;
        int value = 64;
        try {
            value = Integer.parseInt(AccessController.doPrivileged(new PrivilegedAction<String>(){

                @Override
                public String run() {
                    return System.getProperty("org.eclipse.jetty.util.cacheLineBytes", String.valueOf(64));
                }
            }));
        }
        catch (Exception exception) {
            // empty catch block
        }
        cacheLineBytes = value;
    }
}

