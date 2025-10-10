/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.io;

import org.eclipse.jetty.io.ClientConnectionFactory;

public abstract class NegotiatingClientConnectionFactory
implements ClientConnectionFactory {
    private final ClientConnectionFactory connectionFactory;

    protected NegotiatingClientConnectionFactory(ClientConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public ClientConnectionFactory getClientConnectionFactory() {
        return this.connectionFactory;
    }
}

