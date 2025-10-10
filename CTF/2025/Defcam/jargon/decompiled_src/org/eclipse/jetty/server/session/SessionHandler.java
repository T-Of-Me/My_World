/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.session;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionContext;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;
import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.SessionIdManager;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ScopedHandler;
import org.eclipse.jetty.server.session.DefaultSessionCache;
import org.eclipse.jetty.server.session.DefaultSessionIdManager;
import org.eclipse.jetty.server.session.NullSessionDataStore;
import org.eclipse.jetty.server.session.Session;
import org.eclipse.jetty.server.session.SessionCache;
import org.eclipse.jetty.server.session.SessionCacheFactory;
import org.eclipse.jetty.server.session.SessionContext;
import org.eclipse.jetty.server.session.SessionDataStore;
import org.eclipse.jetty.server.session.SessionDataStoreFactory;
import org.eclipse.jetty.server.session.UnreadableSessionDataException;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.annotation.ManagedOperation;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.statistic.CounterStatistic;
import org.eclipse.jetty.util.statistic.SampleStatistic;
import org.eclipse.jetty.util.thread.Locker;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.eclipse.jetty.util.thread.Scheduler;

@ManagedObject
public class SessionHandler
extends ScopedHandler {
    static final Logger LOG = Log.getLogger("org.eclipse.jetty.server.session");
    public static final EnumSet<SessionTrackingMode> DEFAULT_TRACKING = EnumSet.of(SessionTrackingMode.COOKIE, SessionTrackingMode.URL);
    public static final String __SessionCookieProperty = "org.eclipse.jetty.servlet.SessionCookie";
    public static final String __DefaultSessionCookie = "JSESSIONID";
    public static final String __SessionIdPathParameterNameProperty = "org.eclipse.jetty.servlet.SessionIdPathParameterName";
    public static final String __DefaultSessionIdPathParameterName = "jsessionid";
    public static final String __CheckRemoteSessionEncoding = "org.eclipse.jetty.servlet.CheckingRemoteSessionIdEncoding";
    public static final String __SessionDomainProperty = "org.eclipse.jetty.servlet.SessionDomain";
    public static final String __DefaultSessionDomain = null;
    public static final String __SessionPathProperty = "org.eclipse.jetty.servlet.SessionPath";
    public static final String __MaxAgeProperty = "org.eclipse.jetty.servlet.MaxAge";
    public Set<SessionTrackingMode> __defaultSessionTrackingModes = Collections.unmodifiableSet(new HashSet<SessionTrackingMode>(Arrays.asList(SessionTrackingMode.COOKIE, SessionTrackingMode.URL)));
    public static final Class<? extends EventListener>[] SESSION_LISTENER_TYPES = new Class[]{HttpSessionAttributeListener.class, HttpSessionIdListener.class, HttpSessionListener.class};
    public static final BigDecimal MAX_INACTIVE_MINUTES = new BigDecimal(0x2222222);
    static final HttpSessionContext __nullSessionContext = new HttpSessionContext(){

        @Override
        public HttpSession getSession(String sessionId) {
            return null;
        }

        public Enumeration getIds() {
            return Collections.enumeration(Collections.EMPTY_LIST);
        }
    };
    protected int _dftMaxIdleSecs = -1;
    protected boolean _httpOnly = false;
    protected SessionIdManager _sessionIdManager;
    protected boolean _secureCookies = false;
    protected boolean _secureRequestOnly = true;
    protected final List<HttpSessionAttributeListener> _sessionAttributeListeners = new CopyOnWriteArrayList<HttpSessionAttributeListener>();
    protected final List<HttpSessionListener> _sessionListeners = new CopyOnWriteArrayList<HttpSessionListener>();
    protected final List<HttpSessionIdListener> _sessionIdListeners = new CopyOnWriteArrayList<HttpSessionIdListener>();
    protected ClassLoader _loader;
    protected ContextHandler.Context _context;
    protected SessionContext _sessionContext;
    protected String _sessionCookie = "JSESSIONID";
    protected String _sessionIdPathParameterName = "jsessionid";
    protected String _sessionIdPathParameterNamePrefix = ";" + this._sessionIdPathParameterName + "=";
    protected String _sessionDomain;
    protected String _sessionPath;
    protected int _maxCookieAge = -1;
    protected int _refreshCookieAge;
    protected boolean _nodeIdInSessionId;
    protected boolean _checkingRemoteSessionIdEncoding;
    protected String _sessionComment;
    protected SessionCache _sessionCache;
    protected final SampleStatistic _sessionTimeStats = new SampleStatistic();
    protected final CounterStatistic _sessionsCreatedStats = new CounterStatistic();
    public Set<SessionTrackingMode> _sessionTrackingModes;
    protected boolean _usingURLs;
    protected boolean _usingCookies = true;
    protected Set<String> _candidateSessionIdsForExpiry = ConcurrentHashMap.newKeySet();
    protected Scheduler _scheduler;
    protected boolean _ownScheduler = false;
    private SessionCookieConfig _cookieConfig = new CookieConfig();

    public SessionHandler() {
        this.setSessionTrackingModes(this.__defaultSessionTrackingModes);
    }

    @ManagedAttribute(value="path of the session cookie, or null for default")
    public String getSessionPath() {
        return this._sessionPath;
    }

    @ManagedAttribute(value="if greater the zero, the time in seconds a session cookie will last for")
    public int getMaxCookieAge() {
        return this._maxCookieAge;
    }

    public HttpCookie access(HttpSession session, boolean secure) {
        long now = System.currentTimeMillis();
        Session s = ((SessionIf)session).getSession();
        if (s.access(now) && this.isUsingCookies() && (s.isIdChanged() || this.getSessionCookieConfig().getMaxAge() > 0 && this.getRefreshCookieAge() > 0 && (now - s.getCookieSetTime()) / 1000L > (long)this.getRefreshCookieAge())) {
            HttpCookie cookie = this.getSessionCookie(session, this._context == null ? "/" : this._context.getContextPath(), secure);
            s.cookieSet();
            s.setIdChanged(false);
            return cookie;
        }
        return null;
    }

    public void addEventListener(EventListener listener) {
        if (listener instanceof HttpSessionAttributeListener) {
            this._sessionAttributeListeners.add((HttpSessionAttributeListener)listener);
        }
        if (listener instanceof HttpSessionListener) {
            this._sessionListeners.add((HttpSessionListener)listener);
        }
        if (listener instanceof HttpSessionIdListener) {
            this._sessionIdListeners.add((HttpSessionIdListener)listener);
        }
        this.addBean((Object)listener, false);
    }

    public void clearEventListeners() {
        for (EventListener e : this.getBeans(EventListener.class)) {
            this.removeBean(e);
        }
        this._sessionAttributeListeners.clear();
        this._sessionListeners.clear();
        this._sessionIdListeners.clear();
    }

    public void complete(HttpSession session) {
        if (session == null) {
            return;
        }
        Session s = ((SessionIf)session).getSession();
        try {
            s.complete();
            this._sessionCache.put(s.getId(), s);
        }
        catch (Exception e) {
            LOG.warn(e);
        }
    }

    public void complete(Session session, Request request) {
        if (request.isAsyncStarted() && request.getDispatcherType() == DispatcherType.REQUEST) {
            request.getAsyncContext().addListener(new SessionAsyncListener(session));
        } else {
            this.complete(session);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    protected void doStart() throws Exception {
        Server server = this.getServer();
        this._context = ContextHandler.getCurrentContext();
        this._loader = Thread.currentThread().getContextClassLoader();
        Server server2 = server;
        synchronized (server2) {
            if (this._sessionCache == null) {
                SessionCacheFactory ssFactory = server.getBean(SessionCacheFactory.class);
                this.setSessionCache(ssFactory != null ? ssFactory.getSessionCache(this) : new DefaultSessionCache(this));
                SessionDataStore sds = null;
                SessionDataStoreFactory sdsFactory = server.getBean(SessionDataStoreFactory.class);
                sds = sdsFactory != null ? sdsFactory.getSessionDataStore(this) : new NullSessionDataStore();
                this._sessionCache.setSessionDataStore(sds);
            }
            if (this._sessionIdManager == null) {
                this._sessionIdManager = server.getSessionIdManager();
                if (this._sessionIdManager == null) {
                    ClassLoader serverLoader = server.getClass().getClassLoader();
                    try {
                        Thread.currentThread().setContextClassLoader(serverLoader);
                        this._sessionIdManager = new DefaultSessionIdManager(server);
                        server.setSessionIdManager(this._sessionIdManager);
                        server.manage(this._sessionIdManager);
                        this._sessionIdManager.start();
                    }
                    finally {
                        Thread.currentThread().setContextClassLoader(this._loader);
                    }
                }
                this.addBean((Object)this._sessionIdManager, false);
            }
            this._scheduler = server.getBean(Scheduler.class);
            if (this._scheduler == null) {
                this._scheduler = new ScheduledExecutorScheduler();
                this._ownScheduler = true;
                this._scheduler.start();
            }
        }
        if (this._context != null) {
            String tmp = this._context.getInitParameter(__SessionCookieProperty);
            if (tmp != null) {
                this._sessionCookie = tmp;
            }
            if ((tmp = this._context.getInitParameter(__SessionIdPathParameterNameProperty)) != null) {
                this.setSessionIdPathParameterName(tmp);
            }
            if (this._maxCookieAge == -1 && (tmp = this._context.getInitParameter(__MaxAgeProperty)) != null) {
                this._maxCookieAge = Integer.parseInt(tmp.trim());
            }
            if (this._sessionDomain == null) {
                this._sessionDomain = this._context.getInitParameter(__SessionDomainProperty);
            }
            if (this._sessionPath == null) {
                this._sessionPath = this._context.getInitParameter(__SessionPathProperty);
            }
            if ((tmp = this._context.getInitParameter(__CheckRemoteSessionEncoding)) != null) {
                this._checkingRemoteSessionIdEncoding = Boolean.parseBoolean(tmp);
            }
        }
        this._sessionContext = new SessionContext(this._sessionIdManager.getWorkerName(), this._context);
        this._sessionCache.initialize(this._sessionContext);
        super.doStart();
    }

    @Override
    protected void doStop() throws Exception {
        this.shutdownSessions();
        this._sessionCache.stop();
        if (this._ownScheduler && this._scheduler != null) {
            this._scheduler.stop();
        }
        this._scheduler = null;
        super.doStop();
        this._loader = null;
    }

    @ManagedAttribute(value="true if cookies use the http only flag")
    public boolean getHttpOnly() {
        return this._httpOnly;
    }

    public HttpSession getHttpSession(String extendedId) {
        String id = this.getSessionIdManager().getId(extendedId);
        Session session = this.getSession(id);
        if (session != null && !session.getExtendedId().equals(extendedId)) {
            session.setIdChanged(true);
        }
        return session;
    }

    @ManagedAttribute(value="Session ID Manager")
    public SessionIdManager getSessionIdManager() {
        return this._sessionIdManager;
    }

    @ManagedAttribute(value="default maximum time a session may be idle for (in s)")
    public int getMaxInactiveInterval() {
        return this._dftMaxIdleSecs;
    }

    @ManagedAttribute(value="time before a session cookie is re-set (in s)")
    public int getRefreshCookieAge() {
        return this._refreshCookieAge;
    }

    @ManagedAttribute(value="if true, secure cookie flag is set on session cookies")
    public boolean getSecureCookies() {
        return this._secureCookies;
    }

    public boolean isSecureRequestOnly() {
        return this._secureRequestOnly;
    }

    public void setSecureRequestOnly(boolean secureRequestOnly) {
        this._secureRequestOnly = secureRequestOnly;
    }

    @ManagedAttribute(value="the set session cookie")
    public String getSessionCookie() {
        return this._sessionCookie;
    }

    public HttpCookie getSessionCookie(HttpSession session, String contextPath, boolean requestIsSecure) {
        if (this.isUsingCookies()) {
            String sessionPath = this._cookieConfig.getPath() == null ? contextPath : this._cookieConfig.getPath();
            sessionPath = sessionPath == null || sessionPath.length() == 0 ? "/" : sessionPath;
            String id = this.getExtendedId(session);
            HttpCookie cookie = null;
            cookie = this._sessionComment == null ? new HttpCookie(this._cookieConfig.getName(), id, this._cookieConfig.getDomain(), sessionPath, this._cookieConfig.getMaxAge(), this._cookieConfig.isHttpOnly(), this._cookieConfig.isSecure() || this.isSecureRequestOnly() && requestIsSecure) : new HttpCookie(this._cookieConfig.getName(), id, this._cookieConfig.getDomain(), sessionPath, this._cookieConfig.getMaxAge(), this._cookieConfig.isHttpOnly(), this._cookieConfig.isSecure() || this.isSecureRequestOnly() && requestIsSecure, this._sessionComment, 1);
            return cookie;
        }
        return null;
    }

    @ManagedAttribute(value="domain of the session cookie, or null for the default")
    public String getSessionDomain() {
        return this._sessionDomain;
    }

    @ManagedAttribute(value="number of sessions created by this node")
    public int getSessionsCreated() {
        return (int)this._sessionsCreatedStats.getCurrent();
    }

    @ManagedAttribute(value="name of use for URL session tracking")
    public String getSessionIdPathParameterName() {
        return this._sessionIdPathParameterName;
    }

    public String getSessionIdPathParameterNamePrefix() {
        return this._sessionIdPathParameterNamePrefix;
    }

    public boolean isUsingCookies() {
        return this._usingCookies;
    }

    public boolean isValid(HttpSession session) {
        Session s = ((SessionIf)session).getSession();
        return s.isValid();
    }

    public String getId(HttpSession session) {
        Session s = ((SessionIf)session).getSession();
        return s.getId();
    }

    public String getExtendedId(HttpSession session) {
        Session s = ((SessionIf)session).getSession();
        return s.getExtendedId();
    }

    public HttpSession newHttpSession(HttpServletRequest request) {
        long created = System.currentTimeMillis();
        String id = this._sessionIdManager.newSessionId(request, created);
        Session session = this._sessionCache.newSession(request, id, created, this._dftMaxIdleSecs > 0 ? (long)this._dftMaxIdleSecs * 1000L : -1L);
        session.setExtendedId(this._sessionIdManager.getExtendedId(id, request));
        session.getSessionData().setLastNode(this._sessionIdManager.getWorkerName());
        try {
            this._sessionCache.put(id, session);
            this._sessionsCreatedStats.increment();
            if (request.isSecure()) {
                session.setAttribute("org.eclipse.jetty.security.sessionCreatedSecure", Boolean.TRUE);
            }
            if (this._sessionListeners != null) {
                HttpSessionEvent event = new HttpSessionEvent(session);
                for (HttpSessionListener listener : this._sessionListeners) {
                    listener.sessionCreated(event);
                }
            }
            return session;
        }
        catch (Exception e) {
            LOG.warn(e);
            return null;
        }
    }

    public void removeEventListener(EventListener listener) {
        if (listener instanceof HttpSessionAttributeListener) {
            this._sessionAttributeListeners.remove(listener);
        }
        if (listener instanceof HttpSessionListener) {
            this._sessionListeners.remove(listener);
        }
        if (listener instanceof HttpSessionIdListener) {
            this._sessionIdListeners.remove(listener);
        }
        this.removeBean(listener);
    }

    @ManagedOperation(value="reset statistics", impact="ACTION")
    public void statsReset() {
        this._sessionsCreatedStats.reset();
        this._sessionTimeStats.reset();
    }

    public void setHttpOnly(boolean httpOnly) {
        this._httpOnly = httpOnly;
    }

    public void setSessionIdManager(SessionIdManager metaManager) {
        this.updateBean(this._sessionIdManager, metaManager);
        this._sessionIdManager = metaManager;
    }

    public void setMaxInactiveInterval(int seconds) {
        this._dftMaxIdleSecs = seconds;
        if (LOG.isDebugEnabled()) {
            if (this._dftMaxIdleSecs <= 0) {
                LOG.debug("Sessions created by this manager are immortal (default maxInactiveInterval={})", this._dftMaxIdleSecs);
            } else {
                LOG.debug("SessionManager default maxInactiveInterval={}", this._dftMaxIdleSecs);
            }
        }
    }

    public void setRefreshCookieAge(int ageInSeconds) {
        this._refreshCookieAge = ageInSeconds;
    }

    public void setSessionCookie(String cookieName) {
        this._sessionCookie = cookieName;
    }

    public void setSessionIdPathParameterName(String param) {
        this._sessionIdPathParameterName = param == null || "none".equals(param) ? null : param;
        this._sessionIdPathParameterNamePrefix = param == null || "none".equals(param) ? null : ";" + this._sessionIdPathParameterName + "=";
    }

    public void setUsingCookies(boolean usingCookies) {
        this._usingCookies = usingCookies;
    }

    public Session getSession(String id) {
        try {
            Session session = this._sessionCache.get(id);
            if (session != null) {
                if (session.isExpiredAt(System.currentTimeMillis())) {
                    try {
                        session.invalidate();
                    }
                    catch (Exception e) {
                        LOG.warn("Invalidating session {} found to be expired when requested", id, e);
                    }
                    return null;
                }
                session.setExtendedId(this._sessionIdManager.getExtendedId(id, null));
            }
            return session;
        }
        catch (UnreadableSessionDataException e) {
            LOG.warn(e);
            try {
                this.getSessionIdManager().invalidateAll(id);
            }
            catch (Exception x) {
                LOG.warn("Error cross-context invalidating unreadable session {}", id, x);
            }
            return null;
        }
        catch (Exception other) {
            LOG.warn(other);
            return null;
        }
    }

    protected void shutdownSessions() throws Exception {
        this._sessionCache.shutdown();
    }

    public SessionCache getSessionCache() {
        return this._sessionCache;
    }

    public void setSessionCache(SessionCache cache) {
        this.updateBean(this._sessionCache, cache);
        this._sessionCache = cache;
    }

    public boolean isNodeIdInSessionId() {
        return this._nodeIdInSessionId;
    }

    public void setNodeIdInSessionId(boolean nodeIdInSessionId) {
        this._nodeIdInSessionId = nodeIdInSessionId;
    }

    public Session removeSession(String id, boolean invalidate) {
        try {
            Session session = this._sessionCache.delete(id);
            if (session != null && invalidate) {
                session.beginInvalidate();
                if (this._sessionListeners != null) {
                    HttpSessionEvent event = new HttpSessionEvent(session);
                    for (int i = this._sessionListeners.size() - 1; i >= 0; --i) {
                        this._sessionListeners.get(i).sessionDestroyed(event);
                    }
                }
            }
            return session;
        }
        catch (Exception e) {
            LOG.warn(e);
            return null;
        }
    }

    @ManagedAttribute(value="maximum amount of time sessions have remained active (in s)")
    public long getSessionTimeMax() {
        return this._sessionTimeStats.getMax();
    }

    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return this.__defaultSessionTrackingModes;
    }

    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return Collections.unmodifiableSet(this._sessionTrackingModes);
    }

    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
        this._sessionTrackingModes = new HashSet<SessionTrackingMode>(sessionTrackingModes);
        this._usingCookies = this._sessionTrackingModes.contains((Object)SessionTrackingMode.COOKIE);
        this._usingURLs = this._sessionTrackingModes.contains((Object)SessionTrackingMode.URL);
    }

    public boolean isUsingURLs() {
        return this._usingURLs;
    }

    public SessionCookieConfig getSessionCookieConfig() {
        return this._cookieConfig;
    }

    @ManagedAttribute(value="total time sessions have remained valid")
    public long getSessionTimeTotal() {
        return this._sessionTimeStats.getTotal();
    }

    @ManagedAttribute(value="mean time sessions remain valid (in s)")
    public double getSessionTimeMean() {
        return this._sessionTimeStats.getMean();
    }

    @ManagedAttribute(value="standard deviation a session remained valid (in s)")
    public double getSessionTimeStdDev() {
        return this._sessionTimeStats.getStdDev();
    }

    @ManagedAttribute(value="check remote session id encoding")
    public boolean isCheckingRemoteSessionIdEncoding() {
        return this._checkingRemoteSessionIdEncoding;
    }

    public void setCheckingRemoteSessionIdEncoding(boolean remote) {
        this._checkingRemoteSessionIdEncoding = remote;
    }

    public void renewSessionId(String oldId, String oldExtendedId, String newId, String newExtendedId) {
        try {
            Session session = this._sessionCache.renewSessionId(oldId, newId);
            if (session == null) {
                return;
            }
            session.setExtendedId(newExtendedId);
            if (!this._sessionIdListeners.isEmpty()) {
                HttpSessionEvent event = new HttpSessionEvent(session);
                for (HttpSessionIdListener l : this._sessionIdListeners) {
                    l.sessionIdChanged(event, oldId);
                }
            }
        }
        catch (Exception e) {
            LOG.warn(e);
        }
    }

    public void invalidate(String id) {
        if (StringUtil.isBlank(id)) {
            return;
        }
        try {
            Session session = this.removeSession(id, true);
            if (session != null) {
                this._sessionTimeStats.set(Math.round((double)(System.currentTimeMillis() - session.getSessionData().getCreated()) / 1000.0));
                session.finishInvalidate();
            }
        }
        catch (Exception e) {
            LOG.warn(e);
        }
    }

    public void scavenge() {
        if (this.isStopping() || this.isStopped()) {
            return;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("{} scavenging sessions", this);
        }
        String[] ss = this._candidateSessionIdsForExpiry.toArray(new String[0]);
        Set<String> candidates = new HashSet<String>(Arrays.asList(ss));
        this._candidateSessionIdsForExpiry.removeAll(candidates);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Scavenging session ids {}", candidates);
        }
        try {
            candidates = this._sessionCache.checkExpiration(candidates);
            for (String id : candidates) {
                try {
                    this.getSessionIdManager().expireAll(id);
                }
                catch (Exception e) {
                    LOG.warn(e);
                }
            }
        }
        catch (Exception e) {
            LOG.warn(e);
        }
    }

    public void sessionInactivityTimerExpired(Session session) {
        if (session == null) {
            return;
        }
        boolean expired = false;
        try (Locker.Lock lock = session.lockIfNotHeld();){
            if (session.getRequests() > 0L) {
                return;
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Inspecting session {}, valid={}", session.getId(), session.isValid());
            }
            if (!session.isValid()) {
                return;
            }
            if (session.isExpiredAt(System.currentTimeMillis()) && session.getRequests() <= 0L) {
                expired = true;
            }
        }
        if (expired) {
            if (this._sessionIdManager.getSessionHouseKeeper() != null && this._sessionIdManager.getSessionHouseKeeper().getIntervalSec() > 0L) {
                this._candidateSessionIdsForExpiry.add(session.getId());
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Session {} is candidate for expiry", session.getId());
                }
            }
        } else {
            this._sessionCache.checkInactiveSession(session);
        }
    }

    public boolean isIdInUse(String id) throws Exception {
        return this._sessionCache.exists(id);
    }

    public Scheduler getScheduler() {
        return this._scheduler;
    }

    public void doSessionAttributeListeners(Session session, String name, Object old, Object value) {
        if (!this._sessionAttributeListeners.isEmpty()) {
            HttpSessionBindingEvent event = new HttpSessionBindingEvent(session, name, old == null ? value : old);
            for (HttpSessionAttributeListener l : this._sessionAttributeListeners) {
                if (old == null) {
                    l.attributeAdded(event);
                    continue;
                }
                if (value == null) {
                    l.attributeRemoved(event);
                    continue;
                }
                l.attributeReplaced(event);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void doScope(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        SessionHandler old_session_manager = null;
        HttpSession old_session = null;
        HttpSession existingSession = null;
        try {
            HttpCookie cookie;
            old_session_manager = baseRequest.getSessionHandler();
            old_session = baseRequest.getSession(false);
            if (old_session_manager != this) {
                baseRequest.setSessionHandler(this);
                baseRequest.setSession(null);
                this.checkRequestedSessionId(baseRequest, request);
            }
            if ((existingSession = baseRequest.getSession(false)) != null && old_session_manager != this && (cookie = this.access(existingSession, request.isSecure())) != null && (request.getDispatcherType() == DispatcherType.ASYNC || request.getDispatcherType() == DispatcherType.REQUEST)) {
                baseRequest.getResponse().addCookie(cookie);
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("sessionHandler=" + this, new Object[0]);
                LOG.debug("session=" + existingSession, new Object[0]);
            }
            if (this._nextScope != null) {
                this._nextScope.doScope(target, baseRequest, request, response);
            } else if (this._outerScope != null) {
                this._outerScope.doHandle(target, baseRequest, request, response);
            } else {
                this.doHandle(target, baseRequest, request, response);
            }
        }
        finally {
            HttpSession finalSession = baseRequest.getSession(false);
            if (LOG.isDebugEnabled()) {
                LOG.debug("FinalSession=" + finalSession + " old_session_manager=" + old_session_manager + " this=" + this, new Object[0]);
            }
            if (finalSession != null && old_session_manager != this) {
                this.complete((Session)finalSession, baseRequest);
            }
            if (old_session_manager != null && old_session_manager != this) {
                baseRequest.setSessionHandler(old_session_manager);
                baseRequest.setSession(old_session);
            }
        }
    }

    @Override
    public void doHandle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        this.nextHandle(target, baseRequest, request, response);
    }

    protected void checkRequestedSessionId(Request baseRequest, HttpServletRequest request) {
        Cookie[] cookies;
        String requested_session_id = request.getRequestedSessionId();
        if (requested_session_id != null) {
            HttpSession session = this.getHttpSession(requested_session_id);
            if (session != null && this.isValid(session)) {
                baseRequest.setSession(session);
            }
            return;
        }
        if (!DispatcherType.REQUEST.equals((Object)baseRequest.getDispatcherType())) {
            return;
        }
        boolean requested_session_id_from_cookie = false;
        HttpSession session = null;
        if (this.isUsingCookies() && (cookies = request.getCookies()) != null && cookies.length > 0) {
            String sessionCookie = this.getSessionCookieConfig().getName();
            for (int i = 0; i < cookies.length; ++i) {
                if (!sessionCookie.equalsIgnoreCase(cookies[i].getName())) continue;
                requested_session_id = cookies[i].getValue();
                requested_session_id_from_cookie = true;
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Got Session ID {} from cookie", requested_session_id);
                }
                if (requested_session_id != null) {
                    session = this.getHttpSession(requested_session_id);
                    if (session == null || !this.isValid(session)) continue;
                    break;
                }
                LOG.warn("null session id from cookie", new Object[0]);
            }
        }
        if (requested_session_id == null || session == null) {
            int s;
            String uri = request.getRequestURI();
            String prefix = this.getSessionIdPathParameterNamePrefix();
            if (prefix != null && (s = uri.indexOf(prefix)) >= 0) {
                char c;
                int i;
                for (i = s += prefix.length(); i < uri.length() && (c = uri.charAt(i)) != ';' && c != '#' && c != '?' && c != '/'; ++i) {
                }
                requested_session_id = uri.substring(s, i);
                requested_session_id_from_cookie = false;
                session = this.getHttpSession(requested_session_id);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Got Session ID {} from URL", requested_session_id);
                }
            }
        }
        baseRequest.setRequestedSessionId(requested_session_id);
        baseRequest.setRequestedSessionIdFromCookie(requested_session_id != null && requested_session_id_from_cookie);
        if (session != null && this.isValid(session)) {
            baseRequest.setSession(session);
        }
    }

    public String toString() {
        return String.format("%s%d==dftMaxIdleSec=%d", this.getClass().getName(), this.hashCode(), this._dftMaxIdleSecs);
    }

    public final class CookieConfig
    implements SessionCookieConfig {
        @Override
        public String getComment() {
            return SessionHandler.this._sessionComment;
        }

        @Override
        public String getDomain() {
            return SessionHandler.this._sessionDomain;
        }

        @Override
        public int getMaxAge() {
            return SessionHandler.this._maxCookieAge;
        }

        @Override
        public String getName() {
            return SessionHandler.this._sessionCookie;
        }

        @Override
        public String getPath() {
            return SessionHandler.this._sessionPath;
        }

        @Override
        public boolean isHttpOnly() {
            return SessionHandler.this._httpOnly;
        }

        @Override
        public boolean isSecure() {
            return SessionHandler.this._secureCookies;
        }

        @Override
        public void setComment(String comment) {
            if (SessionHandler.this._context != null && SessionHandler.this._context.getContextHandler().isAvailable()) {
                throw new IllegalStateException("CookieConfig cannot be set after ServletContext is started");
            }
            SessionHandler.this._sessionComment = comment;
        }

        @Override
        public void setDomain(String domain) {
            if (SessionHandler.this._context != null && SessionHandler.this._context.getContextHandler().isAvailable()) {
                throw new IllegalStateException("CookieConfig cannot be set after ServletContext is started");
            }
            SessionHandler.this._sessionDomain = domain;
        }

        @Override
        public void setHttpOnly(boolean httpOnly) {
            if (SessionHandler.this._context != null && SessionHandler.this._context.getContextHandler().isAvailable()) {
                throw new IllegalStateException("CookieConfig cannot be set after ServletContext is started");
            }
            SessionHandler.this._httpOnly = httpOnly;
        }

        @Override
        public void setMaxAge(int maxAge) {
            if (SessionHandler.this._context != null && SessionHandler.this._context.getContextHandler().isAvailable()) {
                throw new IllegalStateException("CookieConfig cannot be set after ServletContext is started");
            }
            SessionHandler.this._maxCookieAge = maxAge;
        }

        @Override
        public void setName(String name) {
            if (SessionHandler.this._context != null && SessionHandler.this._context.getContextHandler().isAvailable()) {
                throw new IllegalStateException("CookieConfig cannot be set after ServletContext is started");
            }
            SessionHandler.this._sessionCookie = name;
        }

        @Override
        public void setPath(String path) {
            if (SessionHandler.this._context != null && SessionHandler.this._context.getContextHandler().isAvailable()) {
                throw new IllegalStateException("CookieConfig cannot be set after ServletContext is started");
            }
            SessionHandler.this._sessionPath = path;
        }

        @Override
        public void setSecure(boolean secure) {
            if (SessionHandler.this._context != null && SessionHandler.this._context.getContextHandler().isAvailable()) {
                throw new IllegalStateException("CookieConfig cannot be set after ServletContext is started");
            }
            SessionHandler.this._secureCookies = secure;
        }
    }

    public static interface SessionIf
    extends HttpSession {
        public Session getSession();
    }

    public class SessionAsyncListener
    implements AsyncListener {
        private Session _session;

        public SessionAsyncListener(Session session) {
            this._session = session;
        }

        @Override
        public void onComplete(AsyncEvent event) throws IOException {
            SessionHandler.this.complete(((HttpServletRequest)event.getAsyncContext().getRequest()).getSession(false));
        }

        @Override
        public void onTimeout(AsyncEvent event) throws IOException {
        }

        @Override
        public void onError(AsyncEvent event) throws IOException {
            SessionHandler.this.complete(((HttpServletRequest)event.getAsyncContext().getRequest()).getSession(false));
        }

        @Override
        public void onStartAsync(AsyncEvent event) throws IOException {
            event.getAsyncContext().addListener(this);
        }
    }
}

