/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.session;

import org.eclipse.jetty.server.session.DefaultSessionCache;
import org.eclipse.jetty.server.session.SessionCache;
import org.eclipse.jetty.server.session.SessionCacheFactory;
import org.eclipse.jetty.server.session.SessionHandler;

public class DefaultSessionCacheFactory
implements SessionCacheFactory {
    int _evictionPolicy;
    boolean _saveOnInactiveEvict;
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

    public int getEvictionPolicy() {
        return this._evictionPolicy;
    }

    public void setEvictionPolicy(int evictionPolicy) {
        this._evictionPolicy = evictionPolicy;
    }

    public boolean isSaveOnInactiveEvict() {
        return this._saveOnInactiveEvict;
    }

    public void setSaveOnInactiveEvict(boolean saveOnInactiveEvict) {
        this._saveOnInactiveEvict = saveOnInactiveEvict;
    }

    @Override
    public SessionCache getSessionCache(SessionHandler handler) {
        DefaultSessionCache cache = new DefaultSessionCache(handler);
        cache.setEvictionPolicy(this.getEvictionPolicy());
        cache.setSaveOnInactiveEviction(this.isSaveOnInactiveEvict());
        cache.setSaveOnCreate(this.isSaveOnCreate());
        cache.setRemoveUnloadableSessions(this.isRemoveUnloadableSessions());
        return cache;
    }
}

