/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.resource;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.resource.JarResource;
import org.eclipse.jetty.util.resource.Resource;

class JarFileResource
extends JarResource {
    private static final Logger LOG = Log.getLogger(JarFileResource.class);
    private JarFile _jarFile;
    private File _file;
    private String[] _list;
    private JarEntry _entry;
    private boolean _directory;
    private String _jarUrl;
    private String _path;
    private boolean _exists;

    protected JarFileResource(URL url) {
        super(url);
    }

    protected JarFileResource(URL url, boolean useCaches) {
        super(url, useCaches);
    }

    @Override
    public synchronized void close() {
        this._exists = false;
        this._list = null;
        this._entry = null;
        this._file = null;
        if (!this.getUseCaches() && this._jarFile != null) {
            try {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Closing JarFile " + this._jarFile.getName(), new Object[0]);
                }
                this._jarFile.close();
            }
            catch (IOException ioe) {
                LOG.ignore(ioe);
            }
        }
        this._jarFile = null;
        super.close();
    }

    @Override
    protected synchronized boolean checkConnection() {
        try {
            super.checkConnection();
        }
        finally {
            if (this._jarConnection == null) {
                this._entry = null;
                this._file = null;
                this._jarFile = null;
                this._list = null;
            }
        }
        return this._jarFile != null;
    }

    @Override
    protected synchronized void newConnection() throws IOException {
        super.newConnection();
        this._entry = null;
        this._file = null;
        this._jarFile = null;
        this._list = null;
        int sep = this._urlString.lastIndexOf("!/");
        this._jarUrl = this._urlString.substring(0, sep + 2);
        this._path = URIUtil.decodePath(this._urlString.substring(sep + 2));
        if (this._path.length() == 0) {
            this._path = null;
        }
        this._jarFile = this._jarConnection.getJarFile();
        this._file = new File(this._jarFile.getName());
    }

    @Override
    public boolean exists() {
        if (this._exists) {
            return true;
        }
        if (this._urlString.endsWith("!/")) {
            String file_url = this._urlString.substring(4, this._urlString.length() - 2);
            try {
                return JarFileResource.newResource(file_url).exists();
            }
            catch (Exception e) {
                LOG.ignore(e);
                return false;
            }
        }
        boolean check = this.checkConnection();
        if (this._jarUrl != null && this._path == null) {
            this._directory = check;
            return true;
        }
        boolean close_jar_file = false;
        JarFile jar_file = null;
        if (check) {
            jar_file = this._jarFile;
        } else {
            try {
                JarURLConnection c = (JarURLConnection)new URL(this._jarUrl).openConnection();
                c.setUseCaches(this.getUseCaches());
                jar_file = c.getJarFile();
                close_jar_file = !this.getUseCaches();
            }
            catch (Exception e) {
                LOG.ignore(e);
            }
        }
        if (jar_file != null && this._entry == null && !this._directory) {
            JarEntry entry = jar_file.getJarEntry(this._path);
            if (entry == null) {
                this._exists = false;
            } else if (entry.isDirectory()) {
                this._directory = true;
                this._entry = entry;
            } else {
                JarEntry directory = jar_file.getJarEntry(this._path + '/');
                if (directory != null) {
                    this._directory = true;
                    this._entry = directory;
                } else {
                    this._directory = false;
                    this._entry = entry;
                }
            }
        }
        if (close_jar_file && jar_file != null) {
            try {
                jar_file.close();
            }
            catch (IOException ioe) {
                LOG.ignore(ioe);
            }
        }
        this._exists = this._directory || this._entry != null;
        return this._exists;
    }

    @Override
    public boolean isDirectory() {
        return this._urlString.endsWith("/") || this.exists() && this._directory;
    }

    @Override
    public long lastModified() {
        if (this.checkConnection() && this._file != null) {
            if (this.exists() && this._entry != null) {
                return this._entry.getTime();
            }
            return this._file.lastModified();
        }
        return -1L;
    }

    @Override
    public synchronized String[] list() {
        if (this.isDirectory() && this._list == null) {
            List<String> list = null;
            try {
                list = this.listEntries();
            }
            catch (Exception e) {
                LOG.warn("Retrying list:" + e, new Object[0]);
                LOG.debug(e);
                this.close();
                list = this.listEntries();
            }
            if (list != null) {
                this._list = new String[list.size()];
                list.toArray(this._list);
            }
        }
        return this._list;
    }

    private List<String> listEntries() {
        this.checkConnection();
        ArrayList<String> list = new ArrayList<String>(32);
        JarFile jarFile = this._jarFile;
        if (jarFile == null) {
            try {
                JarURLConnection jc = (JarURLConnection)new URL(this._jarUrl).openConnection();
                jc.setUseCaches(this.getUseCaches());
                jarFile = jc.getJarFile();
            }
            catch (Exception e) {
                e.printStackTrace();
                LOG.ignore(e);
            }
            if (jarFile == null) {
                throw new IllegalStateException();
            }
        }
        Enumeration<JarEntry> e = jarFile.entries();
        String dir = this._urlString.substring(this._urlString.lastIndexOf("!/") + 2);
        while (e.hasMoreElements()) {
            String listName;
            int dash;
            JarEntry entry = e.nextElement();
            String name = entry.getName().replace('\\', '/');
            if (!name.startsWith(dir) || name.length() == dir.length() || (dash = (listName = name.substring(dir.length())).indexOf(47)) >= 0 && (dash == 0 && listName.length() == 1 || list.contains(listName = dash == 0 ? listName.substring(dash + 1, listName.length()) : listName.substring(0, dash + 1)))) continue;
            list.add(listName);
        }
        return list;
    }

    @Override
    public long length() {
        if (this.isDirectory()) {
            return -1L;
        }
        if (this._entry != null) {
            return this._entry.getSize();
        }
        return -1L;
    }

    public static Resource getNonCachingResource(Resource resource) {
        if (!(resource instanceof JarFileResource)) {
            return resource;
        }
        JarFileResource oldResource = (JarFileResource)resource;
        JarFileResource newResource = new JarFileResource(oldResource.getURL(), false);
        return newResource;
    }

    @Override
    public boolean isContainedIn(Resource resource) throws MalformedURLException {
        String string = this._urlString;
        int index = string.lastIndexOf("!/");
        if (index > 0) {
            string = string.substring(0, index);
        }
        if (string.startsWith("jar:")) {
            string = string.substring(4);
        }
        URL url = new URL(string);
        return url.sameFile(resource.getURI().toURL());
    }
}

