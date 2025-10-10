/*
 * Decompiled with CFR 0.152.
 */
package javax.servlet;

public interface SessionCookieConfig {
    public void setName(String var1);

    public String getName();

    public void setDomain(String var1);

    public String getDomain();

    public void setPath(String var1);

    public String getPath();

    public void setComment(String var1);

    public String getComment();

    public void setHttpOnly(boolean var1);

    public boolean isHttpOnly();

    public void setSecure(boolean var1);

    public boolean isSecure();

    public void setMaxAge(int var1);

    public int getMaxAge();
}

