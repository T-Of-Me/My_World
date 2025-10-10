/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server;

import java.io.IOException;
import java.util.HashMap;
import org.eclipse.jetty.http.CompressedContentFormat;
import org.eclipse.jetty.http.HttpContent;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.http.ResourceHttpContent;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;

public class ResourceContentFactory
implements HttpContent.ContentFactory {
    private final ResourceFactory _factory;
    private final MimeTypes _mimeTypes;
    private final CompressedContentFormat[] _precompressedFormats;

    public ResourceContentFactory(ResourceFactory factory, MimeTypes mimeTypes, CompressedContentFormat[] precompressedFormats) {
        this._factory = factory;
        this._mimeTypes = mimeTypes;
        this._precompressedFormats = precompressedFormats;
    }

    @Override
    public HttpContent getContent(String pathInContext, int maxBufferSize) throws IOException {
        Resource resource = this._factory.getResource(pathInContext);
        HttpContent loaded = this.load(pathInContext, resource, maxBufferSize);
        return loaded;
    }

    private HttpContent load(String pathInContext, Resource resource, int maxBufferSize) throws IOException {
        if (resource == null || !resource.exists()) {
            return null;
        }
        if (resource.isDirectory()) {
            return new ResourceHttpContent(resource, this._mimeTypes.getMimeByExtension(resource.toString()), maxBufferSize);
        }
        String mt = this._mimeTypes.getMimeByExtension(pathInContext);
        if (this._precompressedFormats.length > 0) {
            HashMap<CompressedContentFormat, HttpContent> compressedContents = new HashMap<CompressedContentFormat, HttpContent>(this._precompressedFormats.length);
            for (CompressedContentFormat format : this._precompressedFormats) {
                String compressedPathInContext = pathInContext + format._extension;
                Resource compressedResource = this._factory.getResource(compressedPathInContext);
                if (compressedResource == null || !compressedResource.exists() || compressedResource.lastModified() < resource.lastModified() || compressedResource.length() >= resource.length()) continue;
                compressedContents.put(format, new ResourceHttpContent(compressedResource, this._mimeTypes.getMimeByExtension(compressedPathInContext), maxBufferSize));
            }
            if (!compressedContents.isEmpty()) {
                return new ResourceHttpContent(resource, mt, maxBufferSize, compressedContents);
            }
        }
        return new ResourceHttpContent(resource, mt, maxBufferSize);
    }

    public String toString() {
        return "ResourceContentFactory[" + this._factory + "]@" + this.hashCode();
    }
}

