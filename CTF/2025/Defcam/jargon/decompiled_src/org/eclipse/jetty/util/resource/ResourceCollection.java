/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.StringTokenizer;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.resource.Resource;

public class ResourceCollection
extends Resource {
    private static final Logger LOG = Log.getLogger(ResourceCollection.class);
    private Resource[] _resources;

    public ResourceCollection() {
        this._resources = new Resource[0];
    }

    public ResourceCollection(Resource ... resources) {
        ArrayList<Resource> list = new ArrayList<Resource>();
        for (Resource r : resources) {
            if (r == null) continue;
            if (r instanceof ResourceCollection) {
                for (Resource r2 : ((ResourceCollection)r).getResources()) {
                    list.add(r2);
                }
                continue;
            }
            list.add(r);
        }
        this._resources = list.toArray(new Resource[list.size()]);
        for (Resource r : this._resources) {
            if (r.exists() && r.isDirectory()) continue;
            throw new IllegalArgumentException(r + " is not an existing directory.");
        }
    }

    public ResourceCollection(String[] resources) {
        this._resources = new Resource[resources.length];
        try {
            for (int i = 0; i < resources.length; ++i) {
                this._resources[i] = Resource.newResource(resources[i]);
                if (this._resources[i].exists() && this._resources[i].isDirectory()) continue;
                throw new IllegalArgumentException(this._resources[i] + " is not an existing directory.");
            }
        }
        catch (IllegalArgumentException e) {
            throw e;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ResourceCollection(String csvResources) {
        this.setResourcesAsCSV(csvResources);
    }

    public Resource[] getResources() {
        return this._resources;
    }

    public void setResources(Resource[] resources) {
        this._resources = resources != null ? resources : new Resource[]{};
    }

    public void setResourcesAsCSV(String csvResources) {
        StringTokenizer tokenizer = new StringTokenizer(csvResources, ",;");
        int len = tokenizer.countTokens();
        if (len == 0) {
            throw new IllegalArgumentException("ResourceCollection@setResourcesAsCSV(String)  argument must be a string containing one or more comma-separated resource strings.");
        }
        ArrayList<Resource> resources = new ArrayList<Resource>();
        try {
            while (tokenizer.hasMoreTokens()) {
                Resource resource = Resource.newResource(tokenizer.nextToken().trim());
                if (!resource.exists() || !resource.isDirectory()) {
                    LOG.warn(" !exist " + resource, new Object[0]);
                    continue;
                }
                resources.add(resource);
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        this._resources = resources.toArray(new Resource[resources.size()]);
    }

    @Override
    public Resource addPath(String path) throws IOException, MalformedURLException {
        int i;
        if (this._resources == null) {
            throw new IllegalStateException("*resources* not set.");
        }
        if (path == null) {
            throw new MalformedURLException();
        }
        if (path.length() == 0 || "/".equals(path)) {
            return this;
        }
        Resource resource = null;
        ArrayList<Resource> resources = null;
        for (i = 0; i < this._resources.length; ++i) {
            resource = this._resources[i].addPath(path);
            if (!resource.exists()) continue;
            if (resource.isDirectory()) break;
            return resource;
        }
        ++i;
        while (i < this._resources.length) {
            Resource r = this._resources[i].addPath(path);
            if (r.exists() && r.isDirectory()) {
                if (resources == null) {
                    resources = new ArrayList<Resource>();
                }
                if (resource != null) {
                    resources.add(resource);
                    resource = null;
                }
                resources.add(r);
            }
            ++i;
        }
        if (resource != null) {
            return resource;
        }
        if (resources != null) {
            return new ResourceCollection(resources.toArray(new Resource[resources.size()]));
        }
        return null;
    }

    protected Object findResource(String path) throws IOException, MalformedURLException {
        int i;
        Resource resource = null;
        ArrayList<Resource> resources = null;
        for (i = 0; i < this._resources.length; ++i) {
            resource = this._resources[i].addPath(path);
            if (!resource.exists()) continue;
            if (resource.isDirectory()) break;
            return resource;
        }
        ++i;
        while (i < this._resources.length) {
            Resource r = this._resources[i].addPath(path);
            if (r.exists() && r.isDirectory()) {
                if (resource != null) {
                    resources = new ArrayList<Resource>();
                    resources.add(resource);
                }
                resources.add(r);
            }
            ++i;
        }
        if (resource != null) {
            return resource;
        }
        if (resources != null) {
            return resources;
        }
        return null;
    }

    @Override
    public boolean delete() throws SecurityException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean exists() {
        if (this._resources == null) {
            throw new IllegalStateException("*resources* not set.");
        }
        return true;
    }

    @Override
    public File getFile() throws IOException {
        if (this._resources == null) {
            throw new IllegalStateException("*resources* not set.");
        }
        for (Resource r : this._resources) {
            File f = r.getFile();
            if (f == null) continue;
            return f;
        }
        return null;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (this._resources == null) {
            throw new IllegalStateException("*resources* not set.");
        }
        for (Resource r : this._resources) {
            InputStream is = r.getInputStream();
            if (is == null) continue;
            return is;
        }
        return null;
    }

    @Override
    public ReadableByteChannel getReadableByteChannel() throws IOException {
        if (this._resources == null) {
            throw new IllegalStateException("*resources* not set.");
        }
        for (Resource r : this._resources) {
            ReadableByteChannel channel = r.getReadableByteChannel();
            if (channel == null) continue;
            return channel;
        }
        return null;
    }

    @Override
    public String getName() {
        if (this._resources == null) {
            throw new IllegalStateException("*resources* not set.");
        }
        for (Resource r : this._resources) {
            String name = r.getName();
            if (name == null) continue;
            return name;
        }
        return null;
    }

    @Override
    public URL getURL() {
        if (this._resources == null) {
            throw new IllegalStateException("*resources* not set.");
        }
        for (Resource r : this._resources) {
            URL url = r.getURL();
            if (url == null) continue;
            return url;
        }
        return null;
    }

    @Override
    public boolean isDirectory() {
        if (this._resources == null) {
            throw new IllegalStateException("*resources* not set.");
        }
        return true;
    }

    @Override
    public long lastModified() {
        if (this._resources == null) {
            throw new IllegalStateException("*resources* not set.");
        }
        for (Resource r : this._resources) {
            long lm = r.lastModified();
            if (lm == -1L) continue;
            return lm;
        }
        return -1L;
    }

    @Override
    public long length() {
        return -1L;
    }

    @Override
    public String[] list() {
        if (this._resources == null) {
            throw new IllegalStateException("*resources* not set.");
        }
        HashSet<String> set = new HashSet<String>();
        for (Resource r : this._resources) {
            for (String s : r.list()) {
                set.add(s);
            }
        }
        Object[] result = set.toArray(new String[set.size()]);
        Arrays.sort(result);
        return result;
    }

    @Override
    public void close() {
        if (this._resources == null) {
            throw new IllegalStateException("*resources* not set.");
        }
        for (Resource r : this._resources) {
            r.close();
        }
    }

    @Override
    public boolean renameTo(Resource dest) throws SecurityException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void copyTo(File destination) throws IOException {
        int r = this._resources.length;
        while (r-- > 0) {
            this._resources[r].copyTo(destination);
        }
    }

    public String toString() {
        if (this._resources == null) {
            return "[]";
        }
        return String.valueOf(Arrays.asList(this._resources));
    }

    @Override
    public boolean isContainedIn(Resource r) throws MalformedURLException {
        return false;
    }
}

