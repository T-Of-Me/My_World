/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.resource;

import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.ReadableByteChannel;
import java.security.Permission;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.resource.Resource;

public class URLResource
extends Resource {
    private static final Logger LOG = Log.getLogger(URLResource.class);
    protected final URL _url;
    protected final String _urlString;
    protected URLConnection _connection;
    protected InputStream _in = null;
    transient boolean _useCaches = Resource.__defaultUseCaches;

    protected URLResource(URL url, URLConnection connection) {
        this._url = url;
        this._urlString = this._url.toExternalForm();
        this._connection = connection;
    }

    protected URLResource(URL url, URLConnection connection, boolean useCaches) {
        this(url, connection);
        this._useCaches = useCaches;
    }

    protected synchronized boolean checkConnection() {
        if (this._connection == null) {
            try {
                this._connection = this._url.openConnection();
                this._connection.setUseCaches(this._useCaches);
            }
            catch (IOException e) {
                LOG.ignore(e);
            }
        }
        return this._connection != null;
    }

    @Override
    public synchronized void close() {
        if (this._in != null) {
            try {
                this._in.close();
            }
            catch (IOException e) {
                LOG.ignore(e);
            }
            this._in = null;
        }
        if (this._connection != null) {
            this._connection = null;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public boolean exists() {
        try {
            URLResource uRLResource = this;
            synchronized (uRLResource) {
                if (this.checkConnection() && this._in == null) {
                    this._in = this._connection.getInputStream();
                }
            }
        }
        catch (IOException e) {
            LOG.ignore(e);
        }
        return this._in != null;
    }

    @Override
    public boolean isDirectory() {
        return this.exists() && this._urlString.endsWith("/");
    }

    @Override
    public long lastModified() {
        if (this.checkConnection()) {
            return this._connection.getLastModified();
        }
        return -1L;
    }

    @Override
    public long length() {
        if (this.checkConnection()) {
            return this._connection.getContentLength();
        }
        return -1L;
    }

    @Override
    public URL getURL() {
        return this._url;
    }

    @Override
    public File getFile() throws IOException {
        Permission perm;
        if (this.checkConnection() && (perm = this._connection.getPermission()) instanceof FilePermission) {
            return new File(perm.getName());
        }
        try {
            return new File(this._url.getFile());
        }
        catch (Exception e) {
            LOG.ignore(e);
            return null;
        }
    }

    @Override
    public String getName() {
        return this._url.toExternalForm();
    }

    @Override
    public synchronized InputStream getInputStream() throws IOException {
        return this.getInputStream(true);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    protected synchronized InputStream getInputStream(boolean resetConnection) throws IOException {
        if (!this.checkConnection()) {
            throw new IOException("Invalid resource");
        }
        try {
            if (this._in != null) {
                InputStream in = this._in;
                this._in = null;
                InputStream inputStream = in;
                return inputStream;
            }
            InputStream inputStream = this._connection.getInputStream();
            return inputStream;
        }
        finally {
            if (resetConnection) {
                this._connection = null;
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Connection nulled", new Object[0]);
                }
            }
        }
    }

    @Override
    public ReadableByteChannel getReadableByteChannel() throws IOException {
        return null;
    }

    @Override
    public boolean delete() throws SecurityException {
        throw new SecurityException("Delete not supported");
    }

    @Override
    public boolean renameTo(Resource dest) throws SecurityException {
        throw new SecurityException("RenameTo not supported");
    }

    @Override
    public String[] list() {
        return null;
    }

    @Override
    public Resource addPath(String path) throws IOException, MalformedURLException {
        if (path == null) {
            return null;
        }
        path = URIUtil.canonicalPath(path);
        return URLResource.newResource(URIUtil.addEncodedPaths(this._url.toExternalForm(), URIUtil.encodePath(path)), this._useCaches);
    }

    public String toString() {
        return this._urlString;
    }

    public int hashCode() {
        return this._urlString.hashCode();
    }

    public boolean equals(Object o) {
        return o instanceof URLResource && this._urlString.equals(((URLResource)o)._urlString);
    }

    public boolean getUseCaches() {
        return this._useCaches;
    }

    @Override
    public boolean isContainedIn(Resource containingResource) throws MalformedURLException {
        return false;
    }
}

