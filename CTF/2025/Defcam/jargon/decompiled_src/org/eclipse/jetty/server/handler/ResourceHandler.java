/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.CompressedContentFormat;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.http.PreEncodedHttpField;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.ResourceContentFactory;
import org.eclipse.jetty.server.ResourceService;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;

public class ResourceHandler
extends HandlerWrapper
implements ResourceFactory,
ResourceService.WelcomeFactory {
    private static final Logger LOG = Log.getLogger(ResourceHandler.class);
    Resource _baseResource;
    ContextHandler _context;
    Resource _defaultStylesheet;
    MimeTypes _mimeTypes;
    private final ResourceService _resourceService;
    Resource _stylesheet;
    String[] _welcomes = new String[]{"index.html"};

    public ResourceHandler(ResourceService resourceService) {
        this._resourceService = resourceService;
    }

    public ResourceHandler() {
        this(new ResourceService(){

            @Override
            protected void notFound(HttpServletRequest request, HttpServletResponse response) throws IOException {
            }
        });
        this._resourceService.setGzipEquivalentFileExtensions(new ArrayList<String>(Arrays.asList(".svgz")));
    }

    @Override
    public String getWelcomeFile(String pathInContext) {
        if (this._welcomes == null) {
            return null;
        }
        String welcome_servlet = null;
        for (int i = 0; i < this._welcomes.length; ++i) {
            String welcome_in_context = URIUtil.addPaths(pathInContext, this._welcomes[i]);
            Resource welcome = this.getResource(welcome_in_context);
            if (welcome == null || !welcome.exists()) continue;
            return this._welcomes[i];
        }
        return welcome_servlet;
    }

    @Override
    public void doStart() throws Exception {
        ContextHandler.Context scontext = ContextHandler.getCurrentContext();
        this._context = scontext == null ? null : scontext.getContextHandler();
        this._mimeTypes = this._context == null ? new MimeTypes() : this._context.getMimeTypes();
        this._resourceService.setContentFactory(new ResourceContentFactory(this, this._mimeTypes, this._resourceService.getPrecompressedFormats()));
        this._resourceService.setWelcomeFactory(this);
        super.doStart();
    }

    public Resource getBaseResource() {
        if (this._baseResource == null) {
            return null;
        }
        return this._baseResource;
    }

    public String getCacheControl() {
        return this._resourceService.getCacheControl().getValue();
    }

    public List<String> getGzipEquivalentFileExtensions() {
        return this._resourceService.getGzipEquivalentFileExtensions();
    }

    public MimeTypes getMimeTypes() {
        return this._mimeTypes;
    }

    @Deprecated
    public int getMinAsyncContentLength() {
        return -1;
    }

    @Deprecated
    public int getMinMemoryMappedContentLength() {
        return -1;
    }

    @Override
    public Resource getResource(String path) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("{} getResource({})", this._context == null ? this._baseResource : this._context, this._baseResource, path);
        }
        if (path == null || !path.startsWith("/")) {
            return null;
        }
        try {
            Resource r = null;
            if (this._baseResource != null) {
                r = this._baseResource.addPath(path = URIUtil.canonicalPath(path));
                if (r != null && r.isAlias() && (this._context == null || !this._context.checkAlias(path, r))) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("resource={} alias={}", r, r.getAlias());
                    }
                    return null;
                }
            } else if (this._context != null) {
                r = this._context.getResource(path);
            }
            if ((r == null || !r.exists()) && path.endsWith("/jetty-dir.css")) {
                r = this.getStylesheet();
            }
            return r;
        }
        catch (Exception e) {
            LOG.debug(e);
            return null;
        }
    }

    public String getResourceBase() {
        if (this._baseResource == null) {
            return null;
        }
        return this._baseResource.toString();
    }

    public Resource getStylesheet() {
        if (this._stylesheet != null) {
            return this._stylesheet;
        }
        if (this._defaultStylesheet == null) {
            this._defaultStylesheet = Resource.newResource(this.getClass().getResource("/jetty-dir.css"));
        }
        return this._defaultStylesheet;
    }

    public String[] getWelcomeFiles() {
        return this._welcomes;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (baseRequest.isHandled()) {
            return;
        }
        if (!HttpMethod.GET.is(request.getMethod()) && !HttpMethod.HEAD.is(request.getMethod())) {
            super.handle(target, baseRequest, request, response);
            return;
        }
        this._resourceService.doGet(request, response);
        if (response.isCommitted()) {
            baseRequest.setHandled(true);
        } else {
            super.handle(target, baseRequest, request, response);
        }
    }

    public boolean isAcceptRanges() {
        return this._resourceService.isAcceptRanges();
    }

    public boolean isDirAllowed() {
        return this._resourceService.isDirAllowed();
    }

    public boolean isDirectoriesListed() {
        return this._resourceService.isDirAllowed();
    }

    public boolean isEtags() {
        return this._resourceService.isEtags();
    }

    @Deprecated
    public boolean isGzip() {
        for (CompressedContentFormat formats : this._resourceService.getPrecompressedFormats()) {
            if (!CompressedContentFormat.GZIP._encoding.equals(formats._encoding)) continue;
            return true;
        }
        return false;
    }

    public CompressedContentFormat[] getPrecompressedFormats() {
        return this._resourceService.getPrecompressedFormats();
    }

    public boolean isPathInfoOnly() {
        return this._resourceService.isPathInfoOnly();
    }

    public boolean isRedirectWelcome() {
        return this._resourceService.isRedirectWelcome();
    }

    public void setAcceptRanges(boolean acceptRanges) {
        this._resourceService.setAcceptRanges(acceptRanges);
    }

    public void setBaseResource(Resource base) {
        this._baseResource = base;
    }

    public void setCacheControl(String cacheControl) {
        this._resourceService.setCacheControl(new PreEncodedHttpField(HttpHeader.CACHE_CONTROL, cacheControl));
    }

    public void setDirAllowed(boolean dirAllowed) {
        this._resourceService.setDirAllowed(dirAllowed);
    }

    public void setDirectoriesListed(boolean directory) {
        this._resourceService.setDirAllowed(directory);
    }

    public void setEtags(boolean etags) {
        this._resourceService.setEtags(etags);
    }

    @Deprecated
    public void setGzip(boolean gzip) {
        CompressedContentFormat[] compressedContentFormatArray;
        if (gzip) {
            CompressedContentFormat[] compressedContentFormatArray2 = new CompressedContentFormat[1];
            compressedContentFormatArray = compressedContentFormatArray2;
            compressedContentFormatArray2[0] = CompressedContentFormat.GZIP;
        } else {
            compressedContentFormatArray = new CompressedContentFormat[]{};
        }
        this.setPrecompressedFormats(compressedContentFormatArray);
    }

    public void setGzipEquivalentFileExtensions(List<String> gzipEquivalentFileExtensions) {
        this._resourceService.setGzipEquivalentFileExtensions(gzipEquivalentFileExtensions);
    }

    public void setPrecompressedFormats(CompressedContentFormat[] precompressedFormats) {
        this._resourceService.setPrecompressedFormats(precompressedFormats);
    }

    public void setMimeTypes(MimeTypes mimeTypes) {
        this._mimeTypes = mimeTypes;
    }

    @Deprecated
    public void setMinAsyncContentLength(int minAsyncContentLength) {
    }

    @Deprecated
    public void setMinMemoryMappedContentLength(int minMemoryMappedFileSize) {
    }

    public void setPathInfoOnly(boolean pathInfoOnly) {
        this._resourceService.setPathInfoOnly(pathInfoOnly);
    }

    public void setRedirectWelcome(boolean redirectWelcome) {
        this._resourceService.setRedirectWelcome(redirectWelcome);
    }

    public void setResourceBase(String resourceBase) {
        try {
            this.setBaseResource(Resource.newResource(resourceBase));
        }
        catch (Exception e) {
            LOG.warn(e.toString(), new Object[0]);
            LOG.debug(e);
            throw new IllegalArgumentException(resourceBase);
        }
    }

    public void setStylesheet(String stylesheet) {
        try {
            this._stylesheet = Resource.newResource(stylesheet);
            if (!this._stylesheet.exists()) {
                LOG.warn("unable to find custom stylesheet: " + stylesheet, new Object[0]);
                this._stylesheet = null;
            }
        }
        catch (Exception e) {
            LOG.warn(e.toString(), new Object[0]);
            LOG.debug(e);
            throw new IllegalArgumentException(stylesheet);
        }
    }

    public void setWelcomeFiles(String[] welcomeFiles) {
        this._welcomes = welcomeFiles;
    }
}

