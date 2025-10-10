/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.annotation.ManagedOperation;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.component.Dumpable;
import org.eclipse.jetty.util.statistic.CounterStatistic;
import org.eclipse.jetty.util.statistic.SampleStatistic;

@ManagedObject(value="Tracks statistics on connections")
public class ConnectionStatistics
extends AbstractLifeCycle
implements Connection.Listener,
Dumpable {
    private final CounterStatistic _connections = new CounterStatistic();
    private final SampleStatistic _connectionsDuration = new SampleStatistic();
    private final LongAdder _rcvdBytes = new LongAdder();
    private final AtomicLong _bytesInStamp = new AtomicLong();
    private final LongAdder _sentBytes = new LongAdder();
    private final AtomicLong _bytesOutStamp = new AtomicLong();
    private final LongAdder _messagesIn = new LongAdder();
    private final AtomicLong _messagesInStamp = new AtomicLong();
    private final LongAdder _messagesOut = new LongAdder();
    private final AtomicLong _messagesOutStamp = new AtomicLong();

    @ManagedOperation(value="Resets the statistics", impact="ACTION")
    public void reset() {
        this._connections.reset();
        this._connectionsDuration.reset();
        this._rcvdBytes.reset();
        this._bytesInStamp.set(System.nanoTime());
        this._sentBytes.reset();
        this._bytesOutStamp.set(System.nanoTime());
        this._messagesIn.reset();
        this._messagesInStamp.set(System.nanoTime());
        this._messagesOut.reset();
        this._messagesOutStamp.set(System.nanoTime());
    }

    @Override
    protected void doStart() throws Exception {
        this.reset();
    }

    @Override
    public void onOpened(Connection connection) {
        if (!this.isStarted()) {
            return;
        }
        this._connections.increment();
    }

    @Override
    public void onClosed(Connection connection) {
        long messagesOut;
        long messagesIn;
        long bytesOut;
        if (!this.isStarted()) {
            return;
        }
        this._connections.decrement();
        long elapsed = System.currentTimeMillis() - connection.getCreatedTimeStamp();
        this._connectionsDuration.set(elapsed);
        long bytesIn = connection.getBytesIn();
        if (bytesIn > 0L) {
            this._rcvdBytes.add(bytesIn);
        }
        if ((bytesOut = connection.getBytesOut()) > 0L) {
            this._sentBytes.add(bytesOut);
        }
        if ((messagesIn = connection.getMessagesIn()) > 0L) {
            this._messagesIn.add(messagesIn);
        }
        if ((messagesOut = connection.getMessagesOut()) > 0L) {
            this._messagesOut.add(messagesOut);
        }
    }

    @ManagedAttribute(value="Total number of bytes received by tracked connections")
    public long getReceivedBytes() {
        return this._rcvdBytes.sum();
    }

    @ManagedAttribute(value="Total number of bytes received per second since the last invocation of this method")
    public long getReceivedBytesRate() {
        long then;
        long now = System.nanoTime();
        long elapsed = TimeUnit.NANOSECONDS.toMillis(now - (then = this._bytesInStamp.getAndSet(now)));
        return elapsed == 0L ? 0L : this.getReceivedBytes() * 1000L / elapsed;
    }

    @ManagedAttribute(value="Total number of bytes sent by tracked connections")
    public long getSentBytes() {
        return this._sentBytes.sum();
    }

    @ManagedAttribute(value="Total number of bytes sent per second since the last invocation of this method")
    public long getSentBytesRate() {
        long then;
        long now = System.nanoTime();
        long elapsed = TimeUnit.NANOSECONDS.toMillis(now - (then = this._bytesOutStamp.getAndSet(now)));
        return elapsed == 0L ? 0L : this.getSentBytes() * 1000L / elapsed;
    }

    @ManagedAttribute(value="The max duration of a connection in ms")
    public long getConnectionDurationMax() {
        return this._connectionsDuration.getMax();
    }

    @ManagedAttribute(value="The mean duration of a connection in ms")
    public double getConnectionDurationMean() {
        return this._connectionsDuration.getMean();
    }

    @ManagedAttribute(value="The standard deviation of the duration of a connection")
    public double getConnectionDurationStdDev() {
        return this._connectionsDuration.getStdDev();
    }

    @ManagedAttribute(value="The total number of connections opened")
    public long getConnectionsTotal() {
        return this._connections.getTotal();
    }

    @ManagedAttribute(value="The current number of open connections")
    public long getConnections() {
        return this._connections.getCurrent();
    }

    @ManagedAttribute(value="The max number of open connections")
    public long getConnectionsMax() {
        return this._connections.getMax();
    }

    @ManagedAttribute(value="The total number of messages received")
    public long getReceivedMessages() {
        return this._messagesIn.sum();
    }

    @ManagedAttribute(value="Total number of messages received per second since the last invocation of this method")
    public long getReceivedMessagesRate() {
        long then;
        long now = System.nanoTime();
        long elapsed = TimeUnit.NANOSECONDS.toMillis(now - (then = this._messagesInStamp.getAndSet(now)));
        return elapsed == 0L ? 0L : this.getReceivedMessages() * 1000L / elapsed;
    }

    @ManagedAttribute(value="The total number of messages sent")
    public long getSentMessages() {
        return this._messagesOut.sum();
    }

    @ManagedAttribute(value="Total number of messages sent per second since the last invocation of this method")
    public long getSentMessagesRate() {
        long then;
        long now = System.nanoTime();
        long elapsed = TimeUnit.NANOSECONDS.toMillis(now - (then = this._messagesOutStamp.getAndSet(now)));
        return elapsed == 0L ? 0L : this.getSentMessages() * 1000L / elapsed;
    }

    @Override
    public String dump() {
        return ContainerLifeCycle.dump(this);
    }

    @Override
    public void dump(Appendable out, String indent) throws IOException {
        ContainerLifeCycle.dumpObject(out, this);
        ArrayList<String> children = new ArrayList<String>();
        children.add(String.format("connections=%s", this._connections));
        children.add(String.format("durations=%s", this._connectionsDuration));
        children.add(String.format("bytes in/out=%s/%s", this.getReceivedBytes(), this.getSentBytes()));
        children.add(String.format("messages in/out=%s/%s", this.getReceivedMessages(), this.getSentMessages()));
        ContainerLifeCycle.dump(out, indent, children);
    }

    public String toString() {
        return String.format("%s@%x", this.getClass().getSimpleName(), this.hashCode());
    }
}

