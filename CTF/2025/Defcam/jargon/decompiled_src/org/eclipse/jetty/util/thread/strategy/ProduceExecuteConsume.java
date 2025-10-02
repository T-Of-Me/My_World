/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.thread.strategy;

import java.util.concurrent.Executor;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.thread.ExecutionStrategy;
import org.eclipse.jetty.util.thread.Invocable;
import org.eclipse.jetty.util.thread.Locker;

public class ProduceExecuteConsume
implements ExecutionStrategy {
    private static final Logger LOG = Log.getLogger(ProduceExecuteConsume.class);
    private final Locker _locker = new Locker();
    private final ExecutionStrategy.Producer _producer;
    private final Invocable.InvocableExecutor _executor;
    private State _state = State.IDLE;

    public ProduceExecuteConsume(ExecutionStrategy.Producer producer, Executor executor) {
        this(producer, executor, Invocable.InvocationType.NON_BLOCKING);
    }

    public ProduceExecuteConsume(ExecutionStrategy.Producer producer, Executor executor, Invocable.InvocationType preferred) {
        this._producer = producer;
        this._executor = new Invocable.InvocableExecutor(executor, preferred);
    }

    /*
     * Unable to fully structure code
     */
    @Override
    public void produce() {
        locked = this._locker.lock();
        var2_2 = null;
        try {
            switch (1.$SwitchMap$org$eclipse$jetty$util$thread$strategy$ProduceExecuteConsume$State[this._state.ordinal()]) {
                case 1: {
                    this._state = State.PRODUCE;
                    ** break;
lbl8:
                    // 1 sources

                    break;
                }
                case 2: 
                case 3: {
                    this._state = State.EXECUTE;
                    return;
                }
                ** default:
lbl13:
                // 1 sources

                break;
            }
        }
        catch (Throwable var3_5) {
            var2_2 = var3_5;
            throw var3_5;
        }
        finally {
            if (locked != null) {
                if (var2_2 != null) {
                    try {
                        locked.close();
                    }
                    catch (Throwable var3_4) {
                        var2_2.addSuppressed(var3_4);
                    }
                } else {
                    locked.close();
                }
            }
        }
        block34: while (true) {
            task = this._producer.produce();
            if (ProduceExecuteConsume.LOG.isDebugEnabled()) {
                ProduceExecuteConsume.LOG.debug("{} produced {}", new Object[]{this._producer, task});
            }
            if (task == null) {
                locked = this._locker.lock();
                var3_6 = null;
                try {
                    switch (1.$SwitchMap$org$eclipse$jetty$util$thread$strategy$ProduceExecuteConsume$State[this._state.ordinal()]) {
                        case 1: {
                            throw new IllegalStateException();
                        }
                        case 2: {
                            this._state = State.IDLE;
                            return;
                        }
                        case 3: {
                            this._state = State.PRODUCE;
                            continue block34;
                        }
                        ** default:
lbl47:
                        // 1 sources

                        break;
                    }
                }
                catch (Throwable var4_12) {
                    var3_6 = var4_12;
                    throw var4_12;
                }
                finally {
                    if (locked == null) continue;
                    if (var3_6 != null) {
                        try {
                            locked.close();
                        }
                        catch (Throwable var4_9) {
                            var3_6.addSuppressed(var4_9);
                        }
                        continue;
                    }
                    locked.close();
                    continue;
                }
            }
            this._executor.execute(task);
        }
    }

    @Override
    public void dispatch() {
        this.produce();
    }

    private static enum State {
        IDLE,
        PRODUCE,
        EXECUTE;

    }
}

