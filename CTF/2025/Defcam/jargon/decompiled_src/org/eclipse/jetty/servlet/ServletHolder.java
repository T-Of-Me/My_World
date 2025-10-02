/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.servlet;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletSecurityElement;
import javax.servlet.SingleThreadModel;
import javax.servlet.UnavailableException;
import javax.servlet.annotation.ServletSecurity;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.RunAsToken;
import org.eclipse.jetty.server.MultiPartCleanerListener;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.servlet.Holder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletMapping;
import org.eclipse.jetty.servlet.Source;
import org.eclipse.jetty.util.Loader;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

@ManagedObject(value="Servlet Holder")
public class ServletHolder
extends Holder<Servlet>
implements UserIdentity.Scope,
Comparable<ServletHolder> {
    private static final Logger LOG = Log.getLogger(ServletHolder.class);
    private int _initOrder = -1;
    private boolean _initOnStartup = false;
    private Map<String, String> _roleMap;
    private String _forcedPath;
    private String _runAsRole;
    private RunAsToken _runAsToken;
    private IdentityService _identityService;
    private ServletRegistration.Dynamic _registration;
    private JspContainer _jspContainer;
    private transient Servlet _servlet;
    private transient Config _config;
    private transient long _unavailable;
    private transient boolean _enabled = true;
    private transient UnavailableException _unavailableEx;
    public static final String APACHE_SENTINEL_CLASS = "org.apache.tomcat.InstanceManager";
    public static final String JSP_GENERATED_PACKAGE_NAME = "org.eclipse.jetty.servlet.jspPackagePrefix";
    public static final Map<String, String> NO_MAPPED_ROLES = Collections.emptyMap();

    public ServletHolder() {
        this(Source.EMBEDDED);
    }

    public ServletHolder(Source creator) {
        super(creator);
    }

    public ServletHolder(Servlet servlet) {
        this(Source.EMBEDDED);
        this.setServlet(servlet);
    }

    public ServletHolder(String name, Class<? extends Servlet> servlet) {
        this(Source.EMBEDDED);
        this.setName(name);
        this.setHeldClass(servlet);
    }

    public ServletHolder(String name, Servlet servlet) {
        this(Source.EMBEDDED);
        this.setName(name);
        this.setServlet(servlet);
    }

    public ServletHolder(Class<? extends Servlet> servlet) {
        this(Source.EMBEDDED);
        this.setHeldClass(servlet);
    }

    public UnavailableException getUnavailableException() {
        return this._unavailableEx;
    }

    public synchronized void setServlet(Servlet servlet) {
        if (servlet == null || servlet instanceof SingleThreadModel) {
            throw new IllegalArgumentException();
        }
        this._extInstance = true;
        this._servlet = servlet;
        this.setHeldClass(servlet.getClass());
        if (this.getName() == null) {
            this.setName(servlet.getClass().getName() + "-" + super.hashCode());
        }
    }

    @ManagedAttribute(value="initialization order", readonly=true)
    public int getInitOrder() {
        return this._initOrder;
    }

    public void setInitOrder(int order) {
        this._initOnStartup = order >= 0;
        this._initOrder = order;
    }

    @Override
    public int compareTo(ServletHolder sh) {
        if (sh == this) {
            return 0;
        }
        if (sh._initOrder < this._initOrder) {
            return 1;
        }
        if (sh._initOrder > this._initOrder) {
            return -1;
        }
        int c = this._className == null && sh._className == null ? 0 : (this._className == null ? -1 : (sh._className == null ? 1 : this._className.compareTo(sh._className)));
        if (c == 0) {
            c = this._name.compareTo(sh._name);
        }
        return c;
    }

    public boolean equals(Object o) {
        return o instanceof ServletHolder && this.compareTo((ServletHolder)o) == 0;
    }

    public int hashCode() {
        return this._name == null ? System.identityHashCode(this) : this._name.hashCode();
    }

    public synchronized void setUserRoleLink(String name, String link) {
        if (this._roleMap == null) {
            this._roleMap = new HashMap<String, String>();
        }
        this._roleMap.put(name, link);
    }

    public String getUserRoleLink(String name) {
        if (this._roleMap == null) {
            return name;
        }
        String link = this._roleMap.get(name);
        return link == null ? name : link;
    }

    @ManagedAttribute(value="forced servlet path", readonly=true)
    public String getForcedPath() {
        return this._forcedPath;
    }

    public void setForcedPath(String forcedPath) {
        this._forcedPath = forcedPath;
    }

    public boolean isEnabled() {
        return this._enabled;
    }

    public void setEnabled(boolean enabled) {
        this._enabled = enabled;
    }

    @Override
    public void doStart() throws Exception {
        this._unavailable = 0L;
        if (!this._enabled) {
            return;
        }
        if (this._forcedPath != null) {
            String precompiled = this.getClassNameForJsp(this._forcedPath);
            if (!StringUtil.isBlank(precompiled)) {
                ServletHolder jsp;
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Checking for precompiled servlet {} for jsp {}", precompiled, this._forcedPath);
                }
                if ((jsp = this.getServletHandler().getServlet(precompiled)) != null && jsp.getClassName() != null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("JSP file {} for {} mapped to Servlet {}", this._forcedPath, this.getName(), jsp.getClassName());
                    }
                    this.setClassName(jsp.getClassName());
                } else {
                    jsp = this.getServletHandler().getServlet("jsp");
                    if (jsp != null) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("JSP file {} for {} mapped to JspServlet class {}", this._forcedPath, this.getName(), jsp.getClassName());
                        }
                        this.setClassName(jsp.getClassName());
                        for (Map.Entry<String, String> entry : jsp.getInitParameters().entrySet()) {
                            if (this._initParams.containsKey(entry.getKey())) continue;
                            this.setInitParameter(entry.getKey(), entry.getValue());
                        }
                        this.setInitParameter("jspFile", this._forcedPath);
                    }
                }
            } else {
                LOG.warn("Bad jsp-file {} conversion to classname in holder {}", this._forcedPath, this.getName());
            }
        }
        try {
            super.doStart();
        }
        catch (UnavailableException ue) {
            this.makeUnavailable(ue);
            if (this._servletHandler.isStartWithUnavailable()) {
                LOG.ignore(ue);
                return;
            }
            throw ue;
        }
        try {
            this.checkServletType();
        }
        catch (UnavailableException ue) {
            this.makeUnavailable(ue);
            if (this._servletHandler.isStartWithUnavailable()) {
                LOG.ignore(ue);
                return;
            }
            throw ue;
        }
        this.checkInitOnStartup();
        this._identityService = this._servletHandler.getIdentityService();
        if (this._identityService != null && this._runAsRole != null) {
            this._runAsToken = this._identityService.newRunAsToken(this._runAsRole);
        }
        this._config = new Config();
        if (this._class != null && SingleThreadModel.class.isAssignableFrom(this._class)) {
            this._servlet = new SingleThreadedWrapper();
        }
    }

    @Override
    public void initialize() throws Exception {
        if (!this._initialized) {
            super.initialize();
            if (this._extInstance || this._initOnStartup) {
                try {
                    this.initServlet();
                }
                catch (Exception e) {
                    if (this._servletHandler.isStartWithUnavailable()) {
                        LOG.ignore(e);
                    }
                    throw e;
                }
            }
        }
        this._initialized = true;
    }

    @Override
    public void doStop() throws Exception {
        Object old_run_as = null;
        if (this._servlet != null) {
            try {
                if (this._identityService != null) {
                    old_run_as = this._identityService.setRunAs(this._identityService.getSystemUserIdentity(), this._runAsToken);
                }
                this.destroyInstance(this._servlet);
                if (this._identityService != null) {
                    this._identityService.unsetRunAs(old_run_as);
                }
            }
            catch (Exception e) {
                try {
                    LOG.warn(e);
                    if (this._identityService != null) {
                        this._identityService.unsetRunAs(old_run_as);
                    }
                }
                catch (Throwable throwable) {
                    if (this._identityService != null) {
                        this._identityService.unsetRunAs(old_run_as);
                    }
                    throw throwable;
                }
            }
        }
        if (!this._extInstance) {
            this._servlet = null;
        }
        this._config = null;
        this._initialized = false;
    }

    @Override
    public void destroyInstance(Object o) throws Exception {
        if (o == null) {
            return;
        }
        Servlet servlet = (Servlet)o;
        this.getServletHandler().destroyServlet(servlet);
        servlet.destroy();
    }

    public synchronized Servlet getServlet() throws ServletException {
        if (this._unavailable != 0L) {
            if (this._unavailable < 0L || this._unavailable > 0L && System.currentTimeMillis() < this._unavailable) {
                throw this._unavailableEx;
            }
            this._unavailable = 0L;
            this._unavailableEx = null;
        }
        if (this._servlet == null) {
            this.initServlet();
        }
        return this._servlet;
    }

    public Servlet getServletInstance() {
        return this._servlet;
    }

    public void checkServletType() throws UnavailableException {
        if (this._class == null || !Servlet.class.isAssignableFrom(this._class)) {
            throw new UnavailableException("Servlet " + this._class + " is not a javax.servlet.Servlet");
        }
    }

    public boolean isAvailable() {
        if (this.isStarted() && this._unavailable == 0L) {
            return true;
        }
        try {
            this.getServlet();
        }
        catch (Exception e) {
            LOG.ignore(e);
        }
        return this.isStarted() && this._unavailable == 0L;
    }

    private void checkInitOnStartup() {
        if (this._class == null) {
            return;
        }
        if (this._class.getAnnotation(ServletSecurity.class) != null && !this._initOnStartup) {
            this.setInitOrder(Integer.MAX_VALUE);
        }
    }

    private void makeUnavailable(UnavailableException e) {
        if (this._unavailableEx == e && this._unavailable != 0L) {
            return;
        }
        this._servletHandler.getServletContext().log("unavailable", e);
        this._unavailableEx = e;
        this._unavailable = -1L;
        this._unavailable = e.isPermanent() ? -1L : (this._unavailableEx.getUnavailableSeconds() > 0 ? System.currentTimeMillis() + (long)(1000 * this._unavailableEx.getUnavailableSeconds()) : System.currentTimeMillis() + 5000L);
    }

    private void makeUnavailable(final Throwable e) {
        if (e instanceof UnavailableException) {
            this.makeUnavailable((UnavailableException)e);
        } else {
            ServletContext ctx = this._servletHandler.getServletContext();
            if (ctx == null) {
                LOG.info("unavailable", e);
            } else {
                ctx.log("unavailable", e);
            }
            this._unavailableEx = new UnavailableException(String.valueOf(e), -1){
                {
                    super(x0, x1);
                    this.initCause(e);
                }
            };
            this._unavailable = -1L;
        }
    }

    /*
     * Loose catch block
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private void initServlet() throws ServletException {
        Object old_run_as = null;
        try {
            if (this._servlet == null) {
                this._servlet = this.newInstance();
            }
            if (this._config == null) {
                this._config = new Config();
            }
            if (this._identityService != null) {
                old_run_as = this._identityService.setRunAs(this._identityService.getSystemUserIdentity(), this._runAsToken);
            }
            if (this.isJspServlet()) {
                this.initJspServlet();
                this.detectJspContainer();
            } else if (this._forcedPath != null) {
                this.detectJspContainer();
            }
            this.initMultiPart();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Servlet.init {} for {}", this._servlet, this.getName());
            }
            this._servlet.init(this._config);
            if (this._identityService == null) return;
            this._identityService.unsetRunAs(old_run_as);
            return;
        }
        catch (UnavailableException e) {
            try {
                this.makeUnavailable(e);
                this._servlet = null;
                this._config = null;
                throw e;
                catch (ServletException e2) {
                    this.makeUnavailable(e2.getCause() == null ? e2 : e2.getCause());
                    this._servlet = null;
                    this._config = null;
                    throw e2;
                }
                catch (Exception e3) {
                    this.makeUnavailable(e3);
                    this._servlet = null;
                    this._config = null;
                    throw new ServletException(this.toString(), e3);
                }
            }
            catch (Throwable throwable) {
                if (this._identityService == null) throw throwable;
                this._identityService.unsetRunAs(old_run_as);
                throw throwable;
            }
        }
    }

    protected void initJspServlet() throws Exception {
        ContextHandler ch = ContextHandler.getContextHandler(this.getServletHandler().getServletContext());
        ch.setAttribute("org.apache.catalina.jsp_classpath", ch.getClassPath());
        if ("?".equals(this.getInitParameter("classpath"))) {
            String classpath = ch.getClassPath();
            if (LOG.isDebugEnabled()) {
                LOG.debug("classpath=" + classpath, new Object[0]);
            }
            if (classpath != null) {
                this.setInitParameter("classpath", classpath);
            }
        }
        File scratch = null;
        if (this.getInitParameter("scratchdir") == null) {
            File tmp = (File)this.getServletHandler().getServletContext().getAttribute("javax.servlet.context.tempdir");
            scratch = new File(tmp, "jsp");
            this.setInitParameter("scratchdir", scratch.getAbsolutePath());
        }
        if (!(scratch = new File(this.getInitParameter("scratchdir"))).exists()) {
            scratch.mkdir();
        }
    }

    protected void initMultiPart() throws Exception {
        if (((Registration)this.getRegistration()).getMultipartConfig() != null) {
            ContextHandler ch = ContextHandler.getContextHandler(this.getServletHandler().getServletContext());
            ch.addEventListener(MultiPartCleanerListener.INSTANCE);
        }
    }

    @Override
    public String getContextPath() {
        return this._config.getServletContext().getContextPath();
    }

    @Override
    public Map<String, String> getRoleRefMap() {
        return this._roleMap;
    }

    @ManagedAttribute(value="role to run servlet as", readonly=true)
    public String getRunAsRole() {
        return this._runAsRole;
    }

    public void setRunAsRole(String role) {
        this._runAsRole = role;
    }

    protected void prepare(Request baseRequest, ServletRequest request, ServletResponse response) throws ServletException, UnavailableException {
        this.ensureInstance();
        MultipartConfigElement mpce = ((Registration)this.getRegistration()).getMultipartConfig();
        if (mpce != null) {
            baseRequest.setAttribute("org.eclipse.jetty.multipartConfig", mpce);
        }
    }

    public synchronized Servlet ensureInstance() throws ServletException, UnavailableException {
        if (this._class == null) {
            throw new UnavailableException("Servlet Not Initialized");
        }
        Servlet servlet = this._servlet;
        if (!this.isStarted()) {
            throw new UnavailableException("Servlet not initialized", -1);
        }
        if (this._unavailable != 0L || !this._initOnStartup && servlet == null) {
            servlet = this.getServlet();
        }
        if (servlet == null) {
            throw new UnavailableException("Could not instantiate " + this._class);
        }
        return servlet;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void handle(Request baseRequest, ServletRequest request, ServletResponse response) throws ServletException, UnavailableException, IOException {
        if (this._class == null) {
            throw new UnavailableException("Servlet Not Initialized");
        }
        Servlet servlet = this.ensureInstance();
        Object old_run_as = null;
        boolean suspendable = baseRequest.isAsyncSupported();
        try {
            if (this._forcedPath != null) {
                this.adaptForcedPathToJspContainer(request);
            }
            if (this._identityService != null) {
                old_run_as = this._identityService.setRunAs(baseRequest.getResolvedUserIdentity(), this._runAsToken);
            }
            if (baseRequest.isAsyncSupported() && !this.isAsyncSupported()) {
                try {
                    baseRequest.setAsyncSupported(false, this.toString());
                    servlet.service(request, response);
                }
                finally {
                    baseRequest.setAsyncSupported(true, null);
                }
            } else {
                servlet.service(request, response);
            }
            if (this._identityService != null) {
                this._identityService.unsetRunAs(old_run_as);
            }
        }
        catch (UnavailableException e) {
            try {
                this.makeUnavailable(e);
                throw this._unavailableEx;
            }
            catch (Throwable throwable) {
                if (this._identityService != null) {
                    this._identityService.unsetRunAs(old_run_as);
                }
                throw throwable;
            }
        }
    }

    private boolean isJspServlet() {
        if (this._servlet == null) {
            return false;
        }
        boolean result = false;
        for (Class<?> c = this._servlet.getClass(); c != null && !result; c = c.getSuperclass()) {
            result = this.isJspServlet(c.getName());
        }
        return result;
    }

    private boolean isJspServlet(String classname) {
        if (classname == null) {
            return false;
        }
        return "org.apache.jasper.servlet.JspServlet".equals(classname);
    }

    private void adaptForcedPathToJspContainer(ServletRequest request) {
    }

    private void detectJspContainer() {
        if (this._jspContainer == null) {
            try {
                Loader.loadClass(APACHE_SENTINEL_CLASS);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Apache jasper detected", new Object[0]);
                }
                this._jspContainer = JspContainer.APACHE;
            }
            catch (ClassNotFoundException x) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Other jasper detected", new Object[0]);
                }
                this._jspContainer = JspContainer.OTHER;
            }
        }
    }

    public String getNameOfJspClass(String jsp) {
        if (StringUtil.isBlank(jsp)) {
            return "";
        }
        if ("/".equals(jsp = jsp.trim())) {
            return "";
        }
        int i = jsp.lastIndexOf(47);
        if (i == jsp.length() - 1) {
            return "";
        }
        jsp = jsp.substring(i + 1);
        try {
            Class jspUtil = Loader.loadClass("org.apache.jasper.compiler.JspUtil");
            Method makeJavaIdentifier = jspUtil.getMethod("makeJavaIdentifier", String.class);
            return (String)makeJavaIdentifier.invoke(null, jsp);
        }
        catch (Exception e) {
            String tmp = jsp.replace('.', '_');
            if (LOG.isDebugEnabled()) {
                LOG.warn("JspUtil.makeJavaIdentifier failed for jsp " + jsp + " using " + tmp + " instead", new Object[0]);
                LOG.warn(e);
            }
            return tmp;
        }
    }

    public String getPackageOfJspClass(String jsp) {
        if (jsp == null) {
            return "";
        }
        int i = jsp.lastIndexOf(47);
        if (i <= 0) {
            return "";
        }
        try {
            Class jspUtil = Loader.loadClass("org.apache.jasper.compiler.JspUtil");
            Method makeJavaPackage = jspUtil.getMethod("makeJavaPackage", String.class);
            String p = (String)makeJavaPackage.invoke(null, jsp.substring(0, i));
            return p;
        }
        catch (Exception e) {
            String tmp = jsp;
            int s = 0;
            if ('/' == tmp.charAt(0)) {
                s = 1;
            }
            tmp = tmp.substring(s, i);
            String string = tmp = ".".equals(tmp = tmp.replace('/', '.').trim()) ? "" : tmp;
            if (LOG.isDebugEnabled()) {
                LOG.warn("JspUtil.makeJavaPackage failed for " + jsp + " using " + tmp + " instead", new Object[0]);
                LOG.warn(e);
            }
            return tmp;
        }
    }

    public String getJspPackagePrefix() {
        String jspPackageName = null;
        if (this.getServletHandler() != null && this.getServletHandler().getServletContext() != null) {
            jspPackageName = this.getServletHandler().getServletContext().getInitParameter(JSP_GENERATED_PACKAGE_NAME);
        }
        if (jspPackageName == null) {
            jspPackageName = "org.apache.jsp";
        }
        return jspPackageName;
    }

    public String getClassNameForJsp(String jsp) {
        if (jsp == null) {
            return null;
        }
        String name = this.getNameOfJspClass(jsp);
        if (StringUtil.isBlank(name)) {
            return null;
        }
        StringBuffer fullName = new StringBuffer();
        this.appendPath(fullName, this.getJspPackagePrefix());
        this.appendPath(fullName, this.getPackageOfJspClass(jsp));
        this.appendPath(fullName, name);
        return fullName.toString();
    }

    protected void appendPath(StringBuffer path, String element) {
        if (StringUtil.isBlank(element)) {
            return;
        }
        if (path.length() > 0) {
            path.append(".");
        }
        path.append(element);
    }

    public ServletRegistration.Dynamic getRegistration() {
        if (this._registration == null) {
            this._registration = new Registration();
        }
        return this._registration;
    }

    protected Servlet newInstance() throws ServletException, IllegalAccessException, InstantiationException {
        try {
            ServletContext ctx = this.getServletHandler().getServletContext();
            if (ctx instanceof ServletContextHandler.Context) {
                return ((ServletContextHandler.Context)ctx).createServlet(this.getHeldClass());
            }
            return (Servlet)this.getHeldClass().newInstance();
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

    @Override
    public String toString() {
        return String.format("%s@%x==%s,jsp=%s,order=%d,inst=%b", this._name, this.hashCode(), this._className, this._forcedPath, this._initOrder, this._servlet != null);
    }

    private class SingleThreadedWrapper
    implements Servlet {
        Stack<Servlet> _stack = new Stack();

        private SingleThreadedWrapper() {
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public void destroy() {
            SingleThreadedWrapper singleThreadedWrapper = this;
            synchronized (singleThreadedWrapper) {
                while (this._stack.size() > 0) {
                    try {
                        this._stack.pop().destroy();
                    }
                    catch (Exception e) {
                        LOG.warn(e);
                    }
                }
            }
        }

        @Override
        public ServletConfig getServletConfig() {
            return ServletHolder.this._config;
        }

        @Override
        public String getServletInfo() {
            return null;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public void init(ServletConfig config) throws ServletException {
            SingleThreadedWrapper singleThreadedWrapper = this;
            synchronized (singleThreadedWrapper) {
                if (this._stack.size() == 0) {
                    try {
                        Servlet s = ServletHolder.this.newInstance();
                        s.init(config);
                        this._stack.push(s);
                    }
                    catch (ServletException e) {
                        throw e;
                    }
                    catch (Exception e) {
                        throw new ServletException(e);
                    }
                }
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
            Servlet s;
            SingleThreadedWrapper singleThreadedWrapper = this;
            synchronized (singleThreadedWrapper) {
                if (this._stack.size() > 0) {
                    s = this._stack.pop();
                } else {
                    try {
                        s = ServletHolder.this.newInstance();
                        s.init(ServletHolder.this._config);
                    }
                    catch (ServletException e) {
                        throw e;
                    }
                    catch (Exception e) {
                        throw new ServletException(e);
                    }
                }
            }
            try {
                s.service(req, res);
            }
            finally {
                singleThreadedWrapper = this;
                synchronized (singleThreadedWrapper) {
                    this._stack.push(s);
                }
            }
        }
    }

    public class Registration
    extends Holder.HolderRegistration
    implements ServletRegistration.Dynamic {
        protected MultipartConfigElement _multipartConfig;

        public Registration() {
            super(ServletHolder.this);
        }

        @Override
        public Set<String> addMapping(String ... urlPatterns) {
            ServletHolder.this.illegalStateIfContextStarted();
            HashSet<String> clash = null;
            for (String pattern : urlPatterns) {
                ServletMapping mapping = ServletHolder.this._servletHandler.getServletMapping(pattern);
                if (mapping == null || mapping.isDefault()) continue;
                if (clash == null) {
                    clash = new HashSet<String>();
                }
                clash.add(pattern);
            }
            if (clash != null) {
                return clash;
            }
            ServletMapping mapping = new ServletMapping(Source.JAVAX_API);
            mapping.setServletName(ServletHolder.this.getName());
            mapping.setPathSpecs(urlPatterns);
            ServletHolder.this._servletHandler.addServletMapping(mapping);
            return Collections.emptySet();
        }

        @Override
        public Collection<String> getMappings() {
            ServletMapping[] mappings = ServletHolder.this._servletHandler.getServletMappings();
            ArrayList<String> patterns = new ArrayList<String>();
            if (mappings != null) {
                for (ServletMapping mapping : mappings) {
                    String[] specs;
                    if (!mapping.getServletName().equals(this.getName()) || (specs = mapping.getPathSpecs()) == null || specs.length <= 0) continue;
                    patterns.addAll(Arrays.asList(specs));
                }
            }
            return patterns;
        }

        @Override
        public String getRunAsRole() {
            return ServletHolder.this._runAsRole;
        }

        @Override
        public void setLoadOnStartup(int loadOnStartup) {
            ServletHolder.this.illegalStateIfContextStarted();
            ServletHolder.this.setInitOrder(loadOnStartup);
        }

        public int getInitOrder() {
            return ServletHolder.this.getInitOrder();
        }

        @Override
        public void setMultipartConfig(MultipartConfigElement element) {
            this._multipartConfig = element;
        }

        public MultipartConfigElement getMultipartConfig() {
            return this._multipartConfig;
        }

        @Override
        public void setRunAsRole(String role) {
            ServletHolder.this._runAsRole = role;
        }

        @Override
        public Set<String> setServletSecurity(ServletSecurityElement securityElement) {
            return ServletHolder.this._servletHandler.setServletSecurity(this, securityElement);
        }
    }

    protected class Config
    extends Holder.HolderConfig
    implements ServletConfig {
        protected Config() {
            super(ServletHolder.this);
        }

        @Override
        public String getServletName() {
            return ServletHolder.this.getName();
        }
    }

    public static enum JspContainer {
        APACHE,
        OTHER;

    }
}

