/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.handler;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HandlerContainer;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandlerContainer;
import org.eclipse.jetty.util.ArrayUtil;
import org.eclipse.jetty.util.MultiException;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;

@ManagedObject(value="Handler of multiple handlers")
public class HandlerCollection
extends AbstractHandlerContainer {
    private final boolean _mutableWhenRunning;
    private volatile Handler[] _handlers;

    public HandlerCollection() {
        this(false, new Handler[0]);
    }

    public HandlerCollection(Handler ... handlers) {
        this(false, handlers);
    }

    public HandlerCollection(boolean mutableWhenRunning, Handler ... handlers) {
        this._mutableWhenRunning = mutableWhenRunning;
        if (handlers.length > 0) {
            this.setHandlers(handlers);
        }
    }

    @Override
    @ManagedAttribute(value="Wrapped handlers", readonly=true)
    public Handler[] getHandlers() {
        return this._handlers;
    }

    public void setHandlers(Handler[] handlers) {
        if (!this._mutableWhenRunning && this.isStarted()) {
            throw new IllegalStateException("STARTED");
        }
        if (handlers != null) {
            for (Handler handler : handlers) {
                if (handler != this && (!(handler instanceof HandlerContainer) || !Arrays.asList(((HandlerContainer)((Object)handler)).getChildHandlers()).contains(this))) continue;
                throw new IllegalStateException("setHandler loop");
            }
            for (Handler handler : handlers) {
                if (handler.getServer() == this.getServer()) continue;
                handler.setServer(this.getServer());
            }
        }
        Object[] old = this._handlers;
        this._handlers = handlers;
        this.updateBeans(old, handlers);
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (this._handlers != null && this.isStarted()) {
            MultiException mex = null;
            for (int i = 0; i < this._handlers.length; ++i) {
                try {
                    this._handlers[i].handle(target, baseRequest, request, response);
                    continue;
                }
                catch (IOException e) {
                    throw e;
                }
                catch (RuntimeException e) {
                    throw e;
                }
                catch (Exception e) {
                    if (mex == null) {
                        mex = new MultiException();
                    }
                    mex.add(e);
                }
            }
            if (mex != null) {
                if (mex.size() == 1) {
                    throw new ServletException(mex.getThrowable(0));
                }
                throw new ServletException(mex);
            }
        }
    }

    public void addHandler(Handler handler) {
        this.setHandlers(ArrayUtil.addToArray(this.getHandlers(), handler, Handler.class));
    }

    public void prependHandler(Handler handler) {
        this.setHandlers(ArrayUtil.prependToArray(handler, this.getHandlers(), Handler.class));
    }

    public void removeHandler(Handler handler) {
        Handler[] handlers = this.getHandlers();
        if (handlers != null && handlers.length > 0) {
            this.setHandlers(ArrayUtil.removeFromArray(handlers, handler));
        }
    }

    @Override
    protected void expandChildren(List<Handler> list, Class<?> byClass) {
        if (this.getHandlers() != null) {
            for (Handler h : this.getHandlers()) {
                this.expandHandler(h, list, byClass);
            }
        }
    }

    @Override
    public void destroy() {
        if (!this.isStopped()) {
            throw new IllegalStateException("!STOPPED");
        }
        Handler[] children = this.getChildHandlers();
        this.setHandlers(null);
        for (Handler child : children) {
            child.destroy();
        }
        super.destroy();
    }

    public String toString() {
        Handler[] handlers = this.getHandlers();
        return super.toString() + (handlers == null ? "[]" : Arrays.asList(this.getHandlers()).toString());
    }
}

