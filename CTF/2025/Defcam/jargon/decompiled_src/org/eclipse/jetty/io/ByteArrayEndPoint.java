/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.io;

import java.io.EOFException;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import org.eclipse.jetty.io.AbstractEndPoint;
import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.io.RuntimeIOException;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.thread.Locker;
import org.eclipse.jetty.util.thread.Scheduler;

public class ByteArrayEndPoint
extends AbstractEndPoint {
    static final Logger LOG = Log.getLogger(ByteArrayEndPoint.class);
    static final InetAddress NOIP;
    static final InetSocketAddress NOIPPORT;
    private static final ByteBuffer EOF;
    private final Runnable _runFillable = new Runnable(){

        @Override
        public void run() {
            ByteArrayEndPoint.this.getFillInterest().fillable();
        }
    };
    private final Locker _locker = new Locker();
    private final Condition _hasOutput = this._locker.newCondition();
    private final Queue<ByteBuffer> _inQ = new ArrayDeque<ByteBuffer>();
    private ByteBuffer _out;
    private boolean _growOutput;

    public ByteArrayEndPoint() {
        this(null, 0L, null, null);
    }

    public ByteArrayEndPoint(byte[] input, int outputSize) {
        this(null, 0L, input != null ? BufferUtil.toBuffer(input) : null, BufferUtil.allocate(outputSize));
    }

    public ByteArrayEndPoint(String input, int outputSize) {
        this(null, 0L, input != null ? BufferUtil.toBuffer(input) : null, BufferUtil.allocate(outputSize));
    }

    public ByteArrayEndPoint(Scheduler scheduler, long idleTimeoutMs) {
        this(scheduler, idleTimeoutMs, null, null);
    }

    public ByteArrayEndPoint(Scheduler timer, long idleTimeoutMs, byte[] input, int outputSize) {
        this(timer, idleTimeoutMs, input != null ? BufferUtil.toBuffer(input) : null, BufferUtil.allocate(outputSize));
    }

    public ByteArrayEndPoint(Scheduler timer, long idleTimeoutMs, String input, int outputSize) {
        this(timer, idleTimeoutMs, input != null ? BufferUtil.toBuffer(input) : null, BufferUtil.allocate(outputSize));
    }

    public ByteArrayEndPoint(Scheduler timer, long idleTimeoutMs, ByteBuffer input, ByteBuffer output) {
        super(timer);
        if (BufferUtil.hasContent(input)) {
            this.addInput(input);
        }
        this._out = output == null ? BufferUtil.allocate(1024) : output;
        this.setIdleTimeout(idleTimeoutMs);
        this.onOpen();
    }

    @Override
    public void doShutdownOutput() {
        super.doShutdownOutput();
        try (Locker.Lock lock = this._locker.lock();){
            this._hasOutput.signalAll();
        }
    }

    @Override
    public void doClose() {
        super.doClose();
        try (Locker.Lock lock = this._locker.lock();){
            this._hasOutput.signalAll();
        }
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return NOIPPORT;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return NOIPPORT;
    }

    @Override
    protected void onIncompleteFlush() {
    }

    protected void execute(Runnable task) {
        new Thread(task, "BAEPoint-" + Integer.toHexString(this.hashCode())).start();
    }

    @Override
    protected void needsFillInterest() throws IOException {
        try (Locker.Lock lock = this._locker.lock();){
            if (!this.isOpen()) {
                throw new ClosedChannelException();
            }
            ByteBuffer in = this._inQ.peek();
            if (BufferUtil.hasContent(in) || in == EOF) {
                this.execute(this._runFillable);
            }
        }
    }

    public void addInputEOF() {
        this.addInput((ByteBuffer)null);
    }

    public void addInput(ByteBuffer in) {
        boolean fillable = false;
        try (Locker.Lock lock = this._locker.lock();){
            if (this._inQ.peek() == EOF) {
                throw new RuntimeIOException(new EOFException());
            }
            boolean was_empty = this._inQ.isEmpty();
            if (in == null) {
                this._inQ.add(EOF);
                fillable = true;
            }
            if (BufferUtil.hasContent(in)) {
                this._inQ.add(in);
                fillable = was_empty;
            }
        }
        if (fillable) {
            this._runFillable.run();
        }
    }

    public void addInputAndExecute(ByteBuffer in) {
        boolean fillable = false;
        try (Locker.Lock lock = this._locker.lock();){
            if (this._inQ.peek() == EOF) {
                throw new RuntimeIOException(new EOFException());
            }
            boolean was_empty = this._inQ.isEmpty();
            if (in == null) {
                this._inQ.add(EOF);
                fillable = true;
            }
            if (BufferUtil.hasContent(in)) {
                this._inQ.add(in);
                fillable = was_empty;
            }
        }
        if (fillable) {
            this.execute(this._runFillable);
        }
    }

    public void addInput(String s) {
        this.addInput(BufferUtil.toBuffer(s, StandardCharsets.UTF_8));
    }

    public void addInput(String s, Charset charset) {
        this.addInput(BufferUtil.toBuffer(s, charset));
    }

    public ByteBuffer getOutput() {
        try (Locker.Lock lock = this._locker.lock();){
            ByteBuffer byteBuffer = this._out;
            return byteBuffer;
        }
    }

    public String getOutputString() {
        return this.getOutputString(StandardCharsets.UTF_8);
    }

    public String getOutputString(Charset charset) {
        return BufferUtil.toString(this._out, charset);
    }

    public ByteBuffer takeOutput() {
        ByteBuffer b;
        try (Locker.Lock lock = this._locker.lock();){
            b = this._out;
            this._out = BufferUtil.allocate(b.capacity());
        }
        this.getWriteFlusher().completeWrite();
        return b;
    }

    public ByteBuffer waitForOutput(long time, TimeUnit unit) throws InterruptedException {
        ByteBuffer b;
        try (Locker.Lock lock = this._locker.lock();){
            while (BufferUtil.isEmpty(this._out) && !this.isOutputShutdown()) {
                this._hasOutput.await(time, unit);
            }
            b = this._out;
            this._out = BufferUtil.allocate(b.capacity());
        }
        this.getWriteFlusher().completeWrite();
        return b;
    }

    public String takeOutputString() {
        return this.takeOutputString(StandardCharsets.UTF_8);
    }

    public String takeOutputString(Charset charset) {
        ByteBuffer buffer = this.takeOutput();
        return BufferUtil.toString(buffer, charset);
    }

    public void setOutput(ByteBuffer out) {
        try (Locker.Lock lock = this._locker.lock();){
            this._out = out;
        }
        this.getWriteFlusher().completeWrite();
    }

    public boolean hasMore() {
        return this.getOutput().position() > 0;
    }

    @Override
    public int fill(ByteBuffer buffer) throws IOException {
        int filled = 0;
        try (Locker.Lock lock = this._locker.lock();){
            while (true) {
                if (!this.isOpen()) {
                    throw new EofException("CLOSED");
                }
                if (this.isInputShutdown()) {
                    int n = -1;
                    return n;
                }
                if (this._inQ.isEmpty()) {
                    break;
                }
                ByteBuffer in = this._inQ.peek();
                if (in == EOF) {
                    filled = -1;
                    break;
                }
                if (BufferUtil.hasContent(in)) {
                    filled = BufferUtil.append(buffer, in);
                    if (BufferUtil.isEmpty(in)) {
                        this._inQ.poll();
                    }
                    break;
                }
                this._inQ.poll();
            }
        }
        if (filled > 0) {
            this.notIdle();
        } else if (filled < 0) {
            this.shutdownInput();
        }
        return filled;
    }

    @Override
    public boolean flush(ByteBuffer ... buffers) throws IOException {
        boolean flushed = true;
        try (Locker.Lock lock = this._locker.lock();){
            if (!this.isOpen()) {
                throw new IOException("CLOSED");
            }
            if (this.isOutputShutdown()) {
                throw new IOException("OSHUT");
            }
            boolean idle = true;
            for (ByteBuffer b : buffers) {
                if (!BufferUtil.hasContent(b)) continue;
                if (this._growOutput && b.remaining() > BufferUtil.space(this._out)) {
                    BufferUtil.compact(this._out);
                    if (b.remaining() > BufferUtil.space(this._out)) {
                        ByteBuffer n = BufferUtil.allocate(this._out.capacity() + b.remaining() * 2);
                        BufferUtil.append(n, this._out);
                        this._out = n;
                    }
                }
                if (BufferUtil.append(this._out, b) > 0) {
                    idle = false;
                }
                if (!BufferUtil.hasContent(b)) continue;
                flushed = false;
                break;
            }
            if (!idle) {
                this.notIdle();
                this._hasOutput.signalAll();
            }
        }
        return flushed;
    }

    @Override
    public void reset() {
        try (Locker.Lock lock = this._locker.lock();){
            this._inQ.clear();
            this._hasOutput.signalAll();
            BufferUtil.clear(this._out);
        }
        super.reset();
    }

    @Override
    public Object getTransport() {
        return null;
    }

    public boolean isGrowOutput() {
        return this._growOutput;
    }

    public void setGrowOutput(boolean growOutput) {
        this._growOutput = growOutput;
    }

    @Override
    public String toString() {
        String o;
        ByteBuffer b;
        int q;
        try (Locker.Lock lock = this._locker.lock();){
            q = this._inQ.size();
            b = this._inQ.peek();
            o = BufferUtil.toDetailString(this._out);
        }
        return String.format("%s[q=%d,q[0]=%s,o=%s]", super.toString(), q, b, o);
    }

    static {
        InetAddress noip = null;
        try {
            noip = Inet4Address.getByName("0.0.0.0");
        }
        catch (UnknownHostException e) {
            LOG.warn(e);
        }
        finally {
            NOIP = noip;
            NOIPPORT = new InetSocketAddress(NOIP, 0);
        }
        EOF = BufferUtil.allocate(0);
    }
}

