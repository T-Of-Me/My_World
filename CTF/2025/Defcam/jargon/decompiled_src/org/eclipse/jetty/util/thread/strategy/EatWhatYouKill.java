/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.thread.strategy;

import java.util.concurrent.Executor;
import java.util.concurrent.locks.Condition;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.thread.ExecutionStrategy;
import org.eclipse.jetty.util.thread.Invocable;
import org.eclipse.jetty.util.thread.Locker;

public class EatWhatYouKill
extends AbstractLifeCycle
implements ExecutionStrategy,
Runnable {
    private static final Logger LOG = Log.getLogger(EatWhatYouKill.class);
    private final Locker _locker = new Locker();
    private State _state = State.IDLE;
    private final Runnable _runProduce = new RunProduce();
    private final ExecutionStrategy.Producer _producer;
    private final Invocable.InvocableExecutor _executor;
    private int _pendingProducersMax;
    private int _pendingProducers;
    private int _pendingProducersDispatched;
    private int _pendingProducersSignalled;
    private Condition _produce = this._locker.newCondition();

    public EatWhatYouKill(ExecutionStrategy.Producer producer, Executor executor) {
        this(producer, executor, Invocable.InvocationType.NON_BLOCKING, Invocable.InvocationType.BLOCKING);
    }

    public EatWhatYouKill(ExecutionStrategy.Producer producer, Executor executor, int maxProducersPending) {
        this(producer, executor, Invocable.InvocationType.NON_BLOCKING, Invocable.InvocationType.BLOCKING);
    }

    public EatWhatYouKill(ExecutionStrategy.Producer producer, Executor executor, Invocable.InvocationType preferredInvocationPEC, Invocable.InvocationType preferredInvocationEPC) {
        this(producer, executor, preferredInvocationPEC, preferredInvocationEPC, Integer.getInteger("org.eclipse.jetty.util.thread.strategy.EatWhatYouKill.maxProducersPending", 1));
    }

    public EatWhatYouKill(ExecutionStrategy.Producer producer, Executor executor, Invocable.InvocationType preferredInvocationPEC, Invocable.InvocationType preferredInvocationEPC, int maxProducersPending) {
        this._producer = producer;
        this._pendingProducersMax = maxProducersPending;
        this._executor = new Invocable.InvocableExecutor(executor, preferredInvocationPEC, preferredInvocationEPC);
    }

    /*
     * Unable to fully structure code
     */
    @Override
    public void produce() {
        locked = this._locker.lock();
        var3_2 = null;
        try {
            switch (1.$SwitchMap$org$eclipse$jetty$util$thread$strategy$EatWhatYouKill$State[this._state.ordinal()]) {
                case 1: {
                    this._state = State.PRODUCING;
                    produce = true;
                    ** break;
lbl9:
                    // 1 sources

                    break;
                }
                case 2: {
                    this._state = State.REPRODUCING;
                    produce = false;
                    ** break;
lbl14:
                    // 1 sources

                    break;
                }
                default: {
                    produce = false;
                    break;
                }
            }
        }
        catch (Throwable var4_5) {
            var3_2 = var4_5;
            throw var4_5;
        }
        finally {
            if (locked != null) {
                if (var3_2 != null) {
                    try {
                        locked.close();
                    }
                    catch (Throwable var4_4) {
                        var3_2.addSuppressed(var4_4);
                    }
                } else {
                    locked.close();
                }
            }
        }
        if (EatWhatYouKill.LOG.isDebugEnabled()) {
            EatWhatYouKill.LOG.debug("{} execute {}", new Object[]{this, produce});
        }
        if (produce) {
            this.doProduce();
        }
    }

    /*
     * Unable to fully structure code
     */
    @Override
    public void dispatch() {
        dispatch = false;
        locked = this._locker.lock();
        var3_3 = null;
        try {
            switch (1.$SwitchMap$org$eclipse$jetty$util$thread$strategy$EatWhatYouKill$State[this._state.ordinal()]) {
                case 1: {
                    dispatch = true;
                    ** break;
lbl9:
                    // 1 sources

                    break;
                }
                case 2: {
                    this._state = State.REPRODUCING;
                    dispatch = false;
                    ** break;
lbl14:
                    // 1 sources

                    break;
                }
                default: {
                    dispatch = false;
                    break;
                }
            }
        }
        catch (Throwable var4_5) {
            var3_3 = var4_5;
            throw var4_5;
        }
        finally {
            if (locked != null) {
                if (var3_3 != null) {
                    try {
                        locked.close();
                    }
                    catch (Throwable var4_4) {
                        var3_3.addSuppressed(var4_4);
                    }
                } else {
                    locked.close();
                }
            }
        }
        if (EatWhatYouKill.LOG.isDebugEnabled()) {
            EatWhatYouKill.LOG.debug("{} dispatch {}", new Object[]{this, dispatch});
        }
        if (dispatch) {
            this._executor.execute(this._runProduce, Invocable.InvocationType.BLOCKING);
        }
    }

    @Override
    public void run() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("{} run", this);
        }
        if (!this.isRunning()) {
            return;
        }
        boolean producing = false;
        try (Locker.Lock locked = this._locker.lock();){
            --this._pendingProducersDispatched;
            ++this._pendingProducers;
            while (this.isRunning()) {
                try {
                    this._produce.await();
                    if (this._pendingProducersSignalled == 0) continue;
                    --this._pendingProducersSignalled;
                    if (this._state == State.IDLE) {
                        this._state = State.PRODUCING;
                        producing = true;
                    }
                }
                catch (InterruptedException e) {
                    LOG.debug(e);
                    --this._pendingProducers;
                }
                break;
            }
        }
        if (producing) {
            this.doProduce();
        }
    }

    private void doProduce() {
        boolean may_block_caller;
        boolean bl = may_block_caller = !Invocable.isNonBlockingInvocation();
        if (LOG.isDebugEnabled()) {
            LOG.debug("{} produce {}", this, may_block_caller ? "non-blocking" : "blocking");
        }
        while (this.isRunning()) {
            boolean execute_producer;
            boolean consume;
            boolean produce;
            Throwable throwable;
            Locker.Lock locked;
            StringBuilder state;
            Runnable task;
            block43: {
                task = this._producer.produce();
                state = null;
                locked = this._locker.lock();
                throwable = null;
                try {
                    if (LOG.isDebugEnabled()) {
                        state = new StringBuilder();
                        this.getString(state);
                        this.getState(state);
                        state.append("->");
                    }
                    if (task == null) {
                        if (this._state == State.REPRODUCING) {
                            this._state = State.PRODUCING;
                            continue;
                        }
                        this._state = State.IDLE;
                        break;
                    }
                    if (Invocable.getInvocationType(task) == Invocable.InvocationType.NON_BLOCKING) {
                        produce = true;
                        consume = true;
                        execute_producer = false;
                    } else if (may_block_caller && (this._pendingProducers > 0 || this._pendingProducersMax == 0)) {
                        produce = false;
                        consume = true;
                        execute_producer = true;
                        ++this._pendingProducersDispatched;
                        this._state = State.IDLE;
                        --this._pendingProducers;
                        ++this._pendingProducersSignalled;
                        this._produce.signal();
                    } else {
                        produce = true;
                        consume = false;
                        boolean bl2 = execute_producer = this._pendingProducersDispatched + this._pendingProducers < this._pendingProducersMax;
                        if (execute_producer) {
                            ++this._pendingProducersDispatched;
                        }
                    }
                    if (!LOG.isDebugEnabled()) break block43;
                    this.getState(state);
                }
                catch (Throwable throwable2) {
                    throwable = throwable2;
                    throw throwable2;
                }
                finally {
                    if (locked == null) continue;
                    if (throwable != null) {
                        try {
                            locked.close();
                        }
                        catch (Throwable throwable3) {
                            throwable.addSuppressed(throwable3);
                        }
                        continue;
                    }
                    locked.close();
                    continue;
                }
            }
            if (LOG.isDebugEnabled()) {
                Object[] objectArray = new Object[3];
                objectArray[0] = state;
                objectArray[1] = consume ? (execute_producer ? "EPC!" : "PC") : "PEC";
                objectArray[2] = task;
                LOG.debug("{} {} {}", objectArray);
            }
            if (execute_producer) {
                this._executor.execute(this);
            }
            if (consume) {
                this._executor.invoke(task);
            } else {
                this._executor.execute(task);
            }
            if (produce) continue;
            locked = this._locker.lock();
            throwable = null;
            try {
                if (this._state != State.IDLE) break;
                this._state = State.PRODUCING;
            }
            catch (Throwable throwable4) {
                throwable = throwable4;
                throw throwable4;
            }
            finally {
                if (locked == null) continue;
                if (throwable != null) {
                    try {
                        locked.close();
                    }
                    catch (Throwable throwable5) {
                        throwable.addSuppressed(throwable5);
                    }
                    continue;
                }
                locked.close();
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("{} produce exit", this);
        }
    }

    public Boolean isIdle() {
        try (Locker.Lock locked = this._locker.lock();){
            Boolean bl = this._state == State.IDLE;
            return bl;
        }
    }

    @Override
    protected void doStop() throws Exception {
        try (Locker.Lock locked = this._locker.lock();){
            this._pendingProducersSignalled = this._pendingProducers + this._pendingProducersDispatched;
            this._pendingProducers = 0;
            this._produce.signalAll();
        }
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        this.getString(builder);
        try (Locker.Lock locked = this._locker.lock();){
            this.getState(builder);
        }
        return builder.toString();
    }

    private void getString(StringBuilder builder) {
        builder.append(this.getClass().getSimpleName());
        builder.append('@');
        builder.append(Integer.toHexString(this.hashCode()));
        builder.append('/');
        builder.append(this._producer);
        builder.append('/');
    }

    private void getState(StringBuilder builder) {
        builder.append((Object)this._state);
        builder.append('/');
        builder.append(this._pendingProducers);
        builder.append('/');
        builder.append(this._pendingProducersMax);
    }

    private class RunProduce
    implements Runnable {
        private RunProduce() {
        }

        @Override
        public void run() {
            EatWhatYouKill.this.produce();
        }
    }

    static enum State {
        IDLE,
        PRODUCING,
        REPRODUCING;

    }
}

