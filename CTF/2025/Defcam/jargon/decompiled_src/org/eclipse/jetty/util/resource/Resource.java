/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.resource;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import org.eclipse.jetty.util.B64Code;
import org.eclipse.jetty.util.IO;
import org.eclipse.jetty.util.Loader;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.resource.BadResource;
import org.eclipse.jetty.util.resource.JarFileResource;
import org.eclipse.jetty.util.resource.JarResource;
import org.eclipse.jetty.util.resource.PathResource;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.eclipse.jetty.util.resource.URLResource;

public abstract class Resource
implements ResourceFactory,
Closeable {
    private static final Logger LOG = Log.getLogger(Resource.class);
    public static boolean __defaultUseCaches = true;
    volatile Object _associate;

    public static void setDefaultUseCaches(boolean useCaches) {
        __defaultUseCaches = useCaches;
    }

    public static boolean getDefaultUseCaches() {
        return __defaultUseCaches;
    }

    public static Resource newResource(URI uri) throws MalformedURLException {
        return Resource.newResource(uri.toURL());
    }

    public static Resource newResource(URL url) {
        return Resource.newResource(url, __defaultUseCaches);
    }

    static Resource newResource(URL url, boolean useCaches) {
        if (url == null) {
            return null;
        }
        String url_string = url.toExternalForm();
        if (url_string.startsWith("file:")) {
            try {
                return new PathResource(url);
            }
            catch (Exception e) {
                LOG.warn(e.toString(), new Object[0]);
                LOG.debug("EXCEPTION ", e);
                return new BadResource(url, e.toString());
            }
        }
        if (url_string.startsWith("jar:file:")) {
            return new JarFileResource(url, useCaches);
        }
        if (url_string.startsWith("jar:")) {
            return new JarResource(url, useCaches);
        }
        return new URLResource(url, null, useCaches);
    }

    public static Resource newResource(String resource) throws MalformedURLException, IOException {
        return Resource.newResource(resource, __defaultUseCaches);
    }

    public static Resource newResource(String resource, boolean useCaches) throws MalformedURLException, IOException {
        URL url = null;
        try {
            url = new URL(resource);
        }
        catch (MalformedURLException e) {
            if (!(resource.startsWith("ftp:") || resource.startsWith("file:") || resource.startsWith("jar:"))) {
                try {
                    if (resource.startsWith("./")) {
                        resource = resource.substring(2);
                    }
                    File file = new File(resource).getCanonicalFile();
                    return new PathResource(file);
                }
                catch (IOException e2) {
                    e2.addSuppressed(e);
                    throw e2;
                }
            }
            LOG.warn("Bad Resource: " + resource, new Object[0]);
            throw e;
        }
        return Resource.newResource(url, useCaches);
    }

    public static Resource newResource(File file) {
        return new PathResource(file.toPath());
    }

    public static Resource newSystemResource(String resource) throws IOException {
        URL url = null;
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader != null) {
            try {
                url = loader.getResource(resource);
                if (url == null && resource.startsWith("/")) {
                    url = loader.getResource(resource.substring(1));
                }
            }
            catch (IllegalArgumentException e) {
                LOG.ignore(e);
                url = null;
            }
        }
        if (url == null && (loader = Resource.class.getClassLoader()) != null && (url = loader.getResource(resource)) == null && resource.startsWith("/")) {
            url = loader.getResource(resource.substring(1));
        }
        if (url == null && (url = ClassLoader.getSystemResource(resource)) == null && resource.startsWith("/")) {
            url = ClassLoader.getSystemResource(resource.substring(1));
        }
        if (url == null) {
            return null;
        }
        return Resource.newResource(url);
    }

    public static Resource newClassPathResource(String resource) {
        return Resource.newClassPathResource(resource, true, false);
    }

    public static Resource newClassPathResource(String name, boolean useCaches, boolean checkParents) {
        URL url = Resource.class.getResource(name);
        if (url == null) {
            url = Loader.getResource(name);
        }
        if (url == null) {
            return null;
        }
        return Resource.newResource(url, useCaches);
    }

    public static boolean isContainedIn(Resource r, Resource containingResource) throws MalformedURLException {
        return r.isContainedIn(containingResource);
    }

    protected void finalize() {
        this.close();
    }

    public abstract boolean isContainedIn(Resource var1) throws MalformedURLException;

    public final void release() {
        this.close();
    }

    @Override
    public abstract void close();

    public abstract boolean exists();

    public abstract boolean isDirectory();

    public abstract long lastModified();

    public abstract long length();

    @Deprecated
    public abstract URL getURL();

    public URI getURI() {
        try {
            return this.getURL().toURI();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public abstract File getFile() throws IOException;

    public abstract String getName();

    public abstract InputStream getInputStream() throws IOException;

    public abstract ReadableByteChannel getReadableByteChannel() throws IOException;

    public abstract boolean delete() throws SecurityException;

    public abstract boolean renameTo(Resource var1) throws SecurityException;

    public abstract String[] list();

    public abstract Resource addPath(String var1) throws IOException, MalformedURLException;

    @Override
    public Resource getResource(String path) {
        try {
            return this.addPath(path);
        }
        catch (Exception e) {
            LOG.debug(e);
            return null;
        }
    }

    @Deprecated
    public String encode(String uri) {
        return null;
    }

    public Object getAssociate() {
        return this._associate;
    }

    public void setAssociate(Object o) {
        this._associate = o;
    }

    public boolean isAlias() {
        return this.getAlias() != null;
    }

    public URI getAlias() {
        return null;
    }

    public String getListHTML(String base, boolean parent) throws IOException {
        if ((base = URIUtil.canonicalPath(base)) == null || !this.isDirectory()) {
            return null;
        }
        Object[] ls = this.list();
        if (ls == null) {
            return null;
        }
        Arrays.sort(ls);
        String decodedBase = URIUtil.decodePath(base);
        String title = "Directory: " + Resource.deTag(decodedBase);
        StringBuilder buf = new StringBuilder(4096);
        buf.append("<HTML><HEAD>");
        buf.append("<LINK HREF=\"").append("jetty-dir.css").append("\" REL=\"stylesheet\" TYPE=\"text/css\"/><TITLE>");
        buf.append(title);
        buf.append("</TITLE></HEAD><BODY>\n<H1>");
        buf.append(title);
        buf.append("</H1>\n<TABLE BORDER=0>\n");
        if (parent) {
            buf.append("<TR><TD><A HREF=\"");
            buf.append(URIUtil.addEncodedPaths(base, "../"));
            buf.append("\">Parent Directory</A></TD><TD></TD><TD></TD></TR>\n");
        }
        String encodedBase = Resource.hrefEncodeURI(base);
        DateFormat dfmt = DateFormat.getDateTimeInstance(2, 2);
        for (int i = 0; i < ls.length; ++i) {
            Resource item = this.addPath((String)ls[i]);
            buf.append("\n<TR><TD><A HREF=\"");
            String path = URIUtil.addEncodedPaths(encodedBase, URIUtil.encodePath((String)ls[i]));
            buf.append(path);
            if (item.isDirectory() && !path.endsWith("/")) {
                buf.append("/");
            }
            buf.append("\">");
            buf.append(Resource.deTag((String)ls[i]));
            buf.append("&nbsp;");
            buf.append("</A></TD><TD ALIGN=right>");
            buf.append(item.length());
            buf.append(" bytes&nbsp;</TD><TD>");
            buf.append(dfmt.format(new Date(item.lastModified())));
            buf.append("</TD></TR>");
        }
        buf.append("</TABLE>\n");
        buf.append("</BODY></HTML>\n");
        return buf.toString();
    }

    private static String hrefEncodeURI(String raw) {
        char c;
        int i;
        StringBuffer buf = null;
        block9: for (i = 0; i < raw.length(); ++i) {
            c = raw.charAt(i);
            switch (c) {
                case '\"': 
                case '\'': 
                case '<': 
                case '>': {
                    buf = new StringBuffer(raw.length() << 1);
                    break block9;
                }
                default: {
                    continue block9;
                }
            }
        }
        if (buf == null) {
            return raw;
        }
        block10: for (i = 0; i < raw.length(); ++i) {
            c = raw.charAt(i);
            switch (c) {
                case '\"': {
                    buf.append("%22");
                    continue block10;
                }
                case '\'': {
                    buf.append("%27");
                    continue block10;
                }
                case '<': {
                    buf.append("%3C");
                    continue block10;
                }
                case '>': {
                    buf.append("%3E");
                    continue block10;
                }
                default: {
                    buf.append(c);
                    continue block10;
                }
            }
        }
        return buf.toString();
    }

    private static String deTag(String raw) {
        return StringUtil.sanitizeXmlString(raw);
    }

    public void writeTo(OutputStream out, long start, long count) throws IOException {
        try (InputStream in = this.getInputStream();){
            in.skip(start);
            if (count < 0L) {
                IO.copy(in, out);
            } else {
                IO.copy(in, out, count);
            }
        }
    }

    public void copyTo(File destination) throws IOException {
        if (destination.exists()) {
            throw new IllegalArgumentException(destination + " exists");
        }
        try (FileOutputStream out = new FileOutputStream(destination);){
            this.writeTo(out, 0L, -1L);
        }
    }

    public String getWeakETag() {
        return this.getWeakETag("");
    }

    public String getWeakETag(String suffix) {
        try {
            StringBuilder b = new StringBuilder(32);
            b.append("W/\"");
            String name = this.getName();
            int length = name.length();
            long lhash = 0L;
            for (int i = 0; i < length; ++i) {
                lhash = 31L * lhash + (long)name.charAt(i);
            }
            B64Code.encode(this.lastModified() ^ lhash, (Appendable)b);
            B64Code.encode(this.length() ^ lhash, (Appendable)b);
            b.append(suffix);
            b.append('\"');
            return b.toString();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Collection<Resource> getAllResources() {
        try {
            ArrayList<Resource> deep = new ArrayList<Resource>();
            String[] list = this.list();
            if (list != null) {
                for (String i : list) {
                    Resource r = this.addPath(i);
                    if (r.isDirectory()) {
                        deep.addAll(r.getAllResources());
                        continue;
                    }
                    deep.add(r);
                }
            }
            return deep;
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static URL toURL(File file) throws MalformedURLException {
        return file.toURI().toURL();
    }
}

