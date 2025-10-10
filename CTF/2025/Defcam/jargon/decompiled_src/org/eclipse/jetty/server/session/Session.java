/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.session;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionContext;
import javax.servlet.http.HttpSessionEvent;
import org.eclipse.jetty.io.IdleTimeout;
import org.eclipse.jetty.server.session.SessionData;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.thread.Locker;

public class Session
implements SessionHandler.SessionIf {
    private static final Logger LOG = Log.getLogger("org.eclipse.jetty.server.session");
    public static final String SESSION_CREATED_SECURE = "org.eclipse.jetty.security.sessionCreatedSecure";
    protected SessionData _sessionData;
    protected SessionHandler _handler;
    protected String _extendedId;
    protected long _requests;
    protected boolean _idChanged;
    protected boolean _newSession;
    protected State _state = State.VALID;
    protected Locker _lock = new Locker();
    protected boolean _resident = false;
    protected SessionInactivityTimeout _sessionInactivityTimer = null;

    public Session(SessionHandler handler, HttpServletRequest request, SessionData data) {
        this._handler = handler;
        this._sessionData = data;
        this._newSession = true;
        this._sessionData.setDirty(true);
        this._requests = 1L;
    }

    public Session(SessionHandler handler, SessionData data) {
        this._handler = handler;
        this._sessionData = data;
    }

    public long getRequests() {
        try (Locker.Lock lock = this._lock.lockIfNotHeld();){
            long l = this._requests;
            return l;
        }
    }

    public void setExtendedId(String extendedId) {
        this._extendedId = extendedId;
    }

    protected void cookieSet() {
        try (Locker.Lock lock = this._lock.lockIfNotHeld();){
            this._sessionData.setCookieSet(this._sessionData.getAccessed());
        }
    }

    protected boolean access(long time) {
        try (Locker.Lock lock = this._lock.lockIfNotHeld();){
            if (!this.isValid()) {
                boolean bl = false;
                return bl;
            }
            this._newSession = false;
            long lastAccessed = this._sessionData.getAccessed();
            if (this._sessionInactivityTimer != null) {
                this._sessionInactivityTimer.notIdle();
            }
            this._sessionData.setAccessed(time);
            this._sessionData.setLastAccessed(lastAccessed);
            this._sessionData.calcAndSetExpiry(time);
            if (this.isExpiredAt(time)) {
                this.invalidate();
                boolean bl = false;
                return bl;
            }
            ++this._requests;
            boolean bl = true;
            return bl;
        }
    }

    protected void complete() {
        try (Locker.Lock lock = this._lock.lockIfNotHeld();){
            --this._requests;
        }
    }

    protected boolean isExpiredAt(long time) {
        try (Locker.Lock lock = this._lock.lockIfNotHeld();){
            boolean bl = this._sessionData.isExpiredAt(time);
            return bl;
        }
    }

    protected boolean isIdleLongerThan(int sec) {
        long now = System.currentTimeMillis();
        try (Locker.Lock lock = this._lock.lockIfNotHeld();){
            boolean bl = this._sessionData.getAccessed() + (long)(sec * 1000) <= now;
            return bl;
        }
    }

    protected void callSessionAttributeListeners(String name, Object newValue, Object oldValue) {
        if (newValue == null || !newValue.equals(oldValue)) {
            if (oldValue != null) {
                this.unbindValue(name, oldValue);
            }
            if (newValue != null) {
                this.bindValue(name, newValue);
            }
            if (this._handler == null) {
                throw new IllegalStateException("No session manager for session " + this._sessionData.getId());
            }
            this._handler.doSessionAttributeListeners(this, name, oldValue, newValue);
        }
    }

    public void unbindValue(String name, Object value) {
        if (value != null && value instanceof HttpSessionBindingListener) {
            ((HttpSessionBindingListener)value).valueUnbound(new HttpSessionBindingEvent(this, name));
        }
    }

    public void bindValue(String name, Object value) {
        if (value != null && value instanceof HttpSessionBindingListener) {
            ((HttpSessionBindingListener)value).valueBound(new HttpSessionBindingEvent(this, name));
        }
    }

    public void didActivate() {
        HttpSessionEvent event = new HttpSessionEvent(this);
        Iterator<String> iter = this._sessionData.getKeys().iterator();
        while (iter.hasNext()) {
            Object value = this._sessionData.getAttribute(iter.next());
            if (!(value instanceof HttpSessionActivationListener)) continue;
            HttpSessionActivationListener listener = (HttpSessionActivationListener)value;
            listener.sessionDidActivate(event);
        }
    }

    public void willPassivate() {
        HttpSessionEvent event = new HttpSessionEvent(this);
        Iterator<String> iter = this._sessionData.getKeys().iterator();
        while (iter.hasNext()) {
            Object value = this._sessionData.getAttribute(iter.next());
            if (!(value instanceof HttpSessionActivationListener)) continue;
            HttpSessionActivationListener listener = (HttpSessionActivationListener)value;
            listener.sessionWillPassivate(event);
        }
    }

    public boolean isValid() {
        try (Locker.Lock lock = this._lock.lockIfNotHeld();){
            boolean bl = this._state == State.VALID;
            return bl;
        }
    }

    public long getCookieSetTime() {
        try (Locker.Lock lock = this._lock.lockIfNotHeld();){
            long l = this._sessionData.getCookieSet();
            return l;
        }
    }

    @Override
    public long getCreationTime() throws IllegalStateException {
        try (Locker.Lock lock = this._lock.lockIfNotHeld();){
            this.checkValidForRead();
            long l = this._sessionData.getCreated();
            return l;
        }
    }

    @Override
    public String getId() {
        try (Locker.Lock lock = this._lock.lockIfNotHeld();){
            String string = this._sessionData.getId();
            return string;
        }
    }

    public String getExtendedId() {
        return this._extendedId;
    }

    public String getContextPath() {
        return this._sessionData.getContextPath();
    }

    public String getVHost() {
        return this._sessionData.getVhost();
    }

    @Override
    public long getLastAccessedTime() {
        try (Locker.Lock lock = this._lock.lockIfNotHeld();){
            long l = this._sessionData.getLastAccessed();
            return l;
        }
    }

    @Override
    public ServletContext getServletContext() {
        if (this._handler == null) {
            throw new IllegalStateException("No session manager for session " + this._sessionData.getId());
        }
        return this._handler._context;
    }

    @Override
    public void setMaxInactiveInterval(int secs) {
        try (Locker.Lock lock = this._lock.lockIfNotHeld();){
            this._sessionData.setMaxInactiveMs((long)secs * 1000L);
            this._sessionData.calcAndSetExpiry();
            this._sessionData.setDirty(true);
            this.updateInactivityTimer();
            if (LOG.isDebugEnabled()) {
                if (secs <= 0) {
                    LOG.debug("Session {} is now immortal (maxInactiveInterval={})", this._sessionData.getId(), secs);
                } else {
                    LOG.debug("Session {} maxInactiveInterval={}", this._sessionData.getId(), secs);
                }
            }
        }
    }

    public void updateInactivityTimer() {
        try (Locker.Lock lock = this._lock.lockIfNotHeld();){
            if (LOG.isDebugEnabled()) {
                LOG.debug("updateInactivityTimer", new Object[0]);
            }
            long maxInactive = this._sessionData.getMaxInactiveMs();
            int evictionPolicy = this.getSessionHandler().getSessionCache().getEvictionPolicy();
            if (maxInactive <= 0L) {
                if (evictionPolicy < 1) {
                    this.setInactivityTimer(-1L);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Session is immortal && no inactivity eviction: timer cancelled", new Object[0]);
                    }
                } else {
                    this.setInactivityTimer(TimeUnit.SECONDS.toMillis(evictionPolicy));
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Session is immortal; evict after {} sec inactivity", evictionPolicy);
                    }
                }
            } else if (evictionPolicy < 1) {
                this.setInactivityTimer(this._sessionData.getMaxInactiveMs());
                if (LOG.isDebugEnabled()) {
                    LOG.debug("No inactive session eviction", new Object[0]);
                }
            } else {
                this.setInactivityTimer(Math.min(maxInactive, TimeUnit.SECONDS.toMillis(evictionPolicy)));
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Inactivity timer set to lesser of maxInactive={} and inactivityEvict={}", maxInactive, evictionPolicy);
                }
            }
        }
    }

    private void setInactivityTimer(long ms) {
        if (this._sessionInactivityTimer == null) {
            this._sessionInactivityTimer = new SessionInactivityTimeout();
        }
        this._sessionInactivityTimer.setIdleTimeout(ms);
    }

    public void stopInactivityTimer() {
        try (Locker.Lock lock = this._lock.lockIfNotHeld();){
            if (this._sessionInactivityTimer != null) {
                this._sessionInactivityTimer.setIdleTimeout(-1L);
                this._sessionInactivityTimer = null;
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Session timer stopped", new Object[0]);
                }
            }
        }
    }

    @Override
    public int getMaxInactiveInterval() {
        try (Locker.Lock lock = this._lock.lockIfNotHeld();){
            int n = (int)(this._sessionData.getMaxInactiveMs() / 1000L);
            return n;
        }
    }

    @Override
    @Deprecated
    public HttpSessionContext getSessionContext() {
        this.checkValidForRead();
        return SessionHandler.__nullSessionContext;
    }

    public SessionHandler getSessionHandler() {
        return this._handler;
    }

    protected void checkValidForWrite() throws IllegalStateException {
        this.checkLocked();
        if (this._state == State.INVALID) {
            throw new IllegalStateException("Not valid for write: id=" + this._sessionData.getId() + " created=" + this._sessionData.getCreated() + " accessed=" + this._sessionData.getAccessed() + " lastaccessed=" + this._sessionData.getLastAccessed() + " maxInactiveMs=" + this._sessionData.getMaxInactiveMs() + " expiry=" + this._sessionData.getExpiry());
        }
        if (this._state == State.INVALIDATING) {
            return;
        }
        if (!this.isResident()) {
            throw new IllegalStateException("Not valid for write: id=" + this._sessionData.getId() + " not resident");
        }
    }

    protected void checkValidForRead() throws IllegalStateException {
        this.checkLocked();
        if (this._state == State.INVALID) {
            throw new IllegalStateException("Invalid for read: id=" + this._sessionData.getId() + " created=" + this._sessionData.getCreated() + " accessed=" + this._sessionData.getAccessed() + " lastaccessed=" + this._sessionData.getLastAccessed() + " maxInactiveMs=" + this._sessionData.getMaxInactiveMs() + " expiry=" + this._sessionData.getExpiry());
        }
        if (this._state == State.INVALIDATING) {
            return;
        }
        if (!this.isResident()) {
            throw new IllegalStateException("Invalid for read: id=" + this._sessionData.getId() + " not resident");
        }
    }

    protected void checkLocked() throws IllegalStateException {
        if (!this._lock.isLocked()) {
            throw new IllegalStateException("Session not locked");
        }
    }

    @Override
    public Object getAttribute(String name) {
        try (Locker.Lock lock = this._lock.lockIfNotHeld();){
            this.checkValidForRead();
            Object object = this._sessionData.getAttribute(name);
            return object;
        }
    }

    @Override
    @Deprecated
    public Object getValue(String name) {
        try (Locker.Lock lock = this._lock.lockIfNotHeld();){
            Object object = this._sessionData.getAttribute(name);
            return object;
        }
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        try (Locker.Lock lock = this._lock.lockIfNotHeld();){
            this.checkValidForRead();
            final Iterator<String> itor = this._sessionData.getKeys().iterator();
            Enumeration<String> enumeration = new Enumeration<String>(){

                @Override
                public boolean hasMoreElements() {
                    return itor.hasNext();
                }

                @Override
                public String nextElement() {
                    return (String)itor.next();
                }
            };
            return enumeration;
        }
    }

    public int getAttributes() {
        return this._sessionData.getKeys().size();
    }

    public Set<String> getNames() {
        return Collections.unmodifiableSet(this._sessionData.getKeys());
    }

    @Override
    @Deprecated
    public String[] getValueNames() throws IllegalStateException {
        try (Locker.Lock lock = this._lock.lockIfNotHeld();){
            this.checkValidForRead();
            Iterator<String> itor = this._sessionData.getKeys().iterator();
            if (!itor.hasNext()) {
                String[] stringArray = new String[]{};
                return stringArray;
            }
            ArrayList<String> names = new ArrayList<String>();
            while (itor.hasNext()) {
                names.add(itor.next());
            }
            String[] stringArray = names.toArray(new String[names.size()]);
            return stringArray;
        }
    }

    @Override
    public void setAttribute(String name, Object value) {
        Object old = null;
        try (Locker.Lock lock = this._lock.lockIfNotHeld();){
            this.checkValidForWrite();
            old = this._sessionData.setAttribute(name, value);
        }
        if (value == null && old == null) {
            return;
        }
        this.callSessionAttributeListeners(name, value, old);
    }

    @Override
    @Deprecated
    public void putValue(String name, Object value) {
        this.setAttribute(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        this.setAttribute(name, null);
    }

    @Override
    @Deprecated
    public void removeValue(String name) {
        this.setAttribute(name, null);
    }

    public void renewId(HttpServletRequest request) {
        if (this._handler == null) {
            throw new IllegalStateException("No session manager for session " + this._sessionData.getId());
        }
        String id = null;
        String extendedId = null;
        try (Locker.Lock lock = this._lock.lockIfNotHeld();){
            this.checkValidForWrite();
            id = this._sessionData.getId();
            extendedId = this.getExtendedId();
        }
        String newId = this._handler._sessionIdManager.renewSessionId(id, extendedId, request);
        try (Locker.Lock lock = this._lock.lockIfNotHeld();){
            this.checkValidForWrite();
            this._sessionData.setId(newId);
            this.setExtendedId(this._handler._sessionIdManager.getExtendedId(newId, request));
        }
        this.setIdChanged(true);
    }

    @Override
    public void invalidate() {
        if (this._handler == null) {
            throw new IllegalStateException("No session manager for session " + this._sessionData.getId());
        }
        boolean result = this.beginInvalidate();
        try {
            if (result) {
                this._handler.getSessionIdManager().invalidateAll(this._sessionData.getId());
            }
        }
        catch (Exception e) {
            LOG.warn(e);
        }
    }

    public Locker.Lock lock() {
        return this._lock.lock();
    }

    public Locker.Lock lockIfNotHeld() {
        return this._lock.lockIfNotHeld();
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    protected boolean beginInvalidate() {
        boolean result = false;
        try (Locker.Lock lock = this._lock.lockIfNotHeld();){
            switch (this._state) {
                case INVALID: {
                    throw new IllegalStateException();
                }
                case VALID: {
                    result = true;
                    this._state = State.INVALIDATING;
                    return result;
                }
                default: {
                    LOG.info("Session {} already being invalidated", this._sessionData.getId());
                    return result;
                }
            }
        }
    }

    @Deprecated
    protected void doInvalidate() throws IllegalStateException {
        this.finishInvalidate();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    protected void finishInvalidate() throws IllegalStateException {
        try (Locker.Lock lock = this._lock.lockIfNotHeld();){
            try {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("invalidate {}", this._sessionData.getId());
                }
                if (this._state == State.VALID || this._state == State.INVALIDATING) {
                    Set<String> keys = null;
                    keys = this._sessionData.getKeys();
                    for (String key : keys) {
                        Object old = this._sessionData.setAttribute(key, null);
                        if (old == null) {
                            return;
                        }
                        this.callSessionAttributeListeners(key, null, old);
                    }
                }
            }
            finally {
                this._state = State.INVALID;
            }
        }
    }

    @Override
    public boolean isNew() throws IllegalStateException {
        try (Locker.Lock lock = this._lock.lockIfNotHeld();){
            this.checkValidForRead();
            boolean bl = this._newSession;
            return bl;
        }
    }

    public void setIdChanged(boolean changed) {
        try (Locker.Lock lock = this._lock.lockIfNotHeld();){
            this._idChanged = changed;
        }
    }

    public boolean isIdChanged() {
        try (Locker.Lock lock = this._lock.lockIfNotHeld();){
            boolean bl = this._idChanged;
            return bl;
        }
    }

    @Override
    public Session getSession() {
        return this;
    }

    protected SessionData getSessionData() {
        return this._sessionData;
    }

    public void setResident(boolean resident) {
        this._resident = resident;
    }

    public boolean isResident() {
        return this._resident;
    }

    public class SessionInactivityTimeout
    extends IdleTimeout {
        public SessionInactivityTimeout() {
            super(Session.this.getSessionHandler().getScheduler());
        }

        @Override
        protected void onIdleExpired(TimeoutException timeout) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Timer expired for session {}", Session.this.getId());
            }
            Session.this.getSessionHandler().sessionInactivityTimerExpired(Session.this);
        }

        @Override
        public boolean isOpen() {
            try (Locker.Lock lock = Session.this._lock.lockIfNotHeld();){
                boolean bl = Session.this.isValid() && Session.this.isResident();
                return bl;
            }
        }

        @Override
        public void setIdleTimeout(long idleTimeout) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("setIdleTimeout called: old=" + this.getIdleTimeout() + " new=" + idleTimeout, new Object[0]);
            }
            super.setIdleTimeout(idleTimeout);
        }
    }

    public static enum State {
        VALID,
        INVALID,
        INVALIDATING;

    }
}

