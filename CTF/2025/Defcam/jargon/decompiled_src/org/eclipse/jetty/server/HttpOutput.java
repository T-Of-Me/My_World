/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritePendingException;
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import org.eclipse.jetty.http.HttpContent;
import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.IteratingCallback;
import org.eclipse.jetty.util.IteratingNestedCallback;
import org.eclipse.jetty.util.SharedBlockingCallback;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class HttpOutput
extends ServletOutputStream
implements Runnable {
    private static Logger LOG = Log.getLogger(HttpOutput.class);
    private final HttpChannel _channel;
    private final SharedBlockingCallback _writeBlocker;
    private Interceptor _interceptor;
    private long _written;
    private ByteBuffer _aggregate;
    private int _bufferSize;
    private int _commitSize;
    private WriteListener _writeListener;
    private volatile Throwable _onError;
    private final AtomicReference<OutputState> _state = new AtomicReference<OutputState>(OutputState.OPEN);

    public HttpOutput(HttpChannel channel) {
        this._channel = channel;
        this._interceptor = channel;
        this._writeBlocker = new WriteBlocker(channel);
        HttpConfiguration config = channel.getHttpConfiguration();
        this._bufferSize = config.getOutputBufferSize();
        this._commitSize = config.getOutputAggregationSize();
        if (this._commitSize > this._bufferSize) {
            LOG.warn("OutputAggregationSize {} exceeds bufferSize {}", this._commitSize, this._bufferSize);
            this._commitSize = this._bufferSize;
        }
    }

    public HttpChannel getHttpChannel() {
        return this._channel;
    }

    public Interceptor getInterceptor() {
        return this._interceptor;
    }

    public void setInterceptor(Interceptor interceptor) {
        this._interceptor = interceptor;
    }

    public boolean isWritten() {
        return this._written > 0L;
    }

    public long getWritten() {
        return this._written;
    }

    public void reopen() {
        this._state.set(OutputState.OPEN);
    }

    private boolean isLastContentToWrite(int len) {
        this._written += (long)len;
        return this._channel.getResponse().isAllContentWritten(this._written);
    }

    public boolean isAllContentWritten() {
        return this._channel.getResponse().isAllContentWritten(this._written);
    }

    protected SharedBlockingCallback.Blocker acquireWriteBlockingCallback() throws IOException {
        return this._writeBlocker.acquire();
    }

    private void write(ByteBuffer content, boolean complete) throws IOException {
        try (SharedBlockingCallback.Blocker blocker = this._writeBlocker.acquire();){
            this.write(content, complete, blocker);
            blocker.block();
        }
        catch (Exception failure) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(failure);
            }
            this.abort(failure);
            if (failure instanceof IOException) {
                throw failure;
            }
            throw new IOException(failure);
        }
    }

    protected void write(ByteBuffer content, boolean complete, Callback callback) {
        this._interceptor.write(content, complete, callback);
    }

    private void abort(Throwable failure) {
        this.closed();
        this._channel.abort(failure);
    }

    @Override
    public void close() {
        block10: while (true) {
            OutputState state = this._state.get();
            switch (state) {
                case CLOSED: {
                    return;
                }
                case ASYNC: {
                    if (this._state.compareAndSet(state, OutputState.READY)) continue block10;
                    continue block10;
                }
                case UNREADY: 
                case PENDING: {
                    if (!this._state.compareAndSet(state, OutputState.CLOSED)) continue block10;
                    IOException ex = new IOException("Closed while Pending/Unready");
                    LOG.warn(ex.toString(), new Object[0]);
                    LOG.debug(ex);
                    this._channel.abort(ex);
                    return;
                }
            }
            if (this._state.compareAndSet(state, OutputState.CLOSED)) break;
        }
        try {
            this.write(BufferUtil.hasContent(this._aggregate) ? this._aggregate : BufferUtil.EMPTY_BUFFER, !this._channel.getResponse().isIncluding());
        }
        catch (IOException x) {
            LOG.ignore(x);
        }
        finally {
            this.releaseBuffer();
        }
    }

    void closed() {
        block9: while (true) {
            OutputState state = this._state.get();
            switch (state) {
                case CLOSED: {
                    return;
                }
                case UNREADY: {
                    if (!this._state.compareAndSet(state, OutputState.ERROR)) continue block9;
                    this._writeListener.onError(this._onError == null ? new EofException("Async closed") : this._onError);
                    continue block9;
                }
            }
            if (this._state.compareAndSet(state, OutputState.CLOSED)) break;
        }
        try {
            this._channel.getResponse().closeOutput();
        }
        catch (Throwable x) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(x);
            }
            this.abort(x);
        }
        finally {
            this.releaseBuffer();
        }
    }

    private void releaseBuffer() {
        if (this._aggregate != null) {
            this._channel.getConnector().getByteBufferPool().release(this._aggregate);
            this._aggregate = null;
        }
    }

    public boolean isClosed() {
        return this._state.get() == OutputState.CLOSED;
    }

    public boolean isAsync() {
        switch (this._state.get()) {
            case ASYNC: 
            case UNREADY: 
            case PENDING: 
            case READY: {
                return true;
            }
        }
        return false;
    }

    @Override
    public void flush() throws IOException {
        block9: while (true) {
            switch (this._state.get()) {
                case OPEN: {
                    this.write(BufferUtil.hasContent(this._aggregate) ? this._aggregate : BufferUtil.EMPTY_BUFFER, false);
                    return;
                }
                case ASYNC: {
                    throw new IllegalStateException("isReady() not called");
                }
                case READY: {
                    if (!this._state.compareAndSet(OutputState.READY, OutputState.PENDING)) continue block9;
                    new AsyncFlush().iterate();
                    return;
                }
                case PENDING: {
                    return;
                }
                case UNREADY: {
                    throw new WritePendingException();
                }
                case ERROR: {
                    throw new EofException(this._onError);
                }
                case CLOSED: {
                    return;
                }
            }
            break;
        }
        throw new IllegalStateException();
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        block8: while (true) {
            switch (this._state.get()) {
                case OPEN: {
                    break block8;
                }
                case ASYNC: {
                    throw new IllegalStateException("isReady() not called");
                }
                case READY: {
                    if (!this._state.compareAndSet(OutputState.READY, OutputState.PENDING)) continue block8;
                    boolean last = this.isLastContentToWrite(len);
                    if (!last && len <= this._commitSize) {
                        int filled;
                        if (this._aggregate == null) {
                            this._aggregate = this._channel.getByteBufferPool().acquire(this.getBufferSize(), this._interceptor.isOptimizedForDirectBuffers());
                        }
                        if ((filled = BufferUtil.fill(this._aggregate, b, off, len)) == len && !BufferUtil.isFull(this._aggregate)) {
                            if (!this._state.compareAndSet(OutputState.PENDING, OutputState.ASYNC)) {
                                throw new IllegalStateException();
                            }
                            return;
                        }
                        off += filled;
                        len -= filled;
                    }
                    new AsyncWrite(b, off, len, last).iterate();
                    return;
                }
                case UNREADY: 
                case PENDING: {
                    throw new WritePendingException();
                }
                case ERROR: {
                    throw new EofException(this._onError);
                }
                case CLOSED: {
                    throw new EofException("Closed");
                }
                default: {
                    throw new IllegalStateException();
                }
            }
            break;
        }
        int capacity = this.getBufferSize();
        boolean last = this.isLastContentToWrite(len);
        if (!last && len <= this._commitSize) {
            int filled;
            if (this._aggregate == null) {
                this._aggregate = this._channel.getByteBufferPool().acquire(capacity, this._interceptor.isOptimizedForDirectBuffers());
            }
            if ((filled = BufferUtil.fill(this._aggregate, b, off, len)) == len && !BufferUtil.isFull(this._aggregate)) {
                return;
            }
            off += filled;
            len -= filled;
        }
        if (BufferUtil.hasContent(this._aggregate)) {
            this.write(this._aggregate, last && len == 0);
            if (len > 0 && !last && len <= this._commitSize && len <= BufferUtil.space(this._aggregate)) {
                BufferUtil.append(this._aggregate, b, off, len);
                return;
            }
        }
        if (len > 0) {
            ByteBuffer view = ByteBuffer.wrap(b, off, len);
            while (len > this.getBufferSize()) {
                int p = view.position();
                int l = p + this.getBufferSize();
                view.limit(p + this.getBufferSize());
                this.write(view, false);
                view.limit(l + Math.min(len -= this.getBufferSize(), this.getBufferSize()));
                view.position(l);
            }
            this.write(view, last);
        } else if (last) {
            this.write(BufferUtil.EMPTY_BUFFER, true);
        }
        if (last) {
            this.closed();
        }
    }

    public void write(ByteBuffer buffer) throws IOException {
        block8: while (true) {
            switch (this._state.get()) {
                case OPEN: {
                    break block8;
                }
                case ASYNC: {
                    throw new IllegalStateException("isReady() not called");
                }
                case READY: {
                    if (!this._state.compareAndSet(OutputState.READY, OutputState.PENDING)) continue block8;
                    boolean last = this.isLastContentToWrite(buffer.remaining());
                    new AsyncWrite(buffer, last).iterate();
                    return;
                }
                case UNREADY: 
                case PENDING: {
                    throw new WritePendingException();
                }
                case ERROR: {
                    throw new EofException(this._onError);
                }
                case CLOSED: {
                    throw new EofException("Closed");
                }
                default: {
                    throw new IllegalStateException();
                }
            }
            break;
        }
        int len = BufferUtil.length(buffer);
        boolean last = this.isLastContentToWrite(len);
        if (BufferUtil.hasContent(this._aggregate)) {
            this.write(this._aggregate, last && len == 0);
        }
        if (len > 0) {
            this.write(buffer, last);
        } else if (last) {
            this.write(BufferUtil.EMPTY_BUFFER, true);
        }
        if (last) {
            this.closed();
        }
    }

    @Override
    public void write(int b) throws IOException {
        ++this._written;
        boolean complete = this._channel.getResponse().isAllContentWritten(this._written);
        block8: while (true) {
            switch (this._state.get()) {
                case OPEN: {
                    if (this._aggregate == null) {
                        this._aggregate = this._channel.getByteBufferPool().acquire(this.getBufferSize(), this._interceptor.isOptimizedForDirectBuffers());
                    }
                    BufferUtil.append(this._aggregate, (byte)b);
                    if (!complete && !BufferUtil.isFull(this._aggregate)) break block8;
                    this.write(this._aggregate, complete);
                    if (!complete) break block8;
                    this.closed();
                    break block8;
                }
                case ASYNC: {
                    throw new IllegalStateException("isReady() not called");
                }
                case READY: {
                    if (!this._state.compareAndSet(OutputState.READY, OutputState.PENDING)) continue block8;
                    if (this._aggregate == null) {
                        this._aggregate = this._channel.getByteBufferPool().acquire(this.getBufferSize(), this._interceptor.isOptimizedForDirectBuffers());
                    }
                    BufferUtil.append(this._aggregate, (byte)b);
                    if (!complete && !BufferUtil.isFull(this._aggregate)) {
                        if (!this._state.compareAndSet(OutputState.PENDING, OutputState.ASYNC)) {
                            throw new IllegalStateException();
                        }
                        return;
                    }
                    new AsyncFlush().iterate();
                    return;
                }
                case UNREADY: 
                case PENDING: {
                    throw new WritePendingException();
                }
                case ERROR: {
                    throw new EofException(this._onError);
                }
                case CLOSED: {
                    throw new EofException("Closed");
                }
                default: {
                    throw new IllegalStateException();
                }
            }
            break;
        }
    }

    @Override
    public void print(String s) throws IOException {
        if (this.isClosed()) {
            throw new IOException("Closed");
        }
        this.write(s.getBytes(this._channel.getResponse().getCharacterEncoding()));
    }

    public void sendContent(ByteBuffer content) throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("sendContent({})", BufferUtil.toDetailString(content));
        }
        this._written += (long)content.remaining();
        this.write(content, true);
        this.closed();
    }

    public void sendContent(InputStream in) throws IOException {
        try (SharedBlockingCallback.Blocker blocker = this._writeBlocker.acquire();){
            new InputStreamWritingCB(in, blocker).iterate();
            blocker.block();
        }
        catch (Throwable failure) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(failure);
            }
            this.abort(failure);
            throw failure;
        }
    }

    public void sendContent(ReadableByteChannel in) throws IOException {
        try (SharedBlockingCallback.Blocker blocker = this._writeBlocker.acquire();){
            new ReadableByteChannelWritingCB(in, blocker).iterate();
            blocker.block();
        }
        catch (Throwable failure) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(failure);
            }
            this.abort(failure);
            throw failure;
        }
    }

    public void sendContent(HttpContent content) throws IOException {
        try (SharedBlockingCallback.Blocker blocker = this._writeBlocker.acquire();){
            this.sendContent(content, (Callback)blocker);
            blocker.block();
        }
        catch (Throwable failure) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(failure);
            }
            this.abort(failure);
            throw failure;
        }
    }

    public void sendContent(ByteBuffer content, Callback callback) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("sendContent(buffer={},{})", BufferUtil.toDetailString(content), callback);
        }
        this._written += (long)content.remaining();
        this.write(content, true, new Callback.Nested(callback){

            @Override
            public void succeeded() {
                HttpOutput.this.closed();
                super.succeeded();
            }

            @Override
            public void failed(Throwable x) {
                HttpOutput.this.abort(x);
                super.failed(x);
            }
        });
    }

    public void sendContent(InputStream in, Callback callback) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("sendContent(stream={},{})", in, callback);
        }
        new InputStreamWritingCB(in, callback).iterate();
    }

    public void sendContent(ReadableByteChannel in, Callback callback) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("sendContent(channel={},{})", in, callback);
        }
        new ReadableByteChannelWritingCB(in, callback).iterate();
    }

    public void sendContent(HttpContent httpContent, Callback callback) {
        ByteBuffer buffer;
        if (LOG.isDebugEnabled()) {
            LOG.debug("sendContent(http={},{})", httpContent, callback);
        }
        if (BufferUtil.hasContent(this._aggregate)) {
            callback.failed(new IOException("cannot sendContent() after write()"));
            return;
        }
        if (this._channel.isCommitted()) {
            callback.failed(new IOException("cannot sendContent(), output already committed"));
            return;
        }
        block7: while (true) {
            switch (this._state.get()) {
                case OPEN: {
                    if (this._state.compareAndSet(OutputState.OPEN, OutputState.PENDING)) break block7;
                    continue block7;
                }
                case ERROR: {
                    callback.failed(new EofException(this._onError));
                    return;
                }
                case CLOSED: {
                    callback.failed(new EofException("Closed"));
                    return;
                }
                default: {
                    throw new IllegalStateException();
                }
            }
            break;
        }
        ByteBuffer byteBuffer = buffer = this._channel.useDirectBuffers() ? httpContent.getDirectBuffer() : null;
        if (buffer == null) {
            buffer = httpContent.getIndirectBuffer();
        }
        if (buffer != null) {
            this.sendContent(buffer, callback);
            return;
        }
        try {
            ReadableByteChannel rbc = httpContent.getReadableByteChannel();
            if (rbc != null) {
                this.sendContent(rbc, callback);
                return;
            }
            InputStream in = httpContent.getInputStream();
            if (in != null) {
                this.sendContent(in, callback);
                return;
            }
            throw new IllegalArgumentException("unknown content for " + httpContent);
        }
        catch (Throwable th) {
            this.abort(th);
            callback.failed(th);
            return;
        }
    }

    public int getBufferSize() {
        return this._bufferSize;
    }

    public void setBufferSize(int size) {
        this._bufferSize = size;
        this._commitSize = size;
    }

    public void recycle() {
        this._interceptor = this._channel;
        HttpConfiguration config = this._channel.getHttpConfiguration();
        this._bufferSize = config.getOutputBufferSize();
        this._commitSize = config.getOutputAggregationSize();
        if (this._commitSize > this._bufferSize) {
            this._commitSize = this._bufferSize;
        }
        this.releaseBuffer();
        this._written = 0L;
        this._writeListener = null;
        this._onError = null;
        this.reopen();
    }

    public void resetBuffer() {
        this._interceptor.resetBuffer();
        if (BufferUtil.hasContent(this._aggregate)) {
            BufferUtil.clear(this._aggregate);
        }
        this._written = 0L;
        this.reopen();
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
        if (!this._channel.getState().isAsync()) {
            throw new IllegalStateException("!ASYNC");
        }
        if (this._state.compareAndSet(OutputState.OPEN, OutputState.READY)) {
            this._writeListener = writeListener;
            if (this._channel.getState().onWritePossible()) {
                this._channel.execute(this._channel);
            }
        } else {
            throw new IllegalStateException();
        }
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    @Override
    public boolean isReady() {
        block9: while (true) {
            switch (this._state.get()) {
                case OPEN: {
                    return true;
                }
                case ASYNC: {
                    if (!this._state.compareAndSet(OutputState.ASYNC, OutputState.READY)) continue block9;
                    return true;
                }
                case READY: {
                    return true;
                }
                case PENDING: {
                    if (this._state.compareAndSet(OutputState.PENDING, OutputState.UNREADY)) return false;
                    continue block9;
                }
                case UNREADY: {
                    return false;
                }
                case ERROR: {
                    return true;
                }
                case CLOSED: {
                    return true;
                }
            }
            break;
        }
        throw new IllegalStateException();
    }

    @Override
    public void run() {
        while (true) {
            OutputState state = this._state.get();
            if (this._onError != null) {
                switch (state) {
                    case CLOSED: 
                    case ERROR: {
                        this._onError = null;
                        return;
                    }
                }
                if (!this._state.compareAndSet(state, OutputState.ERROR)) continue;
                Throwable th = this._onError;
                this._onError = null;
                if (LOG.isDebugEnabled()) {
                    LOG.debug("onError", th);
                }
                this._writeListener.onError(th);
                this.close();
                return;
            }
            try {
                this._writeListener.onWritePossible();
            }
            catch (Throwable e) {
                this._onError = e;
                continue;
            }
            break;
        }
    }

    private void close(Closeable resource) {
        try {
            resource.close();
        }
        catch (Throwable x) {
            LOG.ignore(x);
        }
    }

    public String toString() {
        return String.format("%s@%x{%s}", new Object[]{this.getClass().getSimpleName(), this.hashCode(), this._state.get()});
    }

    static /* synthetic */ AtomicReference access$200(HttpOutput x0) {
        return x0._state;
    }

    private static class WriteBlocker
    extends SharedBlockingCallback {
        private final HttpChannel _channel;

        private WriteBlocker(HttpChannel channel) {
            this._channel = channel;
        }

        @Override
        protected long getIdleTimeout() {
            long blockingTimeout = this._channel.getHttpConfiguration().getBlockingTimeout();
            if (blockingTimeout == 0L) {
                return this._channel.getIdleTimeout();
            }
            return blockingTimeout;
        }
    }

    private class ReadableByteChannelWritingCB
    extends IteratingNestedCallback {
        private final ReadableByteChannel _in;
        private final ByteBuffer _buffer;
        private boolean _eof;

        public ReadableByteChannelWritingCB(ReadableByteChannel in, Callback callback) {
            super(callback);
            this._in = in;
            this._buffer = HttpOutput.this._channel.getByteBufferPool().acquire(HttpOutput.this.getBufferSize(), HttpOutput.this._channel.useDirectBuffers());
        }

        @Override
        protected IteratingCallback.Action process() throws Exception {
            if (this._eof) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("EOF of {}", this);
                }
                this._in.close();
                HttpOutput.this.closed();
                HttpOutput.this._channel.getByteBufferPool().release(this._buffer);
                return IteratingCallback.Action.SUCCEEDED;
            }
            BufferUtil.clearToFill(this._buffer);
            while (this._buffer.hasRemaining() && !this._eof) {
                this._eof = this._in.read(this._buffer) < 0;
            }
            BufferUtil.flipToFlush(this._buffer, 0);
            HttpOutput.this._written = HttpOutput.this._written + (long)this._buffer.remaining();
            HttpOutput.this.write(this._buffer, this._eof, this);
            return IteratingCallback.Action.SCHEDULED;
        }

        @Override
        public void onCompleteFailure(Throwable x) {
            HttpOutput.this.abort(x);
            HttpOutput.this._channel.getByteBufferPool().release(this._buffer);
            HttpOutput.this.close(this._in);
            super.onCompleteFailure(x);
        }
    }

    private class InputStreamWritingCB
    extends IteratingNestedCallback {
        private final InputStream _in;
        private final ByteBuffer _buffer;
        private boolean _eof;

        public InputStreamWritingCB(InputStream in, Callback callback) {
            super(callback);
            this._in = in;
            this._buffer = HttpOutput.this._channel.getByteBufferPool().acquire(HttpOutput.this.getBufferSize(), false);
        }

        @Override
        protected IteratingCallback.Action process() throws Exception {
            if (this._eof) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("EOF of {}", this);
                }
                this._in.close();
                HttpOutput.this.closed();
                HttpOutput.this._channel.getByteBufferPool().release(this._buffer);
                return IteratingCallback.Action.SUCCEEDED;
            }
            int len = 0;
            while (len < this._buffer.capacity() && !this._eof) {
                int r = this._in.read(this._buffer.array(), this._buffer.arrayOffset() + len, this._buffer.capacity() - len);
                if (r < 0) {
                    this._eof = true;
                    continue;
                }
                len += r;
            }
            this._buffer.position(0);
            this._buffer.limit(len);
            HttpOutput.this._written = HttpOutput.this._written + (long)len;
            HttpOutput.this.write(this._buffer, this._eof, this);
            return IteratingCallback.Action.SCHEDULED;
        }

        @Override
        public void onCompleteFailure(Throwable x) {
            HttpOutput.this.abort(x);
            HttpOutput.this._channel.getByteBufferPool().release(this._buffer);
            HttpOutput.this.close(this._in);
            super.onCompleteFailure(x);
        }
    }

    private class AsyncWrite
    extends AsyncICB {
        private final ByteBuffer _buffer;
        private final ByteBuffer _slice;
        private final int _len;
        protected volatile boolean _completed;

        public AsyncWrite(byte[] b, int off, int len, boolean last) {
            super(last);
            this._buffer = ByteBuffer.wrap(b, off, len);
            this._len = len;
            this._slice = this._len < HttpOutput.this.getBufferSize() ? null : this._buffer.duplicate();
        }

        public AsyncWrite(ByteBuffer buffer, boolean last) {
            super(last);
            this._buffer = buffer;
            this._len = buffer.remaining();
            this._slice = this._buffer.isDirect() || this._len < HttpOutput.this.getBufferSize() ? null : this._buffer.duplicate();
        }

        @Override
        protected IteratingCallback.Action process() {
            if (BufferUtil.hasContent(HttpOutput.this._aggregate)) {
                this._completed = this._len == 0;
                HttpOutput.this.write(HttpOutput.this._aggregate, this._last && this._completed, this);
                return IteratingCallback.Action.SCHEDULED;
            }
            if (!this._last && this._len < BufferUtil.space(HttpOutput.this._aggregate) && this._len < HttpOutput.this._commitSize) {
                int position = BufferUtil.flipToFill(HttpOutput.this._aggregate);
                BufferUtil.put(this._buffer, HttpOutput.this._aggregate);
                BufferUtil.flipToFlush(HttpOutput.this._aggregate, position);
                return IteratingCallback.Action.SUCCEEDED;
            }
            if (this._buffer.hasRemaining()) {
                if (this._slice == null) {
                    this._completed = true;
                    HttpOutput.this.write(this._buffer, this._last, this);
                    return IteratingCallback.Action.SCHEDULED;
                }
                int p = this._buffer.position();
                int l = Math.min(HttpOutput.this.getBufferSize(), this._buffer.remaining());
                int pl = p + l;
                this._slice.limit(pl);
                this._buffer.position(pl);
                this._slice.position(p);
                this._completed = !this._buffer.hasRemaining();
                HttpOutput.this.write(this._slice, this._last && this._completed, this);
                return IteratingCallback.Action.SCHEDULED;
            }
            if (this._last && !this._completed) {
                this._completed = true;
                HttpOutput.this.write(BufferUtil.EMPTY_BUFFER, true, this);
                return IteratingCallback.Action.SCHEDULED;
            }
            if (LOG.isDebugEnabled() && this._completed) {
                LOG.debug("EOF of {}", this);
            }
            return IteratingCallback.Action.SUCCEEDED;
        }
    }

    private class AsyncFlush
    extends AsyncICB {
        protected volatile boolean _flushed;

        public AsyncFlush() {
            super(false);
        }

        @Override
        protected IteratingCallback.Action process() {
            if (BufferUtil.hasContent(HttpOutput.this._aggregate)) {
                this._flushed = true;
                HttpOutput.this.write(HttpOutput.this._aggregate, false, this);
                return IteratingCallback.Action.SCHEDULED;
            }
            if (!this._flushed) {
                this._flushed = true;
                HttpOutput.this.write(BufferUtil.EMPTY_BUFFER, false, this);
                return IteratingCallback.Action.SCHEDULED;
            }
            return IteratingCallback.Action.SUCCEEDED;
        }
    }

    private abstract class AsyncICB
    extends IteratingCallback {
        final boolean _last;

        AsyncICB(boolean last) {
            this._last = last;
        }

        /*
         * Unable to fully structure code
         */
        @Override
        protected void onCompleteSuccess() {
            block5: while (true) {
                last = (OutputState)HttpOutput.access$200(HttpOutput.this).get();
                switch (2.$SwitchMap$org$eclipse$jetty$server$HttpOutput$OutputState[last.ordinal()]) {
                    case 4: {
                        if (HttpOutput.access$200(HttpOutput.this).compareAndSet(OutputState.PENDING, OutputState.ASYNC)) break block5;
                        continue block5;
                    }
                    case 3: {
                        if (HttpOutput.access$200(HttpOutput.this).compareAndSet(OutputState.UNREADY, OutputState.READY)) ** break;
                        continue block5;
                        if (this._last) {
                            HttpOutput.this.closed();
                        }
                        if (!HttpOutput.access$300(HttpOutput.this).getState().onWritePossible()) break block5;
                        HttpOutput.access$300(HttpOutput.this).execute(HttpOutput.access$300(HttpOutput.this));
                        break block5;
                    }
                    case 1: {
                        break block5;
                    }
                    default: {
                        throw new IllegalStateException();
                    }
                }
                break;
            }
        }

        @Override
        public void onCompleteFailure(Throwable e) {
            HttpOutput.this._onError = e == null ? new IOException() : e;
            if (HttpOutput.this._channel.getState().onWritePossible()) {
                HttpOutput.this._channel.execute(HttpOutput.this._channel);
            }
        }
    }

    private static enum OutputState {
        OPEN,
        ASYNC,
        READY,
        PENDING,
        UNREADY,
        ERROR,
        CLOSED;

    }

    public static interface Interceptor {
        public void write(ByteBuffer var1, boolean var2, Callback var3);

        public Interceptor getNextInterceptor();

        public boolean isOptimizedForDirectBuffers();

        default public void resetBuffer() throws IllegalStateException {
            Interceptor next = this.getNextInterceptor();
            if (next != null) {
                next.resetBuffer();
            }
        }
    }
}

