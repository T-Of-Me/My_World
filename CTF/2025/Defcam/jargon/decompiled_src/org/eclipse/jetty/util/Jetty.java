/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util;

public class Jetty {
    public static final String VERSION;
    public static final String POWERED_BY;
    public static final boolean STABLE;

    private Jetty() {
    }

    static {
        Package pkg = Jetty.class.getPackage();
        VERSION = pkg != null && "Eclipse.org - Jetty".equals(pkg.getImplementationVendor()) && pkg.getImplementationVersion() != null ? pkg.getImplementationVersion() : System.getProperty("jetty.version", "9.4.z-SNAPSHOT");
        POWERED_BY = "<a href=\"http://eclipse.org/jetty\">Powered by Jetty:// " + VERSION + "</a>";
        STABLE = !VERSION.matches("^.*\\.(RC|M)[0-9]+$");
    }
}

