/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.WritePendingException;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.thread.Invocable;

public abstract class WriteFlusher {
    private static final Logger LOG = Log.getLogger(WriteFlusher.class);
    private static final boolean DEBUG = LOG.isDebugEnabled();
    private static final ByteBuffer[] EMPTY_BUFFERS = new ByteBuffer[]{BufferUtil.EMPTY_BUFFER};
    private static final EnumMap<StateType, Set<StateType>> __stateTransitions = new EnumMap(StateType.class);
    private static final State __IDLE = new IdleState();
    private static final State __WRITING = new WritingState();
    private static final State __COMPLETING = new CompletingState();
    private final EndPoint _endPoint;
    private final AtomicReference<State> _state = new AtomicReference();

    protected WriteFlusher(EndPoint endPoint) {
        this._state.set(__IDLE);
        this._endPoint = endPoint;
    }

    private boolean updateState(State previous, State next) {
        if (!this.isTransitionAllowed(previous, next)) {
            throw new IllegalStateException();
        }
        boolean updated = this._state.compareAndSet(previous, next);
        if (DEBUG) {
            LOG.debug("update {}:{}{}{}", this, previous, updated ? "-->" : "!->", next);
        }
        return updated;
    }

    private void fail(PendingState pending) {
        FailedState failed;
        State current = this._state.get();
        if (current.getType() == StateType.FAILED && this.updateState(failed = (FailedState)current, __IDLE)) {
            pending.fail(failed.getCause());
            return;
        }
        throw new IllegalStateException();
    }

    private void ignoreFail() {
        State current = this._state.get();
        while (current.getType() == StateType.FAILED) {
            if (this.updateState(current, __IDLE)) {
                return;
            }
            current = this._state.get();
        }
    }

    private boolean isTransitionAllowed(State currentState, State newState) {
        Set<StateType> allowedNewStateTypes = __stateTransitions.get((Object)currentState.getType());
        if (!allowedNewStateTypes.contains((Object)newState.getType())) {
            LOG.warn("{}: {} -> {} not allowed", this, currentState, newState);
            return false;
        }
        return true;
    }

    public Invocable.InvocationType getCallbackInvocationType() {
        State s = this._state.get();
        return s instanceof PendingState ? ((PendingState)s).getCallbackInvocationType() : Invocable.InvocationType.BLOCKING;
    }

    protected abstract void onIncompleteFlush();

    public void write(Callback callback, ByteBuffer ... buffers) throws WritePendingException {
        if (DEBUG) {
            LOG.debug("write: {} {}", this, BufferUtil.toDetailString(buffers));
        }
        if (!this.updateState(__IDLE, __WRITING)) {
            throw new WritePendingException();
        }
        try {
            buffers = this.flush(buffers);
            if (buffers != null) {
                PendingState pending;
                if (DEBUG) {
                    LOG.debug("flushed incomplete", new Object[0]);
                }
                if (this.updateState(__WRITING, pending = new PendingState(buffers, callback))) {
                    this.onIncompleteFlush();
                } else {
                    this.fail(pending);
                }
                return;
            }
            if (!this.updateState(__WRITING, __IDLE)) {
                this.ignoreFail();
            }
            if (callback != null) {
                callback.succeeded();
            }
        }
        catch (IOException e) {
            if (DEBUG) {
                LOG.debug("write exception", e);
            }
            if (this.updateState(__WRITING, __IDLE)) {
                if (callback != null) {
                    callback.failed(e);
                }
            }
            this.fail(new PendingState(buffers, callback));
        }
    }

    public void completeWrite() {
        State previous;
        if (DEBUG) {
            LOG.debug("completeWrite: {}", this);
        }
        if ((previous = this._state.get()).getType() != StateType.PENDING) {
            return;
        }
        PendingState pending = (PendingState)previous;
        if (!this.updateState(pending, __COMPLETING)) {
            return;
        }
        try {
            ByteBuffer[] buffers = pending.getBuffers();
            buffers = this.flush(buffers);
            if (buffers != null) {
                if (DEBUG) {
                    LOG.debug("flushed incomplete {}", BufferUtil.toDetailString(buffers));
                }
                if (buffers != pending.getBuffers()) {
                    pending = new PendingState(buffers, pending._callback);
                }
                if (this.updateState(__COMPLETING, pending)) {
                    this.onIncompleteFlush();
                } else {
                    this.fail(pending);
                }
                return;
            }
            if (!this.updateState(__COMPLETING, __IDLE)) {
                this.ignoreFail();
            }
            pending.complete();
        }
        catch (IOException e) {
            if (DEBUG) {
                LOG.debug("completeWrite exception", e);
            }
            if (this.updateState(__COMPLETING, __IDLE)) {
                pending.fail(e);
            }
            this.fail(pending);
        }
    }

