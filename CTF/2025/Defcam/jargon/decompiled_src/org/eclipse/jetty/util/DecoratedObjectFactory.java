/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.eclipse.jetty.util.Decorator;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class DecoratedObjectFactory
implements Iterable<Decorator> {
    private static final Logger LOG = Log.getLogger(DecoratedObjectFactory.class);
    public static final String ATTR = DecoratedObjectFactory.class.getName();
    private List<Decorator> decorators = new ArrayList<Decorator>();

    public void addDecorator(Decorator decorator) {
        LOG.debug("Adding Decorator: {}", decorator);
        this.decorators.add(decorator);
    }

    public void clear() {
        this.decorators.clear();
    }

    public <T> T createInstance(Class<T> clazz) throws InstantiationException, IllegalAccessException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating Instance: " + clazz, new Object[0]);
        }
        T o = clazz.newInstance();
        return this.decorate(o);
    }

    public <T> T decorate(T obj) {
        T f = obj;
        for (int i = this.decorators.size() - 1; i >= 0; --i) {
            f = this.decorators.get(i).decorate(f);
        }
        return f;
    }

    public void destroy(Object obj) {
        for (Decorator decorator : this.decorators) {
            decorator.destroy(obj);
        }
    }

    public List<Decorator> getDecorators() {
        return Collections.unmodifiableList(this.decorators);
    }

    @Override
    public Iterator<Decorator> iterator() {
        return this.decorators.iterator();
    }

    public void setDecorators(List<? extends Decorator> decorators) {
        this.decorators.clear();
        if (decorators != null) {
            this.decorators.addAll(decorators);
        }
    }

    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append(this.getClass().getName()).append("[decorators=");
        str.append(Integer.toString(this.decorators.size()));
        str.append("]");
        return str.toString();
    }
}

