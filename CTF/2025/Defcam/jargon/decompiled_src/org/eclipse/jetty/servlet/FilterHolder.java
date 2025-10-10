/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.Holder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.Source;
import org.eclipse.jetty.util.TypeUtil;
import org.eclipse.jetty.util.component.Dumpable;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class FilterHolder
extends Holder<Filter> {
    private static final Logger LOG = Log.getLogger(FilterHolder.class);
    private transient Filter _filter;
    private transient Config _config;
    private transient FilterRegistration.Dynamic _registration;

    public FilterHolder() {
        this(Source.EMBEDDED);
    }

    public FilterHolder(Source source) {
        super(source);
    }

    public FilterHolder(Class<? extends Filter> filter) {
        this(Source.EMBEDDED);
        this.setHeldClass(filter);
    }

    public FilterHolder(Filter filter) {
        this(Source.EMBEDDED);
        this.setFilter(filter);
    }

    @Override
    public void doStart() throws Exception {
        super.doStart();
        if (!Filter.class.isAssignableFrom(this._class)) {
            String msg = this._class + " is not a javax.servlet.Filter";
            super.stop();
            throw new IllegalStateException(msg);
        }
    }

    @Override
    public void initialize() throws Exception {
        if (!this._initialized) {
            super.initialize();
            if (this._filter == null) {
                try {
                    ServletContext context = this._servletHandler.getServletContext();
                    this._filter = context instanceof ServletContextHandler.Context ? ((ServletContextHandler.Context)context).createFilter(this.getHeldClass()) : (Filter)this.getHeldClass().newInstance();
                }
                catch (ServletException se) {
                    Throwable cause = se.getRootCause();
                    if (cause instanceof InstantiationException) {
                        throw (InstantiationException)cause;
                    }
                    if (cause instanceof IllegalAccessException) {
                        throw (IllegalAccessException)cause;
                    }
                    throw se;
                }
            }
            this._config = new Config();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Filter.init {}", this._filter);
            }
            this._filter.init(this._config);
        }
        this._initialized = true;
    }

    @Override
    public void doStop() throws Exception {
        if (this._filter != null) {
            try {
                this.destroyInstance(this._filter);
            }
            catch (Exception e) {
                LOG.warn(e);
            }
        }
        if (!this._extInstance) {
            this._filter = null;
        }
        this._config = null;
        this._initialized = false;
        super.doStop();
    }

    @Override
    public void destroyInstance(Object o) throws Exception {
        if (o == null) {
            return;
        }
        Filter f = (Filter)o;
        f.destroy();
        this.getServletHandler().destroyFilter(f);
    }

    public synchronized void setFilter(Filter filter) {
        this._filter = filter;
        this._extInstance = true;
        this.setHeldClass(filter.getClass());
        if (this.getName() == null) {
            this.setName(filter.getClass().getName());
        }
    }

    public Filter getFilter() {
        return this._filter;
    }

    @Override
    public String toString() {
        return this.getName();
    }

    @Override
    public void dump(Appendable out, String indent) throws IOException {
        super.dump(out, indent);
        if (this._filter instanceof Dumpable) {
            ((Dumpable)((Object)this._filter)).dump(out, indent);
        }
    }

    public FilterRegistration.Dynamic getRegistration() {
        if (this._registration == null) {
            this._registration = new Registration();
        }
        return this._registration;
    }

    class Config
    extends Holder.HolderConfig
    implements FilterConfig {
        Config() {
            super(FilterHolder.this);
        }

        @Override
        public String getFilterName() {
            return FilterHolder.this._name;
        }
    }

    protected class Registration
    extends Holder.HolderRegistration
    implements FilterRegistration.Dynamic {
        protected Registration() {
            super(FilterHolder.this);
        }

        @Override
        public void addMappingForServletNames(EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter, String ... servletNames) {
            FilterHolder.this.illegalStateIfContextStarted();
            FilterMapping mapping = new FilterMapping();
            mapping.setFilterHolder(FilterHolder.this);
            mapping.setServletNames(servletNames);
            mapping.setDispatcherTypes(dispatcherTypes);
            if (isMatchAfter) {
                FilterHolder.this._servletHandler.addFilterMapping(mapping);
            } else {
                FilterHolder.this._servletHandler.prependFilterMapping(mapping);
            }
        }

        @Override
        public void addMappingForUrlPatterns(EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter, String ... urlPatterns) {
            FilterHolder.this.illegalStateIfContextStarted();
            FilterMapping mapping = new FilterMapping();
            mapping.setFilterHolder(FilterHolder.this);
            mapping.setPathSpecs(urlPatterns);
            mapping.setDispatcherTypes(dispatcherTypes);
            if (isMatchAfter) {
                FilterHolder.this._servletHandler.addFilterMapping(mapping);
            } else {
                FilterHolder.this._servletHandler.prependFilterMapping(mapping);
            }
        }

        @Override
        public Collection<String> getServletNameMappings() {
            FilterMapping[] mappings = FilterHolder.this._servletHandler.getFilterMappings();
            ArrayList<String> names = new ArrayList<String>();
            for (FilterMapping mapping : mappings) {
                String[] servlets;
                if (mapping.getFilterHolder() != FilterHolder.this || (servlets = mapping.getServletNames()) == null || servlets.length <= 0) continue;
                names.addAll(Arrays.asList(servlets));
            }
            return names;
        }

        @Override
        public Collection<String> getUrlPatternMappings() {
            FilterMapping[] mappings = FilterHolder.this._servletHandler.getFilterMappings();
            ArrayList<String> patterns = new ArrayList<String>();
            for (FilterMapping mapping : mappings) {
                if (mapping.getFilterHolder() != FilterHolder.this) continue;
                String[] specs = mapping.getPathSpecs();
                patterns.addAll(TypeUtil.asList(specs));
            }
            return patterns;
        }
    }
}

