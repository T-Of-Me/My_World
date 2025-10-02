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
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;

@ManagedObject(value="Handler wrapping another Handler")
public class HandlerWrapper
extends AbstractHandlerContainer {
    protected Handler _handler;

    @ManagedAttribute(value="Wrapped Handler", readonly=true)
    public Handler getHandler() {
        return this._handler;
    }

    @Override
    public Handler[] getHandlers() {
        if (this._handler == null) {
            return new Handler[0];
        }
        return new Handler[]{this._handler};
    }

    public void setHandler(Handler handler) {
        if (this.isStarted()) {
            throw new IllegalStateException("STARTED");
        }
        if (handler == this || handler instanceof HandlerContainer && Arrays.asList(((HandlerContainer)((Object)handler)).getChildHandlers()).contains(this)) {
            throw new IllegalStateException("setHandler loop");
        }
        if (handler != null) {
            handler.setServer(this.getServer());
        }
        Handler old = this._handler;
        this._handler = handler;
        this.updateBean(old, this._handler, true);
    }

    public void insertHandler(HandlerWrapper wrapper) {
        if (wrapper == null) {
            throw new IllegalArgumentException();
        }
        HandlerWrapper tail = wrapper;
        while (tail.getHandler() instanceof HandlerWrapper) {
            tail = (HandlerWrapper)tail.getHandler();
        }
        if (tail.getHandler() != null) {
            throw new IllegalArgumentException("bad tail of inserted wrapper chain");
        }
        Handler next = this.getHandler();
        this.setHandler(wrapper);
        tail.setHandler(next);
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        Handler handler = this._handler;
        if (handler != null) {
            handler.handle(target, baseRequest, request, response);
        }
    }

    @Override
    protected void expandChildren(List<Handler> list, Class<?> byClass) {
        this.expandHandler(this._handler, list, byClass);
    }

    @Override
    public void destroy() {
        if (!this.isStopped()) {
            throw new IllegalStateException("!STOPPED");
        }
        Handler child = this.getHandler();
        if (child != null) {
            this.setHandler(null);
            child.destroy();
        }
        super.destroy();
    }
}

