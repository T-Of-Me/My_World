/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.servlet;

import java.io.IOException;
import java.util.EnumSet;
import javax.servlet.DispatcherType;
import org.eclipse.jetty.http.PathMap;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.util.TypeUtil;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.component.Dumpable;

@ManagedObject(value="Filter Mappings")
public class FilterMapping
implements Dumpable {
    public static final int DEFAULT = 0;
    public static final int REQUEST = 1;
    public static final int FORWARD = 2;
    public static final int INCLUDE = 4;
    public static final int ERROR = 8;
    public static final int ASYNC = 16;
    public static final int ALL = 31;
    private int _dispatches = 0;
    private String _filterName;
    private transient FilterHolder _holder;
    private String[] _pathSpecs;
    private String[] _servletNames;

    public static DispatcherType dispatch(String type) {
        if ("request".equalsIgnoreCase(type)) {
            return DispatcherType.REQUEST;
        }
        if ("forward".equalsIgnoreCase(type)) {
            return DispatcherType.FORWARD;
        }
        if ("include".equalsIgnoreCase(type)) {
            return DispatcherType.INCLUDE;
        }
        if ("error".equalsIgnoreCase(type)) {
            return DispatcherType.ERROR;
        }
        if ("async".equalsIgnoreCase(type)) {
            return DispatcherType.ASYNC;
        }
        throw new IllegalArgumentException(type);
    }

    public static int dispatch(DispatcherType type) {
        switch (type) {
            case REQUEST: {
                return 1;
            }
            case ASYNC: {
                return 16;
            }
            case FORWARD: {
                return 2;
            }
            case INCLUDE: {
                return 4;
            }
            case ERROR: {
                return 8;
            }
        }
        throw new IllegalArgumentException(type.toString());
    }

    boolean appliesTo(String path, int type) {
        if (this.appliesTo(type)) {
            for (int i = 0; i < this._pathSpecs.length; ++i) {
                if (this._pathSpecs[i] == null || !PathMap.match(this._pathSpecs[i], path, true)) continue;
                return true;
            }
        }
        return false;
    }

    boolean appliesTo(int type) {
        if (this._dispatches == 0) {
            return type == 1 || type == 16 && this._holder.isAsyncSupported();
        }
        return (this._dispatches & type) != 0;
    }

    public boolean appliesTo(DispatcherType t) {
        return this.appliesTo(FilterMapping.dispatch(t));
    }

    public boolean isDefaultDispatches() {
        return this._dispatches == 0;
    }

    @ManagedAttribute(value="filter name", readonly=true)
    public String getFilterName() {
        return this._filterName;
    }

    FilterHolder getFilterHolder() {
        return this._holder;
    }

    @ManagedAttribute(value="url patterns", readonly=true)
    public String[] getPathSpecs() {
        return this._pathSpecs;
    }

    public void setDispatcherTypes(EnumSet<DispatcherType> dispatcherTypes) {
        this._dispatches = 0;
        if (dispatcherTypes != null) {
            if (dispatcherTypes.contains((Object)DispatcherType.ERROR)) {
                this._dispatches |= 8;
            }
            if (dispatcherTypes.contains((Object)DispatcherType.FORWARD)) {
                this._dispatches |= 2;
            }
            if (dispatcherTypes.contains((Object)DispatcherType.INCLUDE)) {
                this._dispatches |= 4;
            }
            if (dispatcherTypes.contains((Object)DispatcherType.REQUEST)) {
                this._dispatches |= 1;
            }
            if (dispatcherTypes.contains((Object)DispatcherType.ASYNC)) {
                this._dispatches |= 0x10;
            }
        }
    }

    public EnumSet<DispatcherType> getDispatcherTypes() {
        EnumSet<DispatcherType> dispatcherTypes = EnumSet.noneOf(DispatcherType.class);
        if ((this._dispatches & 8) == 8) {
            dispatcherTypes.add(DispatcherType.ERROR);
        }
        if ((this._dispatches & 2) == 2) {
            dispatcherTypes.add(DispatcherType.FORWARD);
        }
        if ((this._dispatches & 4) == 4) {
            dispatcherTypes.add(DispatcherType.INCLUDE);
        }
        if ((this._dispatches & 1) == 1) {
            dispatcherTypes.add(DispatcherType.REQUEST);
        }
        if ((this._dispatches & 0x10) == 16) {
            dispatcherTypes.add(DispatcherType.ASYNC);
        }
        return dispatcherTypes;
    }

    public void setDispatches(int dispatches) {
        this._dispatches = dispatches;
    }

    public void setFilterName(String filterName) {
        this._filterName = filterName;
    }

    void setFilterHolder(FilterHolder holder) {
        this._holder = holder;
        this.setFilterName(holder.getName());
    }

    public void setPathSpecs(String[] pathSpecs) {
        this._pathSpecs = pathSpecs;
    }

    public void setPathSpec(String pathSpec) {
        this._pathSpecs = new String[]{pathSpec};
    }

    @ManagedAttribute(value="servlet names", readonly=true)
    public String[] getServletNames() {
        return this._servletNames;
    }

    public void setServletNames(String[] servletNames) {
        this._servletNames = servletNames;
    }

    public void setServletName(String servletName) {
        this._servletNames = new String[]{servletName};
    }

    public String toString() {
        return TypeUtil.asList(this._pathSpecs) + "/" + TypeUtil.asList(this._servletNames) + "==" + this._dispatches + "=>" + this._filterName;
    }

    @Override
    public void dump(Appendable out, String indent) throws IOException {
        out.append(String.valueOf(this)).append("\n");
    }

    @Override
    public String dump() {
        return ContainerLifeCycle.dump(this);
    }
}

