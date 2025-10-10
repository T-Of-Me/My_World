/*
 * Decompiled with CFR 0.152.
 */
package javax.servlet.http;

import java.io.IOException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

public interface HttpServletRequest
extends ServletRequest {
    public static final String BASIC_AUTH = "BASIC";
    public static final String FORM_AUTH = "FORM";
    public static final String CLIENT_CERT_AUTH = "CLIENT_CERT";
    public static final String DIGEST_AUTH = "DIGEST";

    public String getAuthType();

    public Cookie[] getCookies();

    public long getDateHeader(String var1);

    public String getHeader(String var1);

    public Enumeration<String> getHeaders(String var1);

    public Enumeration<String> getHeaderNames();

    public int getIntHeader(String var1);

    public String getMethod();

    public String getPathInfo();

    public String getPathTranslated();

    public String getContextPath();

    public String getQueryString();

    public String getRemoteUser();

    public boolean isUserInRole(String var1);

    public Principal getUserPrincipal();

    public String getRequestedSessionId();

    public String getRequestURI();

    public StringBuffer getRequestURL();

    public String getServletPath();

    public HttpSession getSession(boolean var1);

    public HttpSession getSession();

    public String changeSessionId();

    public boolean isRequestedSessionIdValid();

    public boolean isRequestedSessionIdFromCookie();

    public boolean isRequestedSessionIdFromURL();

    public boolean isRequestedSessionIdFromUrl();

    public boolean authenticate(HttpServletResponse var1) throws IOException, ServletException;

    public void login(String var1, String var2) throws ServletException;

    public void logout() throws ServletException;

    public Collection<Part> getParts() throws IOException, ServletException;

    public Part getPart(String var1) throws IOException, ServletException;

    public <T extends HttpUpgradeHandler> T upgrade(Class<T> var1) throws IOException, ServletException;
}

