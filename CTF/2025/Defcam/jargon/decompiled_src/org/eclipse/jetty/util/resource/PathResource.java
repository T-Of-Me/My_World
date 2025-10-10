/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import org.eclipse.jetty.util.IO;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.resource.Resource;

public class PathResource
extends Resource {
    private static final Logger LOG = Log.getLogger(PathResource.class);
    private static final LinkOption[] NO_FOLLOW_LINKS = new LinkOption[]{LinkOption.NOFOLLOW_LINKS};
    private static final LinkOption[] FOLLOW_LINKS = new LinkOption[0];
    private final Path path;
    private final Path alias;
    private final URI uri;

    private final Path checkAliasPath() {
        Path abs = this.path;
        if (!URIUtil.equalsIgnoreEncodings(this.uri, this.path.toUri())) {
            try {
                return Paths.get(this.uri).toRealPath(FOLLOW_LINKS);
            }
            catch (IOException ignored) {
                LOG.ignore(ignored);
            }
        }
        if (!abs.isAbsolute()) {
            abs = this.path.toAbsolutePath();
        }
        try {
            if (Files.isSymbolicLink(this.path)) {
                return this.path.getParent().resolve(Files.readSymbolicLink(this.path));
            }
            if (Files.exists(this.path, new LinkOption[0])) {
                int realCount;
                Path real = abs.toRealPath(FOLLOW_LINKS);
                int absCount = abs.getNameCount();
                if (absCount != (realCount = real.getNameCount())) {
                    return real;
                }
                for (int i = realCount - 1; i >= 0; --i) {
                    if (abs.getName(i).toString().equals(real.getName(i).toString())) continue;
                    return real;
                }
            }
        }
        catch (IOException e) {
            LOG.ignore(e);
        }
        catch (Exception e) {
            LOG.warn("bad alias ({} {}) for {}", e.getClass().getName(), e.getMessage(), this.path);
        }
        return null;
    }

    public PathResource(File file) {
        this(file.toPath());
    }

    public PathResource(Path path) {
        this.path = path.toAbsolutePath();
        this.assertValidPath(path);
        this.uri = this.path.toUri();
        this.alias = this.checkAliasPath();
    }

    private PathResource(PathResource parent, String childPath) throws MalformedURLException {
        this.path = parent.path.getFileSystem().getPath(parent.path.toString(), childPath);
        if (this.isDirectory() && !childPath.endsWith("/")) {
            childPath = childPath + "/";
        }
        this.uri = URIUtil.addPath(parent.uri, childPath);
        this.alias = this.checkAliasPath();
    }

    public PathResource(URI uri) throws IOException {
        Path path;
        if (!uri.isAbsolute()) {
            throw new IllegalArgumentException("not an absolute uri");
        }
        if (!uri.getScheme().equalsIgnoreCase("file")) {
            throw new IllegalArgumentException("not file: scheme");
        }
        try {
            path = Paths.get(uri);
        }
        catch (InvalidPathException e) {
            throw e;
        }
        catch (IllegalArgumentException e) {
            throw e;
        }
        catch (Exception e) {
            LOG.ignore(e);
            throw new IOException("Unable to build Path from: " + uri, e);
        }
        this.path = path.toAbsolutePath();
        this.uri = path.toUri();
        this.alias = this.checkAliasPath();
    }

    public PathResource(URL url) throws IOException, URISyntaxException {
        this(url.toURI());
    }

    @Override
    public Resource addPath(String subpath) throws IOException, MalformedURLException {
        String cpath = URIUtil.canonicalPath(subpath);
        if (cpath == null || cpath.length() == 0) {
            throw new MalformedURLException(subpath);
        }
        if ("/".equals(cpath)) {
            return this;
        }
        return new PathResource(this, subpath);
    }

    private void assertValidPath(Path path) {
        String str = path.toString();
        int idx = StringUtil.indexOfControlChars(str);
        if (idx >= 0) {
            throw new InvalidPathException(str, "Invalid Character at index " + idx);
        }
    }

    @Override
    public void close() {
    }

    @Override
    public boolean delete() throws SecurityException {
        try {
            return Files.deleteIfExists(this.path);
        }
        catch (IOException e) {
            LOG.ignore(e);
            return false;
        }
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        PathResource other = (PathResource)obj;
        return !(this.path == null ? other.path != null : !this.path.equals(other.path));
    }

    @Override
    public boolean exists() {
        return Files.exists(this.path, NO_FOLLOW_LINKS);
    }

    @Override
    public File getFile() throws IOException {
        return this.path.toFile();
    }

    public Path getPath() {
        return this.path;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (Files.isDirectory(this.path, new LinkOption[0])) {
            throw new IOException(this.path + " is a directory");
        }
        return Files.newInputStream(this.path, StandardOpenOption.READ);
    }

    @Override
    public String getName() {
        return this.path.toAbsolutePath().toString();
    }

    @Override
    public ReadableByteChannel getReadableByteChannel() throws IOException {
        return FileChannel.open(this.path, StandardOpenOption.READ);
    }

    @Override
    public URI getURI() {
        return this.uri;
    }

    @Override
    public URL getURL() {
        try {
            return this.path.toUri().toURL();
        }
        catch (MalformedURLException e) {
            return null;
        }
    }

    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = 31 * result + (this.path == null ? 0 : this.path.hashCode());
        return result;
    }

    @Override
    public boolean isContainedIn(Resource r) throws MalformedURLException {
        return false;
    }

    @Override
    public boolean isDirectory() {
        return Files.isDirectory(this.path, FOLLOW_LINKS);
    }

    @Override
    public long lastModified() {
        try {
            FileTime ft = Files.getLastModifiedTime(this.path, FOLLOW_LINKS);
            return ft.toMillis();
        }
        catch (IOException e) {
            LOG.ignore(e);
            return 0L;
        }
    }

    @Override
    public long length() {
        try {
            return Files.size(this.path);
        }
        catch (IOException e) {
            return 0L;
        }
    }

    @Override
    public boolean isAlias() {
        return this.alias != null;
    }

    public Path getAliasPath() {
        return this.alias;
    }

    @Override
    public URI getAlias() {
        return this.alias == null ? null : this.alias.toUri();
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    @Override
    public String[] list() {
        try (DirectoryStream<Path> dir = Files.newDirectoryStream(this.path);){
            ArrayList<String> entries = new ArrayList<String>();
            for (Path entry : dir) {
                String name = entry.getFileName().toString();
                if (Files.isDirectory(entry, new LinkOption[0])) {
                    name = name + "/";
                }
                entries.add(name);
            }
            int size = entries.size();
            String[] stringArray = entries.toArray(new String[size]);
            return stringArray;
        }
        catch (DirectoryIteratorException e) {
            LOG.debug(e);
            return null;
        }
        catch (IOException e) {
            LOG.debug(e);
        }
        return null;
    }

    @Override
    public boolean renameTo(Resource dest) throws SecurityException {
        if (dest instanceof PathResource) {
            PathResource destRes = (PathResource)dest;
            try {
                Path result = Files.move(this.path, destRes.path, new CopyOption[0]);
                return Files.exists(result, NO_FOLLOW_LINKS);
            }
            catch (IOException e) {
                LOG.ignore(e);
                return false;
            }
        }
        return false;
    }

    @Override
    public void copyTo(File destination) throws IOException {
        if (this.isDirectory()) {
            IO.copyDir(this.path.toFile(), destination);
        } else {
            Files.copy(this.path, destination.toPath(), new CopyOption[0]);
        }
    }

    public String toString() {
        return this.uri.toASCIIString();
    }
}

