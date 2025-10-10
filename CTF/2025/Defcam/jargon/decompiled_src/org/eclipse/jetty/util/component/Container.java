/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.component;

import java.util.Collection;

public interface Container {
    public boolean addBean(Object var1);

    public Collection<Object> getBeans();

    public <T> Collection<T> getBeans(Class<T> var1);

    public <T> T getBean(Class<T> var1);

    public boolean removeBean(Object var1);

    public void addEventListener(Listener var1);

    public void removeEventListener(Listener var1);

    public static interface InheritedListener
    extends Listener {
    }

    public static interface Listener {
        public void beanAdded(Container var1, Object var2);

        public void beanRemoved(Container var1, Object var2);
    }
}

