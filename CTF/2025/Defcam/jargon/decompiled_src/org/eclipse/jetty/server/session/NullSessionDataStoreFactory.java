/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.session;

import org.eclipse.jetty.server.session.AbstractSessionDataStoreFactory;
import org.eclipse.jetty.server.session.NullSessionDataStore;
import org.eclipse.jetty.server.session.SessionDataStore;
import org.eclipse.jetty.server.session.SessionHandler;

public class NullSessionDataStoreFactory
extends AbstractSessionDataStoreFactory {
    @Override
    public SessionDataStore getSessionDataStore(SessionHandler handler) throws Exception {
        return new NullSessionDataStore();
    }
}

