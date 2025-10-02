/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.session;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.jetty.server.session.AbstractSessionDataStore;
import org.eclipse.jetty.server.session.SessionData;
import org.eclipse.jetty.server.session.UnreadableSessionDataException;
import org.eclipse.jetty.server.session.UnwriteableSessionDataException;
import org.eclipse.jetty.util.ClassLoadingObjectInputStream;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

@ManagedObject
public class FileSessionDataStore
extends AbstractSessionDataStore {
    private static final Logger LOG = Log.getLogger("org.eclipse.jetty.server.session");
    private File _storeDir;
    private boolean _deleteUnrestorableFiles = false;

    @Override
    protected void doStart() throws Exception {
        this.initializeStore();
        super.doStart();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
    }

    @ManagedAttribute(value="dir where sessions are stored", readonly=true)
    public File getStoreDir() {
        return this._storeDir;
    }

    public void setStoreDir(File storeDir) {
        this.checkStarted();
        this._storeDir = storeDir;
    }

    public boolean isDeleteUnrestorableFiles() {
        return this._deleteUnrestorableFiles;
    }

    public void setDeleteUnrestorableFiles(boolean deleteUnrestorableFiles) {
        this.checkStarted();
        this._deleteUnrestorableFiles = deleteUnrestorableFiles;
    }

    @Override
    public boolean delete(String id) throws Exception {
        File file = null;
        if (this._storeDir != null && (file = this.getFile(this._storeDir, id)) != null && file.exists() && file.getParentFile().equals(this._storeDir)) {
            return file.delete();
        }
        return false;
    }

    @Override
    public Set<String> doGetExpired(Set<String> candidates) {
        final long now = System.currentTimeMillis();
        HashSet<String> expired = new HashSet<String>();
        final HashSet idsWithContext = new HashSet();
        File[] files = this._storeDir.listFiles(new FilenameFilter(){

            @Override
            public boolean accept(File dir, String name) {
                if (dir != FileSessionDataStore.this._storeDir) {
                    return false;
                }
                if (!FileSessionDataStore.this.match(name)) {
                    return false;
                }
                String idWithContext = FileSessionDataStore.this.getIdWithContextFromString(name);
                if (!StringUtil.isBlank(idWithContext)) {
                    idsWithContext.add(idWithContext);
                }
                return true;
            }
        });
        for (String idWithContext : idsWithContext) {
            this.deleteOldFiles(this._storeDir, idWithContext);
        }
        files = this._storeDir.listFiles(new FilenameFilter(){

            @Override
            public boolean accept(File dir, String name) {
                if (dir != FileSessionDataStore.this._storeDir) {
                    return false;
                }
                if (!FileSessionDataStore.this.match(name)) {
                    return false;
                }
                try {
                    long expiry = FileSessionDataStore.this.getExpiryFromString(name);
                    return expiry > 0L && expiry < now;
                }
                catch (Exception e) {
                    return false;
                }
            }
        });
        if (files != null) {
            for (File f : files) {
                expired.add(this.getIdFromFile(f));
            }
        }
        for (String c : candidates) {
            File f;
            if (expired.contains(c) || (f = this.getFile(this._storeDir, c)) != null && f.exists()) continue;
            expired.add(c);
        }
        return expired;
    }

    @Override
    public SessionData load(final String id) throws Exception {
        final AtomicReference reference = new AtomicReference();
        final AtomicReference exception = new AtomicReference();
        Runnable r = new Runnable(){

            @Override
            public void run() {
                File file = FileSessionDataStore.this.deleteOldFiles(FileSessionDataStore.this._storeDir, FileSessionDataStore.this.getIdWithContext(id));
                if (file == null || !file.exists()) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("No file: {}", file);
                    }
                    return;
                }
                try (FileInputStream in = new FileInputStream(file);){
                    SessionData data = FileSessionDataStore.this.load(in, id);
                    data.setLastSaved(file.lastModified());
                    reference.set(data);
                }
                catch (UnreadableSessionDataException e) {
                    if (FileSessionDataStore.this.isDeleteUnrestorableFiles() && file.exists() && file.getParentFile().equals(FileSessionDataStore.this._storeDir)) {
                        file.delete();
                        LOG.warn("Deleted unrestorable file for session {}", id);
                    }
                    exception.set(e);
                }
                catch (Exception e) {
                    exception.set(e);
                }
            }
        };
        this._context.run(r);
        if (exception.get() != null) {
            throw (Exception)exception.get();
        }
        return (SessionData)reference.get();
    }

    @Override
    public void doStore(String id, SessionData data, long lastSaveTime) throws Exception {
        File file = null;
        if (this._storeDir != null) {
            this.deleteAllFiles(this._storeDir, this.getIdWithContext(id));
            file = new File(this._storeDir, this.getIdWithContextAndExpiry(data));
            try (FileOutputStream fos = new FileOutputStream(file, false);){
                this.save(fos, id, data);
            }
            catch (Exception e) {
                e.printStackTrace();
                if (file != null) {
                    file.delete();
                }
                throw new UnwriteableSessionDataException(id, this._context, e);
            }
        }
    }

    public void initializeStore() {
        if (this._storeDir == null) {
            throw new IllegalStateException("No file store specified");
        }
        if (!this._storeDir.exists()) {
            this._storeDir.mkdirs();
        }
    }

    @Override
    @ManagedAttribute(value="are sessions serialized by this store", readonly=true)
    public boolean isPassivating() {
        return true;
    }

    @Override
    public boolean exists(String id) throws Exception {
        File sessionFile = this.deleteOldFiles(this._storeDir, this.getIdWithContext(id));
        if (sessionFile == null || !sessionFile.exists()) {
            return false;
        }
        long expiry = this.getExpiryFromFile(sessionFile);
        if (expiry <= 0L) {
            return true;
        }
        return expiry > System.currentTimeMillis();
    }

    private void save(OutputStream os, String id, SessionData data) throws IOException {
        DataOutputStream out = new DataOutputStream(os);
        out.writeUTF(id);
        out.writeUTF(this._context.getCanonicalContextPath());
        out.writeUTF(this._context.getVhost());
        out.writeUTF(data.getLastNode());
        out.writeLong(data.getCreated());
        out.writeLong(data.getAccessed());
        out.writeLong(data.getLastAccessed());
        out.writeLong(data.getCookieSet());
        out.writeLong(data.getExpiry());
        out.writeLong(data.getMaxInactiveMs());
        ArrayList<String> keys = new ArrayList<String>(data.getKeys());
        out.writeInt(keys.size());
        ObjectOutputStream oos = new ObjectOutputStream(out);
        for (String name : keys) {
            oos.writeUTF(name);
            oos.writeObject(data.getAttribute(name));
        }
    }

    private String getIdWithContext(String id) {
        return this._context.getCanonicalContextPath() + "_" + this._context.getVhost() + "_" + id;
    }

    private String getIdWithContextAndExpiry(SessionData data) {
        return "" + data.getExpiry() + "_" + this.getIdWithContext(data.getId());
    }

    private String getIdFromFile(File file) {
        if (file == null) {
            return null;
        }
        String name = file.getName();
        return name.substring(name.lastIndexOf(95) + 1);
    }

    private long getExpiryFromFile(File file) {
        if (file == null) {
            return 0L;
        }
        return this.getExpiryFromString(file.getName());
    }

    private long getExpiryFromString(String filename) {
        if (StringUtil.isBlank(filename) || filename.indexOf("_") < 0) {
            throw new IllegalStateException("Invalid or missing filename");
        }
        String s = filename.substring(0, filename.indexOf(95));
        return s == null ? 0L : Long.parseLong(s);
    }

    private String getIdWithContextFromFile(File file) {
        if (file == null) {
            return null;
        }
        String s = this.getIdWithContextFromString(file.getName());
        return s;
    }

    private String getIdWithContextFromString(String filename) {
        if (StringUtil.isBlank(filename) || filename.indexOf(95) < 0) {
            return null;
        }
        return filename.substring(filename.indexOf(95) + 1);
    }

    private boolean match(String filename) {
        if (StringUtil.isBlank(filename)) {
            return false;
        }
        String[] parts = filename.split("_");
        return parts.length >= 4;
    }

    private File getFile(final File storeDir, final String id) {
        File[] files = storeDir.listFiles(new FilenameFilter(){

            @Override
            public boolean accept(File dir, String name) {
                if (dir != storeDir) {
                    return false;
                }
                return name.contains(FileSessionDataStore.this.getIdWithContext(id));
            }
        });
        if (files == null || files.length < 1) {
            return null;
        }
        return files[0];
    }

    private void deleteAllFiles(final File storeDir, final String idInContext) {
        File[] files = storeDir.listFiles(new FilenameFilter(){

            @Override
            public boolean accept(File dir, String name) {
                if (dir != storeDir) {
                    return false;
                }
                return name.contains(idInContext);
            }
        });
        if (files == null || files.length < 1) {
            return;
        }
        for (File f : files) {
            try {
                Files.deleteIfExists(f.toPath());
            }
            catch (Exception e) {
                LOG.warn("Unable to delete session file", e);
            }
        }
    }

    private File deleteOldFiles(final File storeDir, final String idWithContext) {
        File[] files = storeDir.listFiles(new FilenameFilter(){

            @Override
            public boolean accept(File dir, String name) {
                if (dir != storeDir) {
                    return false;
                }
                if (!FileSessionDataStore.this.match(name)) {
                    return false;
                }
                return name.contains(idWithContext);
            }
        });
        if (files == null || files.length == 0) {
            return null;
        }
        File newest = null;
        for (File f : files) {
            try {
                if (newest == null) {
                    newest = f;
                    continue;
                }
                if (f.lastModified() > newest.lastModified()) {
                    Files.deleteIfExists(newest.toPath());
                    newest = f;
                    continue;
                }
                if (f.lastModified() < newest.lastModified()) {
                    Files.deleteIfExists(f.toPath());
                    continue;
                }
                long exp1 = this.getExpiryFromFile(newest);
                long exp2 = this.getExpiryFromFile(f);
                if (exp2 >= exp1) {
                    Files.deleteIfExists(newest.toPath());
                    newest = f;
                    continue;
                }
                Files.deleteIfExists(f.toPath());
            }
            catch (Exception e) {
                LOG.warn("Unable to delete old session file", e);
            }
        }
        return newest;
    }

    private SessionData load(InputStream is, String expectedId) throws Exception {
        String id = null;
        try {
            SessionData data = null;
            DataInputStream di = new DataInputStream(is);
            id = di.readUTF();
            String contextPath = di.readUTF();
            String vhost = di.readUTF();
            String lastNode = di.readUTF();
            long created = di.readLong();
            long accessed = di.readLong();
            long lastAccessed = di.readLong();
            long cookieSet = di.readLong();
            long expiry = di.readLong();
            long maxIdle = di.readLong();
            data = this.newSessionData(id, created, accessed, lastAccessed, maxIdle);
            data.setContextPath(contextPath);
            data.setVhost(vhost);
            data.setLastNode(lastNode);
            data.setCookieSet(cookieSet);
            data.setExpiry(expiry);
            data.setMaxInactiveMs(maxIdle);
            this.restoreAttributes(di, di.readInt(), data);
            return data;
        }
        catch (Exception e) {
            throw new UnreadableSessionDataException(expectedId, this._context, e);
        }
    }

    private void restoreAttributes(InputStream is, int size, SessionData data) throws Exception {
        if (size > 0) {
            HashMap<String, Object> attributes = new HashMap<String, Object>();
            ClassLoadingObjectInputStream ois = new ClassLoadingObjectInputStream(is);
            for (int i = 0; i < size; ++i) {
                String key = ois.readUTF();
                Object value = ois.readObject();
                attributes.put(key, value);
            }
            data.putAllAttributes(attributes);
        }
    }

    @Override
    public String toString() {
        return String.format("%s[dir=%s,deleteUnrestorableFiles=%b]", super.toString(), this._storeDir, this._deleteUnrestorableFiles);
    }
}

