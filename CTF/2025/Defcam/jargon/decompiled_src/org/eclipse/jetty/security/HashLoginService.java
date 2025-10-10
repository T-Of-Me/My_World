/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.security;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.jetty.security.AbstractLoginService;
import org.eclipse.jetty.security.PropertyUserStore;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.resource.Resource;

public class HashLoginService
extends AbstractLoginService {
    private static final Logger LOG = Log.getLogger(HashLoginService.class);
    private String _config;
    private boolean hotReload = false;
    private UserStore _userStore;
    private boolean _userStoreAutoCreate = false;

    public HashLoginService() {
    }

    public HashLoginService(String name) {
        this.setName(name);
    }

    public HashLoginService(String name, String config) {
        this.setName(name);
        this.setConfig(config);
    }

    public String getConfig() {
        return this._config;
    }

    @Deprecated
    public Resource getConfigResource() {
        return null;
    }

    public void setConfig(String config) {
        this._config = config;
    }

    public boolean isHotReload() {
        return this.hotReload;
    }

    public void setHotReload(boolean enable) {
        if (this.isRunning()) {
            throw new IllegalStateException("Cannot set hot reload while user store is running");
        }
        this.hotReload = enable;
    }

    public void setUserStore(UserStore userStore) {
        this._userStore = userStore;
    }

    @Override
    protected String[] loadRoleInfo(AbstractLoginService.UserPrincipal user) {
        UserIdentity id = this._userStore.getUserIdentity(user.getName());
        if (id == null) {
            return null;
        }
        Set<AbstractLoginService.RolePrincipal> roles = id.getSubject().getPrincipals(AbstractLoginService.RolePrincipal.class);
        if (roles == null) {
            return null;
        }
        List<String> list = roles.stream().map(rolePrincipal -> rolePrincipal.getName()).collect(Collectors.toList());
        return list.toArray(new String[roles.size()]);
    }

    @Override
    protected AbstractLoginService.UserPrincipal loadUserInfo(String userName) {
        UserIdentity id = this._userStore.getUserIdentity(userName);
        if (id != null) {
            return (AbstractLoginService.UserPrincipal)id.getUserPrincipal();
        }
        return null;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        if (this._userStore == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("doStart: Starting new PropertyUserStore. PropertiesFile: " + this._config + " hotReload: " + this.hotReload, new Object[0]);
            }
            PropertyUserStore propertyUserStore = new PropertyUserStore();
            propertyUserStore.setHotReload(this.hotReload);
            propertyUserStore.setConfigPath(this._config);
            propertyUserStore.start();
            this._userStore = propertyUserStore;
            this._userStoreAutoCreate = true;
        }
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        if (this._userStore != null && this._userStoreAutoCreate) {
            this._userStore.stop();
        }
        this._userStore = null;
    }
}

