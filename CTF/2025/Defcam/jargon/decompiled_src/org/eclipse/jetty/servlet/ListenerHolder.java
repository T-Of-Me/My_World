/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.servlet;

import java.util.EventListener;
import org.eclipse.jetty.servlet.BaseHolder;
import org.eclipse.jetty.servlet.Source;

public class ListenerHolder
extends BaseHolder<EventListener> {
    private EventListener _listener;

    public ListenerHolder(Source source) {
        super(source);
    }

    public void setListener(EventListener listener) {
        this._listener = listener;
        this.setClassName(listener.getClass().getName());
        this.setHeldClass(listener.getClass());
        this._extInstance = true;
    }

    public EventListener getListener() {
        return this._listener;
    }

    @Override
    public void doStart() throws Exception {
        if (this._listener == null) {
            throw new IllegalStateException("No listener instance");
        }
        super.doStart();
    }
}

