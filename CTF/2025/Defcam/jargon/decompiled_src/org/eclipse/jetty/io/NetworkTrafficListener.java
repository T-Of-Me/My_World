/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.io;

import java.net.Socket;
import java.nio.ByteBuffer;

public interface NetworkTrafficListener {
    public void opened(Socket var1);

    public void incoming(Socket var1, ByteBuffer var2);

    public void outgoing(Socket var1, ByteBuffer var2);

    public void closed(Socket var1);

    public static class Adapter
    implements NetworkTrafficListener {
        @Override
        public void opened(Socket socket) {
        }

        @Override
        public void incoming(Socket socket, ByteBuffer bytes) {
        }

        @Override
        public void outgoing(Socket socket, ByteBuffer bytes) {
        }

        @Override
        public void closed(Socket socket) {
        }
    }
}

