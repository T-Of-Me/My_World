/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import org.eclipse.jetty.http.BadMessageException;
import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.io.RuntimeIOException;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpChannelState;
import org.eclipse.jetty.server.HttpConnection;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.component.Destroyable;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.thread.Invocable;
import org.eclipse.jetty.util.thread.ThreadPool;

public class HttpInput
extends ServletInputStream
implements Runnable {
    private static final Logger LOG = Log.getLogger(HttpInput.class);
    static final Content EOF_CONTENT = new EofContent("EOF");
    static final Content EARLY_EOF_CONTENT = new EofContent("EARLY_EOF");
    private final byte[] _oneByteBuffer = new byte[1];
    private Content _content;
    private Content _intercepted;
    private final Deque<Content> _inputQ = new ArrayDeque<Content>();
    private final HttpChannelState _channelState;
    private ReadListener _listener;
    private State _state = STREAM;
    private long _firstByteTimeStamp = -1L;
    private long _contentArrived;
    private long _contentConsumed;
    private long _blockUntil;
    private Interceptor _interceptor;
    protected static final State STREAM = new State(){

        @Override
        public boolean blockForContent(HttpInput input) throws IOException {
            input.blockForContent();
            return true;
        }

        public String toString() {
            return "STREAM";
        }
    };
    protected static final State ASYNC = new State(){

        @Override
        public int noContent() throws IOException {
            return 0;
        }

        public String toString() {
            return "ASYNC";
        }
    };
    protected static final State EARLY_EOF = new EOFState(){

        @Override
        public int noContent() throws IOException {
            throw this.getError();
        }

        public String toString() {
            return "EARLY_EOF";
        }

        @Override
        public IOException getError() {
            return new EofException("Early EOF");
        }
    };
    protected static final State EOF = new EOFState(){

        public String toString() {
            return "EOF";
        }
    };
    protected static final State AEOF = new EOFState(){

        public String toString() {
            return "AEOF";
        }
    };

    public HttpInput(HttpChannelState state) {
        this._channelState = state;
    }

    protected HttpChannelState getHttpChannelState() {
        return this._channelState;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void recycle() {
        Deque<Content> deque = this._inputQ;
        synchronized (deque) {
            if (this._content != null) {
                this._content.failed(null);
            }
            this._content = null;
            Content item = this._inputQ.poll();
            while (item != null) {
                item.failed(null);
                item = this._inputQ.poll();
            }
            this._listener = null;
            this._state = STREAM;
            this._contentArrived = 0L;
            this._contentConsumed = 0L;
            this._firstByteTimeStamp = -1L;
            this._blockUntil = 0L;
            if (this._interceptor instanceof Destroyable) {
                ((Destroyable)((Object)this._interceptor)).destroy();
            }
            this._interceptor = null;
        }
    }

    public Interceptor getInterceptor() {
        return this._interceptor;
    }

    public void setInterceptor(Interceptor interceptor) {
        this._interceptor = interceptor;
    }

    public void addInterceptor(Interceptor interceptor) {
        this._interceptor = this._interceptor == null ? interceptor : new ChainedInterceptor(this._interceptor, interceptor);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public int available() {
        int available = 0;
        boolean woken = false;
        Deque<Content> deque = this._inputQ;
        synchronized (deque) {
            if (this._content == null) {
                this._content = this._inputQ.poll();
            }
            if (this._content == null) {
                try {
                    this.produceContent();
                }
                catch (IOException e) {
                    woken = this.failed(e);
                }
                if (this._content == null) {
                    this._content = this._inputQ.poll();
                }
            }
            if (this._content != null) {
                available = this._content.remaining();
            }
        }
        if (woken) {
            this.wake();
        }
        return available;
    }

    protected void wake() {
        HttpChannel channel = this._channelState.getHttpChannel();
        ThreadPool executor = channel.getConnector().getServer().getThreadPool();
        executor.execute(channel);
    }

    private long getBlockingTimeout() {
        return this.getHttpChannelState().getHttpChannel().getHttpConfiguration().getBlockingTimeout();
    }

    @Override
    public int read() throws IOException {
        int read = this.read(this._oneByteBuffer, 0, 1);
        if (read == 0) {
            throw new IllegalStateException("unready read=0");
        }
        return read < 0 ? -1 : this._oneByteBuffer[0] & 0xFF;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int l;
        boolean wake = false;
        Deque<Content> deque = this._inputQ;
        synchronized (deque) {
            block10: {
                long minimum_data;
                long period;
                long minRequestDataRate;
                long blockingTimeout;
                if (!this.isAsync() && this._blockUntil == 0L && (blockingTimeout = this.getBlockingTimeout()) > 0L) {
                    this._blockUntil = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(blockingTimeout);
                }
                if ((minRequestDataRate = this._channelState.getHttpChannel().getHttpConfiguration().getMinRequestDataRate()) > 0L && this._firstByteTimeStamp != -1L && (period = System.nanoTime() - this._firstByteTimeStamp) > 0L && this._contentArrived < (minimum_data = minRequestDataRate * TimeUnit.NANOSECONDS.toMillis(period) / TimeUnit.SECONDS.toMillis(1L))) {
                    throw new BadMessageException(408, String.format("Request data rate < %d B/s", minRequestDataRate));
                }
                do {
                    Content item;
                    if ((item = this.nextContent()) == null) continue;
                    l = this.get(item, b, off, len);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("{} read {} from {}", this, l, item);
                    }
                    if (item.isEmpty()) {
                        this.nextInterceptedContent();
                    }
                    break block10;
                } while (this._state.blockForContent(this));
                l = this._state.noContent();
                if (l < 0) {
                    wake = this._channelState.onReadEof();
                }
            }
        }
        if (wake) {
            this.wake();
        }
        return l;
    }

    protected void produceContent() throws IOException {
    }

    protected Content nextContent() throws IOException {
        Content content = this.nextNonSentinelContent();
        if (content == null && !this.isFinished()) {
            this.produceContent();
            content = this.nextNonSentinelContent();
        }
        return content;
    }

    protected Content nextNonSentinelContent() {
        Content content;
        while ((content = this.nextInterceptedContent()) instanceof SentinelContent) {
            this.consume(content);
        }
        return content;
    }

    protected Content produceNextContext() throws IOException {
        Content content = this.nextInterceptedContent();
        if (content == null && !this.isFinished()) {
            this.produceContent();
            content = this.nextInterceptedContent();
        }
        return content;
    }

    protected Content nextInterceptedContent() {
        if (this._intercepted != null) {
            if (this._intercepted.hasContent()) {
                return this._intercepted;
            }
            this._intercepted.succeeded();
            this._intercepted = null;
        }
        if (this._content == null) {
            this._content = this._inputQ.poll();
        }
        while (this._content != null) {
            if (this._interceptor != null) {
                this._intercepted = this._interceptor.readFrom(this._content);
                if (this._intercepted != null && this._intercepted != this._content) {
                    if (this._intercepted.hasContent()) {
                        return this._intercepted;
                    }
                    this._intercepted.succeeded();
                }
                this._intercepted = null;
            }
            if (this._content.hasContent() || this._content instanceof SentinelContent) {
                return this._content;
            }
            this._content.succeeded();
            this._content = this._inputQ.poll();
        }
        return null;
    }

    private void consume(Content content) {
        if (content instanceof EofContent) {
            this._state = content == EARLY_EOF_CONTENT ? EARLY_EOF : (this._listener == null ? EOF : AEOF);
        }
        content.succeeded();
        if (this._content == content) {
            this._content = null;
        } else if (this._intercepted == content) {
            this._intercepted = null;
        }
    }

    protected int get(Content content, byte[] buffer, int offset, int length) {
        int l = content.get(buffer, offset, length);
        this._contentConsumed += (long)l;
        return l;
    }

    protected void skip(Content content, int length) {
        int l = content.skip(length);
        this._contentConsumed += (long)l;
        if (l > 0 && content.isEmpty()) {
            this.nextNonSentinelContent();
        }
    }

    protected void blockForContent() throws IOException {
        try {
            long timeout = 0L;
            if (this._blockUntil != 0L && (timeout = TimeUnit.NANOSECONDS.toMillis(this._blockUntil - System.nanoTime())) <= 0L) {
                throw new TimeoutException();
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("{} blocking for content timeout={}", this, timeout);
            }
            if (timeout > 0L) {
                this._inputQ.wait(timeout);
            } else {
                this._inputQ.wait();
            }
            if (this._blockUntil != 0L && TimeUnit.NANOSECONDS.toMillis(this._blockUntil - System.nanoTime()) <= 0L) {
                throw new TimeoutException(String.format("Blocking timeout %d ms", this.getBlockingTimeout()));
            }
        }
        catch (Throwable e) {
            throw (IOException)new InterruptedIOException().initCause(e);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean prependContent(Content item) {
        boolean woken = false;
        Deque<Content> deque = this._inputQ;
        synchronized (deque) {
            if (this._content != null) {
                this._inputQ.push(this._content);
            }
            this._content = item;
            this._contentConsumed -= (long)item.remaining();
            if (LOG.isDebugEnabled()) {
                LOG.debug("{} prependContent {}", this, item);
            }
            if (this._listener == null) {
                this._inputQ.notify();
            } else {
                woken = this._channelState.onContentAdded();
            }
        }
        return woken;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean addContent(Content content) {
        boolean woken = false;
        Deque<Content> deque = this._inputQ;
        synchronized (deque) {
            if (this._firstByteTimeStamp == -1L) {
                this._firstByteTimeStamp = System.nanoTime();
            }
            this._contentArrived += (long)content.remaining();
            if (this._content == null && this._inputQ.isEmpty()) {
                this._content = content;
            } else {
                this._inputQ.offer(content);
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("{} addContent {}", this, content);
            }
            if (this.nextInterceptedContent() != null) {
                if (this._listener == null) {
                    this._inputQ.notify();
                } else {
                    woken = this._channelState.onContentAdded();
                }
            }
        }
        return woken;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean hasContent() {
        Deque<Content> deque = this._inputQ;
        synchronized (deque) {
            return this._content != null || this._inputQ.size() > 0;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void unblock() {
        Deque<Content> deque = this._inputQ;
        synchronized (deque) {
            this._inputQ.notify();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public long getContentConsumed() {
        Deque<Content> deque = this._inputQ;
        synchronized (deque) {
            return this._contentConsumed;
        }
    }

    public boolean earlyEOF() {
        return this.addContent(EARLY_EOF_CONTENT);
    }

    public boolean eof() {
        return this.addContent(EOF_CONTENT);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean consumeAll() {
        Deque<Content> deque = this._inputQ;
        synchronized (deque) {
            try {
                Content item;
                while (!this.isFinished() && (item = this.nextContent()) != null) {
                    this.skip(item, item.remaining());
                }
                return this.isFinished() && !this.isError();
            }
            catch (IOException e) {
                LOG.debug(e);
                return false;
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean isError() {
        Deque<Content> deque = this._inputQ;
        synchronized (deque) {
            return this._state instanceof ErrorState;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean isAsync() {
        Deque<Content> deque = this._inputQ;
        synchronized (deque) {
            return this._state == ASYNC;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public boolean isFinished() {
        Deque<Content> deque = this._inputQ;
        synchronized (deque) {
            return this._state instanceof EOFState;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean isAsyncEOF() {
        Deque<Content> deque = this._inputQ;
        synchronized (deque) {
            return this._state == AEOF;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public boolean isReady() {
        try {
            Deque<Content> deque = this._inputQ;
            synchronized (deque) {
                if (this._listener == null) {
                    return true;
                }
                if (this._state instanceof EOFState) {
                    return true;
                }
                if (this.produceNextContext() != null) {
                    return true;
                }
                this._channelState.onReadUnready();
            }
            return false;
        }
        catch (IOException e) {
            LOG.ignore(e);
            return true;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void setReadListener(ReadListener readListener) {
        readListener = Objects.requireNonNull(readListener);
        boolean woken = false;
        try {
            Deque<Content> deque = this._inputQ;
            synchronized (deque) {
                if (this._listener != null) {
                    throw new IllegalStateException("ReadListener already set");
                }
                this._listener = readListener;
                Content content = this.produceNextContext();
                if (content != null) {
                    this._state = ASYNC;
                    woken = this._channelState.onReadReady();
                } else if (this._state == EOF) {
                    this._state = AEOF;
                    woken = this._channelState.onReadEof();
                } else {
                    this._state = ASYNC;
                    this._channelState.onReadUnready();
                }
            }
        }
        catch (IOException e) {
            throw new RuntimeIOException(e);
        }
        if (woken) {
            this.wake();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean failed(Throwable x) {
        boolean woken = false;
        Deque<Content> deque = this._inputQ;
        synchronized (deque) {
            if (this._state instanceof ErrorState) {
                LOG.warn(x);
            } else {
                this._state = new ErrorState(x);
            }
            if (this._listener == null) {
                this._inputQ.notify();
            } else {
                woken = this._channelState.onContentAdded();
            }
        }
        return woken;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void run() {
        Throwable error;
        ReadListener listener;
        boolean aeof = false;
        Deque<Content> deque = this._inputQ;
        synchronized (deque) {
            listener = this._listener;
            if (this._state == EOF) {
                return;
            }
            if (this._state == AEOF) {
                this._state = EOF;
                aeof = true;
            }
            error = this._state.getError();
            if (!aeof && error == null) {
                Content content = this.nextInterceptedContent();
                if (content == null) {
                    return;
                }
                if (content instanceof EofContent) {
                    this.consume(content);
                    if (this._state == EARLY_EOF) {
                        error = this._state.getError();
                    } else if (this._state == AEOF) {
                        aeof = true;
                        this._state = EOF;
                    }
                }
            }
        }
        try {
            if (error != null) {
                this._channelState.getHttpChannel().getResponse().getHttpFields().add(HttpConnection.CONNECTION_CLOSE);
                listener.onError(error);
            } else if (aeof) {
                listener.onAllDataRead();
            } else {
                listener.onDataAvailable();
            }
        }
        catch (Throwable e) {
            LOG.warn(e.toString(), new Object[0]);
            LOG.debug(e);
            try {
                if (aeof || error == null) {
                    this._channelState.getHttpChannel().getResponse().getHttpFields().add(HttpConnection.CONNECTION_CLOSE);
                    listener.onError(e);
                }
            }
            catch (Throwable e2) {
                LOG.warn(e2.toString(), new Object[0]);
                LOG.debug(e2);
                throw new RuntimeIOException(e2);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public String toString() {
        Content content;
        int q;
        long consumed;
        State state;
        Deque<Content> deque = this._inputQ;
        synchronized (deque) {
            state = this._state;
            consumed = this._contentConsumed;
            q = this._inputQ.size();
            content = this._inputQ.peekFirst();
        }
        return String.format("%s@%x[c=%d,q=%d,[0]=%s,s=%s]", this.getClass().getSimpleName(), this.hashCode(), consumed, q, content, state);
    }

    protected class ErrorState
    extends EOFState {
        final Throwable _error;

        ErrorState(Throwable error) {
            this._error = error;
        }

        @Override
        public Throwable getError() {
            return this._error;
        }

        @Override
        public int noContent() throws IOException {
            if (this._error instanceof IOException) {
                throw (IOException)this._error;
            }
            throw new IOException(this._error);
        }

        public String toString() {
            return "ERROR:" + this._error;
        }
    }

    protected static class EOFState
    extends State {
        protected EOFState() {
        }
    }

    protected static abstract class State {
        protected State() {
        }

        public boolean blockForContent(HttpInput in) throws IOException {
            return false;
        }

        public int noContent() throws IOException {
            return -1;
        }

        public Throwable getError() {
            return null;
        }
    }

    public static class Content
    implements Callback {
        protected final ByteBuffer _content;

        public Content(ByteBuffer content) {
            this._content = content;
        }

        public ByteBuffer getByteBuffer() {
            return this._content;
        }

        @Override
        public Invocable.InvocationType getInvocationType() {
            return Invocable.InvocationType.NON_BLOCKING;
        }

        public int get(byte[] buffer, int offset, int length) {
            length = Math.min(this._content.remaining(), length);
            this._content.get(buffer, offset, length);
            return length;
        }

        public int skip(int length) {
            length = Math.min(this._content.remaining(), length);
            this._content.position(this._content.position() + length);
            return length;
        }

        public boolean hasContent() {
            return this._content.hasRemaining();
        }

        public int remaining() {
            return this._content.remaining();
        }

        public boolean isEmpty() {
            return !this._content.hasRemaining();
        }

        public String toString() {
            return String.format("Content@%x{%s}", this.hashCode(), BufferUtil.toDetailString(this._content));
        }
    }

    public static class EofContent
    extends SentinelContent {
        EofContent(String name) {
            super(name);
        }
    }

    public static class SentinelContent
    extends Content {
        private final String _name;

        public SentinelContent(String name) {
            super(BufferUtil.EMPTY_BUFFER);
            this._name = name;
        }

        @Override
        public String toString() {
            return this._name;
        }
    }

    public static class ChainedInterceptor
    implements Interceptor,
    Destroyable {
        private final Interceptor _prev;
        private final Interceptor _next;

        public ChainedInterceptor(Interceptor prev, Interceptor next) {
            this._prev = prev;
            this._next = next;
        }

        public Interceptor getPrev() {
            return this._prev;
        }

        public Interceptor getNext() {
            return this._next;
        }

        @Override
        public Content readFrom(Content content) {
            return this.getNext().readFrom(this.getPrev().readFrom(content));
        }

        @Override
        public void destroy() {
            if (this._prev instanceof Destroyable) {
                ((Destroyable)((Object)this._prev)).destroy();
            }
            if (this._next instanceof Destroyable) {
                ((Destroyable)((Object)this._next)).destroy();
            }
        }
    }

    public static interface Interceptor {
        public Content readFrom(Content var1);
    }
}

