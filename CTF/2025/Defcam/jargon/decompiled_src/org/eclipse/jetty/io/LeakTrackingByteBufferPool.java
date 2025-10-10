/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.io;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.LeakDetector;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class LeakTrackingByteBufferPool
extends ContainerLifeCycle
implements ByteBufferPool {
    private static final Logger LOG = Log.getLogger(LeakTrackingByteBufferPool.class);
    private final LeakDetector<ByteBuffer> leakDetector = new LeakDetector<ByteBuffer>(){

        @Override
        public String id(ByteBuffer resource) {
            return BufferUtil.toIDString(resource);
        }

        @Override
        protected void leaked(LeakDetector.LeakInfo leakInfo) {
            LeakTrackingByteBufferPool.this.leaked.incrementAndGet();
            LeakTrackingByteBufferPool.this.leaked(leakInfo);
        }
    };
    private static final boolean NOISY = Boolean.getBoolean(LeakTrackingByteBufferPool.class.getName() + ".NOISY");
    private final ByteBufferPool delegate;
    private final AtomicLong leakedReleases = new AtomicLong(0L);
    private final AtomicLong leakedAcquires = new AtomicLong(0L);
    private final AtomicLong leaked = new AtomicLong(0L);

    public LeakTrackingByteBufferPool(ByteBufferPool delegate) {
        this.delegate = delegate;
        this.addBean(this.leakDetector);
        this.addBean(delegate);
    }

    @Override
    public ByteBuffer acquire(int size, boolean direct) {
        ByteBuffer buffer = this.delegate.acquire(size, direct);
        boolean leaked = this.leakDetector.acquired(buffer);
        if (NOISY || !leaked) {
            this.leakedAcquires.incrementAndGet();
            LOG.info(String.format("ByteBuffer acquire %s leaked.acquired=%s", this.leakDetector.id(buffer), leaked ? "normal" : "LEAK"), new Throwable("LeakStack.Acquire"));
        }
        return buffer;
    }

    @Override
    public void release(ByteBuffer buffer) {
        if (buffer == null) {
            return;
        }
        boolean leaked = this.leakDetector.released(buffer);
        if (NOISY || !leaked) {
            this.leakedReleases.incrementAndGet();
            LOG.info(String.format("ByteBuffer release %s leaked.released=%s", this.leakDetector.id(buffer), leaked ? "normal" : "LEAK"), new Throwable("LeakStack.Release"));
        }
        this.delegate.release(buffer);
    }

    public void clearTracking() {
        this.leakedAcquires.set(0L);
        this.leakedReleases.set(0L);
    }

    public long getLeakedAcquires() {
        return this.leakedAcquires.get();
    }

    public long getLeakedReleases() {
        return this.leakedReleases.get();
    }

    public long getLeakedResources() {
        return this.leaked.get();
    }

    protected void leaked(LeakDetector.LeakInfo leakInfo) {
        LOG.warn("ByteBuffer " + leakInfo.getResourceDescription() + " leaked at:", leakInfo.getStackFrames());
    }
}

