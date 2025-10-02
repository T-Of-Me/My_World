/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server;

import java.net.Socket;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.SocketChannelEndPoint;
import org.eclipse.jetty.io.ssl.SslConnection;

public class SocketCustomizationListener
implements Connection.Listener {
    private final boolean _ssl;

    public SocketCustomizationListener() {
        this(true);
    }

    public SocketCustomizationListener(boolean ssl) {
        this._ssl = ssl;
    }

    @Override
    public void onOpened(Connection connection) {
        EndPoint endp = connection.getEndPoint();
        boolean ssl = false;
        if (this._ssl && endp instanceof SslConnection.DecryptedEndPoint) {
            endp = ((SslConnection.DecryptedEndPoint)endp).getSslConnection().getEndPoint();
            ssl = true;
        }
        if (endp instanceof SocketChannelEndPoint) {
            Socket socket = ((SocketChannelEndPoint)endp).getSocket();
            this.customize(socket, connection.getClass(), ssl);
        }
    }

    protected void customize(Socket socket, Class<? extends Connection> connection, boolean ssl) {
    }

    @Override
    public void onClosed(Connection connection) {
    }
}

