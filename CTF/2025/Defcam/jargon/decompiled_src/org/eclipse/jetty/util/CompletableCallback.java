/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util;

import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.jetty.util.Callback;

public abstract class CompletableCallback
implements Callback {
    private final AtomicReference<State> state = new AtomicReference<State>(State.IDLE);

    /*
     * Unable to fully structure code
     */
    @Override
    public void succeeded() {
        block5: while (true) lbl-1000:
        // 3 sources

        {
            current = this.state.get();
            switch (1.$SwitchMap$org$eclipse$jetty$util$CompletableCallback$State[current.ordinal()]) {
                case 1: {
                    if (!this.state.compareAndSet(current, State.SUCCEEDED)) ** GOTO lbl-1000
                    return;
                }
                case 2: {
                    if (!this.state.compareAndSet(current, State.SUCCEEDED)) continue block5;
                    this.resume();
                    return;
                }
                case 3: {
                    return;
                }
            }
            break;
        }
        throw new IllegalStateException(current.toString());
    }

    @Override
    public void failed(Throwable x) {
        State current;
        block4: while (true) {
            current = this.state.get();
            switch (current) {
                case IDLE: 
                case COMPLETED: {
                    if (!this.state.compareAndSet(current, State.FAILED)) continue block4;
                    this.abort(x);
                    return;
                }
                case FAILED: {
                    return;
                }
            }
            break;
        }
        throw new IllegalStateException(current.toString());
    }

    public abstract void resume();

    public abstract void abort(Throwable var1);

    public boolean tryComplete() {
        State current;
        block4: while (true) {
            current = this.state.get();
            switch (current) {
                case IDLE: {
                    if (!this.state.compareAndSet(current, State.COMPLETED)) continue block4;
                    return true;
                }
                case FAILED: 
                case SUCCEEDED: {
                    return false;
                }
            }
            break;
        }
        throw new IllegalStateException(current.toString());
    }

    private static enum State {
        IDLE,
        SUCCEEDED,
        FAILED,
        COMPLETED;

    }
}

