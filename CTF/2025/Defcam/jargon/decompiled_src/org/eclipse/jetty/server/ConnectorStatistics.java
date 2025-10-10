/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.annotation.ManagedOperation;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.Container;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.component.Dumpable;
import org.eclipse.jetty.util.statistic.CounterStatistic;
import org.eclipse.jetty.util.statistic.SampleStatistic;

@Deprecated
@ManagedObject(value="Connector Statistics")
public class ConnectorStatistics
extends AbstractLifeCycle
implements Dumpable,
Connection.Listener {
    private static final Sample ZERO = new Sample();
    private final AtomicLong _startMillis = new AtomicLong(-1L);
    private final CounterStatistic _connectionStats = new CounterStatistic();
    private final SampleStatistic _messagesIn = new SampleStatistic();
    private final SampleStatistic _messagesOut = new SampleStatistic();
    private final SampleStatistic _connectionDurationStats = new SampleStatistic();
    private final ConcurrentMap<Connection, Sample> _samples = new ConcurrentHashMap<Connection, Sample>();
    private final LongAdder _closedIn = new LongAdder();
    private final LongAdder _closedOut = new LongAdder();
    private AtomicLong _nanoStamp = new AtomicLong();
    private volatile int _messagesInPerSecond;
    private volatile int _messagesOutPerSecond;
    private static final long SECOND_NANOS = TimeUnit.SECONDS.toNanos(1L);

    @Override
    public void onOpened(Connection connection) {
        if (this.isStarted()) {
            this._connectionStats.increment();
            this._samples.put(connection, ZERO);
        }
    }

    @Override
    public void onClosed(Connection connection) {
        if (this.isStarted()) {
            long msgsIn = connection.getMessagesIn();
            long msgsOut = connection.getMessagesOut();
            this._messagesIn.set(msgsIn);
            this._messagesOut.set(msgsOut);
            this._connectionStats.decrement();
            this._connectionDurationStats.set(System.currentTimeMillis() - connection.getCreatedTimeStamp());
            Sample sample = (Sample)this._samples.remove(connection);
            if (sample != null) {
                this._closedIn.add(msgsIn - sample._messagesIn);
                this._closedOut.add(msgsOut - sample._messagesOut);
            }
        }
    }

    @ManagedAttribute(value="Total number of bytes received by this connector")
    public int getBytesIn() {
        return -1;
    }

    @ManagedAttribute(value="Total number of bytes sent by this connector")
    public int getBytesOut() {
        return -1;
    }

    @ManagedAttribute(value="Total number of connections seen by this connector")
    public int getConnections() {
        return (int)this._connectionStats.getTotal();
    }

    @ManagedAttribute(value="Connection duration maximum in ms")
    public long getConnectionDurationMax() {
        return this._connectionDurationStats.getMax();
    }

    @ManagedAttribute(value="Connection duration mean in ms")
    public double getConnectionDurationMean() {
        return this._connectionDurationStats.getMean();
    }

    @ManagedAttribute(value="Connection duration standard deviation")
    public double getConnectionDurationStdDev() {
        return this._connectionDurationStats.getStdDev();
    }

    @ManagedAttribute(value="Messages In for all connections")
    public int getMessagesIn() {
        return (int)this._messagesIn.getTotal();
    }

    @ManagedAttribute(value="Messages In per connection maximum")
    public int getMessagesInPerConnectionMax() {
        return (int)this._messagesIn.getMax();
    }

    @ManagedAttribute(value="Messages In per connection mean")
    public double getMessagesInPerConnectionMean() {
        return this._messagesIn.getMean();
    }

    @ManagedAttribute(value="Messages In per connection standard deviation")
    public double getMessagesInPerConnectionStdDev() {
        return this._messagesIn.getStdDev();
    }

    @ManagedAttribute(value="Connections open")
    public int getConnectionsOpen() {
        return (int)this._connectionStats.getCurrent();
    }

    @ManagedAttribute(value="Connections open maximum")
    public int getConnectionsOpenMax() {
        return (int)this._connectionStats.getMax();
    }

    @ManagedAttribute(value="Messages Out for all connections")
    public int getMessagesOut() {
        return (int)this._messagesIn.getTotal();
    }

    @ManagedAttribute(value="Messages In per connection maximum")
    public int getMessagesOutPerConnectionMax() {
        return (int)this._messagesIn.getMax();
    }

    @ManagedAttribute(value="Messages In per connection mean")
    public double getMessagesOutPerConnectionMean() {
        return this._messagesIn.getMean();
    }

    @ManagedAttribute(value="Messages In per connection standard deviation")
    public double getMessagesOutPerConnectionStdDev() {
        return this._messagesIn.getStdDev();
    }

    @ManagedAttribute(value="Connection statistics started ms since epoch")
    public long getStartedMillis() {
        long start = this._startMillis.get();
        return start < 0L ? 0L : System.currentTimeMillis() - start;
    }

    @ManagedAttribute(value="Messages in per second calculated over period since last called")
    public int getMessagesInPerSecond() {
        this.update();
        return this._messagesInPerSecond;
    }

    @ManagedAttribute(value="Messages out per second calculated over period since last called")
    public int getMessagesOutPerSecond() {
        this.update();
        return this._messagesOutPerSecond;
    }

    @Override
    public void doStart() {
        this.reset();
    }

    @Override
    public void doStop() {
        this._samples.clear();
    }

    @ManagedOperation(value="Reset the statistics")
    public void reset() {
        this._startMillis.set(System.currentTimeMillis());
        this._messagesIn.reset();
        this._messagesOut.reset();
        this._connectionStats.reset();
        this._connectionDurationStats.reset();
        this._samples.clear();
    }

    @Override
    @ManagedOperation(value="dump thread state")
    public String dump() {
        return ContainerLifeCycle.dump(this);
    }

    @Override
    public void dump(Appendable out, String indent) throws IOException {
        ContainerLifeCycle.dumpObject(out, this);
        ContainerLifeCycle.dump(out, indent, Arrays.asList("connections=" + this._connectionStats, "duration=" + this._connectionDurationStats, "in=" + this._messagesIn, "out=" + this._messagesOut));
    }

    public static void addToAllConnectors(Server server) {
        for (Connector connector : server.getConnectors()) {
            if (!(connector instanceof Container)) continue;
            ((Container)((Object)connector)).addBean(new ConnectorStatistics());
        }
    }

    private synchronized void update() {
        long then;
        long now = System.nanoTime();
        long duration = now - (then = this._nanoStamp.get());
        if (duration > SECOND_NANOS / 2L && this._nanoStamp.compareAndSet(then, now)) {
            long msgsIn = this._closedIn.sumThenReset();
            long msgsOut = this._closedOut.sumThenReset();
            for (Map.Entry entry : this._samples.entrySet()) {
                Sample next;
                Sample sample;
                Connection connection = (Connection)entry.getKey();
                if (!this._samples.replace(connection, sample = (Sample)entry.getValue(), next = new Sample(connection))) continue;
                msgsIn += next._messagesIn - sample._messagesIn;
                msgsOut += next._messagesOut - sample._messagesOut;
            }
            this._messagesInPerSecond = (int)(msgsIn * SECOND_NANOS / duration);
            this._messagesOutPerSecond = (int)(msgsOut * SECOND_NANOS / duration);
        }
    }

    private static class Sample {
        final long _messagesIn;
        final long _messagesOut;

        Sample() {
            this._messagesIn = 0L;
            this._messagesOut = 0L;
        }

        Sample(Connection connection) {
            this._messagesIn = connection.getMessagesIn();
            this._messagesOut = connection.getMessagesOut();
        }
    }
}

