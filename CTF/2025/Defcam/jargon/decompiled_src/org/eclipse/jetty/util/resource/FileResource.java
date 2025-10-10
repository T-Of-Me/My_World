/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.InvalidPathException;
import java.nio.file.StandardOpenOption;
import java.security.Permission;
import org.eclipse.jetty.util.IO;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.resource.Resource;

@Deprecated
public class FileResource
extends Resource {
    private static final Logger LOG = Log.getLogger(FileResource.class);
    private final File _file;
    private final URI _uri;
    private final URI _alias;

    public FileResource(URL url) throws IOException, URISyntaxException {
        File file;
        try {
            file = new File(url.toURI());
            this.assertValidPath(file.toString());
        }
        catch (URISyntaxException e) {
            throw e;
        }
        catch (Exception e) {
            if (!url.toString().startsWith("file:")) {
                throw new IllegalArgumentException("!file:");
            }
            LOG.ignore(e);
            try {
                String file_url = "file:" + URIUtil.encodePath(url.toString().substring(5));
                URI uri = new URI(file_url);
                file = uri.getAuthority() == null ? new File(uri) : new File("//" + uri.getAuthority() + URIUtil.decodePath(url.getFile()));
            }
            catch (Exception e2) {
                LOG.ignore(e2);
                URLConnection connection = url.openConnection();
                Permission perm = connection.getPermission();
                file = new File(perm == null ? url.getFile() : perm.getName());
            }
        }
        this._file = file;
        this._uri = FileResource.normalizeURI(this._file, url.toURI());
        this._alias = FileResource.checkFileAlias(this._uri, this._file);
    }

    public FileResource(URI uri) {
        File file;
        this._file = file = new File(uri);
        try {
            URI file_uri = this._file.toURI();
            this._uri = FileResource.normalizeURI(this._file, uri);
            this.assertValidPath(file.toString());
            this._alias = !URIUtil.equalsIgnoreEncodings(this._uri.toASCIIString(), file_uri.toString()) ? this._file.toURI() : FileResource.checkFileAlias(this._uri, this._file);
        }
        catch (URISyntaxException e) {
            throw new InvalidPathException(this._file.toString(), e.getMessage()){
                {
                    super(x0, x1);
                    this.initCause(e);
                }
            };
        }
    }

    public FileResource(File file) {
        this.assertValidPath(file.toString());
        this._file = file;
        try {
            this._uri = FileResource.normalizeURI(this._file, this._file.toURI());
        }
        catch (URISyntaxException e) {
            throw new InvalidPathException(this._file.toString(), e.getMessage()){
                {
                    super(x0, x1);
                    this.initCause(e);
                }
            };
        }
        this._alias = FileResource.checkFileAlias(this._uri, this._file);
    }

    public FileResource(File base, String childPath) {
        URI uri;
        String encoded = URIUtil.encodePath(childPath);
        this._file = new File(base, childPath);
        try {
            uri = base.isDirectory() ? new URI(URIUtil.addEncodedPaths(base.toURI().toASCIIString(), encoded)) : new URI(base.toURI().toASCIIString() + encoded);
        }
        catch (URISyntaxException e) {
            throw new InvalidPathException(base.toString() + childPath, e.getMessage()){
                {
                    super(x0, x1);
                    this.initCause(e);
                }
            };
        }
        this._uri = uri;
        this._alias = FileResource.checkFileAlias(this._uri, this._file);
    }

    private static URI normalizeURI(File file, URI uri) throws URISyntaxException {
        String u = uri.toASCIIString();
        if (file.isDirectory()) {
            if (!u.endsWith("/")) {
                u = u + "/";
            }
        } else if (file.exists() && u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        return new URI(u);
    }

    private static URI checkFileAlias(URI uri, File file) {
        try {
            String can;
            if (!URIUtil.equalsIgnoreEncodings(uri, file.toURI())) {
                return new File(uri).getAbsoluteFile().toURI();
            }
            String abs = file.getAbsolutePath();
            if (!abs.equals(can = file.getCanonicalPath())) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("ALIAS abs={} can={}", abs, can);
                }
                URI alias = new File(can).toURI();
                return new URI("file://" + URIUtil.encodePath(alias.getPath()));
            }
        }
        catch (Exception e) {
            LOG.warn("bad alias for {}: {}", file, e.toString());
            LOG.debug(e);
            try {
                return new URI("http://eclipse.org/bad/canonical/alias");
            }
            catch (Exception e2) {
                LOG.ignore(e2);
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    @Override
    public Resource addPath(String path) throws IOException, MalformedURLException {
        this.assertValidPath(path);
        path = URIUtil.canonicalPath(path);
        if (path == null) {
            throw new MalformedURLException();
        }
        if ("/".equals(path)) {
            return this;
        }
        return new FileResource(this._file, path);
    }

    private void assertValidPath(String path) {
        int idx = StringUtil.indexOfControlChars(path);
        if (idx >= 0) {
            throw new InvalidPathException(path, "Invalid Character at index " + idx);
        }
    }

    @Override
    public URI getAlias() {
        return this._alias;
    }

    @Override
    public boolean exists() {
        return this._file.exists();
    }

    @Override
    public long lastModified() {
        return this._file.lastModified();
    }

    @Override
    public boolean isDirectory() {
        return this._file.exists() && this._file.isDirectory() || this._uri.toASCIIString().endsWith("/");
    }

    @Override
    public long length() {
        return this._file.length();
    }

    @Override
    public String getName() {
        return this._file.getAbsolutePath();
    }

    @Override
    public File getFile() {
        return this._file;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new FileInputStream(this._file);
    }

    @Override
    public ReadableByteChannel getReadableByteChannel() throws IOException {
        return FileChannel.open(this._file.toPath(), StandardOpenOption.READ);
    }

    @Override
    public boolean delete() throws SecurityException {
        return this._file.delete();
    }

    @Override
    public boolean renameTo(Resource dest) throws SecurityException {
        if (dest instanceof FileResource) {
            return this._file.renameTo(((FileResource)dest)._file);
        }
        return false;
    }

    @Override
    public String[] list() {
        String[] list = this._file.list();
        if (list == null) {
            return null;
        }
        int i = list.length;
        while (i-- > 0) {
            if (!new File(this._file, list[i]).isDirectory() || list[i].endsWith("/")) continue;
            int n = i;
            list[n] = list[n] + "/";
        }
        return list;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (null == o || !(o instanceof FileResource)) {
            return false;
        }
        FileResource f = (FileResource)o;
        return f._file == this._file || null != this._file && this._file.equals(f._file);
    }

    public int hashCode() {
        return null == this._file ? super.hashCode() : this._file.hashCode();
    }

    @Override
    public void copyTo(File destination) throws IOException {
        if (this.isDirectory()) {
            IO.copyDir(this.getFile(), destination);
        } else {
            if (destination.exists()) {
                throw new IllegalArgumentException(destination + " exists");
            }
            IO.copy(this.getFile(), destination);
        }
    }

    @Override
    public boolean isContainedIn(Resource r) throws MalformedURLException {
        return false;
    }

    @Override
    public void close() {
    }

    @Override
    public URL getURL() {
        try {
            return this._uri.toURL();
        }
        catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public URI getURI() {
        return this._uri;
    }

    public String toString() {
        return this._uri.toString();
    }
}

