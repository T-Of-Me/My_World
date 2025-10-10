/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.thread;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Locker {
    private static final Lock LOCKED = new Lock();
    private final ReentrantLock _lock = new ReentrantLock();
    private final Lock _unlock = new UnLock();

    public Lock lock() {
        if (this._lock.isHeldByCurrentThread()) {
            throw new IllegalStateException("Locker is not reentrant");
        }
        this._lock.lock();
        return this._unlock;
    }

    public Lock lockIfNotHeld() {
        if (this._lock.isHeldByCurrentThread()) {
            return LOCKED;
        }
        this._lock.lock();
        return this._unlock;
    }

    public boolean isLocked() {
        return this._lock.isLocked();
    }

    public Condition newCondition() {
        return this._lock.newCondition();
    }

    public class UnLock
    extends Lock {
        @Override
        public void close() {
            Locker.this._lock.unlock();
        }
    }

    public static class Lock
    implements AutoCloseable {
        @Override
        public void close() {
        }
    }
}

