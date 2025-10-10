/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.io;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import org.eclipse.jetty.io.ManagedSelector;
import org.eclipse.jetty.io.SocketChannelEndPoint;
import org.eclipse.jetty.util.thread.Scheduler;

@Deprecated
public class SelectChannelEndPoint
extends SocketChannelEndPoint {
    public SelectChannelEndPoint(SelectableChannel channel, ManagedSelector selector, SelectionKey key, Scheduler scheduler, long idleTimeout) {
        super(channel, selector, key, scheduler);
        this.setIdleTimeout(idleTimeout);
    }
}

