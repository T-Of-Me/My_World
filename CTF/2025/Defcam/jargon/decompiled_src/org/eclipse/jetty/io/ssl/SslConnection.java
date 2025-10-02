/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.io.ssl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import org.eclipse.jetty.io.AbstractConnection;
import org.eclipse.jetty.io.AbstractEndPoint;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.WriteFlusher;
import org.eclipse.jetty.io.ssl.SslHandshakeListener;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.thread.Invocable;

public class SslConnection
extends AbstractConnection {
    private static final Logger LOG = Log.getLogger(SslConnection.class);
    private final List<SslHandshakeListener> handshakeListeners = new ArrayList<SslHandshakeListener>();
    private final ByteBufferPool _bufferPool;
    private final SSLEngine _sslEngine;
    private final DecryptedEndPoint _decryptedEndPoint;
    private ByteBuffer _decryptedInput;
    private ByteBuffer _encryptedInput;
    private ByteBuffer _encryptedOutput;
    private final boolean _encryptedDirectBuffers = true;
    private final boolean _decryptedDirectBuffers = false;
    private boolean _renegotiationAllowed;
    private int _renegotiationLimit = -1;
    private boolean _closedOutbound;
    private boolean _allowMissingCloseMessage = true;
    private final Runnable _runCompleteWrite = new RunnableTask("runCompleteWrite"){

        @Override
        public void run() {
            SslConnection.this._decryptedEndPoint.getWriteFlusher().completeWrite();
        }

        @Override
        public Invocable.InvocationType getInvocationType() {
            return SslConnection.this.getDecryptedEndPoint().getWriteFlusher().getCallbackInvocationType();
        }
    };
    private final Runnable _runFillable = new RunnableTask("runFillable"){

        @Override
        public void run() {
            SslConnection.this._decryptedEndPoint.getFillInterest().fillable();
        }

        @Override
        public Invocable.InvocationType getInvocationType() {
            return SslConnection.this.getDecryptedEndPoint().getFillInterest().getCallbackInvocationType();
        }
    };
    private final Callback _sslReadCallback = new Callback(){

        @Override
        public void succeeded() {
            SslConnection.this.onFillable();
        }

        @Override
        public void failed(Throwable x) {
            SslConnection.this.onFillInterestedFailed(x);
        }

        @Override
        public Invocable.InvocationType getInvocationType() {
            return SslConnection.this.getDecryptedEndPoint().getFillInterest().getCallbackInvocationType();
        }

        public String toString() {
            return String.format("SSLC.NBReadCB@%x{%s}", SslConnection.this.hashCode(), SslConnection.this);
        }
    };

    public SslConnection(ByteBufferPool byteBufferPool, Executor executor, EndPoint endPoint, SSLEngine sslEngine) {
        super(endPoint, executor);
        this._bufferPool = byteBufferPool;
        this._sslEngine = sslEngine;
        this._decryptedEndPoint = this.newDecryptedEndPoint();
    }

    public void addHandshakeListener(SslHandshakeListener listener) {
        this.handshakeListeners.add(listener);
    }

    public boolean removeHandshakeListener(SslHandshakeListener listener) {
        return this.handshakeListeners.remove(listener);
    }

    protected DecryptedEndPoint newDecryptedEndPoint() {
        return new DecryptedEndPoint();
    }

    public SSLEngine getSSLEngine() {
        return this._sslEngine;
    }

    public DecryptedEndPoint getDecryptedEndPoint() {
        return this._decryptedEndPoint;
    }

    public boolean isRenegotiationAllowed() {
        return this._renegotiationAllowed;
    }

    public void setRenegotiationAllowed(boolean renegotiationAllowed) {
        this._renegotiationAllowed = renegotiationAllowed;
    }

    public int getRenegotiationLimit() {
        return this._renegotiationLimit;
    }

    public void setRenegotiationLimit(int renegotiationLimit) {
        this._renegotiationLimit = renegotiationLimit;
    }

    public boolean isAllowMissingCloseMessage() {
        return this._allowMissingCloseMessage;
    }

    public void setAllowMissingCloseMessage(boolean allowMissingCloseMessage) {
        this._allowMissingCloseMessage = allowMissingCloseMessage;
    }

    @Override
    public void onOpen() {
        super.onOpen();
        this.getDecryptedEndPoint().getConnection().onOpen();
    }

    @Override
    public void onClose() {
        this._decryptedEndPoint.getConnection().onClose();
        super.onClose();
    }

    @Override
    public void close() {
        this.getDecryptedEndPoint().getConnection().close();
    }

    @Override
    public boolean onIdleExpired() {
        return this.getDecryptedEndPoint().getConnection().onIdleExpired();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void onFillable() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("onFillable enter {}", this._decryptedEndPoint);
        }
        if (this._decryptedEndPoint.isInputShutdown()) {
            this._decryptedEndPoint.close();
        }
        this._decryptedEndPoint.getFillInterest().fillable();
        boolean runComplete = false;
        DecryptedEndPoint decryptedEndPoint = this._decryptedEndPoint;
        synchronized (decryptedEndPoint) {
            if (this._decryptedEndPoint._flushRequiresFillToProgress) {
                this._decryptedEndPoint._flushRequiresFillToProgress = false;
                runComplete = true;
            }
        }
        if (runComplete) {
            this._runCompleteWrite.run();
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("onFillable exit {}", this._decryptedEndPoint);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void onFillInterestedFailed(Throwable cause) {
        this._decryptedEndPoint.getFillInterest().onFail(cause);
        boolean failFlusher = false;
        DecryptedEndPoint decryptedEndPoint = this._decryptedEndPoint;
        synchronized (decryptedEndPoint) {
            if (this._decryptedEndPoint._flushRequiresFillToProgress) {
                this._decryptedEndPoint._flushRequiresFillToProgress = false;
                failFlusher = true;
            }
        }
        if (failFlusher) {
            this._decryptedEndPoint.getWriteFlusher().onFail(cause);
        }
    }

    @Override
    public String toConnectionString() {
        ByteBuffer b = this._encryptedInput;
        int ei = b == null ? -1 : b.remaining();
        b = this._encryptedOutput;
        int eo = b == null ? -1 : b.remaining();
        b = this._decryptedInput;
        int di = b == null ? -1 : b.remaining();
        Connection connection = this._decryptedEndPoint.getConnection();
        return String.format("%s@%x{%s,eio=%d/%d,di=%d}=>%s", new Object[]{this.getClass().getSimpleName(), this.hashCode(), this._sslEngine.getHandshakeStatus(), ei, eo, di, connection instanceof AbstractConnection ? ((AbstractConnection)connection).toConnectionString() : connection});
    }

    static /* synthetic */ ByteBuffer access$1302(SslConnection x0, ByteBuffer x1) {
        x0._encryptedInput = x1;
        return x0._encryptedInput;
    }

    static /* synthetic */ ByteBuffer access$1202(SslConnection x0, ByteBuffer x1) {
        x0._decryptedInput = x1;
        return x0._decryptedInput;
    }

    static /* synthetic */ Executor access$1800(SslConnection x0) {
        return x0.getExecutor();
    }

    public class DecryptedEndPoint
    extends AbstractEndPoint {
        private boolean _fillRequiresFlushToProgress;
        private boolean _flushRequiresFillToProgress;
        private boolean _cannotAcceptMoreAppDataToFlush;
        private boolean _handshaken;
        private boolean _underFlown;
        private final Callback _writeCallback;

        public DecryptedEndPoint() {
            super(null);
            this._writeCallback = new WriteCallBack();
            super.setIdleTimeout(-1L);
        }

        @Override
        public long getIdleTimeout() {
            return SslConnection.this.getEndPoint().getIdleTimeout();
        }

        @Override
        public void setIdleTimeout(long idleTimeout) {
            SslConnection.this.getEndPoint().setIdleTimeout(idleTimeout);
        }

        @Override
        public boolean isOpen() {
            return SslConnection.this.getEndPoint().isOpen();
        }

        @Override
        public InetSocketAddress getLocalAddress() {
            return SslConnection.this.getEndPoint().getLocalAddress();
        }

        @Override
        public InetSocketAddress getRemoteAddress() {
            return SslConnection.this.getEndPoint().getRemoteAddress();
        }

        @Override
        protected WriteFlusher getWriteFlusher() {
            return super.getWriteFlusher();
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        protected void onIncompleteFlush() {
            boolean try_again = false;
            boolean write = false;
            boolean need_fill_interest = false;
            DecryptedEndPoint decryptedEndPoint = this;
            synchronized (decryptedEndPoint) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("onIncompleteFlush {}", SslConnection.this);
                }
                if (BufferUtil.hasContent(SslConnection.this._encryptedOutput)) {
                    this._cannotAcceptMoreAppDataToFlush = true;
                    write = true;
                } else if (SslConnection.this._sslEngine.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_UNWRAP) {
                    this._flushRequiresFillToProgress = true;
                    need_fill_interest = !SslConnection.this.isFillInterested();
                } else {
                    try_again = true;
                }
            }
            if (write) {
                SslConnection.this.getEndPoint().write(this._writeCallback, SslConnection.this._encryptedOutput);
            } else if (need_fill_interest) {
                this.ensureFillInterested();
            } else if (try_again) {
                if (this.isOutputShutdown()) {
                    this.getWriteFlusher().onClose();
                } else {
                    SslConnection.this.getExecutor().execute(SslConnection.this._runCompleteWrite);
                }
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        protected void needsFillInterest() throws IOException {
            boolean fillable;
            boolean write = false;
            DecryptedEndPoint decryptedEndPoint = this;
            synchronized (decryptedEndPoint) {
                boolean bl = fillable = BufferUtil.hasContent(SslConnection.this._decryptedInput) || BufferUtil.hasContent(SslConnection.this._encryptedInput) && !this._underFlown;
                if (!fillable && this._fillRequiresFlushToProgress) {
                    if (BufferUtil.hasContent(SslConnection.this._encryptedOutput)) {
                        this._cannotAcceptMoreAppDataToFlush = true;
                        write = true;
                    } else {
                        this._fillRequiresFlushToProgress = false;
                        fillable = true;
                    }
                }
            }
            if (write) {
                SslConnection.this.getEndPoint().write(this._writeCallback, SslConnection.this._encryptedOutput);
            } else if (fillable) {
                SslConnection.this.getExecutor().execute(SslConnection.this._runFillable);
            } else {
                this.ensureFillInterested();
            }
        }

        @Override
        public void setConnection(Connection connection) {
            AbstractConnection a;
            if (connection instanceof AbstractConnection && (a = (AbstractConnection)connection).getInputBufferSize() < SslConnection.this._sslEngine.getSession().getApplicationBufferSize()) {
                a.setInputBufferSize(SslConnection.this._sslEngine.getSession().getApplicationBufferSize());
            }
            super.setConnection(connection);
        }

        public SslConnection getSslConnection() {
            return SslConnection.this;
        }

        /*
         * Exception decompiling
         */
        @Override
        public int fill(ByteBuffer buffer) throws IOException {
            /*
             * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
             * 
             * org.benf.cfr.reader.util.ConfusedCFRException: Tried to end blocks [0[TRYBLOCK]], but top level block is 26[MONITOR]
             *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.processEndingBlocks(Op04StructuredStatement.java:435)
             *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:484)
             *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
             *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
             *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
             *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
             *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
             *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
             *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
             *     at org.benf.cfr.reader.entities.ClassFile.analyseInnerClassesPass1(ClassFile.java:923)
             *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1035)
             *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
             *     at org.benf.cfr.reader.Driver.doJarVersionTypes(Driver.java:257)
             *     at org.benf.cfr.reader.Driver.doJar(Driver.java:139)
             *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:76)
             *     at org.benf.cfr.reader.Main.main(Main.java:54)
             */
            throw new IllegalStateException("Decompilation failed");
        }

        private void handshakeFinished() {
            if (this._handshaken) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Renegotiated {}", SslConnection.this);
                }
                if (SslConnection.this._renegotiationLimit > 0) {
                    SslConnection.this._renegotiationLimit--;
                }
            } else {
                this._handshaken = true;
                if (LOG.isDebugEnabled()) {
                    LOG.debug("{} handshake succeeded {}/{} {}", SslConnection.this._sslEngine.getUseClientMode() ? "client" : "resumed server", SslConnection.this._sslEngine.getSession().getProtocol(), SslConnection.this._sslEngine.getSession().getCipherSuite(), SslConnection.this);
                }
                this.notifyHandshakeSucceeded(SslConnection.this._sslEngine);
            }
        }

        private boolean allowRenegotiate(SSLEngineResult.HandshakeStatus handshakeStatus) {
            if (!this._handshaken || handshakeStatus == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
                return true;
            }
            if (!SslConnection.this.isRenegotiationAllowed()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Renegotiation denied {}", SslConnection.this);
                }
                this.terminateInput();
                return false;
            }
            if (SslConnection.this.getRenegotiationLimit() == 0) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Renegotiation limit exceeded {}", SslConnection.this);
                }
                this.terminateInput();
                return false;
            }
            return true;
        }

        private void terminateInput() {
            try {
                SslConnection.this._sslEngine.closeInbound();
            }
            catch (Throwable x) {
                LOG.ignore(x);
            }
        }

        private void closeInbound() throws SSLException {
            SSLEngineResult.HandshakeStatus handshakeStatus = SslConnection.this._sslEngine.getHandshakeStatus();
            try {
                SslConnection.this._sslEngine.closeInbound();
            }
            catch (SSLException x) {
                if (handshakeStatus == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING && !SslConnection.this.isAllowMissingCloseMessage()) {
                    throw x;
                }
                LOG.ignore(x);
            }
        }

        /*
         * Exception decompiling
         */
        @Override
        public boolean flush(ByteBuffer ... appOuts) throws IOException {
            /*
             * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
             * 
             * org.benf.cfr.reader.util.ConfusedCFRException: Tried to end blocks [0[TRYBLOCK]], but top level block is 18[MONITOR]
             *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.processEndingBlocks(Op04StructuredStatement.java:435)
             *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:484)
             *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
             *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
             *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
             *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
             *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
             *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
             *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
             *     at org.benf.cfr.reader.entities.ClassFile.analyseInnerClassesPass1(ClassFile.java:923)
             *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1035)
             *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
             *     at org.benf.cfr.reader.Driver.doJarVersionTypes(Driver.java:257)
             *     at org.benf.cfr.reader.Driver.doJar(Driver.java:139)
             *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:76)
             *     at org.benf.cfr.reader.Main.main(Main.java:54)
             */
            throw new IllegalStateException("Decompilation failed");
        }

        private void releaseEncryptedOutputBuffer() {
            if (!Thread.holdsLock(this)) {
                throw new IllegalStateException();
            }
            if (SslConnection.this._encryptedOutput != null && !SslConnection.this._encryptedOutput.hasRemaining()) {
                SslConnection.this._bufferPool.release(SslConnection.this._encryptedOutput);
                SslConnection.this._encryptedOutput = null;
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public void doShutdownOutput() {
            try {
                boolean flush = false;
                boolean close = false;
                DecryptedEndPoint decryptedEndPoint = SslConnection.this._decryptedEndPoint;
                synchronized (decryptedEndPoint) {
                    boolean ishut = this.isInputShutdown();
                    boolean oshut = this.isOutputShutdown();
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("shutdownOutput: oshut={}, ishut={} {}", oshut, ishut, SslConnection.this);
                    }
                    if (oshut) {
                        return;
                    }
                    if (!SslConnection.this._closedOutbound) {
                        SslConnection.this._closedOutbound = true;
                        SslConnection.this._sslEngine.closeOutbound();
                        flush = true;
                    }
                    if (ishut) {
                        close = true;
                    }
                }
                if (flush) {
                    this.flush(BufferUtil.EMPTY_BUFFER);
                }
                if (close) {
                    SslConnection.this.getEndPoint().close();
                } else {
                    this.ensureFillInterested();
                }
            }
            catch (Throwable x) {
                LOG.ignore(x);
                SslConnection.this.getEndPoint().close();
            }
        }

        private void ensureFillInterested() {
            if (LOG.isDebugEnabled()) {
                LOG.debug("fillInterested SSL NB {}", SslConnection.this);
            }
            SslConnection.this.tryFillInterested(SslConnection.this._sslReadCallback);
        }

        @Override
        public boolean isOutputShutdown() {
            return SslConnection.this._sslEngine.isOutboundDone() || SslConnection.this.getEndPoint().isOutputShutdown();
        }

        @Override
        public void doClose() {
            this.doShutdownOutput();
            SslConnection.this.getEndPoint().close();
            super.doClose();
        }

        @Override
        public Object getTransport() {
            return SslConnection.this.getEndPoint();
        }

        @Override
        public boolean isInputShutdown() {
            return SslConnection.this._sslEngine.isInboundDone();
        }

        private void notifyHandshakeSucceeded(SSLEngine sslEngine) {
            SslHandshakeListener.Event event = null;
            for (SslHandshakeListener listener : SslConnection.this.handshakeListeners) {
                if (event == null) {
                    event = new SslHandshakeListener.Event(sslEngine);
                }
                try {
                    listener.handshakeSucceeded(event);
                }
                catch (Throwable x) {
                    LOG.info("Exception while notifying listener " + listener, x);
                }
            }
        }

        private void notifyHandshakeFailed(SSLEngine sslEngine, Throwable failure) {
            SslHandshakeListener.Event event = null;
            for (SslHandshakeListener listener : SslConnection.this.handshakeListeners) {
                if (event == null) {
                    event = new SslHandshakeListener.Event(sslEngine);
                }
                try {
                    listener.handshakeFailed(event, failure);
                }
                catch (Throwable x) {
                    LOG.info("Exception while notifying listener " + listener, x);
                }
            }
        }

        @Override
        public String toString() {
            return super.toString() + "->" + SslConnection.this.getEndPoint().toString();
        }

        private class FailWrite
        extends RunnableTask {
            private final Throwable failure;

            private FailWrite(Throwable failure) {
                super("runFailWrite");
                this.failure = failure;
            }

            @Override
            public void run() {
                DecryptedEndPoint.this.getWriteFlusher().onFail(this.failure);
            }

            @Override
            public Invocable.InvocationType getInvocationType() {
                return DecryptedEndPoint.this.getWriteFlusher().getCallbackInvocationType();
            }
        }

        private final class WriteCallBack
        implements Callback,
        Invocable {
            private WriteCallBack() {
            }

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            @Override
            public void succeeded() {
                boolean fillable = false;
                DecryptedEndPoint decryptedEndPoint = DecryptedEndPoint.this;
                synchronized (decryptedEndPoint) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("write.complete {}", SslConnection.this.getEndPoint());
                    }
                    DecryptedEndPoint.this.releaseEncryptedOutputBuffer();
                    DecryptedEndPoint.this._cannotAcceptMoreAppDataToFlush = false;
                    if (DecryptedEndPoint.this._fillRequiresFlushToProgress) {
                        DecryptedEndPoint.this._fillRequiresFlushToProgress = false;
                        fillable = true;
                    }
                }
                if (fillable) {
                    DecryptedEndPoint.this.getFillInterest().fillable();
                }
                SslConnection.this._runCompleteWrite.run();
            }

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            @Override
            public void failed(Throwable x) {
                boolean fail_filler;
                DecryptedEndPoint decryptedEndPoint = DecryptedEndPoint.this;
                synchronized (decryptedEndPoint) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("write failed {}", SslConnection.this, x);
                    }
                    BufferUtil.clear(SslConnection.this._encryptedOutput);
                    DecryptedEndPoint.this.releaseEncryptedOutputBuffer();
                    DecryptedEndPoint.this._cannotAcceptMoreAppDataToFlush = false;
                    fail_filler = DecryptedEndPoint.this._fillRequiresFlushToProgress;
                    if (DecryptedEndPoint.this._fillRequiresFlushToProgress) {
                        DecryptedEndPoint.this._fillRequiresFlushToProgress = false;
                    }
                }
                SslConnection.this.failedCallback(new Callback(){

                    @Override
                    public void failed(Throwable x) {
                        if (fail_filler) {
                            DecryptedEndPoint.this.getFillInterest().onFail(x);
                        }
                        DecryptedEndPoint.this.getWriteFlusher().onFail(x);
                    }
                }, x);
            }

            @Override
            public Invocable.InvocationType getInvocationType() {
                return DecryptedEndPoint.this.getWriteFlusher().getCallbackInvocationType();
            }

            public String toString() {
                return String.format("SSL@%h.DEP.writeCallback", SslConnection.this);
            }
        }
    }

    private abstract class RunnableTask
    implements Runnable,
    Invocable {
        private final String _operation;

        protected RunnableTask(String op) {
            this._operation = op;
        }

        public String toString() {
            return String.format("SSL:%s:%s:%s", new Object[]{SslConnection.this, this._operation, this.getInvocationType()});
        }
    }
}

