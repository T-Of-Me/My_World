/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.session;

import org.eclipse.jetty.server.session.NullSessionCache;
import org.eclipse.jetty.server.session.SessionCache;
import org.eclipse.jetty.server.session.SessionCacheFactory;
import org.eclipse.jetty.server.session.SessionHandler;

public class NullSessionCacheFactory
implements SessionCacheFactory {
    boolean _saveOnCreate;
    boolean _removeUnloadableSessions;

    public boolean isSaveOnCreate() {
        return this._saveOnCreate;
    }

    public void setSaveOnCreate(boolean saveOnCreate) {
        this._saveOnCreate = saveOnCreate;
    }

    public boolean isRemoveUnloadableSessions() {
        return this._removeUnloadableSessions;
    }

    public void setRemoveUnloadableSessions(boolean removeUnloadableSessions) {
        this._removeUnloadableSessions = removeUnloadableSessions;
    }

    @Override
    public SessionCache getSessionCache(SessionHandler handler) {
        NullSessionCache cache = new NullSessionCache(handler);
        cache.setSaveOnCreate(this.isSaveOnCreate());
        cache.setRemoveUnloadableSessions(this.isRemoveUnloadableSessions());
        return cache;
    }
}

