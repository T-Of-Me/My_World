/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.thread;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.annotation.ManagedOperation;
import org.eclipse.jetty.util.annotation.Name;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.component.Dumpable;
import org.eclipse.jetty.util.component.DumpableCollection;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.thread.ThreadPool;

@ManagedObject(value="A thread pool")
public class QueuedThreadPool
extends AbstractLifeCycle
implements ThreadPool.SizedThreadPool,
Dumpable {
    private static final Logger LOG = Log.getLogger(QueuedThreadPool.class);
    private final AtomicInteger _threadsStarted = new AtomicInteger();
    private final AtomicInteger _threadsIdle = new AtomicInteger();
    private final AtomicLong _lastShrink = new AtomicLong();
    private final Set<Thread> _threads = ConcurrentHashMap.newKeySet();
    private final Object _joinLock = new Object();
    private final BlockingQueue<Runnable> _jobs;
    private final ThreadGroup _threadGroup;
    private String _name = "qtp" + this.hashCode();
    private int _idleTimeout;
    private int _maxThreads;
    private int _minThreads;
    private int _priority = 5;
    private boolean _daemon = false;
    private boolean _detailedDump = false;
    private int _lowThreadsThreshold = 1;
    private Runnable _runnable = new Runnable(){

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         * Loose catch block
         */
        @Override
        public void run() {
            boolean shrink = false;
            boolean ignore = false;
            try {
                Runnable job = (Runnable)QueuedThreadPool.this._jobs.poll();
                if (job != null && QueuedThreadPool.this._threadsIdle.get() == 0) {
                    QueuedThreadPool.this.startThreads(1);
                }
                block10: while (QueuedThreadPool.this.isRunning()) {
                    while (job != null && QueuedThreadPool.this.isRunning()) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("run {}", job);
                        }
                        QueuedThreadPool.this.runJob(job);
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("ran {}", job);
                        }
                        if (Thread.interrupted()) {
                            ignore = true;
                            break block10;
                        }
                        job = (Runnable)QueuedThreadPool.this._jobs.poll();
                    }
                    try {
                        QueuedThreadPool.this._threadsIdle.incrementAndGet();
                        while (QueuedThreadPool.this.isRunning() && job == null) {
                            if (QueuedThreadPool.this._idleTimeout <= 0) {
                                job = (Runnable)QueuedThreadPool.this._jobs.take();
                                continue;
                            }
                            int size = QueuedThreadPool.this._threadsStarted.get();
                            if (size > QueuedThreadPool.this._minThreads) {
                                long last = QueuedThreadPool.this._lastShrink.get();
                                long now = System.nanoTime();
                                if ((last == 0L || now - last > TimeUnit.MILLISECONDS.toNanos(QueuedThreadPool.this._idleTimeout)) && QueuedThreadPool.this._lastShrink.compareAndSet(last, now) && QueuedThreadPool.this._threadsStarted.compareAndSet(size, size - 1)) {
                                    shrink = true;
                                    break block10;
                                }
                            }
                            job = QueuedThreadPool.this.idleJobPoll();
                        }
                    }
                    finally {
                        if (QueuedThreadPool.this._threadsIdle.decrementAndGet() != 0) continue;
                        QueuedThreadPool.this.startThreads(1);
                    }
                }
            }
            catch (InterruptedException e) {
                ignore = true;
                LOG.ignore(e);
                if (!shrink && QueuedThreadPool.this.isRunning()) {
                    if (!ignore) {
                        LOG.warn("Unexpected thread death: {} in {}", this, QueuedThreadPool.this);
                    }
                    if (QueuedThreadPool.this._threadsStarted.decrementAndGet() < QueuedThreadPool.this.getMaxThreads()) {
                        QueuedThreadPool.this.startThreads(1);
                    }
                }
                QueuedThreadPool.this._threads.remove(Thread.currentThread());
            }
            catch (Throwable e2) {
                LOG.warn(e2);
                {
                    catch (Throwable throwable) {
                        if (!shrink && QueuedThreadPool.this.isRunning()) {
                            if (!ignore) {
                                LOG.warn("Unexpected thread death: {} in {}", this, QueuedThreadPool.this);
                            }
                            if (QueuedThreadPool.this._threadsStarted.decrementAndGet() < QueuedThreadPool.this.getMaxThreads()) {
                                QueuedThreadPool.this.startThreads(1);
                            }
                        }
                        QueuedThreadPool.this._threads.remove(Thread.currentThread());
                        throw throwable;
                    }
                }
                if (!shrink && QueuedThreadPool.this.isRunning()) {
                    if (!ignore) {
                        LOG.warn("Unexpected thread death: {} in {}", this, QueuedThreadPool.this);
                    }
                    if (QueuedThreadPool.this._threadsStarted.decrementAndGet() < QueuedThreadPool.this.getMaxThreads()) {
                        QueuedThreadPool.this.startThreads(1);
                    }
                }
                QueuedThreadPool.this._threads.remove(Thread.currentThread());
            }
            if (!shrink && QueuedThreadPool.this.isRunning()) {
                if (!ignore) {
                    LOG.warn("Unexpected thread death: {} in {}", this, QueuedThreadPool.this);
                }
                if (QueuedThreadPool.this._threadsStarted.decrementAndGet() < QueuedThreadPool.this.getMaxThreads()) {
                    QueuedThreadPool.this.startThreads(1);
                }
            }
            QueuedThreadPool.this._threads.remove(Thread.currentThread());
        }
    };

    public QueuedThreadPool() {
        this(200);
    }

    public QueuedThreadPool(@Name(value="maxThreads") int maxThreads) {
        this(maxThreads, 8);
    }

    public QueuedThreadPool(@Name(value="maxThreads") int maxThreads, @Name(value="minThreads") int minThreads) {
        this(maxThreads, minThreads, 60000);
    }

    public QueuedThreadPool(@Name(value="maxThreads") int maxThreads, @Name(value="minThreads") int minThreads, @Name(value="idleTimeout") int idleTimeout) {
        this(maxThreads, minThreads, idleTimeout, null);
    }

    public QueuedThreadPool(@Name(value="maxThreads") int maxThreads, @Name(value="minThreads") int minThreads, @Name(value="idleTimeout") int idleTimeout, @Name(value="queue") BlockingQueue<Runnable> queue) {
        this(maxThreads, minThreads, idleTimeout, queue, null);
    }

    public QueuedThreadPool(@Name(value="maxThreads") int maxThreads, @Name(value="minThreads") int minThreads, @Name(value="idleTimeout") int idleTimeout, @Name(value="queue") BlockingQueue<Runnable> queue, @Name(value="threadGroup") ThreadGroup threadGroup) {
        this.setMinThreads(minThreads);
        this.setMaxThreads(maxThreads);
        this.setIdleTimeout(idleTimeout);
        this.setStopTimeout(5000L);
        if (queue == null) {
            int capacity = Math.max(this._minThreads, 8);
            queue = new BlockingArrayQueue<Runnable>(capacity, capacity);
        }
        this._jobs = queue;
        this._threadGroup = threadGroup;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        this._threadsStarted.set(0);
        this.startThreads(this._minThreads);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    protected void doStop() throws Exception {
        long canwait;
        super.doStop();
        long timeout = this.getStopTimeout();
        BlockingQueue<Runnable> jobs = this.getQueue();
        if (timeout <= 0L) {
            jobs.clear();
        }
        Runnable noop = () -> {};
        int i = this._threadsStarted.get();
        while (i-- > 0) {
            jobs.offer(noop);
        }
        long stopby = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeout) / 2L;
        for (Thread thread : this._threads) {
            canwait = TimeUnit.NANOSECONDS.toMillis(stopby - System.nanoTime());
            if (canwait <= 0L) continue;
            thread.join(canwait);
        }
        if (this._threadsStarted.get() > 0) {
            for (Thread thread : this._threads) {
                thread.interrupt();
            }
        }
        stopby = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeout) / 2L;
        for (Thread thread : this._threads) {
            canwait = TimeUnit.NANOSECONDS.toMillis(stopby - System.nanoTime());
            if (canwait <= 0L) continue;
            thread.join(canwait);
        }
        Thread.yield();
        int size = this._threads.size();
        if (size > 0) {
            Thread.yield();
            if (LOG.isDebugEnabled()) {
                for (Thread unstopped : this._threads) {
                    StringBuilder dmp = new StringBuilder();
                    for (StackTraceElement element : unstopped.getStackTrace()) {
                        dmp.append(System.lineSeparator()).append("\tat ").append(element);
                    }
                    LOG.warn("Couldn't stop {}{}", unstopped, dmp.toString());
                }
            } else {
                for (Thread unstopped : this._threads) {
                    LOG.warn("{} Couldn't stop {}", this, unstopped);
                }
            }
        }
        Iterator<Thread> iterator = this._joinLock;
        synchronized (iterator) {
            this._joinLock.notifyAll();
        }
    }

    public void setDaemon(boolean daemon) {
        this._daemon = daemon;
    }

    public void setIdleTimeout(int idleTimeout) {
        this._idleTimeout = idleTimeout;
    }

    @Override
    public void setMaxThreads(int maxThreads) {
        this._maxThreads = maxThreads;
        if (this._minThreads > this._maxThreads) {
            this._minThreads = this._maxThreads;
        }
    }

    @Override
    public void setMinThreads(int minThreads) {
        this._minThreads = minThreads;
        if (this._minThreads > this._maxThreads) {
            this._maxThreads = this._minThreads;
        }
        int threads = this._threadsStarted.get();
        if (this.isStarted() && threads < this._minThreads) {
            this.startThreads(this._minThreads - threads);
        }
    }

    public void setName(String name) {
        if (this.isRunning()) {
            throw new IllegalStateException("started");
        }
        this._name = name;
    }

    public void setThreadsPriority(int priority) {
        this._priority = priority;
    }

    @ManagedAttribute(value="maximum time a thread may be idle in ms")
    public int getIdleTimeout() {
        return this._idleTimeout;
    }

    @Override
    @ManagedAttribute(value="maximum number of threads in the pool")
    public int getMaxThreads() {
        return this._maxThreads;
    }

    @Override
    @ManagedAttribute(value="minimum number of threads in the pool")
    public int getMinThreads() {
        return this._minThreads;
    }

    @ManagedAttribute(value="name of the thread pool")
    public String getName() {
        return this._name;
    }

    @ManagedAttribute(value="priority of threads in the pool")
    public int getThreadsPriority() {
        return this._priority;
    }

    @ManagedAttribute(value="size of the job queue")
    public int getQueueSize() {
        return this._jobs.size();
    }

    @ManagedAttribute(value="thread pool uses daemon threads")
    public boolean isDaemon() {
        return this._daemon;
    }

    @ManagedAttribute(value="reports additional details in the dump")
    public boolean isDetailedDump() {
        return this._detailedDump;
    }

    public void setDetailedDump(boolean detailedDump) {
        this._detailedDump = detailedDump;
    }

    @ManagedAttribute(value="threshold at which the pool is low on threads")
    public int getLowThreadsThreshold() {
        return this._lowThreadsThreshold;
    }

    public void setLowThreadsThreshold(int lowThreadsThreshold) {
        this._lowThreadsThreshold = lowThreadsThreshold;
    }

    @Override
    public void execute(Runnable job) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("queue {}", job);
        }
        if (!this.isRunning() || !this._jobs.offer(job)) {
            LOG.warn("{} rejected {}", this, job);
            throw new RejectedExecutionException(job.toString());
        }
        if (this.getThreads() == 0) {
            this.startThreads(1);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void join() throws InterruptedException {
        Object object = this._joinLock;
        synchronized (object) {
            while (this.isRunning()) {
                this._joinLock.wait();
            }
        }
        while (this.isStopping()) {
            Thread.sleep(1L);
        }
    }

    @Override
    @ManagedAttribute(value="number of threads in the pool")
    public int getThreads() {
        return this._threadsStarted.get();
    }

    @Override
    @ManagedAttribute(value="number of idle threads in the pool")
    public int getIdleThreads() {
        return this._threadsIdle.get();
    }

    @ManagedAttribute(value="number of busy threads in the pool")
    public int getBusyThreads() {
        return this.getThreads() - this.getIdleThreads();
    }

    @Override
    @ManagedAttribute(value="thread pool is low on threads", readonly=true)
    public boolean isLowOnThreads() {
        return this.getMaxThreads() - this.getThreads() + this.getIdleThreads() - this.getQueueSize() <= this.getLowThreadsThreshold();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private boolean startThreads(int threadsToStart) {
        while (threadsToStart > 0 && this.isRunning()) {
            int threads = this._threadsStarted.get();
            if (threads >= this._maxThreads) {
                return false;
            }
            if (!this._threadsStarted.compareAndSet(threads, threads + 1)) continue;
            boolean started = false;
            try {
                Thread thread = this.newThread(this._runnable);
                thread.setDaemon(this.isDaemon());
                thread.setPriority(this.getThreadsPriority());
                thread.setName(this._name + "-" + thread.getId());
                this._threads.add(thread);
                thread.start();
                started = true;
                --threadsToStart;
            }
            finally {
                if (started) continue;
                this._threadsStarted.decrementAndGet();
            }
        }
        return true;
    }

    protected Thread newThread(Runnable runnable) {
        return new Thread(this._threadGroup, runnable);
    }

    @Override
    @ManagedOperation(value="dumps thread pool state")
    public String dump() {
        return ContainerLifeCycle.dump(this);
    }

    @Override
    public void dump(Appendable out, String indent) throws IOException {
        ArrayList<Object> threads = new ArrayList<Object>(this.getMaxThreads());
        for (final Thread thread : this._threads) {
            final StackTraceElement[] trace = thread.getStackTrace();
            boolean inIdleJobPoll = false;
            for (StackTraceElement t : trace) {
                if (!"idleJobPoll".equals(t.getMethodName())) continue;
                inIdleJobPoll = true;
                break;
            }
            final boolean idle = inIdleJobPoll;
            if (this.isDetailedDump()) {
                threads.add(new Dumpable(){

                    @Override
                    public void dump(Appendable out, String indent) throws IOException {
                        out.append(String.valueOf(thread.getId())).append(' ').append(thread.getName()).append(' ').append(thread.getState().toString()).append(idle ? " IDLE" : "");
                        if (thread.getPriority() != 5) {
                            out.append(" prio=").append(String.valueOf(thread.getPriority()));
                        }
                        out.append(System.lineSeparator());
                        if (!idle) {
                            ContainerLifeCycle.dump(out, indent, Arrays.asList(trace));
                        }
                    }

                    @Override
                    public String dump() {
                        return null;
                    }
                });
                continue;
            }
            int p = thread.getPriority();
            threads.add(thread.getId() + " " + thread.getName() + " " + (Object)((Object)thread.getState()) + " @ " + (trace.length > 0 ? trace[0] : "???") + (idle ? " IDLE" : "") + (p == 5 ? "" : " prio=" + p));
        }
        List jobs = Collections.emptyList();
        if (this.isDetailedDump()) {
            jobs = new ArrayList<Runnable>(this.getQueue());
        }
        ContainerLifeCycle.dumpObject(out, this);
        ContainerLifeCycle.dump(out, indent, threads, Collections.singletonList(new DumpableCollection("jobs", jobs)));
    }

    public String toString() {
        return String.format("%s{%s,%d<=%d<=%d,i=%d,q=%d}", this._name, this.getState(), this.getMinThreads(), this.getThreads(), this.getMaxThreads(), this.getIdleThreads(), this._jobs == null ? -1 : this._jobs.size());
    }

    private Runnable idleJobPoll() throws InterruptedException {
        return this._jobs.poll(this._idleTimeout, TimeUnit.MILLISECONDS);
    }

    protected void runJob(Runnable job) {
        job.run();
    }

    protected BlockingQueue<Runnable> getQueue() {
        return this._jobs;
    }

    @Deprecated
    public void setQueue(BlockingQueue<Runnable> queue) {
        throw new UnsupportedOperationException("Use constructor injection");
    }

    @ManagedOperation(value="interrupts a pool thread")
    public boolean interruptThread(@Name(value="id") long id) {
        for (Thread thread : this._threads) {
            if (thread.getId() != id) continue;
            thread.interrupt();
            return true;
        }
        return false;
    }

    @ManagedOperation(value="dumps a pool thread stack")
    public String dumpThread(@Name(value="id") long id) {
        for (Thread thread : this._threads) {
            if (thread.getId() != id) continue;
            StringBuilder buf = new StringBuilder();
            buf.append(thread.getId()).append(" ").append(thread.getName()).append(" ");
            buf.append((Object)thread.getState()).append(":").append(System.lineSeparator());
            for (StackTraceElement element : thread.getStackTrace()) {
                buf.append("  at ").append(element.toString()).append(System.lineSeparator());
            }
            return buf.toString();
        }
        return null;
    }
}

