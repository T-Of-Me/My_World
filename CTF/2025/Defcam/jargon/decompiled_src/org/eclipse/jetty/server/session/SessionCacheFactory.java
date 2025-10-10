/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.session;

import org.eclipse.jetty.server.session.SessionCache;
import org.eclipse.jetty.server.session.SessionHandler;

public interface SessionCacheFactory {
    public SessionCache getSessionCache(SessionHandler var1);
}

