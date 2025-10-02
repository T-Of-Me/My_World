/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.annotation.ManagedOperation;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.Container;
import org.eclipse.jetty.util.component.Destroyable;
import org.eclipse.jetty.util.component.Dumpable;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

@ManagedObject(value="Implementation of Container and LifeCycle")
public class ContainerLifeCycle
extends AbstractLifeCycle
implements Container,
Destroyable,
Dumpable {
    private static final Logger LOG = Log.getLogger(ContainerLifeCycle.class);
    private final List<Bean> _beans = new CopyOnWriteArrayList<Bean>();
    private final List<Container.Listener> _listeners = new CopyOnWriteArrayList<Container.Listener>();
    private boolean _doStarted;
    private boolean _destroyed;

    @Override
    protected void doStart() throws Exception {
        if (this._destroyed) {
            throw new IllegalStateException("Destroyed container cannot be restarted");
        }
        this._doStarted = true;
        for (Bean b : this._beans) {
            if (!(b._bean instanceof LifeCycle)) continue;
            LifeCycle l = (LifeCycle)b._bean;
            switch (b._managed) {
                case MANAGED: {
                    if (l.isRunning()) break;
                    this.start(l);
                    break;
                }
                case AUTO: {
                    if (l.isRunning()) {
                        this.unmanage(b);
                        break;
                    }
                    this.manage(b);
                    this.start(l);
                }
            }
        }
        super.doStart();
    }

    protected void start(LifeCycle l) throws Exception {
        l.start();
    }

    protected void stop(LifeCycle l) throws Exception {
        l.stop();
    }

    @Override
    protected void doStop() throws Exception {
        this._doStarted = false;
        super.doStop();
        ArrayList<Bean> reverse = new ArrayList<Bean>(this._beans);
        Collections.reverse(reverse);
        for (Bean b : reverse) {
            if (b._managed != Managed.MANAGED || !(b._bean instanceof LifeCycle)) continue;
            LifeCycle l = (LifeCycle)b._bean;
            this.stop(l);
        }
    }

    @Override
    public void destroy() {
        this._destroyed = true;
        ArrayList<Bean> reverse = new ArrayList<Bean>(this._beans);
        Collections.reverse(reverse);
        for (Bean b : reverse) {
            if (!(b._bean instanceof Destroyable) || b._managed != Managed.MANAGED && b._managed != Managed.POJO) continue;
            Destroyable d = (Destroyable)b._bean;
            d.destroy();
        }
        this._beans.clear();
    }

    public boolean contains(Object bean) {
        for (Bean b : this._beans) {
            if (b._bean != bean) continue;
            return true;
        }
        return false;
    }

    public boolean isManaged(Object bean) {
        for (Bean b : this._beans) {
            if (b._bean != bean) continue;
            return b.isManaged();
        }
        return false;
    }

    @Override
    public boolean addBean(Object o) {
        if (o instanceof LifeCycle) {
            LifeCycle l = (LifeCycle)o;
            return this.addBean(o, l.isRunning() ? Managed.UNMANAGED : Managed.AUTO);
        }
        return this.addBean(o, Managed.POJO);
    }

    public boolean addBean(Object o, boolean managed) {
        if (o instanceof LifeCycle) {
            return this.addBean(o, managed ? Managed.MANAGED : Managed.UNMANAGED);
        }
        return this.addBean(o, managed ? Managed.POJO : Managed.UNMANAGED);
    }

    public boolean addBean(Object o, Managed managed) {
        if (o == null || this.contains(o)) {
            return false;
        }
        Bean new_bean = new Bean(o);
        if (o instanceof Container.Listener) {
            this.addEventListener((Container.Listener)o);
        }
        this._beans.add(new_bean);
        for (Container.Listener l : this._listeners) {
            l.beanAdded(this, o);
        }
        try {
            switch (managed) {
                case UNMANAGED: {
                    this.unmanage(new_bean);
                    break;
                }
                case MANAGED: {
                    this.manage(new_bean);
                    if (!this.isStarting() || !this._doStarted) break;
                    LifeCycle l = (LifeCycle)o;
                    if (!l.isRunning()) {
                        this.start(l);
                    }
                    break;
                }
                case AUTO: {
                    LifeCycle l;
                    if (o instanceof LifeCycle) {
                        l = (LifeCycle)o;
                        if (this.isStarting()) {
                            if (l.isRunning()) {
                                this.unmanage(new_bean);
                                break;
                            }
                            if (this._doStarted) {
                                this.manage(new_bean);
                                this.start(l);
                                break;
                            }
                            new_bean._managed = Managed.AUTO;
                            break;
                        }
                        if (this.isStarted()) {
                            this.unmanage(new_bean);
                            break;
                        }
                        new_bean._managed = Managed.AUTO;
                        break;
                    }
                    new_bean._managed = Managed.POJO;
                    break;
                }
                case POJO: {
                    new_bean._managed = Managed.POJO;
                }
            }
        }
        catch (Error | RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("{} added {}", this, new_bean);
        }
        return true;
    }

    public void addManaged(LifeCycle lifecycle) {
        this.addBean((Object)lifecycle, true);
        try {
            if (this.isRunning() && !lifecycle.isRunning()) {
                this.start(lifecycle);
            }
        }
        catch (Error | RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addEventListener(Container.Listener listener) {
        if (this._listeners.contains(listener)) {
            return;
        }
        this._listeners.add(listener);
        for (Bean b : this._beans) {
            listener.beanAdded(this, b._bean);
            if (!(listener instanceof Container.InheritedListener) || !b.isManaged() || !(b._bean instanceof Container)) continue;
            if (b._bean instanceof ContainerLifeCycle) {
                ((ContainerLifeCycle)b._bean).addBean((Object)listener, false);
                continue;
            }
            ((Container)b._bean).addBean(listener);
        }
    }

    public void manage(Object bean) {
        for (Bean b : this._beans) {
            if (b._bean != bean) continue;
            this.manage(b);
            return;
        }
        throw new IllegalArgumentException("Unknown bean " + bean);
    }

    private void manage(Bean bean) {
        if (bean._managed != Managed.MANAGED) {
            bean._managed = Managed.MANAGED;
            if (bean._bean instanceof Container) {
                for (Container.Listener l : this._listeners) {
                    if (!(l instanceof Container.InheritedListener)) continue;
                    if (bean._bean instanceof ContainerLifeCycle) {
                        ((ContainerLifeCycle)bean._bean).addBean((Object)l, false);
                        continue;
                    }
                    ((Container)bean._bean).addBean(l);
                }
            }
            if (bean._bean instanceof AbstractLifeCycle) {
                ((AbstractLifeCycle)bean._bean).setStopTimeout(this.getStopTimeout());
            }
        }
    }

    public void unmanage(Object bean) {
        for (Bean b : this._beans) {
            if (b._bean != bean) continue;
            this.unmanage(b);
            return;
        }
        throw new IllegalArgumentException("Unknown bean " + bean);
    }

    private void unmanage(Bean bean) {
        if (bean._managed != Managed.UNMANAGED) {
            if (bean._managed == Managed.MANAGED && bean._bean instanceof Container) {
                for (Container.Listener l : this._listeners) {
                    if (!(l instanceof Container.InheritedListener)) continue;
                    ((Container)bean._bean).removeBean(l);
                }
            }
            bean._managed = Managed.UNMANAGED;
        }
    }

    @Override
    public Collection<Object> getBeans() {
        return this.getBeans(Object.class);
    }

    public void setBeans(Collection<Object> beans) {
        for (Object bean : beans) {
            this.addBean(bean);
        }
    }

    @Override
    public <T> Collection<T> getBeans(Class<T> clazz) {
        ArrayList<T> beans = new ArrayList<T>();
        for (Bean b : this._beans) {
            if (!clazz.isInstance(b._bean)) continue;
            beans.add(clazz.cast(b._bean));
        }
        return beans;
    }

    @Override
    public <T> T getBean(Class<T> clazz) {
        for (Bean b : this._beans) {
            if (!clazz.isInstance(b._bean)) continue;
            return clazz.cast(b._bean);
        }
        return null;
    }

    public void removeBeans() {
        ArrayList<Bean> beans = new ArrayList<Bean>(this._beans);
        for (Bean b : beans) {
            this.remove(b);
        }
    }

    private Bean getBean(Object o) {
        for (Bean b : this._beans) {
            if (b._bean != o) continue;
            return b;
        }
        return null;
    }

    @Override
    public boolean removeBean(Object o) {
        Bean b = this.getBean(o);
        return b != null && this.remove(b);
    }

    private boolean remove(Bean bean) {
        if (this._beans.remove(bean)) {
            boolean wasManaged = bean.isManaged();
            this.unmanage(bean);
            for (Container.Listener l : this._listeners) {
                l.beanRemoved(this, bean._bean);
            }
            if (bean._bean instanceof Container.Listener) {
                this.removeEventListener((Container.Listener)bean._bean);
            }
            if (wasManaged && bean._bean instanceof LifeCycle) {
                try {
                    this.stop((LifeCycle)bean._bean);
                }
                catch (Error | RuntimeException e) {
                    throw e;
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void removeEventListener(Container.Listener listener) {
        if (this._listeners.remove(listener)) {
            for (Bean b : this._beans) {
                listener.beanRemoved(this, b._bean);
                if (!(listener instanceof Container.InheritedListener) || !b.isManaged() || !(b._bean instanceof Container)) continue;
                ((Container)b._bean).removeBean(listener);
            }
        }
    }

    @Override
    public void setStopTimeout(long stopTimeout) {
        super.setStopTimeout(stopTimeout);
        for (Bean bean : this._beans) {
            if (!bean.isManaged() || !(bean._bean instanceof AbstractLifeCycle)) continue;
            ((AbstractLifeCycle)bean._bean).setStopTimeout(stopTimeout);
        }
    }

    @ManagedOperation(value="Dump the object to stderr")
    public void dumpStdErr() {
        try {
            this.dump(System.err, "");
        }
        catch (IOException e) {
            LOG.warn(e);
        }
    }

    @Override
    @ManagedOperation(value="Dump the object to a string")
    public String dump() {
        return ContainerLifeCycle.dump(this);
    }

    public static String dump(Dumpable dumpable) {
        StringBuilder b = new StringBuilder();
        try {
            dumpable.dump(b, "");
        }
        catch (IOException e) {
            LOG.warn(e);
        }
        return b.toString();
    }

    public void dump(Appendable out) throws IOException {
        this.dump(out, "");
    }

    protected void dumpThis(Appendable out) throws IOException {
        out.append(String.valueOf(this)).append(" - ").append(this.getState()).append("\n");
    }

    public static void dumpObject(Appendable out, Object o) throws IOException {
        try {
            if (o instanceof LifeCycle) {
                out.append(String.valueOf(o)).append(" - ").append(AbstractLifeCycle.getState((LifeCycle)o)).append("\n");
            } else {
                out.append(String.valueOf(o)).append("\n");
            }
        }
        catch (Throwable th) {
            out.append(" => ").append(th.toString()).append('\n');
        }
    }

    @Override
    public void dump(Appendable out, String indent) throws IOException {
        this.dumpBeans(out, indent, new Collection[0]);
    }

    protected void dumpBeans(Appendable out, String indent, Collection<?> ... collections) throws IOException {
        this.dumpThis(out);
        int size = this._beans.size();
        for (Collection<?> c : collections) {
            size += c.size();
        }
        if (size == 0) {
            return;
        }
        int i = 0;
        for (Bean b : this._beans) {
            ++i;
            switch (b._managed) {
                case POJO: {
                    out.append(indent).append(" +- ");
                    if (b._bean instanceof Dumpable) {
                        ((Dumpable)b._bean).dump(out, indent + (i == size ? "    " : " |  "));
                        break;
                    }
                    ContainerLifeCycle.dumpObject(out, b._bean);
                    break;
                }
                case MANAGED: {
                    out.append(indent).append(" += ");
                    if (b._bean instanceof Dumpable) {
                        ((Dumpable)b._bean).dump(out, indent + (i == size ? "    " : " |  "));
                        break;
                    }
                    ContainerLifeCycle.dumpObject(out, b._bean);
                    break;
                }
                case UNMANAGED: {
                    out.append(indent).append(" +~ ");
                    ContainerLifeCycle.dumpObject(out, b._bean);
                    break;
                }
                case AUTO: {
                    out.append(indent).append(" +? ");
                    if (b._bean instanceof Dumpable) {
                        ((Dumpable)b._bean).dump(out, indent + (i == size ? "    " : " |  "));
                        break;
                    }
                    ContainerLifeCycle.dumpObject(out, b._bean);
                }
            }
        }
        if (i < size) {
            out.append(indent).append(" |\n");
        }
        for (Collection<?> c : collections) {
            for (Object o : c) {
                ++i;
                out.append(indent).append(" +> ");
                if (o instanceof Dumpable) {
                    ((Dumpable)o).dump(out, indent + (i == size ? "    " : " |  "));
                    continue;
                }
                ContainerLifeCycle.dumpObject(out, o);
            }
        }
    }

    public static void dump(Appendable out, String indent, Collection<?> ... collections) throws IOException {
        if (collections.length == 0) {
            return;
        }
        int size = 0;
        for (Collection<?> c : collections) {
            size += c.size();
        }
        if (size == 0) {
            return;
        }
        int i = 0;
        for (Collection<?> c : collections) {
            for (Object o : c) {
                ++i;
                out.append(indent).append(" +- ");
                if (o instanceof Dumpable) {
                    ((Dumpable)o).dump(out, indent + (i == size ? "    " : " |  "));
                    continue;
                }
                ContainerLifeCycle.dumpObject(out, o);
            }
        }
    }

    public void updateBean(Object oldBean, Object newBean) {
        if (newBean != oldBean) {
            if (oldBean != null) {
                this.removeBean(oldBean);
            }
            if (newBean != null) {
                this.addBean(newBean);
            }
        }
    }

    public void updateBean(Object oldBean, Object newBean, boolean managed) {
        if (newBean != oldBean) {
            if (oldBean != null) {
                this.removeBean(oldBean);
            }
            if (newBean != null) {
                this.addBean(newBean, managed);
            }
        }
    }

    public void updateBeans(Object[] oldBeans, Object[] newBeans) {
        if (oldBeans != null) {
            block0: for (Object o : oldBeans) {
                if (newBeans != null) {
                    for (Object n : newBeans) {
                        if (o == n) continue block0;
                    }
                }
                this.removeBean(o);
            }
        }
        if (newBeans != null) {
            block2: for (Object n : newBeans) {
                if (oldBeans != null) {
                    for (Object o : oldBeans) {
                        if (o == n) continue block2;
                    }
                }
                this.addBean(n);
            }
        }
    }

    private static class Bean {
        private final Object _bean;
        private volatile Managed _managed = Managed.POJO;

        private Bean(Object b) {
            if (b == null) {
                throw new NullPointerException();
            }
            this._bean = b;
        }

        public boolean isManaged() {
            return this._managed == Managed.MANAGED;
        }

        public String toString() {
            return String.format("{%s,%s}", new Object[]{this._bean, this._managed});
        }
    }

    static enum Managed {
        POJO,
        MANAGED,
        UNMANAGED,
        AUTO;

    }
}

