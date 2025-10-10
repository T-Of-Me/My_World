/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.handler.gzip;

import java.nio.ByteBuffer;
import org.eclipse.jetty.http.GZIPContentDecoder;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.server.HttpInput;
import org.eclipse.jetty.util.component.Destroyable;

public class GzipHttpInputInterceptor
implements HttpInput.Interceptor,
Destroyable {
    private final Decoder _decoder;
    private ByteBuffer _chunk;

    public GzipHttpInputInterceptor(ByteBufferPool pool, int bufferSize) {
        this._decoder = new Decoder(pool, bufferSize);
    }

    @Override
    public HttpInput.Content readFrom(HttpInput.Content content) {
        this._decoder.decodeChunks(content.getByteBuffer());
        final ByteBuffer chunk = this._chunk;
        if (chunk == null) {
            return null;
        }
        return new HttpInput.Content(chunk){

            @Override
            public void succeeded() {
                GzipHttpInputInterceptor.this._decoder.release(chunk);
            }
        };
    }

    @Override
    public void destroy() {
        this._decoder.destroy();
    }

    private class Decoder
    extends GZIPContentDecoder {
        private Decoder(ByteBufferPool pool, int bufferSize) {
            super(pool, bufferSize);
        }

        @Override
        protected boolean decodedChunk(ByteBuffer chunk) {
            GzipHttpInputInterceptor.this._chunk = chunk;
            return true;
        }

        @Override
        public void decodeChunks(ByteBuffer compressed) {
            GzipHttpInputInterceptor.this._chunk = null;
            super.decodeChunks(compressed);
        }
    }
}

