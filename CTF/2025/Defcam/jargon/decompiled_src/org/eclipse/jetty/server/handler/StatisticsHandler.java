/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.handler;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.AsyncContextEvent;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpChannelState;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.util.FutureCallback;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.annotation.ManagedOperation;
import org.eclipse.jetty.util.component.Graceful;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.statistic.CounterStatistic;
import org.eclipse.jetty.util.statistic.SampleStatistic;

@ManagedObject(value="Request Statistics Gathering")
public class StatisticsHandler
extends HandlerWrapper
implements Graceful {
    private static final Logger LOG = Log.getLogger(StatisticsHandler.class);
    private final AtomicLong _statsStartedAt = new AtomicLong();
    private final CounterStatistic _requestStats = new CounterStatistic();
    private final SampleStatistic _requestTimeStats = new SampleStatistic();
    private final CounterStatistic _dispatchedStats = new CounterStatistic();
    private final SampleStatistic _dispatchedTimeStats = new SampleStatistic();
    private final CounterStatistic _asyncWaitStats = new CounterStatistic();
    private final LongAdder _asyncDispatches = new LongAdder();
    private final LongAdder _expires = new LongAdder();
    private final LongAdder _responses1xx = new LongAdder();
    private final LongAdder _responses2xx = new LongAdder();
    private final LongAdder _responses3xx = new LongAdder();
    private final LongAdder _responses4xx = new LongAdder();
    private final LongAdder _responses5xx = new LongAdder();
    private final LongAdder _responsesTotalBytes = new LongAdder();
    private final AtomicReference<FutureCallback> _shutdown = new AtomicReference();
    private final AtomicBoolean _wrapWarning = new AtomicBoolean();
    private final AsyncListener _onCompletion = new AsyncListener(){

        @Override
        public void onTimeout(AsyncEvent event) throws IOException {
            StatisticsHandler.this._expires.increment();
        }

        @Override
        public void onStartAsync(AsyncEvent event) throws IOException {
            event.getAsyncContext().addListener(this);
        }

        @Override
        public void onError(AsyncEvent event) throws IOException {
        }

        @Override
        public void onComplete(AsyncEvent event) throws IOException {
            FutureCallback shutdown;
            HttpChannelState state = ((AsyncContextEvent)event).getHttpChannelState();
            Request request = state.getBaseRequest();
            long elapsed = System.currentTimeMillis() - request.getTimeStamp();
            long d = StatisticsHandler.this._requestStats.decrement();
            StatisticsHandler.this._requestTimeStats.set(elapsed);
            StatisticsHandler.this.updateResponse(request);
            StatisticsHandler.this._asyncWaitStats.decrement();
            if (d == 0L && (shutdown = (FutureCallback)StatisticsHandler.this._shutdown.get()) != null) {
                shutdown.succeeded();
            }
        }
    };

    @ManagedOperation(value="resets statistics", impact="ACTION")
    public void statsReset() {
        this._statsStartedAt.set(System.currentTimeMillis());
        this._requestStats.reset();
        this._requestTimeStats.reset();
        this._dispatchedStats.reset();
        this._dispatchedTimeStats.reset();
        this._asyncWaitStats.reset();
        this._asyncDispatches.reset();
        this._expires.reset();
        this._responses1xx.reset();
        this._responses2xx.reset();
        this._responses3xx.reset();
        this._responses4xx.reset();
        this._responses5xx.reset();
        this._responsesTotalBytes.reset();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void handle(String path, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        long start;
        this._dispatchedStats.increment();
        HttpChannelState state = baseRequest.getHttpChannelState();
        if (state.isInitial()) {
            this._requestStats.increment();
            start = baseRequest.getTimeStamp();
        } else {
            start = System.currentTimeMillis();
            this._asyncDispatches.increment();
        }
        try {
            Handler handler = this.getHandler();
            if (handler != null && this._shutdown.get() == null && this.isStarted()) {
                handler.handle(path, baseRequest, request, response);
            } else if (baseRequest.isHandled()) {
                if (this._wrapWarning.compareAndSet(false, true)) {
                    LOG.warn("Bad statistics configuration. Latencies will be incorrect in {}", this);
                }
            } else {
                baseRequest.setHandled(true);
                response.sendError(503);
            }
        }
        finally {
            long now = System.currentTimeMillis();
            long dispatched = now - start;
            this._dispatchedStats.decrement();
            this._dispatchedTimeStats.set(dispatched);
            if (state.isSuspended()) {
                if (state.isInitial()) {
                    state.addListener(this._onCompletion);
                    this._asyncWaitStats.increment();
                }
            } else if (state.isInitial()) {
                long d = this._requestStats.decrement();
                this._requestTimeStats.set(dispatched);
                this.updateResponse(baseRequest);
                FutureCallback shutdown = this._shutdown.get();
                if (shutdown != null) {
                    response.flushBuffer();
                    if (d == 0L) {
                        shutdown.succeeded();
                    }
                }
            }
        }
    }

    protected void updateResponse(Request request) {
        Response response = request.getResponse();
        if (request.isHandled()) {
            switch (response.getStatus() / 100) {
                case 1: {
                    this._responses1xx.increment();
                    break;
                }
                case 2: {
                    this._responses2xx.increment();
                    break;
                }
                case 3: {
                    this._responses3xx.increment();
                    break;
                }
                case 4: {
                    this._responses4xx.increment();
                    break;
                }
                case 5: {
                    this._responses5xx.increment();
                    break;
                }
            }
        } else {
            this._responses4xx.increment();
        }
        this._responsesTotalBytes.add(response.getContentCount());
    }

    @Override
    protected void doStart() throws Exception {
        this._shutdown.set(null);
        super.doStart();
        this.statsReset();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        FutureCallback shutdown = this._shutdown.get();
        if (shutdown != null && !shutdown.isDone()) {
            shutdown.failed(new TimeoutException());
        }
    }

    @ManagedAttribute(value="number of requests")
    public int getRequests() {
        return (int)this._requestStats.getTotal();
    }

    @ManagedAttribute(value="number of requests currently active")
    public int getRequestsActive() {
        return (int)this._requestStats.getCurrent();
    }

    @ManagedAttribute(value="maximum number of active requests")
    public int getRequestsActiveMax() {
        return (int)this._requestStats.getMax();
    }

    @ManagedAttribute(value="maximum time spend handling requests (in ms)")
    public long getRequestTimeMax() {
        return this._requestTimeStats.getMax();
    }

    @ManagedAttribute(value="total time spend in all request handling (in ms)")
    public long getRequestTimeTotal() {
        return this._requestTimeStats.getTotal();
    }

    @ManagedAttribute(value="mean time spent handling requests (in ms)")
    public double getRequestTimeMean() {
        return this._requestTimeStats.getMean();
    }

    @ManagedAttribute(value="standard deviation for request handling (in ms)")
    public double getRequestTimeStdDev() {
        return this._requestTimeStats.getStdDev();
    }

    @ManagedAttribute(value="number of dispatches")
    public int getDispatched() {
        return (int)this._dispatchedStats.getTotal();
    }

    @ManagedAttribute(value="number of dispatches currently active")
    public int getDispatchedActive() {
        return (int)this._dispatchedStats.getCurrent();
    }

    @ManagedAttribute(value="maximum number of active dispatches being handled")
    public int getDispatchedActiveMax() {
        return (int)this._dispatchedStats.getMax();
    }

    @ManagedAttribute(value="maximum time spend in dispatch handling")
    public long getDispatchedTimeMax() {
        return this._dispatchedTimeStats.getMax();
    }

    @ManagedAttribute(value="total time spent in dispatch handling (in ms)")
    public long getDispatchedTimeTotal() {
        return this._dispatchedTimeStats.getTotal();
    }

    @ManagedAttribute(value="mean time spent in dispatch handling (in ms)")
    public double getDispatchedTimeMean() {
        return this._dispatchedTimeStats.getMean();
    }

    @ManagedAttribute(value="standard deviation for dispatch handling (in ms)")
    public double getDispatchedTimeStdDev() {
        return this._dispatchedTimeStats.getStdDev();
    }

    @ManagedAttribute(value="total number of async requests")
    public int getAsyncRequests() {
        return (int)this._asyncWaitStats.getTotal();
    }

    @ManagedAttribute(value="currently waiting async requests")
    public int getAsyncRequestsWaiting() {
        return (int)this._asyncWaitStats.getCurrent();
    }

    @ManagedAttribute(value="maximum number of waiting async requests")
    public int getAsyncRequestsWaitingMax() {
        return (int)this._asyncWaitStats.getMax();
    }

    @ManagedAttribute(value="number of requested that have been asynchronously dispatched")
    public int getAsyncDispatches() {
        return this._asyncDispatches.intValue();
    }

    @ManagedAttribute(value="number of async requests requests that have expired")
    public int getExpires() {
        return this._expires.intValue();
    }

    @ManagedAttribute(value="number of requests with 1xx response status")
    public int getResponses1xx() {
        return this._responses1xx.intValue();
    }

    @ManagedAttribute(value="number of requests with 2xx response status")
    public int getResponses2xx() {
        return this._responses2xx.intValue();
    }

    @ManagedAttribute(value="number of requests with 3xx response status")
    public int getResponses3xx() {
        return this._responses3xx.intValue();
    }

    @ManagedAttribute(value="number of requests with 4xx response status")
    public int getResponses4xx() {
        return this._responses4xx.intValue();
    }

    @ManagedAttribute(value="number of requests with 5xx response status")
    public int getResponses5xx() {
        return this._responses5xx.intValue();
    }

    @ManagedAttribute(value="time in milliseconds stats have been collected for")
    public long getStatsOnMs() {
        return System.currentTimeMillis() - this._statsStartedAt.get();
    }

    @ManagedAttribute(value="total number of bytes across all responses")
    public long getResponsesBytesTotal() {
        return this._responsesTotalBytes.longValue();
    }

    public String toStatsHTML() {
        StringBuilder sb = new StringBuilder();
        sb.append("<h1>Statistics:</h1>\n");
        sb.append("Statistics gathering started ").append(this.getStatsOnMs()).append("ms ago").append("<br />\n");
        sb.append("<h2>Requests:</h2>\n");
        sb.append("Total requests: ").append(this.getRequests()).append("<br />\n");
        sb.append("Active requests: ").append(this.getRequestsActive()).append("<br />\n");
        sb.append("Max active requests: ").append(this.getRequestsActiveMax()).append("<br />\n");
        sb.append("Total requests time: ").append(this.getRequestTimeTotal()).append("<br />\n");
        sb.append("Mean request time: ").append(this.getRequestTimeMean()).append("<br />\n");
        sb.append("Max request time: ").append(this.getRequestTimeMax()).append("<br />\n");
        sb.append("Request time standard deviation: ").append(this.getRequestTimeStdDev()).append("<br />\n");
        sb.append("<h2>Dispatches:</h2>\n");
        sb.append("Total dispatched: ").append(this.getDispatched()).append("<br />\n");
        sb.append("Active dispatched: ").append(this.getDispatchedActive()).append("<br />\n");
        sb.append("Max active dispatched: ").append(this.getDispatchedActiveMax()).append("<br />\n");
        sb.append("Total dispatched time: ").append(this.getDispatchedTimeTotal()).append("<br />\n");
        sb.append("Mean dispatched time: ").append(this.getDispatchedTimeMean()).append("<br />\n");
        sb.append("Max dispatched time: ").append(this.getDispatchedTimeMax()).append("<br />\n");
        sb.append("Dispatched time standard deviation: ").append(this.getDispatchedTimeStdDev()).append("<br />\n");
        sb.append("Total requests suspended: ").append(this.getAsyncRequests()).append("<br />\n");
        sb.append("Total requests expired: ").append(this.getExpires()).append("<br />\n");
        sb.append("Total requests resumed: ").append(this.getAsyncDispatches()).append("<br />\n");
        sb.append("<h2>Responses:</h2>\n");
        sb.append("1xx responses: ").append(this.getResponses1xx()).append("<br />\n");
        sb.append("2xx responses: ").append(this.getResponses2xx()).append("<br />\n");
        sb.append("3xx responses: ").append(this.getResponses3xx()).append("<br />\n");
        sb.append("4xx responses: ").append(this.getResponses4xx()).append("<br />\n");
        sb.append("5xx responses: ").append(this.getResponses5xx()).append("<br />\n");
        sb.append("Bytes sent total: ").append(this.getResponsesBytesTotal()).append("<br />\n");
        return sb.toString();
    }

    @Override
    public Future<Void> shutdown() {
        FutureCallback shutdown = new FutureCallback(false);
        this._shutdown.compareAndSet(null, shutdown);
        shutdown = this._shutdown.get();
        if (this._dispatchedStats.getCurrent() == 0L) {
            shutdown.succeeded();
        }
        return shutdown;
    }
}

