/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.session;

import org.eclipse.jetty.server.session.SessionDataStore;
import org.eclipse.jetty.server.session.SessionHandler;

public interface SessionDataStoreFactory {
    public SessionDataStore getSessionDataStore(SessionHandler var1) throws Exception;
}

