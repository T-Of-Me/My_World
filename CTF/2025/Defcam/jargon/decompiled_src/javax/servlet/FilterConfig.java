/*
 * Decompiled with CFR 0.152.
 */
package javax.servlet;

import java.util.Enumeration;
import javax.servlet.ServletContext;

public interface FilterConfig {
    public String getFilterName();

    public ServletContext getServletContext();

    public String getInitParameter(String var1);

    public Enumeration<String> getInitParameterNames();
}

