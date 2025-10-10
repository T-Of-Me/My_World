/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.session;

import java.io.File;
import org.eclipse.jetty.server.session.AbstractSessionDataStoreFactory;
import org.eclipse.jetty.server.session.FileSessionDataStore;
import org.eclipse.jetty.server.session.SessionDataStore;
import org.eclipse.jetty.server.session.SessionHandler;

public class FileSessionDataStoreFactory
extends AbstractSessionDataStoreFactory {
    boolean _deleteUnrestorableFiles;
    File _storeDir;

    public boolean isDeleteUnrestorableFiles() {
        return this._deleteUnrestorableFiles;
    }

    public void setDeleteUnrestorableFiles(boolean deleteUnrestorableFiles) {
        this._deleteUnrestorableFiles = deleteUnrestorableFiles;
    }

    public File getStoreDir() {
        return this._storeDir;
    }

    public void setStoreDir(File storeDir) {
        this._storeDir = storeDir;
    }

    @Override
    public SessionDataStore getSessionDataStore(SessionHandler handler) {
        FileSessionDataStore fsds = new FileSessionDataStore();
        fsds.setDeleteUnrestorableFiles(this.isDeleteUnrestorableFiles());
        fsds.setStoreDir(this.getStoreDir());
        fsds.setGracePeriodSec(this.getGracePeriodSec());
        fsds.setSavePeriodSec(this.getSavePeriodSec());
        return fsds;
    }
}

