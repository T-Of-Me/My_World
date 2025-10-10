/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.handler;

import java.io.IOException;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;

public class IdleTimeoutHandler
extends HandlerWrapper {
    private long _idleTimeoutMs = 1000L;
    private boolean _applyToAsync = false;

    public boolean isApplyToAsync() {
        return this._applyToAsync;
    }

    public void setApplyToAsync(boolean applyToAsync) {
        this._applyToAsync = applyToAsync;
    }

    public long getIdleTimeoutMs() {
        return this._idleTimeoutMs;
    }

    public void setIdleTimeoutMs(long idleTimeoutMs) {
        this._idleTimeoutMs = idleTimeoutMs;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        HttpChannel channel = baseRequest.getHttpChannel();
        long idle_timeout = baseRequest.getHttpChannel().getIdleTimeout();
        channel.setIdleTimeout(this._idleTimeoutMs);
        try {
            super.handle(target, baseRequest, request, response);
        }
        catch (Throwable throwable) {
            if (this._applyToAsync && request.isAsyncStarted()) {
                request.getAsyncContext().addListener(new AsyncListener(channel, idle_timeout){
                    final /* synthetic */ HttpChannel val$channel;
                    final /* synthetic */ long val$idle_timeout;
                    {
                        this.val$channel = httpChannel;
                        this.val$idle_timeout = l;
                    }

                    @Override
                    public void onTimeout(AsyncEvent event) throws IOException {
                    }

                    @Override
                    public void onStartAsync(AsyncEvent event) throws IOException {
                    }

                    @Override
                    public void onError(AsyncEvent event) throws IOException {
                        this.val$channel.setIdleTimeout(this.val$idle_timeout);
                    }

                    @Override
                    public void onComplete(AsyncEvent event) throws IOException {
                        this.val$channel.setIdleTimeout(this.val$idle_timeout);
                    }
                });
            } else {
                channel.setIdleTimeout(idle_timeout);
            }
            throw throwable;
        }
        if (this._applyToAsync && request.isAsyncStarted()) {
            request.getAsyncContext().addListener(new /* invalid duplicate definition of identical inner class */);
        } else {
            channel.setIdleTimeout(idle_timeout);
        }
    }
}

