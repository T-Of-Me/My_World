/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.AsyncListener;
import javax.servlet.ServletContext;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;
import org.eclipse.jetty.http.BadMessageException;
import org.eclipse.jetty.server.AsyncContextEvent;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.thread.Locker;

public class HttpChannelState {
    private static final Logger LOG = Log.getLogger(HttpChannelState.class);
    private static final long DEFAULT_TIMEOUT = Long.getLong("org.eclipse.jetty.server.HttpChannelState.DEFAULT_TIMEOUT", 30000L);
    private final Locker _locker = new Locker();
    private final HttpChannel _channel;
    private List<AsyncListener> _asyncListeners;
    private State _state;
    private Async _async;
    private boolean _initial;
    private AsyncRead _asyncRead = AsyncRead.IDLE;
    private boolean _asyncWritePossible;
    private long _timeoutMs = DEFAULT_TIMEOUT;
    private AsyncContextEvent _event;

    protected HttpChannelState(HttpChannel channel) {
        this._channel = channel;
        this._state = State.IDLE;
        this._async = Async.NOT_ASYNC;
        this._initial = true;
    }

    public State getState() {
        try (Locker.Lock lock = this._locker.lock();){
            State state = this._state;
            return state;
        }
    }

    public void addListener(AsyncListener listener) {
        try (Locker.Lock lock = this._locker.lock();){
            if (this._asyncListeners == null) {
                this._asyncListeners = new ArrayList<AsyncListener>();
            }
            this._asyncListeners.add(listener);
        }
    }

    public void setTimeout(long ms) {
        try (Locker.Lock lock = this._locker.lock();){
            this._timeoutMs = ms;
        }
    }

    public long getTimeout() {
        try (Locker.Lock lock = this._locker.lock();){
            long l = this._timeoutMs;
            return l;
        }
    }

    public AsyncContextEvent getAsyncContextEvent() {
        try (Locker.Lock lock = this._locker.lock();){
            AsyncContextEvent asyncContextEvent = this._event;
            return asyncContextEvent;
        }
    }

    public String toString() {
        try (Locker.Lock lock = this._locker.lock();){
            String string = this.toStringLocked();
            return string;
        }
    }

    public String toStringLocked() {
        return String.format("%s@%x{s=%s a=%s i=%b r=%s w=%b}", new Object[]{this.getClass().getSimpleName(), this.hashCode(), this._state, this._async, this._initial, this._asyncRead, this._asyncWritePossible});
    }

    private String getStatusStringLocked() {
        return String.format("s=%s i=%b a=%s", new Object[]{this._state, this._initial, this._async});
    }

    public String getStatusString() {
        try (Locker.Lock lock = this._locker.lock();){
            String string = this.getStatusStringLocked();
            return string;
        }
    }

    protected Action handling() {
        Throwable throwable = null;
        try (Locker.Lock lock = this._locker.lock();){
            if (LOG.isDebugEnabled()) {
                LOG.debug("handling {}", this.toStringLocked());
            }
            switch (this._state) {
                case IDLE: {
                    this._initial = true;
                    this._state = State.DISPATCHED;
                    Action action = Action.DISPATCH;
                    return action;
                }
                case COMPLETING: 
                case COMPLETED: {
                    Action action = Action.TERMINATED;
                    return action;
                }
                case ASYNC_WOKEN: {
                    switch (this._asyncRead) {
                        case POSSIBLE: {
                            this._state = State.ASYNC_IO;
                            this._asyncRead = AsyncRead.PRODUCING;
                            Action action = Action.READ_PRODUCE;
                            return action;
                        }
                        case READY: {
                            this._state = State.ASYNC_IO;
                            this._asyncRead = AsyncRead.IDLE;
                            Action action = Action.READ_CALLBACK;
                            return action;
                        }
                        case REGISTER: 
                        case PRODUCING: {
                            throw new IllegalStateException(this.toStringLocked());
                        }
                    }
                    if (this._asyncWritePossible) {
                        this._state = State.ASYNC_IO;
                        this._asyncWritePossible = false;
                        Action action = Action.WRITE_CALLBACK;
                        return action;
                    }
                    switch (this._async) {
                        case COMPLETE: {
                            this._state = State.COMPLETING;
                            Action action = Action.COMPLETE;
                            return action;
                        }
                        case DISPATCH: {
                            this._state = State.DISPATCHED;
                            this._async = Async.NOT_ASYNC;
                            Action action = Action.ASYNC_DISPATCH;
                            return action;
                        }
                        case EXPIRED: 
                        case ERRORED: {
                            this._state = State.DISPATCHED;
                            this._async = Async.NOT_ASYNC;
                            Action action = Action.ERROR_DISPATCH;
                            return action;
                        }
                        case STARTED: 
                        case EXPIRING: 
                        case ERRORING: {
                            Action action = Action.WAIT;
                            return action;
                        }
                        case NOT_ASYNC: {
                            break;
                        }
                        default: {
                            throw new IllegalStateException(this.getStatusStringLocked());
                        }
                    }
                    Action action = Action.WAIT;
                    return action;
                }
                case ASYNC_ERROR: {
                    Action action = Action.ASYNC_ERROR;
                    return action;
                }
            }
            try {
                throw new IllegalStateException(this.getStatusStringLocked());
            }
            catch (Throwable throwable2) {
                throwable = throwable2;
                throw throwable2;
            }
        }
    }

