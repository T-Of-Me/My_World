/*
 * Decompiled with CFR 0.152.
 */
package javax.servlet;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Map;
import java.util.Set;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;

public interface ServletContext {
    public static final String TEMPDIR = "javax.servlet.context.tempdir";
    public static final String ORDERED_LIBS = "javax.servlet.context.orderedLibs";

    public String getContextPath();

    public ServletContext getContext(String var1);

    public int getMajorVersion();

    public int getMinorVersion();

    public int getEffectiveMajorVersion();

    public int getEffectiveMinorVersion();

    public String getMimeType(String var1);

    public Set<String> getResourcePaths(String var1);

    public URL getResource(String var1) throws MalformedURLException;

    public InputStream getResourceAsStream(String var1);

    public RequestDispatcher getRequestDispatcher(String var1);

    public RequestDispatcher getNamedDispatcher(String var1);

    public Servlet getServlet(String var1) throws ServletException;

    public Enumeration<Servlet> getServlets();

    public Enumeration<String> getServletNames();

    public void log(String var1);

    public void log(Exception var1, String var2);

    public void log(String var1, Throwable var2);

    public String getRealPath(String var1);

    public String getServerInfo();

    public String getInitParameter(String var1);

    public Enumeration<String> getInitParameterNames();

    public boolean setInitParameter(String var1, String var2);

    public Object getAttribute(String var1);

    public Enumeration<String> getAttributeNames();

    public void setAttribute(String var1, Object var2);

    public void removeAttribute(String var1);

    public String getServletContextName();

    public ServletRegistration.Dynamic addServlet(String var1, String var2);

    public ServletRegistration.Dynamic addServlet(String var1, Servlet var2);

    public ServletRegistration.Dynamic addServlet(String var1, Class<? extends Servlet> var2);

    public <T extends Servlet> T createServlet(Class<T> var1) throws ServletException;

    public ServletRegistration getServletRegistration(String var1);

    public Map<String, ? extends ServletRegistration> getServletRegistrations();

    public FilterRegistration.Dynamic addFilter(String var1, String var2);

    public FilterRegistration.Dynamic addFilter(String var1, Filter var2);

    public FilterRegistration.Dynamic addFilter(String var1, Class<? extends Filter> var2);

    public <T extends Filter> T createFilter(Class<T> var1) throws ServletException;

    public FilterRegistration getFilterRegistration(String var1);

    public Map<String, ? extends FilterRegistration> getFilterRegistrations();

    public SessionCookieConfig getSessionCookieConfig();

    public void setSessionTrackingModes(Set<SessionTrackingMode> var1);

    public Set<SessionTrackingMode> getDefaultSessionTrackingModes();

    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes();

    public void addListener(String var1);

    public <T extends EventListener> void addListener(T var1);

    public void addListener(Class<? extends EventListener> var1);

    public <T extends EventListener> T createListener(Class<T> var1) throws ServletException;

    public JspConfigDescriptor getJspConfigDescriptor();

    public ClassLoader getClassLoader();

    public void declareRoles(String ... var1);

    public String getVirtualServerName();
}

