/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server;

import java.nio.ByteBuffer;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.util.Callback;

public interface HttpTransport {
    public void send(MetaData.Response var1, boolean var2, ByteBuffer var3, boolean var4, Callback var5);

    public boolean isPushSupported();

    public void push(MetaData.Request var1);

    public void onCompleted();

    public void abort(Throwable var1);

    public boolean isOptimizedForDirectBuffers();
}

