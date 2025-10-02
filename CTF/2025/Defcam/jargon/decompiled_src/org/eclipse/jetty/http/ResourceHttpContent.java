/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.http;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.jetty.http.CompressedContentFormat;
import org.eclipse.jetty.http.DateGenerator;
import org.eclipse.jetty.http.HttpContent;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.http.PrecompressedHttpContent;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.resource.Resource;

public class ResourceHttpContent
implements HttpContent {
    final Resource _resource;
    final String _contentType;
    final int _maxBuffer;
    Map<CompressedContentFormat, HttpContent> _precompressedContents;
    String _etag;

    public ResourceHttpContent(Resource resource, String contentType) {
        this(resource, contentType, -1, null);
    }

    public ResourceHttpContent(Resource resource, String contentType, int maxBuffer) {
        this(resource, contentType, maxBuffer, null);
    }

    public ResourceHttpContent(Resource resource, String contentType, int maxBuffer, Map<CompressedContentFormat, HttpContent> precompressedContents) {
        this._resource = resource;
        this._contentType = contentType;
        this._maxBuffer = maxBuffer;
        if (precompressedContents == null) {
            this._precompressedContents = null;
        } else {
            this._precompressedContents = new HashMap<CompressedContentFormat, HttpContent>(precompressedContents.size());
            for (Map.Entry<CompressedContentFormat, HttpContent> entry : precompressedContents.entrySet()) {
                this._precompressedContents.put(entry.getKey(), new PrecompressedHttpContent(this, entry.getValue(), entry.getKey()));
            }
        }
    }

    @Override
    public String getContentTypeValue() {
        return this._contentType;
    }

    @Override
    public HttpField getContentType() {
        return this._contentType == null ? null : new HttpField(HttpHeader.CONTENT_TYPE, this._contentType);
    }

    @Override
    public HttpField getContentEncoding() {
        return null;
    }

    @Override
    public String getContentEncodingValue() {
        return null;
    }

    @Override
    public String getCharacterEncoding() {
        return this._contentType == null ? null : MimeTypes.getCharsetFromContentType(this._contentType);
    }

    @Override
    public MimeTypes.Type getMimeType() {
        return this._contentType == null ? null : MimeTypes.CACHE.get(MimeTypes.getContentTypeWithoutCharset(this._contentType));
    }

    @Override
    public HttpField getLastModified() {
        long lm = this._resource.lastModified();
        return lm >= 0L ? new HttpField(HttpHeader.LAST_MODIFIED, DateGenerator.formatDate(lm)) : null;
    }

    @Override
    public String getLastModifiedValue() {
        long lm = this._resource.lastModified();
        return lm >= 0L ? DateGenerator.formatDate(lm) : null;
    }

    @Override
    public ByteBuffer getDirectBuffer() {
        if (this._resource.length() <= 0L || this._maxBuffer > 0 && (long)this._maxBuffer < this._resource.length()) {
            return null;
        }
        try {
            return BufferUtil.toBuffer(this._resource, true);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public HttpField getETag() {
        return new HttpField(HttpHeader.ETAG, this.getETagValue());
    }

    @Override
    public String getETagValue() {
        return this._resource.getWeakETag();
    }

    @Override
    public ByteBuffer getIndirectBuffer() {
        if (this._resource.length() <= 0L || this._maxBuffer > 0 && (long)this._maxBuffer < this._resource.length()) {
            return null;
        }
        try {
            return BufferUtil.toBuffer(this._resource, false);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public HttpField getContentLength() {
        long l = this._resource.length();
        return l == -1L ? null : new HttpField.LongValueHttpField(HttpHeader.CONTENT_LENGTH, this._resource.length());
    }

    @Override
    public long getContentLengthValue() {
        return this._resource.length();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return this._resource.getInputStream();
    }

    @Override
    public ReadableByteChannel getReadableByteChannel() throws IOException {
        return this._resource.getReadableByteChannel();
    }

    @Override
    public Resource getResource() {
        return this._resource;
    }

    @Override
    public void release() {
        this._resource.close();
    }

    public String toString() {
        return String.format("%s@%x{r=%s,ct=%s,c=%b}", this.getClass().getSimpleName(), this.hashCode(), this._resource, this._contentType, this._precompressedContents != null);
    }

    public Map<CompressedContentFormat, HttpContent> getPrecompressedContents() {
        return this._precompressedContents;
    }
}

