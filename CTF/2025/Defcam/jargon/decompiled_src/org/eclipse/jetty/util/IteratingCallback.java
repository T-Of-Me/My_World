/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util;

import java.nio.channels.ClosedChannelException;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.thread.Locker;

public abstract class IteratingCallback
implements Callback {
    private Locker _locker = new Locker();
    private State _state;
    private boolean _iterate;

    protected IteratingCallback() {
        this._state = State.IDLE;
    }

    protected IteratingCallback(boolean needReset) {
        this._state = needReset ? State.SUCCEEDED : State.IDLE;
    }

    protected abstract Action process() throws Throwable;

    protected void onCompleteSuccess() {
    }

    protected void onCompleteFailure(Throwable cause) {
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public void iterate() {
        boolean process = false;
        try (Locker.Lock lock = this._locker.lock();){
            switch (this._state) {
                case PENDING: 
                case CALLED: {
                    break;
                }
                case IDLE: {
                    this._state = State.PROCESSING;
                    process = true;
                    break;
                }
                case PROCESSING: {
                    this._iterate = true;
                    break;
                }
                case FAILED: 
                case SUCCEEDED: {
                    break;
                }
                default: {
                    throw new IllegalStateException(this.toString());
                }
            }
        }
        if (process) {
            this.processing();
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private void processing() {
        boolean on_complete_success = false;
        block40: while (true) {
            Action action;
            try {
                action = this.process();
            }
            catch (Throwable x) {
                this.failed(x);
                break;
            }
            Locker.Lock lock = this._locker.lock();
            Throwable throwable = null;
            try {
                switch (this._state) {
                    case PROCESSING: {
                        switch (action) {
                            case IDLE: {
                                if (this._iterate) {
                                    this._iterate = false;
                                    this._state = State.PROCESSING;
                                    continue block40;
                                }
                                this._state = State.IDLE;
                                break block40;
                            }
                            case SCHEDULED: {
                                this._state = State.PENDING;
                                break block40;
                            }
                            case SUCCEEDED: {
                                this._iterate = false;
                                this._state = State.SUCCEEDED;
                                on_complete_success = true;
                                break block40;
                            }
                            default: {
                                throw new IllegalStateException(String.format("%s[action=%s]", new Object[]{this, action}));
                            }
                        }
                    }
                    case CALLED: {
                        switch (action) {
                            case SCHEDULED: {
                                this._state = State.PROCESSING;
                                continue block40;
                            }
                        }
                        throw new IllegalStateException(String.format("%s[action=%s]", new Object[]{this, action}));
                    }
                    case FAILED: 
                    case SUCCEEDED: 
                    case CLOSED: {
                        break block40;
                    }
                    default: {
                        throw new IllegalStateException(String.format("%s[action=%s]", new Object[]{this, action}));
                    }
                }
            }
            catch (Throwable throwable2) {
                throwable = throwable2;
                throw throwable2;
            }
            finally {
                if (lock == null) continue;
                if (throwable != null) {
                    try {
                        lock.close();
                    }
                    catch (Throwable throwable3) {
                        throwable.addSuppressed(throwable3);
                    }
                    continue;
                }
                lock.close();
                continue;
            }
            break;
        }
        if (on_complete_success) {
            this.onCompleteSuccess();
        }
    }

    /*
     * Unable to fully structure code
     */
    @Override
    public void succeeded() {
        process = false;
        lock = this._locker.lock();
        var3_3 = null;
        try {
            switch (1.$SwitchMap$org$eclipse$jetty$util$IteratingCallback$State[this._state.ordinal()]) {
                case 4: {
                    this._state = State.CALLED;
                    ** break;
lbl9:
                    // 1 sources

                    break;
                }
                case 1: {
                    this._state = State.PROCESSING;
                    process = true;
                    ** break;
lbl14:
                    // 1 sources

                    break;
                }
                case 5: 
                case 7: {
                    ** break;
lbl17:
                    // 1 sources

                    break;
                }
                default: {
                    throw new IllegalStateException(this.toString());
                }
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
        if (process) {
            this.processing();
        }
    }

    /*
     * Unable to fully structure code
     */
    @Override
    public void failed(Throwable x) {
        failure = false;
        lock = this._locker.lock();
        var4_4 = null;
        try {
            switch (1.$SwitchMap$org$eclipse$jetty$util$IteratingCallback$State[this._state.ordinal()]) {
                case 2: 
                case 3: 
                case 5: 
                case 6: 
                case 7: {
                    ** break;
lbl8:
                    // 1 sources

                    break;
                }
                case 1: 
                case 4: {
                    this._state = State.FAILED;
                    failure = true;
                    ** break;
lbl13:
                    // 1 sources

                    break;
                }
                default: {
                    throw new IllegalStateException(this.toString());
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
        if (failure) {
            this.onCompleteFailure(x);
        }
    }

    /*
     * Unable to fully structure code
     */
    public void close() {
        failure = false;
        lock = this._locker.lock();
        var3_3 = null;
        try {
            switch (1.$SwitchMap$org$eclipse$jetty$util$IteratingCallback$State[this._state.ordinal()]) {
                case 3: 
                case 5: 
                case 6: {
                    this._state = State.CLOSED;
                    ** break;
lbl9:
                    // 1 sources

                    break;
                }
                case 7: {
                    ** break;
lbl12:
                    // 1 sources

                    break;
                }
                default: {
                    this._state = State.CLOSED;
                    failure = true;
                    break;
                }
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
        if (failure) {
            this.onCompleteFailure(new ClosedChannelException());
        }
    }

    boolean isIdle() {
        try (Locker.Lock lock = this._locker.lock();){
            boolean bl = this._state == State.IDLE;
            return bl;
        }
    }

    public boolean isClosed() {
        try (Locker.Lock lock = this._locker.lock();){
            boolean bl = this._state == State.CLOSED;
            return bl;
        }
    }

    public boolean isFailed() {
        try (Locker.Lock lock = this._locker.lock();){
            boolean bl = this._state == State.FAILED;
            return bl;
        }
    }

    public boolean isSucceeded() {
        try (Locker.Lock lock = this._locker.lock();){
            boolean bl = this._state == State.SUCCEEDED;
            return bl;
        }
    }

    public boolean reset() {
        try (Locker.Lock lock = this._locker.lock();){
            switch (this._state) {
                case IDLE: {
                    boolean bl = true;
                    return bl;
                }
                case FAILED: 
                case SUCCEEDED: {
                    this._iterate = false;
                    this._state = State.IDLE;
                    boolean bl = true;
                    return bl;
                }
            }
            boolean bl = false;
            return bl;
        }
    }

    public String toString() {
        return String.format("%s[%s]", new Object[]{super.toString(), this._state});
    }

    protected static enum Action {
        IDLE,
        SCHEDULED,
        SUCCEEDED;

    }

    private static enum State {
        IDLE,
        PROCESSING,
        PENDING,
        CALLED,
        SUCCEEDED,
        FAILED,
        CLOSED;

    }
}