    public void startAsync(final AsyncContextEvent event) {
        List<AsyncListener> lastAsyncListeners;
        try (Locker.Lock lock = this._locker.lock();){
            if (LOG.isDebugEnabled()) {
                LOG.debug("startAsync {}", this.toStringLocked());
            }
            if (this._state != State.DISPATCHED || this._async != Async.NOT_ASYNC) {
                throw new IllegalStateException(this.getStatusStringLocked());
            }
            this._async = Async.STARTED;
            this._event = event;
            lastAsyncListeners = this._asyncListeners;
            this._asyncListeners = null;
        }
        if (lastAsyncListeners != null) {
            Runnable callback = new Runnable(){

                @Override
                public void run() {
                    for (AsyncListener listener : lastAsyncListeners) {
                        try {
                            listener.onStartAsync(event);
                        }
                        catch (Throwable e) {
                            LOG.warn(e);
                        }
                    }
                }

                public String toString() {
                    return "startAsync";
                }
            };
            this.runInContext(event, callback);
        }
    }

    /*
     * Unable to fully structure code
     */
    public void asyncError(Throwable failure) {
        event = null;
        lock = this._locker.lock();
        var4_4 = null;
        try {
            switch (5.$SwitchMap$org$eclipse$jetty$server$HttpChannelState$State[this._state.ordinal()]) {
                case 1: 
                case 2: 
                case 3: 
                case 4: 
                case 5: 
                case 6: 
                case 8: 
                case 9: {
                    ** break;
lbl8:
                    // 1 sources

                    break;
                }
                case 7: {
                    this._event.addThrowable(failure);
                    this._state = State.ASYNC_ERROR;
                    event = this._event;
                    ** break;
lbl14:
                    // 1 sources

                    break;
                }
                default: {
                    throw new IllegalStateException(this.getStatusStringLocked());
                }
            }
        }
        catch (Throwable var5_6) {
            var4_4 = var5_6;
            throw var5_6;
        }
        finally {
            if (lock != null) {
                if (var4_4 != null) {
                    try {
                        lock.close();
                    }
                    catch (Throwable var5_5) {
                        var4_4.addSuppressed(var5_5);
                    }
                } else {
                    lock.close();
                }
            }
        }
        if (event != null) {
            this.cancelTimeout(event);
            this.runInContext(event, this._channel);
        }
    }

