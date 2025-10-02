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

public class ExecuteProduceConsume
implements ExecutionStrategy,
Runnable {
    private static final Logger LOG = Log.getLogger(ExecuteProduceConsume.class);
    private final Locker _locker = new Locker();
    private final Runnable _runProduce = new RunProduce();
    private final ExecutionStrategy.Producer _producer;
    private final Invocable.InvocableExecutor _executor;
    private boolean _idle = true;
    private boolean _execute;
    private boolean _producing;
    private boolean _pending;

    public ExecuteProduceConsume(ExecutionStrategy.Producer producer, Executor executor) {
        this(producer, executor, Invocable.InvocationType.BLOCKING);
    }

    public ExecuteProduceConsume(ExecutionStrategy.Producer producer, Executor executor, Invocable.InvocationType preferred) {
        this._producer = producer;
        this._executor = new Invocable.InvocableExecutor(executor, preferred);
    }

    @Override
    public void produce() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("{} execute", this);
        }
        boolean produce = false;
        try (Locker.Lock locked = this._locker.lock();){
            if (this._idle) {
                if (this._producing) {
                    throw new IllegalStateException();
                }
                this._producing = true;
                produce = true;
                this._idle = false;
            } else {
                this._execute = true;
            }
        }
        if (produce) {
            this.produceConsume();
        }
    }

    @Override
    public void dispatch() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("{} spawning", this);
        }
        boolean dispatch = false;
        try (Locker.Lock locked = this._locker.lock();){
            if (this._idle) {
                dispatch = true;
            } else {
                this._execute = true;
            }
        }
        if (dispatch) {
            this._executor.execute(this._runProduce);
        }
    }

    @Override
    public void run() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("{} run", this);
        }
        boolean produce = false;
        try (Locker.Lock locked = this._locker.lock();){
            this._pending = false;
            if (!this._idle && !this._producing) {
                this._producing = true;
                produce = true;
            }
        }
        if (produce) {
            this.produceConsume();
        }
    }

    private void produceConsume() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("{} produce enter", this);
        }
        while (true) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("{} producing", this);
            }
            Runnable task = this._producer.produce();
            if (LOG.isDebugEnabled()) {
                LOG.debug("{} produced {}", this, task);
            }
            boolean dispatch = false;
            Locker.Lock locked = this._locker.lock();
            Throwable throwable = null;
            try {
                this._producing = false;
                if (task == null) {
                    if (this._execute) {
                        this._idle = false;
                        this._producing = true;
                        this._execute = false;
                        continue;
                    }
                    this._idle = true;
                    break;
                }
                if (!this._pending) {
                    this._pending = Invocable.getInvocationType(task) != Invocable.InvocationType.NON_BLOCKING;
                    dispatch = this._pending;
                }
                this._execute = false;
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
            if (dispatch) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("{} dispatch", this);
                }
                if (!this._executor.tryExecute(this)) {
                    task = null;
                }
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("{} run {}", this, task);
            }
            if (task != null) {
                this._executor.invoke(task);
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("{} ran {}", this, task);
            }
            locked = this._locker.lock();
            throwable = null;
            try {
                if (this._producing || this._idle) break;
                this._producing = true;
                continue;
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
                continue;
            }
            break;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("{} produce exit", this);
        }
    }

    public Boolean isIdle() {
        try (Locker.Lock locked = this._locker.lock();){
            Boolean bl = this._idle;
            return bl;
        }
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("EPC ");
        try (Locker.Lock locked = this._locker.lock();){
            builder.append(this._idle ? "Idle/" : "");
            builder.append(this._producing ? "Prod/" : "");
            builder.append(this._pending ? "Pend/" : "");
            builder.append(this._execute ? "Exec/" : "");
        }
        builder.append(this._producer);
        return builder.toString();
    }

    private class RunProduce
    implements Runnable {
        private RunProduce() {
        }

        @Override
        public void run() {
            ExecuteProduceConsume.this.produce();
        }
    }
}

