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
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.util.resource.Resource;

public interface HttpContent {
    public HttpField getContentType();

    public String getContentTypeValue();

    public String getCharacterEncoding();

    public MimeTypes.Type getMimeType();

    public HttpField getContentEncoding();

    public String getContentEncodingValue();

    public HttpField getContentLength();

    public long getContentLengthValue();

    public HttpField getLastModified();

    public String getLastModifiedValue();

    public HttpField getETag();

    public String getETagValue();

    public ByteBuffer getIndirectBuffer();

    public ByteBuffer getDirectBuffer();

    public Resource getResource();

    public InputStream getInputStream() throws IOException;

    public ReadableByteChannel getReadableByteChannel() throws IOException;

    public void release();

    public Map<CompressedContentFormat, ? extends HttpContent> getPrecompressedContents();

    public static interface ContentFactory {
        public HttpContent getContent(String var1, int var2) throws IOException;
    }
}

