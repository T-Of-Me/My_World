/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.log;

public interface Logger {
    public String getName();

    public void warn(String var1, Object ... var2);

    public void warn(Throwable var1);

    public void warn(String var1, Throwable var2);

    public void info(String var1, Object ... var2);

    public void info(Throwable var1);

    public void info(String var1, Throwable var2);

    public boolean isDebugEnabled();

    public void setDebugEnabled(boolean var1);

    public void debug(String var1, Object ... var2);

    public void debug(String var1, long var2);

    public void debug(Throwable var1);

    public void debug(String var1, Throwable var2);

    public Logger getLogger(String var1);

    public void ignore(Throwable var1);
}

