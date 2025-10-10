/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.security;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.util.PathWatcher;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.resource.JarResource;
import org.eclipse.jetty.util.resource.PathResource;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.security.Credential;

public class PropertyUserStore
extends UserStore
implements PathWatcher.Listener {
    private static final Logger LOG = Log.getLogger(PropertyUserStore.class);
    private static final String JAR_FILE = "jar:file:";
    protected Path _configPath;
    protected Resource _configResource;
    protected PathWatcher pathWatcher;
    protected boolean hotReload = false;
    protected boolean _firstLoad = true;
    protected List<UserListener> _listeners;

    @Deprecated
    public String getConfig() {
        if (this._configPath != null) {
            return this._configPath.toString();
        }
        return null;
    }

    public void setConfig(String config) {
        try {
            Resource configResource = Resource.newResource(config);
            if (configResource.getFile() == null) {
                throw new IllegalArgumentException(config + " is not a file");
            }
            this.setConfigPath(configResource.getFile());
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public Path getConfigPath() {
        return this._configPath;
    }

    public void setConfigPath(String configFile) {
        if (configFile == null) {
            this._configPath = null;
        } else if (new File(configFile).exists()) {
            this._configPath = new File(configFile).toPath();
        }
        if (!new File(configFile).exists() && configFile.startsWith(JAR_FILE)) {
            try {
                this._configPath = this.extractPackedFile(configFile);
            }
            catch (IOException e) {
                throw new RuntimeException("cannot extract file from url:" + configFile, e);
            }
        }
    }

    private Path extractPackedFile(String configFile) throws IOException {
        int fileIndex = configFile.indexOf("!");
        String entryPath = configFile.substring(fileIndex + 1, configFile.length());
        Path tmpDirectory = Files.createTempDirectory("users_store", new FileAttribute[0]);
        Path extractedPath = Paths.get(tmpDirectory.toString(), entryPath);
        Files.deleteIfExists(extractedPath);
        extractedPath.toFile().deleteOnExit();
        JarResource.newResource(configFile).copyTo(tmpDirectory.toFile());
        return extractedPath;
    }

    public void setConfigPath(File configFile) {
        if (configFile == null) {
            this._configPath = null;
            return;
        }
        this._configPath = configFile.toPath();
    }

    public void setConfigPath(Path configPath) {
        this._configPath = configPath;
    }

    public Resource getConfigResource() throws IOException {
        if (this._configResource == null) {
            this._configResource = new PathResource(this._configPath);
        }
        return this._configResource;
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

    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(this.getClass().getName());
        s.append("[");
        s.append("users.count=").append(this.getKnownUserIdentities().size());
        s.append("identityService=").append(this.getIdentityService());
        s.append("]");
        return s.toString();
    }

    protected void loadUsers() throws IOException {
        if (this._configPath == null) {
            return;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Loading " + this + " from " + this._configPath, new Object[0]);
        }
        Properties properties = new Properties();
        if (this.getConfigResource().exists()) {
            properties.load(this.getConfigResource().getInputStream());
        }
        HashSet<String> known = new HashSet<String>();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String username = ((String)entry.getKey()).trim();
            String credentials = ((String)entry.getValue()).trim();
            String roles = null;
            int c = credentials.indexOf(44);
            if (c > 0) {
                roles = credentials.substring(c + 1).trim();
                credentials = credentials.substring(0, c).trim();
            }
            if (username == null || username.length() <= 0 || credentials == null || credentials.length() <= 0) continue;
            String[] roleArray = IdentityService.NO_ROLES;
            if (roles != null && roles.length() > 0) {
                roleArray = StringUtil.csvSplit(roles);
            }
            known.add(username);
            Credential credential = Credential.getCredential(credentials);
            this.addUser(username, credential, roleArray);
            this.notifyUpdate(username, credential, roleArray);
        }
        ArrayList<String> currentlyKnownUsers = new ArrayList<String>(this.getKnownUserIdentities().keySet());
        if (!this._firstLoad) {
            for (String user : currentlyKnownUsers) {
                if (known.contains(user)) continue;
                this.removeUser(user);
                this.notifyRemove(user);
            }
        }
        this._firstLoad = false;
        if (LOG.isDebugEnabled()) {
            LOG.debug("Loaded " + this + " from " + this._configPath, new Object[0]);
        }
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        this.loadUsers();
        if (this.isHotReload() && this._configPath != null) {
            this.pathWatcher = new PathWatcher();
            this.pathWatcher.watch(this._configPath);
            this.pathWatcher.addListener(this);
            this.pathWatcher.setNotifyExistingOnStart(false);
            this.pathWatcher.start();
        }
    }

    @Override
    public void onPathWatchEvent(PathWatcher.PathWatchEvent event) {
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("PATH WATCH EVENT: {}", new Object[]{event.getType()});
            }
            this.loadUsers();
        }
        catch (IOException e) {
            LOG.warn(e);
        }
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        if (this.pathWatcher != null) {
            this.pathWatcher.stop();
        }
        this.pathWatcher = null;
    }

    private void notifyUpdate(String username, Credential credential, String[] roleArray) {
        if (this._listeners != null) {
            Iterator<UserListener> i = this._listeners.iterator();
            while (i.hasNext()) {
                i.next().update(username, credential, roleArray);
            }
        }
    }

    private void notifyRemove(String username) {
        if (this._listeners != null) {
            Iterator<UserListener> i = this._listeners.iterator();
            while (i.hasNext()) {
                i.next().remove(username);
            }
        }
    }

    public void registerUserListener(UserListener listener) {
        if (this._listeners == null) {
            this._listeners = new ArrayList<UserListener>();
        }
        this._listeners.add(listener);
    }

    public static interface UserListener {
        public void update(String var1, Credential var2, String[] var3);

        public void remove(String var1);
    }
}

