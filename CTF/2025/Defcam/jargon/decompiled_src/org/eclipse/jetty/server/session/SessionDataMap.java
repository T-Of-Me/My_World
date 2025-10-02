/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.session;

import org.eclipse.jetty.server.session.SessionContext;
import org.eclipse.jetty.server.session.SessionData;
import org.eclipse.jetty.util.component.LifeCycle;

public interface SessionDataMap
extends LifeCycle {
    public void initialize(SessionContext var1) throws Exception;

    public SessionData load(String var1) throws Exception;

    public void store(String var1, SessionData var2) throws Exception;

    public boolean delete(String var1) throws Exception;
}

