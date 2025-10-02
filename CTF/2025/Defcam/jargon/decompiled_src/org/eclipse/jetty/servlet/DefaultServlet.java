/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.servlet;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.StringTokenizer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.CompressedContentFormat;
import org.eclipse.jetty.http.HttpContent;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.http.PreEncodedHttpField;
import org.eclipse.jetty.http.pathmap.MappedResource;
import org.eclipse.jetty.server.CachedContentFactory;
import org.eclipse.jetty.server.ResourceContentFactory;
import org.eclipse.jetty.server.ResourceService;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;

public class DefaultServlet
extends HttpServlet
implements ResourceFactory,
ResourceService.WelcomeFactory {
    public static final String CONTEXT_INIT = "org.eclipse.jetty.servlet.Default.";
    private static final Logger LOG = Log.getLogger(DefaultServlet.class);
    private static final long serialVersionUID = 4930458713846881193L;
    private final ResourceService _resourceService;
    private ServletContext _servletContext;
    private ContextHandler _contextHandler;
    private boolean _welcomeServlets = false;
    private boolean _welcomeExactServlets = false;
    private Resource _resourceBase;
    private CachedContentFactory _cache;
    private MimeTypes _mimeTypes;
    private String[] _welcomes;
    private Resource _stylesheet;
    private boolean _useFileMappedBuffer = false;
    private String _relativeResourceBase;
    private ServletHandler _servletHandler;
    private ServletHolder _defaultHolder;

    public DefaultServlet(ResourceService resourceService) {
        this._resourceService = resourceService;
    }

    public DefaultServlet() {
        this(new ResourceService());
    }

    @Override
    public void init() throws UnavailableException {
        String cc;
        this._servletContext = this.getServletContext();
        this._contextHandler = this.initContextHandler(this._servletContext);
        this._mimeTypes = this._contextHandler.getMimeTypes();
        this._welcomes = this._contextHandler.getWelcomeFiles();
        if (this._welcomes == null) {
            this._welcomes = new String[]{"index.html", "index.jsp"};
        }
        this._resourceService.setAcceptRanges(this.getInitBoolean("acceptRanges", this._resourceService.isAcceptRanges()));
        this._resourceService.setDirAllowed(this.getInitBoolean("dirAllowed", this._resourceService.isDirAllowed()));
        this._resourceService.setRedirectWelcome(this.getInitBoolean("redirectWelcome", this._resourceService.isRedirectWelcome()));
        this._resourceService.setPrecompressedFormats(this.parsePrecompressedFormats(this.getInitParameter("precompressed"), this.getInitBoolean("gzip", false)));
        this._resourceService.setPathInfoOnly(this.getInitBoolean("pathInfoOnly", this._resourceService.isPathInfoOnly()));
        this._resourceService.setEtags(this.getInitBoolean("etags", this._resourceService.isEtags()));
        if ("exact".equals(this.getInitParameter("welcomeServlets"))) {
            this._welcomeExactServlets = true;
            this._welcomeServlets = false;
        } else {
            this._welcomeServlets = this.getInitBoolean("welcomeServlets", this._welcomeServlets);
        }
        this._useFileMappedBuffer = this.getInitBoolean("useFileMappedBuffer", this._useFileMappedBuffer);
        this._relativeResourceBase = this.getInitParameter("relativeResourceBase");
        String rb = this.getInitParameter("resourceBase");
        if (rb != null) {
            if (this._relativeResourceBase != null) {
                throw new UnavailableException("resourceBase & relativeResourceBase");
            }
            try {
                this._resourceBase = this._contextHandler.newResource(rb);
            }
            catch (Exception e) {
                LOG.warn("EXCEPTION ", e);
                throw new UnavailableException(e.toString());
            }
        }
        String css = this.getInitParameter("stylesheet");
        try {
            if (css != null) {
                this._stylesheet = Resource.newResource(css);
                if (!this._stylesheet.exists()) {
                    LOG.warn("!" + css, new Object[0]);
                    this._stylesheet = null;
                }
            }
            if (this._stylesheet == null) {
                this._stylesheet = Resource.newResource(this.getClass().getResource("/jetty-dir.css"));
            }
        }
        catch (Exception e) {
            LOG.warn(e.toString(), new Object[0]);
            LOG.debug(e);
        }
        int encodingHeaderCacheSize = this.getInitInt("encodingHeaderCacheSize", -1);
        if (encodingHeaderCacheSize >= 0) {
            this._resourceService.setEncodingCacheSize(encodingHeaderCacheSize);
        }
        if ((cc = this.getInitParameter("cacheControl")) != null) {
            this._resourceService.setCacheControl(new PreEncodedHttpField(HttpHeader.CACHE_CONTROL, cc));
        }
        String resourceCache = this.getInitParameter("resourceCache");
        int max_cache_size = this.getInitInt("maxCacheSize", -2);
        int max_cached_file_size = this.getInitInt("maxCachedFileSize", -2);
        int max_cached_files = this.getInitInt("maxCachedFiles", -2);
        if (resourceCache != null) {
            if (max_cache_size != -1 || max_cached_file_size != -2 || max_cached_files != -2) {
                LOG.debug("ignoring resource cache configuration, using resourceCache attribute", new Object[0]);
            }
            if (this._relativeResourceBase != null || this._resourceBase != null) {
                throw new UnavailableException("resourceCache specified with resource bases");
            }
            this._cache = (CachedContentFactory)this._servletContext.getAttribute(resourceCache);
        }
        try {
            if (this._cache == null && (max_cached_files != -2 || max_cache_size != -2 || max_cached_file_size != -2)) {
                this._cache = new CachedContentFactory(null, this, this._mimeTypes, this._useFileMappedBuffer, this._resourceService.isEtags(), this._resourceService.getPrecompressedFormats());
                if (max_cache_size >= 0) {
                    this._cache.setMaxCacheSize(max_cache_size);
                }
                if (max_cached_file_size >= -1) {
                    this._cache.setMaxCachedFileSize(max_cached_file_size);
                }
                if (max_cached_files >= -1) {
                    this._cache.setMaxCachedFiles(max_cached_files);
                }
                this._servletContext.setAttribute(resourceCache == null ? "resourceCache" : resourceCache, this._cache);
            }
        }
        catch (Exception e) {
            LOG.warn("EXCEPTION ", e);
            throw new UnavailableException(e.toString());
        }
        HttpContent.ContentFactory contentFactory = this._cache;
        if (contentFactory == null) {
            contentFactory = new ResourceContentFactory(this, this._mimeTypes, this._resourceService.getPrecompressedFormats());
            if (resourceCache != null) {
                this._servletContext.setAttribute(resourceCache, contentFactory);
            }
        }
        this._resourceService.setContentFactory(contentFactory);
        this._resourceService.setWelcomeFactory(this);
        ArrayList<String> gzip_equivalent_file_extensions = new ArrayList<String>();
        String otherGzipExtensions = this.getInitParameter("otherGzipFileExtensions");
        if (otherGzipExtensions != null) {
            StringTokenizer tok = new StringTokenizer(otherGzipExtensions, ",", false);
            while (tok.hasMoreTokens()) {
                String s = tok.nextToken().trim();
                gzip_equivalent_file_extensions.add(s.charAt(0) == '.' ? s : "." + s);
            }
        } else {
            gzip_equivalent_file_extensions.add(".svgz");
        }
        this._resourceService.setGzipEquivalentFileExtensions(gzip_equivalent_file_extensions);
        this._servletHandler = this._contextHandler.getChildHandlerByClass(ServletHandler.class);
        for (ServletHolder h : this._servletHandler.getServlets()) {
            if (h.getServletInstance() != this) continue;
            this._defaultHolder = h;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("resource base = " + this._resourceBase, new Object[0]);
        }
    }

    private CompressedContentFormat[] parsePrecompressedFormats(String precompressed, boolean gzip) {
        ArrayList<CompressedContentFormat> ret = new ArrayList<CompressedContentFormat>();
        if (precompressed != null && precompressed.indexOf(61) > 0) {
            for (String pair : precompressed.split(",")) {
                String[] setting = pair.split("=");
                String encoding = setting[0].trim();
                String extension = setting[1].trim();
                ret.add(new CompressedContentFormat(encoding, extension));
                if (!gzip || ret.contains(CompressedContentFormat.GZIP)) continue;
                ret.add(CompressedContentFormat.GZIP);
            }
        } else if (precompressed != null) {
            if (Boolean.parseBoolean(precompressed)) {
                ret.add(CompressedContentFormat.BR);
                ret.add(CompressedContentFormat.GZIP);
            }
        } else if (gzip) {
            ret.add(CompressedContentFormat.GZIP);
        }
        return ret.toArray(new CompressedContentFormat[ret.size()]);
    }

    protected ContextHandler initContextHandler(ServletContext servletContext) {
        ContextHandler.Context scontext = ContextHandler.getCurrentContext();
        if (scontext == null) {
            if (servletContext instanceof ContextHandler.Context) {
                return ((ContextHandler.Context)servletContext).getContextHandler();
            }
            throw new IllegalArgumentException("The servletContext " + servletContext + " " + servletContext.getClass().getName() + " is not " + ContextHandler.Context.class.getName());
        }
        return ContextHandler.getCurrentContext().getContextHandler();
    }

    @Override
    public String getInitParameter(String name) {
        String value = this.getServletContext().getInitParameter(CONTEXT_INIT + name);
        if (value == null) {
            value = super.getInitParameter(name);
        }
        return value;
    }

    private boolean getInitBoolean(String name, boolean dft) {
        String value = this.getInitParameter(name);
        if (value == null || value.length() == 0) {
            return dft;
        }
        return value.startsWith("t") || value.startsWith("T") || value.startsWith("y") || value.startsWith("Y") || value.startsWith("1");
    }

    private int getInitInt(String name, int dft) {
        String value = this.getInitParameter(name);
        if (value == null) {
            value = this.getInitParameter(name);
        }
        if (value != null && value.length() > 0) {
            return Integer.parseInt(value);
        }
        return dft;
    }

    @Override
    public Resource getResource(String pathInContext) {
        Resource r = null;
        if (this._relativeResourceBase != null) {
            pathInContext = URIUtil.addPaths(this._relativeResourceBase, pathInContext);
        }
        try {
            if (this._resourceBase != null) {
                r = this._resourceBase.addPath(pathInContext);
                if (!this._contextHandler.checkAlias(pathInContext, r)) {
                    r = null;
                }
            } else if (this._servletContext instanceof ContextHandler.Context) {
                r = this._contextHandler.getResource(pathInContext);
            } else {
                URL u = this._servletContext.getResource(pathInContext);
                r = this._contextHandler.newResource(u);
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Resource " + pathInContext + "=" + r, new Object[0]);
            }
        }
        catch (IOException e) {
            LOG.ignore(e);
        }
        if ((r == null || !r.exists()) && pathInContext.endsWith("/jetty-dir.css")) {
            r = this._stylesheet;
        }
        return r;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this._resourceService.doGet(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.doGet(request, response);
    }

    @Override
    protected void doTrace(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.sendError(405);
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Allow", "GET,HEAD,POST,OPTIONS");
    }

    @Override
    public void destroy() {
        if (this._cache != null) {
            this._cache.flushCache();
        }
        super.destroy();
    }

    @Override
    public String getWelcomeFile(String pathInContext) {
        if (this._welcomes == null) {
            return null;
        }
        String welcome_servlet = null;
        for (int i = 0; i < this._welcomes.length; ++i) {
            MappedResource<ServletHolder> entry;
            String welcome_in_context = URIUtil.addPaths(pathInContext, this._welcomes[i]);
            Resource welcome = this.getResource(welcome_in_context);
            if (welcome != null && welcome.exists()) {
                return welcome_in_context;
            }
            if (!this._welcomeServlets && !this._welcomeExactServlets || welcome_servlet != null || (entry = this._servletHandler.getMappedServlet(welcome_in_context)) == null || entry.getResource() == this._defaultHolder || !this._welcomeServlets && (!this._welcomeExactServlets || !entry.getPathSpec().getDeclaration().equals(welcome_in_context))) continue;
            welcome_servlet = welcome_in_context;
        }
        return welcome_servlet;
    }
}

