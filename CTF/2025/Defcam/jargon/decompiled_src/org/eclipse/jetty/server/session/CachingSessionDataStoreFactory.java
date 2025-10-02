/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.session;

import org.eclipse.jetty.server.session.AbstractSessionDataStoreFactory;
import org.eclipse.jetty.server.session.CachingSessionDataStore;
import org.eclipse.jetty.server.session.SessionDataMapFactory;
import org.eclipse.jetty.server.session.SessionDataStore;
import org.eclipse.jetty.server.session.SessionDataStoreFactory;
import org.eclipse.jetty.server.session.SessionHandler;

public class CachingSessionDataStoreFactory
extends AbstractSessionDataStoreFactory {
    protected SessionDataStoreFactory _sessionStoreFactory;
    protected SessionDataMapFactory _mapFactory;

    public SessionDataMapFactory getMapFactory() {
        return this._mapFactory;
    }

    public void setSessionDataMapFactory(SessionDataMapFactory mapFactory) {
        this._mapFactory = mapFactory;
    }

    public void setSessionStoreFactory(SessionDataStoreFactory factory) {
        this._sessionStoreFactory = factory;
    }

    @Override
    public SessionDataStore getSessionDataStore(SessionHandler handler) throws Exception {
        return new CachingSessionDataStore(this._mapFactory.getSessionDataMap(), this._sessionStoreFactory.getSessionDataStore(handler));
    }
}

