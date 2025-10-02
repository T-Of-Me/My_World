/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.io;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.List;
import org.eclipse.jetty.io.ManagedSelector;
import org.eclipse.jetty.io.NetworkTrafficListener;
import org.eclipse.jetty.io.SelectChannelEndPoint;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.thread.Scheduler;

public class NetworkTrafficSelectChannelEndPoint
extends SelectChannelEndPoint {
    private static final Logger LOG = Log.getLogger(NetworkTrafficSelectChannelEndPoint.class);
    private final List<NetworkTrafficListener> listeners;

    public NetworkTrafficSelectChannelEndPoint(SocketChannel channel, ManagedSelector selectSet, SelectionKey key, Scheduler scheduler, long idleTimeout, List<NetworkTrafficListener> listeners) throws IOException {
        super(channel, selectSet, key, scheduler, idleTimeout);
        this.listeners = listeners;
    }

    @Override
    public int fill(ByteBuffer buffer) throws IOException {
        int read = super.fill(buffer);
        this.notifyIncoming(buffer, read);
        return read;
    }

    @Override
    public boolean flush(ByteBuffer ... buffers) throws IOException {
        boolean flushed = true;
        for (ByteBuffer b : buffers) {
            if (!b.hasRemaining()) continue;
            int position = b.position();
            ByteBuffer view = b.slice();
            ByteBuffer[] byteBufferArray = new ByteBuffer[]{b};
            int l = b.position() - position;
            view.limit(view.position() + l);
            this.notifyOutgoing(view);
            if (!(flushed &= super.flush(byteBufferArray))) break;
        }
        return flushed;
    }

    @Override
    public void onOpen() {
        super.onOpen();
        if (this.listeners != null && !this.listeners.isEmpty()) {
            for (NetworkTrafficListener listener : this.listeners) {
                try {
                    listener.opened(this.getSocket());
                }
                catch (Exception x) {
                    LOG.warn(x);
                }
            }
        }
    }

    @Override
    public void onClose() {
        super.onClose();
        if (this.listeners != null && !this.listeners.isEmpty()) {
            for (NetworkTrafficListener listener : this.listeners) {
                try {
                    listener.closed(this.getSocket());
                }
                catch (Exception x) {
                    LOG.warn(x);
                }
            }
        }
    }

    public void notifyIncoming(ByteBuffer buffer, int read) {
        if (this.listeners != null && !this.listeners.isEmpty() && read > 0) {
            for (NetworkTrafficListener listener : this.listeners) {
                try {
                    ByteBuffer view = buffer.asReadOnlyBuffer();
                    listener.incoming(this.getSocket(), view);
                }
                catch (Exception x) {
                    LOG.warn(x);
                }
            }
        }
    }

    public void notifyOutgoing(ByteBuffer view) {
        if (this.listeners != null && !this.listeners.isEmpty() && view.hasRemaining()) {
            Socket socket = this.getSocket();
            for (NetworkTrafficListener listener : this.listeners) {
                try {
                    listener.outgoing(socket, view);
                }
                catch (Exception x) {
                    LOG.warn(x);
                }
            }
        }
    }
}

