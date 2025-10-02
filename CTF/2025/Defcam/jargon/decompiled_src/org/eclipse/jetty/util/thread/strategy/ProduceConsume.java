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
import org.eclipse.jetty.util.thread.strategy.ExecuteProduceConsume;

public class ProduceConsume
implements ExecutionStrategy,
Runnable {
    private static final Logger LOG = Log.getLogger(ExecuteProduceConsume.class);
    private final Locker _locker = new Locker();
    private final ExecutionStrategy.Producer _producer;
    private final Executor _executor;
    private State _state = State.IDLE;

    public ProduceConsume(ExecutionStrategy.Producer producer, Executor executor) {
        this._producer = producer;
        this._executor = executor;
    }

    /*
     * Unable to fully structure code
     */
    @Override
    public void produce() {
        lock = this._locker.lock();
        var2_2 = null;
        try {
            switch (1.$SwitchMap$org$eclipse$jetty$util$thread$strategy$ProduceConsume$State[this._state.ordinal()]) {
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
            if (lock != null) {
                if (var2_2 != null) {
                    try {
                        lock.close();
                    }
                    catch (Throwable var3_4) {
                        var2_2.addSuppressed(var3_4);
                    }
                } else {
                    lock.close();
                }
            }
        }
        block34: while (true) {
            task = this._producer.produce();
            if (ProduceConsume.LOG.isDebugEnabled()) {
                ProduceConsume.LOG.debug("{} produced {}", new Object[]{this._producer, task});
            }
            if (task == null) {
                lock = this._locker.lock();
                var3_6 = null;
                try {
                    switch (1.$SwitchMap$org$eclipse$jetty$util$thread$strategy$ProduceConsume$State[this._state.ordinal()]) {
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
                    if (lock == null) continue;
                    if (var3_6 != null) {
                        try {
                            lock.close();
                        }
                        catch (Throwable var4_9) {
                            var3_6.addSuppressed(var4_9);
                        }
                        continue;
                    }
                    lock.close();
                    continue;
                }
            }
            Invocable.invokePreferNonBlocking(task);
        }
    }

    @Override
    public void dispatch() {
        this._executor.execute(this);
    }

    @Override
    public void run() {
        this.produce();
    }

    private static enum State {
        IDLE,
        PRODUCE,
        EXECUTE;

    }
}

