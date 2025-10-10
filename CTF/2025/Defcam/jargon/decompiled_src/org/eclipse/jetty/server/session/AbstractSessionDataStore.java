/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.session;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.eclipse.jetty.server.session.SessionContext;
import org.eclipse.jetty.server.session.SessionData;
import org.eclipse.jetty.server.session.SessionDataStore;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

@ManagedObject
public abstract class AbstractSessionDataStore
extends ContainerLifeCycle
implements SessionDataStore {
    static final Logger LOG = Log.getLogger("org.eclipse.jetty.server.session");
    protected SessionContext _context;
    protected int _gracePeriodSec = 3600;
    protected long _lastExpiryCheckTime = 0L;
    protected int _savePeriodSec = 0;

    public abstract void doStore(String var1, SessionData var2, long var3) throws Exception;

    public abstract Set<String> doGetExpired(Set<String> var1);

    @Override
    public void initialize(SessionContext context) throws Exception {
        if (this.isStarted()) {
            throw new IllegalStateException("Context set after SessionDataStore started");
        }
        this._context = context;
    }

    @Override
    public void store(String id, SessionData data) throws Exception {
        long savePeriodMs;
        if (data == null) {
            return;
        }
        long lastSave = data.getLastSaved();
        long l = savePeriodMs = this._savePeriodSec <= 0 ? 0L : TimeUnit.SECONDS.toMillis(this._savePeriodSec);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Store: id={}, dirty={}, lsave={}, period={}, elapsed={}", id, data.isDirty(), data.getLastSaved(), savePeriodMs, System.currentTimeMillis() - lastSave);
        }
        if (data.isDirty() || lastSave <= 0L || System.currentTimeMillis() - lastSave > savePeriodMs) {
            data.setLastSaved(System.currentTimeMillis());
            try {
                this.doStore(id, data, lastSave);
                data.setDirty(false);
            }
            catch (Exception e) {
                data.setLastSaved(lastSave);
                throw e;
            }
        }
    }

    @Override
    public Set<String> getExpired(Set<String> candidates) {
        try {
            Set<String> set = this.doGetExpired(candidates);
            return set;
        }
        finally {
            this._lastExpiryCheckTime = System.currentTimeMillis();
        }
    }

    @Override
    public SessionData newSessionData(String id, long created, long accessed, long lastAccessed, long maxInactiveMs) {
        return new SessionData(id, this._context.getCanonicalContextPath(), this._context.getVhost(), created, accessed, lastAccessed, maxInactiveMs);
    }

    protected void checkStarted() throws IllegalStateException {
        if (this.isStarted()) {
            throw new IllegalStateException("Already started");
        }
    }

    @Override
    protected void doStart() throws Exception {
        if (this._context == null) {
            throw new IllegalStateException("No SessionContext");
        }
        super.doStart();
    }

    @ManagedAttribute(value="interval in secs to prevent too eager session scavenging", readonly=true)
    public int getGracePeriodSec() {
        return this._gracePeriodSec;
    }

    public void setGracePeriodSec(int sec) {
        this._gracePeriodSec = sec;
    }

    @ManagedAttribute(value="min secs between saves", readonly=true)
    public int getSavePeriodSec() {
        return this._savePeriodSec;
    }

    public void setSavePeriodSec(int savePeriodSec) {
        this._savePeriodSec = savePeriodSec;
    }

    public String toString() {
        return String.format("%s@%x[passivating=%b,graceSec=%d]", this.getClass().getName(), this.hashCode(), this.isPassivating(), this.getGracePeriodSec());
    }
}