    protected ByteBuffer[] flush(ByteBuffer[] buffers) throws IOException {
        boolean progress = true;
        while (progress && buffers != null) {
            int r;
            int before = buffers.length == 0 ? 0 : buffers[0].remaining();
            boolean flushed = this._endPoint.flush(buffers);
            int n = r = buffers.length == 0 ? 0 : buffers[0].remaining();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Flushed={} {}/{}+{} {}", flushed, before - r, before, buffers.length - 1, this);
            }
            if (flushed) {
                return null;
            }
            progress = before != r;
            int not_empty = 0;
            while (r == 0) {
                if (++not_empty == buffers.length) {
                    buffers = null;
                    not_empty = 0;
                    break;
                }
                progress = true;
                r = buffers[not_empty].remaining();
            }
            if (not_empty <= 0) continue;
            buffers = Arrays.copyOfRange(buffers, not_empty, buffers.length);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("!fully flushed {}", this);
        }
        return buffers == null ? EMPTY_BUFFERS : buffers;
    }

    public boolean onFail(Throwable cause) {
        block4: while (true) {
            State current = this._state.get();
            switch (current.getType()) {
                case IDLE: 
                case FAILED: {
                    if (DEBUG) {
                        LOG.debug("ignored: {} {}", this, cause);
                    }
                    return false;
                }
                case PENDING: {
                    PendingState pending;
                    if (DEBUG) {
                        LOG.debug("failed: {} {}", this, cause);
                    }
                    if (!this.updateState(pending = (PendingState)current, __IDLE)) continue block4;
                    return pending.fail(cause);
                }
            }
            if (DEBUG) {
                LOG.debug("failed: {} {}", this, cause);
            }
            if (this.updateState(current, new FailedState(cause))) break;
        }
        return false;
    }

    public void onClose() {
        this.onFail(new ClosedChannelException());
    }

    boolean isIdle() {
        return this._state.get().getType() == StateType.IDLE;
    }

    public boolean isInProgress() {
        switch (this._state.get().getType()) {
            case PENDING: 
            case WRITING: 
            case COMPLETING: {
                return true;
            }
        }
        return false;
    }

    public String toString() {
        State s = this._state.get();
        return String.format("WriteFlusher@%x{%s}->%s", this.hashCode(), s, s instanceof PendingState ? ((PendingState)s).getCallback() : null);
    }

    public String toStateString() {
        switch (this._state.get().getType()) {
            case WRITING: {
                return "W";
            }
            case PENDING: {
                return "P";
            }
            case COMPLETING: {
                return "C";
            }
            case IDLE: {
                return "-";
            }
            case FAILED: {
                return "F";
            }
        }
        return "?";
    }

    static {
        __stateTransitions.put(StateType.IDLE, EnumSet.of(StateType.WRITING));
        __stateTransitions.put(StateType.WRITING, EnumSet.of(StateType.IDLE, StateType.PENDING, StateType.FAILED));
        __stateTransitions.put(StateType.PENDING, EnumSet.of(StateType.COMPLETING, StateType.IDLE));
        __stateTransitions.put(StateType.COMPLETING, EnumSet.of(StateType.IDLE, StateType.PENDING, StateType.FAILED));
        __stateTransitions.put(StateType.FAILED, EnumSet.of(StateType.IDLE));
    }

    private class PendingState
    extends State {
        private final Callback _callback;
        private final ByteBuffer[] _buffers;

        private PendingState(ByteBuffer[] buffers, Callback callback) {
            super(StateType.PENDING);
            this._buffers = buffers;
            this._callback = callback;
        }

        public ByteBuffer[] getBuffers() {
            return this._buffers;
        }

        protected boolean fail(Throwable cause) {
            if (this._callback != null) {
                this._callback.failed(cause);
                return true;
            }
            return false;
        }

        protected void complete() {
            if (this._callback != null) {
                this._callback.succeeded();
            }
        }

        Invocable.InvocationType getCallbackInvocationType() {
            return Invocable.getInvocationType(this._callback);
        }

        public Object getCallback() {
            return this._callback;
        }
    }

    private static class CompletingState
    extends State {
        private CompletingState() {
            super(StateType.COMPLETING);
        }
    }

    private static class FailedState
    extends State {
        private final Throwable _cause;

        private FailedState(Throwable cause) {
            super(StateType.FAILED);
            this._cause = cause;
        }

        public Throwable getCause() {
            return this._cause;
        }
    }

    private static class WritingState
    extends State {
        private WritingState() {
            super(StateType.WRITING);
        }
    }

    private static class IdleState
    extends State {
        private IdleState() {
            super(StateType.IDLE);
        }
    }

    private static class State {
        private final StateType _type;

        private State(StateType stateType) {
            this._type = stateType;
        }

        public StateType getType() {
            return this._type;
        }

        public String toString() {
            return String.format("%s", new Object[]{this._type});
        }
    }

    private static enum StateType {
        IDLE,
        WRITING,
        PENDING,
        COMPLETING,
        FAILED;

    }
}

