/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.session;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.eclipse.jetty.server.session.Session;
import org.eclipse.jetty.server.session.SessionCache;
import org.eclipse.jetty.server.session.SessionContext;
import org.eclipse.jetty.server.session.SessionData;
import org.eclipse.jetty.server.session.SessionDataStore;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.server.session.UnreadableSessionDataException;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.thread.Locker;

@ManagedObject
public abstract class AbstractSessionCache
extends ContainerLifeCycle
implements SessionCache {
    static final Logger LOG = Log.getLogger("org.eclipse.jetty.server.session");
    protected SessionDataStore _sessionDataStore;
    protected final SessionHandler _handler;
    protected SessionContext _context;
    protected int _evictionPolicy = -1;
    protected boolean _saveOnCreate = false;
    protected boolean _saveOnInactiveEviction;
    protected boolean _removeUnloadableSessions;

    @Override
    public abstract Session newSession(SessionData var1);

    public abstract Session newSession(HttpServletRequest var1, SessionData var2);

    public abstract Session doGet(String var1);

    public abstract Session doPutIfAbsent(String var1, Session var2);

    public abstract boolean doReplace(String var1, Session var2, Session var3);

    public abstract Session doDelete(String var1);

    public AbstractSessionCache(SessionHandler handler) {
        this._handler = handler;
    }

    @Override
    public SessionHandler getSessionHandler() {
        return this._handler;
    }

    @Override
    public void initialize(SessionContext context) {
        if (this.isStarted()) {
            throw new IllegalStateException("Context set after session store started");
        }
        this._context = context;
    }

    @Override
    protected void doStart() throws Exception {
        if (this._sessionDataStore == null) {
            throw new IllegalStateException("No session data store configured");
        }
        if (this._handler == null) {
            throw new IllegalStateException("No session manager");
        }
        if (this._context == null) {
            throw new IllegalStateException("No ContextId");
        }
        this._sessionDataStore.initialize(this._context);
        super.doStart();
    }

    @Override
    protected void doStop() throws Exception {
        this._sessionDataStore.stop();
        super.doStop();
    }

    @Override
    public SessionDataStore getSessionDataStore() {
        return this._sessionDataStore;
    }

    @Override
    public void setSessionDataStore(SessionDataStore sessionStore) {
        this.updateBean(this._sessionDataStore, sessionStore);
        this._sessionDataStore = sessionStore;
    }

    @Override
    @ManagedAttribute(value="session eviction policy", readonly=true)
    public int getEvictionPolicy() {
        return this._evictionPolicy;
    }

    @Override
    public void setEvictionPolicy(int evictionTimeout) {
        this._evictionPolicy = evictionTimeout;
    }

    @Override
    @ManagedAttribute(value="immediately save new sessions", readonly=true)
    public boolean isSaveOnCreate() {
        return this._saveOnCreate;
    }

    @Override
    public void setSaveOnCreate(boolean saveOnCreate) {
        this._saveOnCreate = saveOnCreate;
    }

    @Override
    @ManagedAttribute(value="delete unreadable stored sessions", readonly=true)
    public boolean isRemoveUnloadableSessions() {
        return this._removeUnloadableSessions;
    }

    @Override
    public void setRemoveUnloadableSessions(boolean removeUnloadableSessions) {
        this._removeUnloadableSessions = removeUnloadableSessions;
    }

    @Override
    public Session get(String id) throws Exception {
        Session session = null;
        Exception ex = null;
        while (true) {
            session = this.doGet(id);
            if (this._sessionDataStore == null) break;
            if (session == null) {
                Throwable throwable;
                Locker.Lock lock;
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Session {} not found locally, attempting to load", id);
                }
                PlaceHolderSession phs = new PlaceHolderSession(new SessionData(id, null, null, 0L, 0L, 0L, 0L));
                Locker.Lock phsLock = phs.lock();
                Session s = this.doPutIfAbsent(id, phs);
                if (s == null) {
                    try {
                        session = this.loadSession(id);
                        if (session == null) {
                            this.doDelete(id);
                            phsLock.close();
                            break;
                        }
                        lock = session.lock();
                        throwable = null;
                        try {
                            boolean success = this.doReplace(id, phs, session);
                            if (!success) {
                                this.doDelete(id);
                                session = null;
                                LOG.warn("Replacement of placeholder for session {} failed", id);
                                phsLock.close();
                            } else {
                                session.setResident(true);
                                session.updateInactivityTimer();
                                phsLock.close();
                            }
                        }
                        catch (Throwable throwable2) {
                            throwable = throwable2;
                            throw throwable2;
                        }
                        finally {
                            if (lock != null) {
                                if (throwable != null) {
                                    try {
                                        lock.close();
                                    }
                                    catch (Throwable throwable3) {
                                        throwable.addSuppressed(throwable3);
                                    }
                                } else {
                                    lock.close();
                                }
                            }
                        }
                    }
                    catch (Exception e) {
                        ex = e;
                        this.doDelete(id);
                        phsLock.close();
                        session = null;
                    }
                    break;
                }
                phsLock.close();
                lock = s.lock();
                throwable = null;
                try {
                    if (!s.isResident() || s instanceof PlaceHolderSession) {
                        session = null;
                        continue;
                    }
                    session = s;
                    break;
                }
                catch (Throwable throwable4) {
                    throwable = throwable4;
                    throw throwable4;
                }
                finally {
                    if (lock == null) continue;
                    if (throwable != null) {
                        try {
                            lock.close();
                        }
                        catch (Throwable throwable5) {
                            throwable.addSuppressed(throwable5);
                        }
                        continue;
                    }
                    lock.close();
                    continue;
                }
            }
            Locker.Lock lock = session.lock();
            Throwable throwable = null;
            try {
                if (session.isResident() && !(session instanceof PlaceHolderSession)) break;
                session = null;
                continue;
            }
            catch (Throwable throwable6) {
                throwable = throwable6;
                throw throwable6;
            }
            finally {
                if (lock == null) continue;
                if (throwable != null) {
                    try {
                        lock.close();
                    }
                    catch (Throwable throwable7) {
                        throwable.addSuppressed(throwable7);
                    }
                    continue;
                }
                lock.close();
                continue;
            }
            break;
        }
        if (ex != null) {
            throw ex;
        }
        return session;
    }

    private Session loadSession(String id) throws Exception {
        SessionData data = null;
        Session session = null;
        if (this._sessionDataStore == null) {
            return null;
        }
        try {
            data = this._sessionDataStore.load(id);
            if (data == null) {
                return null;
            }
            data.setLastNode(this._context.getWorkerName());
            session = this.newSession(data);
            return session;
        }
        catch (UnreadableSessionDataException e) {
            if (this.isRemoveUnloadableSessions()) {
                this._sessionDataStore.delete(id);
            }
            throw e;
        }
    }

    @Override
    public void put(String id, Session session) throws Exception {
        if (id == null || session == null) {
            throw new IllegalArgumentException("Put key=" + id + " session=" + (session == null ? "null" : session.getId()));
        }
        try (Locker.Lock lock = session.lock();){
            if (session.getSessionHandler() == null) {
                throw new IllegalStateException("Session " + id + " is not managed");
            }
            if (!session.isValid()) {
                return;
            }
            if (this._sessionDataStore == null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("No SessionDataStore, putting into SessionCache only id={}", id);
                }
                session.setResident(true);
                if (this.doPutIfAbsent(id, session) == null) {
                    session.updateInactivityTimer();
                }
                return;
            }
            if (session.getRequests() <= 0L) {
                if (!this._sessionDataStore.isPassivating()) {
                    this._sessionDataStore.store(id, session.getSessionData());
                    if (this.getEvictionPolicy() == 0) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Eviction on request exit id={}", id);
                        }
                        this.doDelete(session.getId());
                        session.setResident(false);
                    } else {
                        session.setResident(true);
                        if (this.doPutIfAbsent(id, session) == null) {
                            session.updateInactivityTimer();
                        }
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Non passivating SessionDataStore, session in SessionCache only id={}", id);
                        }
                    }
                } else {
                    session.willPassivate();
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Session passivating id={}", id);
                    }
                    this._sessionDataStore.store(id, session.getSessionData());
                    if (this.getEvictionPolicy() == 0) {
                        this.doDelete(id);
                        session.setResident(false);
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Evicted on request exit id={}", id);
                        }
                    } else {
                        session.didActivate();
                        session.setResident(true);
                        if (this.doPutIfAbsent(id, session) == null) {
                            session.updateInactivityTimer();
                        }
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Session reactivated id={}", id);
                        }
                    }
                }
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Req count={} for id={}", session.getRequests(), id);
                }
                session.setResident(true);
                if (this.doPutIfAbsent(id, session) == null) {
                    session.updateInactivityTimer();
                }
            }
        }
    }

    @Override
    public boolean exists(String id) throws Exception {
        Session s = this.doGet(id);
        if (s != null) {
            try (Locker.Lock lock = s.lock();){
                boolean bl = s.isValid();
                return bl;
            }
        }
        return this._sessionDataStore.exists(id);
    }

    @Override
    public boolean contains(String id) throws Exception {
        return this.doGet(id) != null;
    }

    @Override
    public Session delete(String id) throws Exception {
        Session session = this.get(id);
        if (this._sessionDataStore != null) {
            boolean dsdel = this._sessionDataStore.delete(id);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Session {} deleted in db {}", id, dsdel);
            }
        }
        if (session != null) {
            session.stopInactivityTimer();
            session.setResident(false);
        }
        return this.doDelete(id);
    }

    @Override
    public Set<String> checkExpiration(Set<String> candidates) {
        if (!this.isStarted()) {
            return Collections.emptySet();
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("SessionDataStore checking expiration on {}", candidates);
        }
        Set<String> allCandidates = this._sessionDataStore.getExpired(candidates);
        HashSet<String> sessionsInUse = new HashSet<String>();
        if (allCandidates != null) {
            for (String c : allCandidates) {
                Session s = this.doGet(c);
                if (s == null || s.getRequests() <= 0L) continue;
                sessionsInUse.add(c);
            }
            try {
                allCandidates.removeAll(sessionsInUse);
            }
            catch (UnsupportedOperationException e) {
                HashSet<String> tmp = new HashSet<String>(allCandidates);
                tmp.removeAll(sessionsInUse);
                allCandidates = tmp;
            }
        }
        return allCandidates;
    }

    @Override
    public void checkInactiveSession(Session session) {
        if (session == null) {
            return;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Checking for idle {}", session.getId());
        }
        try (Locker.Lock s = session.lock();){
            if (this.getEvictionPolicy() > 0 && session.isIdleLongerThan(this.getEvictionPolicy()) && session.isValid() && session.isResident() && session.getRequests() <= 0L) {
                try {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Evicting idle session {}", session.getId());
                    }
                    if (this.isSaveOnInactiveEviction() && this._sessionDataStore != null) {
                        if (this._sessionDataStore.isPassivating()) {
                            session.willPassivate();
                        }
                        this._sessionDataStore.store(session.getId(), session.getSessionData());
                    }
                    this.doDelete(session.getId());
                    session.setResident(false);
                }
                catch (Exception e) {
                    LOG.warn("Passivation of idle session {} failed", session.getId(), e);
                    session.updateInactivityTimer();
                }
            }
        }
    }

    @Override
    public Session renewSessionId(String oldId, String newId) throws Exception {
        if (StringUtil.isBlank(oldId)) {
            throw new IllegalArgumentException("Old session id is null");
        }
        if (StringUtil.isBlank(newId)) {
            throw new IllegalArgumentException("New session id is null");
        }
        Session session = this.get(oldId);
        if (session == null) {
            return null;
        }
        try (Locker.Lock lock = session.lock();){
            session.checkValidForWrite();
            session.getSessionData().setId(newId);
            session.getSessionData().setLastSaved(0L);
            session.getSessionData().setDirty(true);
            this.doPutIfAbsent(newId, session);
            this.doDelete(oldId);
            if (this._sessionDataStore != null) {
                this._sessionDataStore.delete(oldId);
                this._sessionDataStore.store(newId, session.getSessionData());
            }
            LOG.info("Session id {} swapped for new id {}", oldId, newId);
            Session session2 = session;
            return session2;
        }
    }

    @Override
    public void setSaveOnInactiveEviction(boolean saveOnEvict) {
        this._saveOnInactiveEviction = saveOnEvict;
    }

    @Override
    @ManagedAttribute(value="save sessions before evicting from cache", readonly=true)
    public boolean isSaveOnInactiveEviction() {
        return this._saveOnInactiveEviction;
    }

    @Override
    public Session newSession(HttpServletRequest request, String id, long time, long maxInactiveMs) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating new session id=" + id, new Object[0]);
        }
        Session session = this.newSession(request, this._sessionDataStore.newSessionData(id, time, time, time, maxInactiveMs));
        session.getSessionData().setLastNode(this._context.getWorkerName());
        try {
            if (this.isSaveOnCreate() && this._sessionDataStore != null) {
                this._sessionDataStore.store(id, session.getSessionData());
            }
        }
        catch (Exception e) {
            LOG.warn("Save of new session {} failed", id, e);
        }
        return session;
    }

    public String toString() {
        return String.format("%s@%x[evict=%d,removeUnloadable=%b,saveOnCreate=%b,saveOnInactiveEvict=%b]", this.getClass().getName(), this.hashCode(), this._evictionPolicy, this._removeUnloadableSessions, this._saveOnCreate, this._saveOnInactiveEviction);
    }

    protected class PlaceHolderSession
    extends Session {
        public PlaceHolderSession(SessionData data) {
            super(null, data);
        }
    }
}

