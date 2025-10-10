/*
 * Decompiled with CFR 0.152.
 */
package javax.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletResponse;

public interface ServletRequest {
    public Object getAttribute(String var1);

    public Enumeration<String> getAttributeNames();

    public String getCharacterEncoding();

    public void setCharacterEncoding(String var1) throws UnsupportedEncodingException;

    public int getContentLength();

    public long getContentLengthLong();

    public String getContentType();

    public ServletInputStream getInputStream() throws IOException;

    public String getParameter(String var1);

    public Enumeration<String> getParameterNames();

    public String[] getParameterValues(String var1);

    public Map<String, String[]> getParameterMap();

    public String getProtocol();

    public String getScheme();

    public String getServerName();

    public int getServerPort();

    public BufferedReader getReader() throws IOException;

    public String getRemoteAddr();

    public String getRemoteHost();

    public void setAttribute(String var1, Object var2);

    public void removeAttribute(String var1);

    public Locale getLocale();

    public Enumeration<Locale> getLocales();

    public boolean isSecure();

    public RequestDispatcher getRequestDispatcher(String var1);

    public String getRealPath(String var1);

    public int getRemotePort();

    public String getLocalName();

    public String getLocalAddr();

    public int getLocalPort();

    public ServletContext getServletContext();

    public AsyncContext startAsync() throws IllegalStateException;

    public AsyncContext startAsync(ServletRequest var1, ServletResponse var2) throws IllegalStateException;

    public boolean isAsyncStarted();

    public boolean isAsyncSupported();

    public AsyncContext getAsyncContext();

    public DispatcherType getDispatcherType();
}

