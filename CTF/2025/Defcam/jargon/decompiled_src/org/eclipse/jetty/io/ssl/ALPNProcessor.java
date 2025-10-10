/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.io.ssl;

import java.util.List;
import javax.net.ssl.SSLEngine;

public interface ALPNProcessor {

    public static interface Client {
        public static final Client NOOP = new Client(){};

        default public void configure(SSLEngine sslEngine, List<String> protocols) {
        }

        default public void process(SSLEngine sslEngine) {
        }
    }

    public static interface Server {
        public static final Server NOOP = new Server(){};

        default public void configure(SSLEngine sslEngine) {
        }
    }
}

