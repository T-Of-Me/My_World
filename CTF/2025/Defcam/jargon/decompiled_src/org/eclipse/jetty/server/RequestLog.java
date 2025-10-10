/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

public interface RequestLog {
    public void log(Request var1, Response var2);
}

