/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.session;

import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.eclipse.jetty.server.session.Session;
import org.eclipse.jetty.server.session.SessionContext;
import org.eclipse.jetty.server.session.SessionData;
import org.eclipse.jetty.server.session.SessionDataStore;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.util.component.LifeCycle;

public interface SessionCache
extends LifeCycle {
    public static final int NEVER_EVICT = -1;
    public static final int EVICT_ON_SESSION_EXIT = 0;
    public static final int EVICT_ON_INACTIVITY = 1;

    public void initialize(SessionContext var1);

    public void shutdown();

    public SessionHandler getSessionHandler();

    public Session newSession(HttpServletRequest var1, String var2, long var3, long var5);

    public Session newSession(SessionData var1);

    public Session renewSessionId(String var1, String var2) throws Exception;

    public Session get(String var1) throws Exception;

    public void put(String var1, Session var2) throws Exception;

    public boolean contains(String var1) throws Exception;

    public boolean exists(String var1) throws Exception;

    public Session delete(String var1) throws Exception;

    public Set<String> checkExpiration(Set<String> var1);

    public void checkInactiveSession(Session var1);

    public void setSessionDataStore(SessionDataStore var1);

    public SessionDataStore getSessionDataStore();

    public void setEvictionPolicy(int var1);

    public int getEvictionPolicy();

    public void setSaveOnInactiveEviction(boolean var1);

    public boolean isSaveOnInactiveEviction();

    public void setSaveOnCreate(boolean var1);

    public boolean isSaveOnCreate();

    public void setRemoveUnloadableSessions(boolean var1);

    public boolean isRemoveUnloadableSessions();
}

