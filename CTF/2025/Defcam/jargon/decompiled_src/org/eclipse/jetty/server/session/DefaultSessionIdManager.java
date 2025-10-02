/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.session;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import javax.servlet.http.HttpServletRequest;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.SessionIdManager;
import org.eclipse.jetty.server.session.HouseKeeper;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

@ManagedObject
public class DefaultSessionIdManager
extends ContainerLifeCycle
implements SessionIdManager {
    private static final Logger LOG = Log.getLogger("org.eclipse.jetty.server.session");
    private static final String __NEW_SESSION_ID = "org.eclipse.jetty.server.newSessionId";
    protected static final AtomicLong COUNTER = new AtomicLong();
    protected Random _random;
    protected boolean _weakRandom;
    protected String _workerName;
    protected String _workerAttr;
    protected long _reseed = 100000L;
    protected Server _server;
    protected HouseKeeper _houseKeeper;
    protected boolean _ownHouseKeeper;

    public DefaultSessionIdManager(Server server) {
        this._server = server;
    }

    public DefaultSessionIdManager(Server server, Random random) {
        this(server);
        this._random = random;
    }

    public void setServer(Server server) {
        this._server = server;
    }

    public Server getServer() {
        return this._server;
    }

    @Override
    public void setSessionHouseKeeper(HouseKeeper houseKeeper) {
        this.updateBean(this._houseKeeper, houseKeeper);
        this._houseKeeper = houseKeeper;
        this._houseKeeper.setSessionIdManager(this);
    }

    @Override
    public HouseKeeper getSessionHouseKeeper() {
        return this._houseKeeper;
    }

    @Override
    @ManagedAttribute(value="unique name for this node", readonly=true)
    public String getWorkerName() {
        return this._workerName;
    }

    public void setWorkerName(String workerName) {
        if (this.isRunning()) {
            throw new IllegalStateException(this.getState());
        }
        if (workerName == null) {
            this._workerName = "";
        } else {
            if (workerName.contains(".")) {
                throw new IllegalArgumentException("Name cannot contain '.'");
            }
            this._workerName = workerName;
        }
    }

    public Random getRandom() {
        return this._random;
    }

    public synchronized void setRandom(Random random) {
        this._random = random;
        this._weakRandom = false;
    }

    public long getReseed() {
        return this._reseed;
    }

    public void setReseed(long reseed) {
        this._reseed = reseed;
    }

    @Override
    public String newSessionId(HttpServletRequest request, long created) {
        String cluster_id;
        if (request == null) {
            return this.newSessionId(created);
        }
        String requested_id = request.getRequestedSessionId();
        if (requested_id != null && this.isIdInUse(cluster_id = this.getId(requested_id))) {
            return cluster_id;
        }
        String new_id = (String)request.getAttribute(__NEW_SESSION_ID);
        if (new_id != null && this.isIdInUse(new_id)) {
            return new_id;
        }
        String id = this.newSessionId(request.hashCode());
        request.setAttribute(__NEW_SESSION_ID, id);
        return id;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public String newSessionId(long seedTerm) {
        String id = null;
        Random random = this._random;
        synchronized (random) {
            while (id == null || id.length() == 0) {
                long r1;
                long r0;
                long l = r0 = this._weakRandom ? (long)this.hashCode() ^ Runtime.getRuntime().freeMemory() ^ (long)this._random.nextInt() ^ seedTerm << 32 : this._random.nextLong();
                if (r0 < 0L) {
                    r0 = -r0;
                }
                if (this._reseed > 0L && r0 % this._reseed == 1L) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Reseeding {}", this);
                    }
                    if (this._random instanceof SecureRandom) {
                        SecureRandom secure = (SecureRandom)this._random;
                        secure.setSeed(secure.generateSeed(8));
                    } else {
                        this._random.setSeed(this._random.nextLong() ^ System.currentTimeMillis() ^ seedTerm ^ Runtime.getRuntime().freeMemory());
                    }
                }
                long l2 = r1 = this._weakRandom ? (long)this.hashCode() ^ Runtime.getRuntime().freeMemory() ^ (long)this._random.nextInt() ^ seedTerm << 32 : this._random.nextLong();
                if (r1 < 0L) {
                    r1 = -r1;
                }
                id = Long.toString(r0, 36) + Long.toString(r1, 36);
                if (!StringUtil.isBlank(this._workerName)) {
                    id = this._workerName + id;
                }
                id = id + Long.toString(COUNTER.getAndIncrement());
            }
        }
        return id;
    }

    @Override
    public boolean isIdInUse(String id) {
        if (id == null) {
            return false;
        }
        boolean inUse = false;
        if (LOG.isDebugEnabled()) {
            LOG.debug("Checking {} is in use by at least one context", id);
        }
        try {
            for (SessionHandler manager : this.getSessionHandlers()) {
                if (!manager.isIdInUse(id)) continue;
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Context {} reports id in use", manager);
                }
                inUse = true;
                break;
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Checked {}, in use:", id, inUse);
            }
            return inUse;
        }
        catch (Exception e) {
            LOG.warn("Problem checking if id {} is in use", id, e);
            return false;
        }
    }

    @Override
    protected void doStart() throws Exception {
        if (this._server == null) {
            throw new IllegalStateException("No Server for SessionIdManager");
        }
        this.initRandom();
        if (this._workerName == null) {
            String inst = System.getenv("JETTY_WORKER_INSTANCE");
            this._workerName = "node" + (inst == null ? "0" : inst);
        }
        LOG.info("DefaultSessionIdManager workerName={}", this._workerName);
        String string = this._workerAttr = this._workerName != null && this._workerName.startsWith("$") ? this._workerName.substring(1) : null;
        if (this._houseKeeper == null) {
            LOG.info("No SessionScavenger set, using defaults", new Object[0]);
            this._ownHouseKeeper = true;
            this._houseKeeper = new HouseKeeper();
            this._houseKeeper.setSessionIdManager(this);
            this.addBean((Object)this._houseKeeper, true);
        }
        this._houseKeeper.start();
    }

    @Override
    protected void doStop() throws Exception {
        this._houseKeeper.stop();
        if (this._ownHouseKeeper) {
            this._houseKeeper = null;
        }
        this._random = null;
    }

    public void initRandom() {
        if (this._random == null) {
            try {
                this._random = new SecureRandom();
            }
            catch (Exception e) {
                LOG.warn("Could not generate SecureRandom for session-id randomness", e);
                this._random = new Random();
                this._weakRandom = true;
            }
        } else {
            this._random.setSeed(this._random.nextLong() ^ System.currentTimeMillis() ^ (long)this.hashCode() ^ Runtime.getRuntime().freeMemory());
        }
    }

    @Override
    public String getExtendedId(String clusterId, HttpServletRequest request) {
        if (!StringUtil.isBlank(this._workerName)) {
            if (this._workerAttr == null) {
                return clusterId + '.' + this._workerName;
            }
            String worker = (String)request.getAttribute(this._workerAttr);
            if (worker != null) {
                return clusterId + '.' + worker;
            }
        }
        return clusterId;
    }

    @Override
    public String getId(String extendedId) {
        int dot = extendedId.lastIndexOf(46);
        return dot > 0 ? extendedId.substring(0, dot) : extendedId;
    }

    @Override
    public void expireAll(String id) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Expiring {}", id);
        }
        for (SessionHandler manager : this.getSessionHandlers()) {
            manager.invalidate(id);
        }
    }

    @Override
    public void invalidateAll(String id) {
        for (SessionHandler manager : this.getSessionHandlers()) {
            manager.invalidate(id);
        }
    }

    @Override
    public String renewSessionId(String oldClusterId, String oldNodeId, HttpServletRequest request) {
        String newClusterId = this.newSessionId(request.hashCode());
        for (SessionHandler manager : this.getSessionHandlers()) {
            manager.renewSessionId(oldClusterId, oldNodeId, newClusterId, this.getExtendedId(newClusterId, request));
        }
        return newClusterId;
    }

    @Override
    public Set<SessionHandler> getSessionHandlers() {
        HashSet<SessionHandler> handlers = new HashSet<SessionHandler>();
        Handler[] tmp = this._server.getChildHandlersByClass(SessionHandler.class);
        if (tmp != null) {
            for (Handler h : tmp) {
                handlers.add((SessionHandler)h);
            }
        }
        return handlers;
    }

    public String toString() {
        return String.format("%s[worker=%s]", super.toString(), this._workerName);
    }
}

