/*
 * Decompiled with CFR 0.152.
 */
package javax.servlet.http;

import java.util.Enumeration;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionContext;

public interface HttpSession {
    public long getCreationTime();

    public String getId();

    public long getLastAccessedTime();

    public ServletContext getServletContext();

    public void setMaxInactiveInterval(int var1);

    public int getMaxInactiveInterval();

    public HttpSessionContext getSessionContext();

    public Object getAttribute(String var1);

    public Object getValue(String var1);

    public Enumeration<String> getAttributeNames();

    public String[] getValueNames();

    public void setAttribute(String var1, Object var2);

    public void putValue(String var1, Object var2);

    public void removeAttribute(String var1);

    public void removeValue(String var1);

    public void invalidate();

    public boolean isNew();
}

