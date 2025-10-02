/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server;

import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.eclipse.jetty.server.session.HouseKeeper;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.util.component.LifeCycle;

public interface SessionIdManager
extends LifeCycle {
    public boolean isIdInUse(String var1);

    public void expireAll(String var1);

    public void invalidateAll(String var1);

    public String newSessionId(HttpServletRequest var1, long var2);

    public String getWorkerName();

    public String getId(String var1);

    public String getExtendedId(String var1, HttpServletRequest var2);

    public String renewSessionId(String var1, String var2, HttpServletRequest var3);

    public Set<SessionHandler> getSessionHandlers();

    public void setSessionHouseKeeper(HouseKeeper var1);

    public HouseKeeper getSessionHouseKeeper();
}

