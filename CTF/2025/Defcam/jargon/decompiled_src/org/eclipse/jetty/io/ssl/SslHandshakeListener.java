/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.io.ssl;

import java.util.EventListener;
import java.util.EventObject;
import javax.net.ssl.SSLEngine;

public interface SslHandshakeListener
extends EventListener {
    default public void handshakeSucceeded(Event event) {
    }

    default public void handshakeFailed(Event event, Throwable failure) {
    }

    public static class Event
    extends EventObject {
        public Event(Object source) {
            super(source);
        }

        public SSLEngine getSSLEngine() {
            return (SSLEngine)this.getSource();
        }
    }
}

