/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.session;

import javax.servlet.http.HttpServletRequest;
import org.eclipse.jetty.server.session.AbstractSessionCache;
import org.eclipse.jetty.server.session.Session;
import org.eclipse.jetty.server.session.SessionData;
import org.eclipse.jetty.server.session.SessionHandler;

public class NullSessionCache
extends AbstractSessionCache {
    public NullSessionCache(SessionHandler handler) {
        super(handler);
        super.setEvictionPolicy(0);
    }

    @Override
    public void shutdown() {
    }

    @Override
    public Session newSession(SessionData data) {
        return new Session(this.getSessionHandler(), data);
    }

    @Override
    public Session newSession(HttpServletRequest request, SessionData data) {
        return new Session(this.getSessionHandler(), request, data);
    }

    @Override
    public Session doGet(String id) {
        return null;
    }

    @Override
    public Session doPutIfAbsent(String id, Session session) {
        return null;
    }

    @Override
    public boolean doReplace(String id, Session oldValue, Session newValue) {
        return true;
    }

    @Override
    public Session doDelete(String id) {
        return null;
    }

    @Override
    public void setEvictionPolicy(int evictionTimeout) {
        LOG.warn("Ignoring eviction setting:" + evictionTimeout, new Object[0]);
    }
}

