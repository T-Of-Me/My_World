/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.session;

import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpServletRequest;
import org.eclipse.jetty.server.session.AbstractSessionCache;
import org.eclipse.jetty.server.session.Session;
import org.eclipse.jetty.server.session.SessionData;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.annotation.ManagedOperation;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.statistic.CounterStatistic;

@ManagedObject
public class DefaultSessionCache
extends AbstractSessionCache {
    private static final Logger LOG = Log.getLogger("org.eclipse.jetty.server.session");
    protected ConcurrentHashMap<String, Session> _sessions = new ConcurrentHashMap();
    private final CounterStatistic _stats = new CounterStatistic();

    public DefaultSessionCache(SessionHandler manager) {
        super(manager);
    }

    @ManagedAttribute(value="current sessions in cache", readonly=true)
    public long getSessionsCurrent() {
        return this._stats.getCurrent();
    }

    @ManagedAttribute(value="max sessions in cache", readonly=true)
    public long getSessionsMax() {
        return this._stats.getMax();
    }

    @ManagedAttribute(value="total sessions in cache", readonly=true)
    public long getSessionsTotal() {
        return this._stats.getTotal();
    }

    @ManagedOperation(value="reset statistics", impact="ACTION")
    public void resetStats() {
        this._stats.reset();
    }

    @Override
    public Session doGet(String id) {
        if (id == null) {
            return null;
        }
        Session session = this._sessions.get(id);
        return session;
    }

    @Override
    public Session doPutIfAbsent(String id, Session session) {
        Session s = this._sessions.putIfAbsent(id, session);
        if (s == null && !(session instanceof AbstractSessionCache.PlaceHolderSession)) {
            this._stats.increment();
        }
        return s;
    }

    @Override
    public Session doDelete(String id) {
        Session s = this._sessions.remove(id);
        if (s != null && !(s instanceof AbstractSessionCache.PlaceHolderSession)) {
            this._stats.decrement();
        }
        return s;
    }

    @Override
    public void shutdown() {
        int loop = 100;
        while (!this._sessions.isEmpty() && loop-- > 0) {
            for (Session session : this._sessions.values()) {
                if (this._sessionDataStore != null) {
                    session.willPassivate();
                    try {
                        this._sessionDataStore.store(session.getId(), session.getSessionData());
                    }
                    catch (Exception e) {
                        LOG.warn(e);
                    }
                    this.doDelete(session.getId());
                    continue;
                }
                try {
                    session.invalidate();
                }
                catch (Exception e) {
                    LOG.ignore(e);
                }
            }
        }
    }

    @Override
    public Session newSession(HttpServletRequest request, SessionData data) {
        Session s = new Session(this.getSessionHandler(), request, data);
        return s;
    }

    @Override
    public Session newSession(SessionData data) {
        Session s = new Session(this.getSessionHandler(), data);
        return s;
    }

    @Override
    public boolean doReplace(String id, Session oldValue, Session newValue) {
        boolean result = this._sessions.replace(id, oldValue, newValue);
        if (result && oldValue instanceof AbstractSessionCache.PlaceHolderSession) {
            this._stats.increment();
        }
        return result;
    }
}

