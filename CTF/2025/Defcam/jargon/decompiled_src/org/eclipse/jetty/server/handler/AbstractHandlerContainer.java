/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HandlerContainer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

public abstract class AbstractHandlerContainer
extends AbstractHandler
implements HandlerContainer {
    @Override
    public Handler[] getChildHandlers() {
        ArrayList<Handler> list = new ArrayList<Handler>();
        this.expandChildren(list, null);
        return list.toArray(new Handler[list.size()]);
    }

    @Override
    public Handler[] getChildHandlersByClass(Class<?> byclass) {
        ArrayList<Handler> list = new ArrayList<Handler>();
        this.expandChildren(list, byclass);
        return list.toArray(new Handler[list.size()]);
    }

    @Override
    public <T extends Handler> T getChildHandlerByClass(Class<T> byclass) {
        ArrayList<Handler> list = new ArrayList<Handler>();
        this.expandChildren(list, byclass);
        if (list.isEmpty()) {
            return null;
        }
        return (T)((Handler)list.get(0));
    }

    protected void expandChildren(List<Handler> list, Class<?> byClass) {
    }

    protected void expandHandler(Handler handler, List<Handler> list, Class<?> byClass) {
        if (handler == null) {
            return;
        }
        if (byClass == null || byClass.isAssignableFrom(handler.getClass())) {
            list.add(handler);
        }
        if (handler instanceof AbstractHandlerContainer) {
            ((AbstractHandlerContainer)handler).expandChildren(list, byClass);
        } else if (handler instanceof HandlerContainer) {
            HandlerContainer container = (HandlerContainer)((Object)handler);
            Handler[] handlers = byClass == null ? container.getChildHandlers() : container.getChildHandlersByClass(byClass);
            list.addAll(Arrays.asList(handlers));
        }
    }

    public static <T extends HandlerContainer> T findContainerOf(HandlerContainer root, Class<T> type, Handler handler) {
        if (root == null || handler == null) {
            return null;
        }
        Handler[] branches = root.getChildHandlersByClass(type);
        if (branches != null) {
            for (Handler h : branches) {
                HandlerContainer container = (HandlerContainer)((Object)h);
                Handler[] candidates = container.getChildHandlersByClass(handler.getClass());
                if (candidates == null) continue;
                for (Handler c : candidates) {
                    if (c != handler) continue;
                    return (T)container;
                }
            }
        }
        return null;
    }

    @Override
    public void setServer(Server server) {
        if (server == this.getServer()) {
            return;
        }
        if (this.isStarted()) {
            throw new IllegalStateException("STARTED");
        }
        super.setServer(server);
        Handler[] handlers = this.getHandlers();
        if (handlers != null) {
            for (Handler h : handlers) {
                h.setServer(server);
            }
        }
    }
}

