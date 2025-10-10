/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.io;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ReadPendingException;
import java.nio.channels.WritePendingException;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.util.Callback;

public interface EndPoint
extends Closeable {
    public InetSocketAddress getLocalAddress();

    public InetSocketAddress getRemoteAddress();

    public boolean isOpen();

    public long getCreatedTimeStamp();

    public void shutdownOutput();

    public boolean isOutputShutdown();

    public boolean isInputShutdown();

    @Override
    public void close();

    public int fill(ByteBuffer var1) throws IOException;

    public boolean flush(ByteBuffer ... var1) throws IOException;

    public Object getTransport();

    public long getIdleTimeout();

    public void setIdleTimeout(long var1);

    public void fillInterested(Callback var1) throws ReadPendingException;

    public boolean tryFillInterested(Callback var1);

    public boolean isFillInterested();

    public void write(Callback var1, ByteBuffer ... var2) throws WritePendingException;

    public Connection getConnection();

    public void setConnection(Connection var1);

    public void onOpen();

    public void onClose();

    public boolean isOptimizedForDirectBuffers();

    public void upgrade(Connection var1);
}