    /*
     * Exception decompiling
     */
    protected Action unhandle() {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Tried to end blocks [1[TRYBLOCK]], but top level block is 48[CASE]
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.processEndingBlocks(Op04StructuredStatement.java:435)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:484)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doJarVersionTypes(Driver.java:257)
         *     at org.benf.cfr.reader.Driver.doJar(Driver.java:139)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:76)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    /*
     * Unable to fully structure code
     */
    public void dispatch(ServletContext context, String path) {
        block24: {
            dispatch = false;
            lock = this._locker.lock();
            var6_5 = null;
            try {
                if (HttpChannelState.LOG.isDebugEnabled()) {
                    HttpChannelState.LOG.debug("dispatch {} -> {}", new Object[]{this.toStringLocked(), path});
                }
                started = false;
                event = this._event;
                switch (5.$SwitchMap$org$eclipse$jetty$server$HttpChannelState$Async[this._async.ordinal()]) {
                    case 5: {
                        started = true;
                        break;
                    }
                    case 4: 
                    case 6: 
                    case 7: {
                        break;
                    }
                    default: {
                        throw new IllegalStateException(this.getStatusStringLocked());
                    }
                }
                this._async = Async.DISPATCH;
                if (context != null) {
                    this._event.setDispatchContext(context);
                }
                if (path != null) {
                    this._event.setDispatchPath(path);
                }
                if (!started) break block24;
                switch (5.$SwitchMap$org$eclipse$jetty$server$HttpChannelState$State[this._state.ordinal()]) {
                    case 4: 
                    case 6: 
                    case 8: {
                        ** break;
lbl26:
                        // 1 sources

                        break;
                    }
                    case 7: {
                        this._state = State.ASYNC_WOKEN;
                        dispatch = true;
                        ** break;
lbl31:
                        // 1 sources

                        break;
                    }
                    default: {
                        HttpChannelState.LOG.warn("async dispatched when complete {}", new Object[]{this});
                        break;
                    }
                }
            }
            catch (Throwable var7_8) {
                var6_5 = var7_8;
                throw var7_8;
            }
            finally {
                if (lock != null) {
                    if (var6_5 != null) {
                        try {
                            lock.close();
                        }
                        catch (Throwable var7_7) {
                            var6_5.addSuppressed(var7_7);
                        }
                    } else {
                        lock.close();
                    }
                }
            }
        }
        this.cancelTimeout(event);
        if (dispatch) {
            this.scheduleDispatch();
        }
    }

    protected void onTimeout() {
        List<AsyncListener> listeners;
        AsyncContextEvent event;
        try (Locker.Lock lock = this._locker.lock();){
            if (LOG.isDebugEnabled()) {
                LOG.debug("onTimeout {}", this.toStringLocked());
            }
            if (this._async != Async.STARTED) {
                return;
            }
            this._async = Async.EXPIRING;
            event = this._event;
            listeners = this._asyncListeners;
        }
        final AtomicReference error = new AtomicReference();
        if (listeners != null) {
            Runnable task = new Runnable(){

                @Override
                public void run() {
                    for (AsyncListener listener : listeners) {
                        try {
                            listener.onTimeout(event);
                        }
                        catch (Throwable x) {
                            LOG.warn(x + " while invoking onTimeout listener " + listener, new Object[0]);
                            LOG.debug(x);
                            if (error.get() == null) {
                                error.set(x);
                                continue;
                            }
                            ((Throwable)error.get()).addSuppressed(x);
                        }
                    }
                }

                public String toString() {
                    return "onTimeout";
                }
            };
            this.runInContext(event, task);
        }
        Throwable th = (Throwable)error.get();
        boolean dispatch = false;
        try (Locker.Lock lock = this._locker.lock();){
            switch (this._async) {
                case EXPIRING: {
                    this._async = th == null ? Async.EXPIRED : Async.ERRORING;
                    break;
                }
                case COMPLETE: 
                case DISPATCH: {
                    if (th == null) break;
                    LOG.ignore(th);
                    th = null;
                    break;
                }
                default: {
                    throw new IllegalStateException();
                }
            }
            if (this._state == State.ASYNC_WAIT) {
                this._state = State.ASYNC_WOKEN;
                dispatch = true;
            }
        }
        if (th != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Error after async timeout {}", this, th);
            }
            this.onError(th);
        }
        if (dispatch) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Dispatch after async timeout {}", this);
            }
            this.scheduleDispatch();
        }
    }

    public void complete() {
        AsyncContextEvent event;
        boolean handle = false;
        try (Locker.Lock lock = this._locker.lock();){
            if (LOG.isDebugEnabled()) {
                LOG.debug("complete {}", this.toStringLocked());
            }
            boolean started = false;
            event = this._event;
            switch (this._async) {
                case STARTED: {
                    started = true;
                    break;
                }
                case ERRORED: 
                case EXPIRING: 
                case ERRORING: {
                    break;
                }
                case COMPLETE: {
                    return;
                }
                default: {
                    throw new IllegalStateException(this.getStatusStringLocked());
                }
            }
            this._async = Async.COMPLETE;
            if (started && this._state == State.ASYNC_WAIT) {
                handle = true;
                this._state = State.ASYNC_WOKEN;
            }
        }
        this.cancelTimeout(event);
        if (handle) {
            this.runInContext(event, this._channel);
        }
    }

    public void errorComplete() {
        try (Locker.Lock lock = this._locker.lock();){
            if (LOG.isDebugEnabled()) {
                LOG.debug("error complete {}", this.toStringLocked());
            }
            this._async = Async.COMPLETE;
            this._event.setDispatchContext(null);
            this._event.setDispatchPath(null);
        }
        this.cancelTimeout();
    }

    protected void onError(Throwable failure) {
        AsyncContextEvent event;
        List<AsyncListener> listeners;
        Request baseRequest = this._channel.getRequest();
        int code = 500;
        String reason = null;
        if (failure instanceof BadMessageException) {
            BadMessageException bme = (BadMessageException)failure;
            code = bme.getCode();
            reason = bme.getReason();
        } else if (failure instanceof UnavailableException) {
            code = ((UnavailableException)failure).isPermanent() ? 404 : 503;
        }
        try (Locker.Lock lock = this._locker.lock();){
            if (LOG.isDebugEnabled()) {
                LOG.debug("onError {} {}", this.toStringLocked(), failure);
            }
            if (this._event != null) {
                this._event.addThrowable(failure);
                this._event.getSuppliedRequest().setAttribute("javax.servlet.error.status_code", code);
                this._event.getSuppliedRequest().setAttribute("javax.servlet.error.exception", failure);
                this._event.getSuppliedRequest().setAttribute("javax.servlet.error.exception_type", failure == null ? null : failure.getClass());
                this._event.getSuppliedRequest().setAttribute("javax.servlet.error.message", reason);
            } else {
                Throwable error = (Throwable)baseRequest.getAttribute("javax.servlet.error.exception");
                if (error != null) {
                    throw new IllegalStateException("Error already set", error);
                }
                baseRequest.setAttribute("javax.servlet.error.status_code", code);
                baseRequest.setAttribute("javax.servlet.error.exception", failure);
                baseRequest.setAttribute("javax.servlet.error.exception_type", failure == null ? null : failure.getClass());
                baseRequest.setAttribute("javax.servlet.error.message", reason);
            }
            if (this._async == Async.NOT_ASYNC) {
                if (this._state == State.DISPATCHED) {
                    this._state = State.THROWN;
                    return;
                }
                throw new IllegalStateException(this.getStatusStringLocked());
            }
            this._async = Async.ERRORING;
            listeners = this._asyncListeners;
            event = this._event;
        }
        if (listeners != null) {
            Runnable task = new Runnable(){

                @Override
                public void run() {
                    for (AsyncListener listener : listeners) {
                        try {
                            listener.onError(event);
                        }
                        catch (Throwable x) {
                            LOG.warn(x + " while invoking onError listener " + listener, new Object[0]);
                            LOG.debug(x);
                        }
                    }
                }

                public String toString() {
                    return "onError";
                }
            };
            this.runInContext(event, task);
        }
        boolean dispatch = false;
        try (Locker.Lock lock = this._locker.lock();){
            switch (this._async) {
                case ERRORING: {
                    this._async = Async.ERRORED;
                    break;
                }
                case COMPLETE: 
                case DISPATCH: {
                    break;
                }
                default: {
                    throw new IllegalStateException(this.toString());
                }
            }
            if (this._state == State.ASYNC_WAIT) {
                this._state = State.ASYNC_WOKEN;
                dispatch = true;
            }
        }
        if (dispatch) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Dispatch after error {}", this);
            }
            this.scheduleDispatch();
        }
    }

    /*
     * Unable to fully structure code
     */
    protected void onComplete() {
        lock = this._locker.lock();
        var4_2 = null;
        try {
            if (HttpChannelState.LOG.isDebugEnabled()) {
                HttpChannelState.LOG.debug("onComplete {}", new Object[]{this.toStringLocked()});
            }
            switch (5.$SwitchMap$org$eclipse$jetty$server$HttpChannelState$State[this._state.ordinal()]) {
                case 2: {
                    aListeners = this._asyncListeners;
                    event = this._event;
                    this._state = State.COMPLETED;
                    this._async = Async.NOT_ASYNC;
                    ** break;
lbl13:
                    // 1 sources

                    break;
                }
                default: {
                    throw new IllegalStateException(this.getStatusStringLocked());
                }
            }
        }
        catch (Throwable var5_6) {
            var4_2 = var5_6;
            throw var5_6;
        }
        finally {
            if (lock != null) {
                if (var4_2 != null) {
                    try {
                        lock.close();
                    }
                    catch (Throwable var5_5) {
                        var4_2.addSuppressed(var5_5);
                    }
                } else {
                    lock.close();
                }
            }
        }
        if (event != null) {
            if (aListeners != null) {
                callback = new Runnable(){

                    @Override
                    public void run() {
                        for (AsyncListener listener : aListeners) {
                            try {
                                listener.onComplete(event);
                            }
                            catch (Throwable e) {
                                LOG.warn(e + " while invoking onComplete listener " + listener, new Object[0]);
                                LOG.debug(e);
                            }
                        }
                    }

                    public String toString() {
                        return "onComplete";
                    }
                };
                this.runInContext(event, callback);
            }
            event.completed();
        }
    }

    protected void recycle() {
        this.cancelTimeout();
        try (Locker.Lock lock = this._locker.lock();){
            if (LOG.isDebugEnabled()) {
                LOG.debug("recycle {}", this.toStringLocked());
            }
            switch (this._state) {
                case ASYNC_IO: 
                case DISPATCHED: {
                    throw new IllegalStateException(this.getStatusStringLocked());
                }
                case UPGRADED: {
                    return;
                }
            }
            this._asyncListeners = null;
            this._state = State.IDLE;
            this._async = Async.NOT_ASYNC;
            this._initial = true;
            this._asyncRead = AsyncRead.IDLE;
            this._asyncWritePossible = false;
            this._timeoutMs = DEFAULT_TIMEOUT;
            this._event = null;
        }
    }

    public void upgrade() {
        this.cancelTimeout();
        try (Locker.Lock lock = this._locker.lock();){
            if (LOG.isDebugEnabled()) {
                LOG.debug("upgrade {}", this.toStringLocked());
            }
            switch (this._state) {
                case IDLE: 
                case COMPLETED: {
                    break;
                }
                default: {
                    throw new IllegalStateException(this.getStatusStringLocked());
                }
            }
            this._asyncListeners = null;
            this._state = State.UPGRADED;
            this._async = Async.NOT_ASYNC;
            this._initial = true;
            this._asyncRead = AsyncRead.IDLE;
            this._asyncWritePossible = false;
            this._timeoutMs = DEFAULT_TIMEOUT;
            this._event = null;
        }
    }

    protected void scheduleDispatch() {
        this._channel.execute(this._channel);
    }

    protected void cancelTimeout() {
        AsyncContextEvent event;
        try (Locker.Lock lock = this._locker.lock();){
            event = this._event;
        }
        this.cancelTimeout(event);
    }

    protected void cancelTimeout(AsyncContextEvent event) {
        if (event != null) {
            event.cancelTimeoutTask();
        }
    }

    public boolean isIdle() {
        try (Locker.Lock lock = this._locker.lock();){
            boolean bl = this._state == State.IDLE;
            return bl;
        }
    }

    public boolean isExpired() {
        try (Locker.Lock lock = this._locker.lock();){
            boolean bl = this._async == Async.EXPIRED;
            return bl;
        }
    }

    public boolean isInitial() {
        try (Locker.Lock lock = this._locker.lock();){
            boolean bl = this._initial;
            return bl;
        }
    }

    public boolean isSuspended() {
        try (Locker.Lock lock = this._locker.lock();){
            boolean bl = this._state == State.ASYNC_WAIT || this._state == State.DISPATCHED && this._async == Async.STARTED;
            return bl;
        }
    }

    boolean isCompleting() {
        try (Locker.Lock lock = this._locker.lock();){
            boolean bl = this._state == State.COMPLETING;
            return bl;
        }
    }

    boolean isCompleted() {
        try (Locker.Lock lock = this._locker.lock();){
            boolean bl = this._state == State.COMPLETED;
            return bl;
        }
    }

    public boolean isAsyncStarted() {
        try (Locker.Lock lock = this._locker.lock();){
            if (this._state == State.DISPATCHED) {
                boolean bl = this._async != Async.NOT_ASYNC;
                return bl;
            }
            boolean bl = this._async == Async.STARTED || this._async == Async.EXPIRING;
            return bl;
        }
    }

    public boolean isAsyncComplete() {
        try (Locker.Lock lock = this._locker.lock();){
            boolean bl = this._async == Async.COMPLETE;
            return bl;
        }
    }

    public boolean isAsync() {
        try (Locker.Lock lock = this._locker.lock();){
            boolean bl = !this._initial || this._async != Async.NOT_ASYNC;
            return bl;
        }
    }

    public Request getBaseRequest() {
        return this._channel.getRequest();
    }

    public HttpChannel getHttpChannel() {
        return this._channel;
    }

    public ContextHandler getContextHandler() {
        AsyncContextEvent event;
        try (Locker.Lock lock = this._locker.lock();){
            event = this._event;
        }
        return this.getContextHandler(event);
    }

    ContextHandler getContextHandler(AsyncContextEvent event) {
        ContextHandler.Context context;
        if (event != null && (context = (ContextHandler.Context)event.getServletContext()) != null) {
            return context.getContextHandler();
        }
        return null;
    }

    public ServletResponse getServletResponse() {
        AsyncContextEvent event;
        try (Locker.Lock lock = this._locker.lock();){
            event = this._event;
        }
        return this.getServletResponse(event);
    }

    public ServletResponse getServletResponse(AsyncContextEvent event) {
        if (event != null && event.getSuppliedResponse() != null) {
            return event.getSuppliedResponse();
        }
        return this._channel.getResponse();
    }

    void runInContext(AsyncContextEvent event, Runnable runnable) {
        ContextHandler contextHandler = this.getContextHandler(event);
        if (contextHandler == null) {
            runnable.run();
        } else {
            contextHandler.handle(this._channel.getRequest(), runnable);
        }
    }

    public Object getAttribute(String name) {
        return this._channel.getRequest().getAttribute(name);
    }

    public void removeAttribute(String name) {
        this._channel.getRequest().removeAttribute(name);
    }

    public void setAttribute(String name, Object attribute) {
        this._channel.getRequest().setAttribute(name, attribute);
    }

    /*
     * Unable to fully structure code
     */
    public void onReadUnready() {
        interested = false;
        lock = this._locker.lock();
        var3_3 = null;
        try {
            if (HttpChannelState.LOG.isDebugEnabled()) {
                HttpChannelState.LOG.debug("onReadUnready {}", new Object[]{this.toStringLocked()});
            }
            switch (5.$SwitchMap$org$eclipse$jetty$server$HttpChannelState$AsyncRead[this._asyncRead.ordinal()]) {
                case 2: 
                case 5: {
                    if (this._state == State.ASYNC_WAIT) {
                        interested = true;
                        this._asyncRead = AsyncRead.REGISTERED;
                        ** break;
lbl13:
                        // 1 sources

                    } else {
                        this._asyncRead = AsyncRead.REGISTER;
                        ** break;
                    }
lbl16:
                    // 1 sources

                    break;
                }
                ** default:
lbl18:
                // 1 sources

                break;
            }
        }
        catch (Throwable var4_5) {
            var3_3 = var4_5;
            throw var4_5;
        }
        finally {
            if (lock != null) {
                if (var3_3 != null) {
                    try {
                        lock.close();
                    }
                    catch (Throwable var4_4) {
                        var3_3.addSuppressed(var4_4);
                    }
                } else {
                    lock.close();
                }
            }
        }
        if (interested) {
            this._channel.asyncReadFillInterested();
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public boolean onContentAdded() {
        boolean woken = false;
        try (Locker.Lock lock = this._locker.lock();){
            if (LOG.isDebugEnabled()) {
                LOG.debug("onContentAdded {}", this.toStringLocked());
            }
            switch (this._asyncRead) {
                case READY: 
                case IDLE: {
                    return woken;
                }
                case PRODUCING: {
                    this._asyncRead = AsyncRead.READY;
                    return woken;
                }
                case REGISTER: 
                case REGISTERED: {
                    this._asyncRead = AsyncRead.READY;
                    if (this._state != State.ASYNC_WAIT) return woken;
                    woken = true;
                    this._state = State.ASYNC_WOKEN;
                    return woken;
                }
                case POSSIBLE: {
                    throw new IllegalStateException(this.toStringLocked());
                }
            }
            return woken;
        }
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public boolean onReadReady() {
        boolean woken = false;
        try (Locker.Lock lock = this._locker.lock();){
            if (LOG.isDebugEnabled()) {
                LOG.debug("onReadReady {}", this.toStringLocked());
            }
            switch (this._asyncRead) {
                case IDLE: {
                    this._asyncRead = AsyncRead.READY;
                    if (this._state != State.ASYNC_WAIT) return woken;
                    woken = true;
                    this._state = State.ASYNC_WOKEN;
                    return woken;
                }
                default: {
                    throw new IllegalStateException(this.toStringLocked());
                }
            }
        }
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public boolean onReadPossible() {
        boolean woken = false;
        try (Locker.Lock lock = this._locker.lock();){
            if (LOG.isDebugEnabled()) {
                LOG.debug("onReadPossible {}", this.toStringLocked());
            }
            switch (this._asyncRead) {
                case REGISTERED: {
                    this._asyncRead = AsyncRead.POSSIBLE;
                    if (this._state != State.ASYNC_WAIT) return woken;
                    woken = true;
                    this._state = State.ASYNC_WOKEN;
                    return woken;
                }
                default: {
                    throw new IllegalStateException(this.toStringLocked());
                }
            }
        }
    }

    public boolean onReadEof() {
        boolean woken = false;
        try (Locker.Lock lock = this._locker.lock();){
            if (LOG.isDebugEnabled()) {
                LOG.debug("onEof {}", this.toStringLocked());
            }
            this._asyncRead = AsyncRead.READY;
            if (this._state == State.ASYNC_WAIT) {
                woken = true;
                this._state = State.ASYNC_WOKEN;
            }
        }
        return woken;
    }

    public boolean onWritePossible() {
        boolean wake = false;
        try (Locker.Lock lock = this._locker.lock();){
            if (LOG.isDebugEnabled()) {
                LOG.debug("onWritePossible {}", this.toStringLocked());
            }
            this._asyncWritePossible = true;
            if (this._state == State.ASYNC_WAIT) {
                this._state = State.ASYNC_WOKEN;
                wake = true;
            }
        }
        return wake;
    }

    private static enum AsyncRead {
        IDLE,
        REGISTER,
        REGISTERED,
        POSSIBLE,
        PRODUCING,
        READY;

    }

    private static enum Async {
        NOT_ASYNC,
        STARTED,
        DISPATCH,
        COMPLETE,
        EXPIRING,
        EXPIRED,
        ERRORING,
        ERRORED;

    }

    public static enum Action {
        DISPATCH,
        ASYNC_DISPATCH,
        ERROR_DISPATCH,
        ASYNC_ERROR,
        WRITE_CALLBACK,
        READ_PRODUCE,
        READ_CALLBACK,
        COMPLETE,
        TERMINATED,
        WAIT;

    }

    public static enum State {
        IDLE,
        DISPATCHED,
        THROWN,
        ASYNC_WAIT,
        ASYNC_WOKEN,
        ASYNC_IO,
        ASYNC_ERROR,
        COMPLETING,
        COMPLETED,
        UPGRADED;

    }
}

