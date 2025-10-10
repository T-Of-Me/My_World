/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.jetty.http.CompressedContentFormat;
import org.eclipse.jetty.http.DateGenerator;
import org.eclipse.jetty.http.HttpContent;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.http.PreEncodedHttpField;
import org.eclipse.jetty.http.PrecompressedHttpContent;
import org.eclipse.jetty.http.ResourceHttpContent;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;

public class CachedContentFactory
implements HttpContent.ContentFactory {
    private static final Logger LOG = Log.getLogger(CachedContentFactory.class);
    private static final Map<CompressedContentFormat, CachedPrecompressedHttpContent> NO_PRECOMPRESSED = Collections.unmodifiableMap(Collections.emptyMap());
    private final ConcurrentMap<String, CachedHttpContent> _cache;
    private final AtomicInteger _cachedSize;
    private final AtomicInteger _cachedFiles;
    private final ResourceFactory _factory;
    private final CachedContentFactory _parent;
    private final MimeTypes _mimeTypes;
    private final boolean _etags;
    private final CompressedContentFormat[] _precompressedFormats;
    private final boolean _useFileMappedBuffer;
    private int _maxCachedFileSize = 0x8000000;
    private int _maxCachedFiles = 2048;
    private int _maxCacheSize = 0x10000000;

    public CachedContentFactory(CachedContentFactory parent, ResourceFactory factory, MimeTypes mimeTypes, boolean useFileMappedBuffer, boolean etags, CompressedContentFormat[] precompressedFormats) {
        this._factory = factory;
        this._cache = new ConcurrentHashMap<String, CachedHttpContent>();
        this._cachedSize = new AtomicInteger();
        this._cachedFiles = new AtomicInteger();
        this._mimeTypes = mimeTypes;
        this._parent = parent;
        this._useFileMappedBuffer = useFileMappedBuffer;
        this._etags = etags;
        this._precompressedFormats = precompressedFormats;
    }

    public int getCachedSize() {
        return this._cachedSize.get();
    }

    public int getCachedFiles() {
        return this._cachedFiles.get();
    }

    public int getMaxCachedFileSize() {
        return this._maxCachedFileSize;
    }

    public void setMaxCachedFileSize(int maxCachedFileSize) {
        this._maxCachedFileSize = maxCachedFileSize;
        this.shrinkCache();
    }

    public int getMaxCacheSize() {
        return this._maxCacheSize;
    }

    public void setMaxCacheSize(int maxCacheSize) {
        this._maxCacheSize = maxCacheSize;
        this.shrinkCache();
    }

    public int getMaxCachedFiles() {
        return this._maxCachedFiles;
    }

    public void setMaxCachedFiles(int maxCachedFiles) {
        this._maxCachedFiles = maxCachedFiles;
        this.shrinkCache();
    }

    public boolean isUseFileMappedBuffer() {
        return this._useFileMappedBuffer;
    }

    public void flushCache() {
        if (this._cache != null) {
            while (this._cache.size() > 0) {
                for (String path : this._cache.keySet()) {
                    CachedHttpContent content = (CachedHttpContent)this._cache.remove(path);
                    if (content == null) continue;
                    content.invalidate();
                }
            }
        }
    }

    @Deprecated
    public HttpContent lookup(String pathInContext) throws IOException {
        return this.getContent(pathInContext, this._maxCachedFileSize);
    }

    @Override
    public HttpContent getContent(String pathInContext, int maxBufferSize) throws IOException {
        HttpContent httpContent;
        CachedHttpContent content = (CachedHttpContent)this._cache.get(pathInContext);
        if (content != null && content.isValid()) {
            return content;
        }
        Resource resource = this._factory.getResource(pathInContext);
        HttpContent loaded = this.load(pathInContext, resource, maxBufferSize);
        if (loaded != null) {
            return loaded;
        }
        if (this._parent != null && (httpContent = this._parent.getContent(pathInContext, maxBufferSize)) != null) {
            return httpContent;
        }
        return null;
    }

    protected boolean isCacheable(Resource resource) {
        if (this._maxCachedFiles <= 0) {
            return false;
        }
        long len = resource.length();
        return len > 0L && (this._useFileMappedBuffer || len < (long)this._maxCachedFileSize && len < (long)this._maxCacheSize);
    }

    private HttpContent load(String pathInContext, Resource resource, int maxBufferSize) throws IOException {
        if (resource == null || !resource.exists()) {
            return null;
        }
        if (resource.isDirectory()) {
            return new ResourceHttpContent(resource, this._mimeTypes.getMimeByExtension(resource.toString()), this.getMaxCachedFileSize());
        }
        if (this.isCacheable(resource)) {
            CachedHttpContent content = null;
            if (this._precompressedFormats.length > 0) {
                HashMap<CompressedContentFormat, CachedHttpContent> precompresssedContents = new HashMap<CompressedContentFormat, CachedHttpContent>(this._precompressedFormats.length);
                for (CompressedContentFormat format : this._precompressedFormats) {
                    String compressedPathInContext = pathInContext + format._extension;
                    CachedHttpContent compressedContent = (CachedHttpContent)this._cache.get(compressedPathInContext);
                    if (compressedContent == null || compressedContent.isValid()) {
                        CachedHttpContent added;
                        compressedContent = null;
                        Resource compressedResource = this._factory.getResource(compressedPathInContext);
                        if (compressedResource.exists() && compressedResource.lastModified() >= resource.lastModified() && compressedResource.length() < resource.length() && (added = this._cache.putIfAbsent(compressedPathInContext, compressedContent = new CachedHttpContent(compressedPathInContext, compressedResource, null))) != null) {
                            compressedContent.invalidate();
                            compressedContent = added;
                        }
                    }
                    if (compressedContent == null) continue;
                    precompresssedContents.put(format, compressedContent);
                }
                content = new CachedHttpContent(pathInContext, resource, precompresssedContents);
            } else {
                content = new CachedHttpContent(pathInContext, resource, null);
            }
            CachedHttpContent added = this._cache.putIfAbsent(pathInContext, content);
            if (added != null) {
                content.invalidate();
                content = added;
            }
            return content;
        }
        String mt = this._mimeTypes.getMimeByExtension(pathInContext);
        if (this._precompressedFormats.length > 0) {
            HashMap<CompressedContentFormat, HttpContent> compressedContents = new HashMap<CompressedContentFormat, HttpContent>();
            for (CompressedContentFormat format : this._precompressedFormats) {
                Resource compressedResource;
                String compressedPathInContext = pathInContext + format._extension;
                CachedHttpContent compressedContent = (CachedHttpContent)this._cache.get(compressedPathInContext);
                if (compressedContent != null && compressedContent.isValid() && compressedContent.getResource().lastModified() >= resource.lastModified()) {
                    compressedContents.put(format, compressedContent);
                }
                if (!(compressedResource = this._factory.getResource(compressedPathInContext)).exists() || compressedResource.lastModified() < resource.lastModified() || compressedResource.length() >= resource.length()) continue;
                compressedContents.put(format, new ResourceHttpContent(compressedResource, this._mimeTypes.getMimeByExtension(compressedPathInContext), maxBufferSize));
            }
            if (!compressedContents.isEmpty()) {
                return new ResourceHttpContent(resource, mt, maxBufferSize, compressedContents);
            }
        }
        return new ResourceHttpContent(resource, mt, maxBufferSize);
    }

    private void shrinkCache() {
        block0: while (this._cache.size() > 0 && (this._cachedFiles.get() > this._maxCachedFiles || this._cachedSize.get() > this._maxCacheSize)) {
            TreeSet<CachedHttpContent> sorted = new TreeSet<CachedHttpContent>(new Comparator<CachedHttpContent>(){

                @Override
                public int compare(CachedHttpContent c1, CachedHttpContent c2) {
                    if (c1._lastAccessed < c2._lastAccessed) {
                        return -1;
                    }
                    if (c1._lastAccessed > c2._lastAccessed) {
                        return 1;
                    }
                    if (c1._contentLengthValue < c2._contentLengthValue) {
                        return -1;
                    }
                    return c1._key.compareTo(c2._key);
                }
            });
            for (CachedHttpContent content : this._cache.values()) {
                sorted.add(content);
            }
            for (CachedHttpContent content : sorted) {
                if (this._cachedFiles.get() <= this._maxCachedFiles && this._cachedSize.get() <= this._maxCacheSize) continue block0;
                if (content != this._cache.remove(content.getKey())) continue;
                content.invalidate();
            }
        }
    }

    protected ByteBuffer getIndirectBuffer(Resource resource) {
        try {
            return BufferUtil.toBuffer(resource, true);
        }
        catch (IOException | IllegalArgumentException e) {
            LOG.warn(e);
            return null;
        }
    }

    protected ByteBuffer getMappedBuffer(Resource resource) {
        try {
            if (this._useFileMappedBuffer && resource.getFile() != null && resource.length() < Integer.MAX_VALUE) {
                return BufferUtil.toMappedBuffer(resource.getFile());
            }
        }
        catch (IOException | IllegalArgumentException e) {
            LOG.warn(e);
        }
        return null;
    }

    protected ByteBuffer getDirectBuffer(Resource resource) {
        try {
            return BufferUtil.toBuffer(resource, true);
        }
        catch (IOException | IllegalArgumentException e) {
            LOG.warn(e);
            return null;
        }
    }

    public String toString() {
        return "ResourceCache[" + this._parent + "," + this._factory + "]@" + this.hashCode();
    }

    public class CachedPrecompressedHttpContent
    extends PrecompressedHttpContent {
        private final CachedHttpContent _content;
        private final CachedHttpContent _precompressedContent;
        private final HttpField _etag;

        CachedPrecompressedHttpContent(CachedHttpContent content, CachedHttpContent precompressedContent, CompressedContentFormat format) {
            super(content, precompressedContent, format);
            this._content = content;
            this._precompressedContent = precompressedContent;
            this._etag = CachedContentFactory.this._etags ? new PreEncodedHttpField(HttpHeader.ETAG, this._content.getResource().getWeakETag(format._etag)) : null;
        }

        public boolean isValid() {
            return this._precompressedContent.isValid() && this._content.isValid() && this._content.getResource().lastModified() <= this._precompressedContent.getResource().lastModified();
        }

        @Override
        public HttpField getETag() {
            if (this._etag != null) {
                return this._etag;
            }
            return super.getETag();
        }

        @Override
        public String getETagValue() {
            if (this._etag != null) {
                return this._etag.getValue();
            }
            return super.getETagValue();
        }

        @Override
        public String toString() {
            return "Cached" + super.toString();
        }
    }

    public class CachedHttpContent
    implements HttpContent {
        final String _key;
        final Resource _resource;
        final int _contentLengthValue;
        final HttpField _contentType;
        final String _characterEncoding;
        final MimeTypes.Type _mimeType;
        final HttpField _contentLength;
        final HttpField _lastModified;
        final long _lastModifiedValue;
        final HttpField _etag;
        final Map<CompressedContentFormat, CachedPrecompressedHttpContent> _precompressed;
        volatile long _lastAccessed;
        AtomicReference<ByteBuffer> _indirectBuffer = new AtomicReference();
        AtomicReference<ByteBuffer> _directBuffer = new AtomicReference();

        CachedHttpContent(String pathInContext, Resource resource, Map<CompressedContentFormat, CachedHttpContent> precompressedResources) {
            this._key = pathInContext;
            this._resource = resource;
            String contentType = CachedContentFactory.this._mimeTypes.getMimeByExtension(this._resource.toString());
            this._contentType = contentType == null ? null : new PreEncodedHttpField(HttpHeader.CONTENT_TYPE, contentType);
            this._characterEncoding = this._contentType == null ? null : MimeTypes.getCharsetFromContentType(contentType);
            this._mimeType = this._contentType == null ? null : MimeTypes.CACHE.get(MimeTypes.getContentTypeWithoutCharset(contentType));
            boolean exists = resource.exists();
            this._lastModifiedValue = exists ? resource.lastModified() : -1L;
            this._lastModified = this._lastModifiedValue == -1L ? null : new PreEncodedHttpField(HttpHeader.LAST_MODIFIED, DateGenerator.formatDate(this._lastModifiedValue));
            this._contentLengthValue = exists ? (int)resource.length() : 0;
            this._contentLength = new PreEncodedHttpField(HttpHeader.CONTENT_LENGTH, Long.toString(this._contentLengthValue));
            if (CachedContentFactory.this._cachedFiles.incrementAndGet() > CachedContentFactory.this._maxCachedFiles) {
                CachedContentFactory.this.shrinkCache();
            }
            this._lastAccessed = System.currentTimeMillis();
            HttpField httpField = this._etag = CachedContentFactory.this._etags ? new PreEncodedHttpField(HttpHeader.ETAG, resource.getWeakETag()) : null;
            if (precompressedResources != null) {
                this._precompressed = new HashMap<CompressedContentFormat, CachedPrecompressedHttpContent>(precompressedResources.size());
                for (Map.Entry<CompressedContentFormat, CachedHttpContent> entry : precompressedResources.entrySet()) {
                    this._precompressed.put(entry.getKey(), new CachedPrecompressedHttpContent(this, entry.getValue(), entry.getKey()));
                }
            } else {
                this._precompressed = NO_PRECOMPRESSED;
            }
        }

        public String getKey() {
            return this._key;
        }

        public boolean isCached() {
            return this._key != null;
        }

        public boolean isMiss() {
            return false;
        }

        @Override
        public Resource getResource() {
            return this._resource;
        }

        @Override
        public HttpField getETag() {
            return this._etag;
        }

        @Override
        public String getETagValue() {
            return this._etag.getValue();
        }

        boolean isValid() {
            if (this._lastModifiedValue == this._resource.lastModified() && (long)this._contentLengthValue == this._resource.length()) {
                this._lastAccessed = System.currentTimeMillis();
                return true;
            }
            if (this == CachedContentFactory.this._cache.remove(this._key)) {
                this.invalidate();
            }
            return false;
        }

        protected void invalidate() {
            ByteBuffer direct;
            ByteBuffer indirect = this._indirectBuffer.get();
            if (indirect != null && this._indirectBuffer.compareAndSet(indirect, null)) {
                CachedContentFactory.this._cachedSize.addAndGet(-BufferUtil.length(indirect));
            }
            if ((direct = this._directBuffer.get()) != null && !BufferUtil.isMappedBuffer(direct) && this._directBuffer.compareAndSet(direct, null)) {
                CachedContentFactory.this._cachedSize.addAndGet(-BufferUtil.length(direct));
            }
            CachedContentFactory.this._cachedFiles.decrementAndGet();
            this._resource.close();
        }

        @Override
        public HttpField getLastModified() {
            return this._lastModified;
        }

        @Override
        public String getLastModifiedValue() {
            return this._lastModified == null ? null : this._lastModified.getValue();
        }

        @Override
        public HttpField getContentType() {
            return this._contentType;
        }

        @Override
        public String getContentTypeValue() {
            return this._contentType == null ? null : this._contentType.getValue();
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
            return this._characterEncoding;
        }

        @Override
        public MimeTypes.Type getMimeType() {
            return this._mimeType;
        }

        @Override
        public void release() {
        }

        @Override
        public ByteBuffer getIndirectBuffer() {
            ByteBuffer buffer = this._indirectBuffer.get();
            if (buffer == null) {
                ByteBuffer buffer2 = CachedContentFactory.this.getIndirectBuffer(this._resource);
                if (buffer2 == null) {
                    LOG.warn("Could not load " + this, new Object[0]);
                } else if (this._indirectBuffer.compareAndSet(null, buffer2)) {
                    buffer = buffer2;
                    if (CachedContentFactory.this._cachedSize.addAndGet(BufferUtil.length(buffer)) > CachedContentFactory.this._maxCacheSize) {
                        CachedContentFactory.this.shrinkCache();
                    }
                } else {
                    buffer = this._indirectBuffer.get();
                }
            }
            if (buffer == null) {
                return null;
            }
            return buffer.slice();
        }

        @Override
        public ByteBuffer getDirectBuffer() {
            ByteBuffer buffer = this._directBuffer.get();
            if (buffer == null) {
                ByteBuffer direct;
                ByteBuffer mapped = CachedContentFactory.this.getMappedBuffer(this._resource);
                ByteBuffer byteBuffer = direct = mapped == null ? CachedContentFactory.this.getDirectBuffer(this._resource) : mapped;
                if (direct == null) {
                    LOG.warn("Could not load " + this, new Object[0]);
                } else if (this._directBuffer.compareAndSet(null, direct)) {
                    buffer = direct;
                    if (mapped == null && CachedContentFactory.this._cachedSize.addAndGet(BufferUtil.length(buffer)) > CachedContentFactory.this._maxCacheSize) {
                        CachedContentFactory.this.shrinkCache();
                    }
                } else {
                    buffer = this._directBuffer.get();
                }
            }
            if (buffer == null) {
                return null;
            }
            return buffer.asReadOnlyBuffer();
        }

        @Override
        public HttpField getContentLength() {
            return this._contentLength;
        }

        @Override
        public long getContentLengthValue() {
            return this._contentLengthValue;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            ByteBuffer indirect = this.getIndirectBuffer();
            if (indirect != null && indirect.hasArray()) {
                return new ByteArrayInputStream(indirect.array(), indirect.arrayOffset() + indirect.position(), indirect.remaining());
            }
            return this._resource.getInputStream();
        }

        @Override
        public ReadableByteChannel getReadableByteChannel() throws IOException {
            return this._resource.getReadableByteChannel();
        }

        public String toString() {
            return String.format("CachedContent@%x{r=%s,e=%b,lm=%s,ct=%s,c=%d}", this.hashCode(), this._resource, this._resource.exists(), this._lastModified, this._contentType, this._precompressed.size());
        }

        @Override
        public Map<CompressedContentFormat, ? extends HttpContent> getPrecompressedContents() {
            if (this._precompressed.size() == 0) {
                return null;
            }
            Map<CompressedContentFormat, CachedPrecompressedHttpContent> ret = this._precompressed;
            for (Map.Entry<CompressedContentFormat, CachedPrecompressedHttpContent> entry : this._precompressed.entrySet()) {
                if (entry.getValue().isValid()) continue;
                if (ret == this._precompressed) {
                    ret = new HashMap<CompressedContentFormat, CachedPrecompressedHttpContent>(this._precompressed);
                }
                ret.remove(entry.getKey());
            }
            return ret;
        }
    }
}

