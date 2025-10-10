/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.session;

import java.util.Set;
import org.eclipse.jetty.server.session.SessionContext;
import org.eclipse.jetty.server.session.SessionData;
import org.eclipse.jetty.server.session.SessionDataMap;
import org.eclipse.jetty.server.session.SessionDataStore;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class CachingSessionDataStore
extends ContainerLifeCycle
implements SessionDataStore {
    private static final Logger LOG = Log.getLogger("org.eclipse.jetty.server.session");
    protected SessionDataStore _store;
    protected SessionDataMap _cache;

    public CachingSessionDataStore(SessionDataMap cache, SessionDataStore store) {
        this._cache = cache;
        this.addBean((Object)this._cache, true);
        this._store = store;
        this.addBean((Object)this._store, true);
    }

    public SessionDataStore getSessionStore() {
        return this._store;
    }

    public SessionDataMap getSessionDataMap() {
        return this._cache;
    }

    @Override
    public SessionData load(String id) throws Exception {
        SessionData d = null;
        try {
            d = this._cache.load(id);
        }
        catch (Exception e) {
            LOG.warn(e);
        }
        if (d != null) {
            return d;
        }
        d = this._store.load(id);
        return d;
    }

    @Override
    public boolean delete(String id) throws Exception {
        boolean deleted = this._store.delete(id);
        this._cache.delete(id);
        return deleted;
    }

    @Override
    public Set<String> getExpired(Set<String> candidates) {
        return this._store.getExpired(candidates);
    }

    @Override
    public void store(String id, SessionData data) throws Exception {
        long lastSaved = data.getLastSaved();
        this._store.store(id, data);
        if (data.getLastSaved() != lastSaved) {
            this._cache.store(id, data);
        }
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
    }

    @Override
    public boolean isPassivating() {
        return this._store.isPassivating();
    }

    @Override
    public boolean exists(String id) throws Exception {
        try {
            SessionData data = this._cache.load(id);
            if (data != null) {
                return true;
            }
        }
        catch (Exception e) {
            LOG.warn(e);
        }
        return this._store.exists(id);
    }

    @Override
    public void initialize(SessionContext context) throws Exception {
        this._store.initialize(context);
        this._cache.initialize(context);
    }

    @Override
    public SessionData newSessionData(String id, long created, long accessed, long lastAccessed, long maxInactiveMs) {
        return this._store.newSessionData(id, created, accessed, lastAccessed, maxInactiveMs);
    }
}

