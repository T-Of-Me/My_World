/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.session;

import java.util.Set;
import org.eclipse.jetty.server.session.SessionData;
import org.eclipse.jetty.server.session.SessionDataMap;

public interface SessionDataStore
extends SessionDataMap {
    public SessionData newSessionData(String var1, long var2, long var4, long var6, long var8);

    public Set<String> getExpired(Set<String> var1);

    public boolean isPassivating();

    public boolean exists(String var1) throws Exception;
}

