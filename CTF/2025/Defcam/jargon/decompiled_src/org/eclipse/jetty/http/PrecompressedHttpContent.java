/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.http;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Map;
import org.eclipse.jetty.http.CompressedContentFormat;
import org.eclipse.jetty.http.HttpContent;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.util.resource.Resource;

public class PrecompressedHttpContent
implements HttpContent {
    private final HttpContent _content;
    private final HttpContent _precompressedContent;
    private final CompressedContentFormat _format;

    public PrecompressedHttpContent(HttpContent content, HttpContent precompressedContent, CompressedContentFormat format) {
        this._content = content;
        this._precompressedContent = precompressedContent;
        this._format = format;
        if (this._precompressedContent == null || this._format == null) {
            throw new NullPointerException("Missing compressed content and/or format");
        }
    }

    public int hashCode() {
        return this._content.hashCode();
    }

    public boolean equals(Object obj) {
        return this._content.equals(obj);
    }

    @Override
    public Resource getResource() {
        return this._content.getResource();
    }

    @Override
    public HttpField getETag() {
        return new HttpField(HttpHeader.ETAG, this.getETagValue());
    }

    @Override
    public String getETagValue() {
        return this._content.getResource().getWeakETag(this._format._etag);
    }

    @Override
    public HttpField getLastModified() {
        return this._content.getLastModified();
    }

    @Override
    public String getLastModifiedValue() {
        return this._content.getLastModifiedValue();
    }

    @Override
    public HttpField getContentType() {
        return this._content.getContentType();
    }

    @Override
    public String getContentTypeValue() {
        return this._content.getContentTypeValue();
    }

    @Override
    public HttpField getContentEncoding() {
        return this._format._contentEncoding;
    }

    @Override
    public String getContentEncodingValue() {
        return this._format._contentEncoding.getValue();
    }

    @Override
    public String getCharacterEncoding() {
        return this._content.getCharacterEncoding();
    }

    @Override
    public MimeTypes.Type getMimeType() {
        return this._content.getMimeType();
    }

    @Override
    public void release() {
        this._content.release();
    }

    @Override
    public ByteBuffer getIndirectBuffer() {
        return this._precompressedContent.getIndirectBuffer();
    }

    @Override
    public ByteBuffer getDirectBuffer() {
        return this._precompressedContent.getDirectBuffer();
    }

    @Override
    public HttpField getContentLength() {
        return this._precompressedContent.getContentLength();
    }

    @Override
    public long getContentLengthValue() {
        return this._precompressedContent.getContentLengthValue();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return this._precompressedContent.getInputStream();
    }

    @Override
    public ReadableByteChannel getReadableByteChannel() throws IOException {
        return this._precompressedContent.getReadableByteChannel();
    }

    public String toString() {
        return String.format("PrecompressedHttpContent@%x{e=%s,r=%s|%s,lm=%s|%s,ct=%s}", this.hashCode(), this._format._encoding, this._content.getResource(), this._precompressedContent.getResource(), this._content.getResource().lastModified(), this._precompressedContent.getResource().lastModified(), this.getContentType());
    }

    public Map<CompressedContentFormat, HttpContent> getPrecompressedContents() {
        return null;
    }
}

