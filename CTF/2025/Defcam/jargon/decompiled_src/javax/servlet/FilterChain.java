/*
 * Decompiled with CFR 0.152.
 */
package javax.servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public interface FilterChain {
    public void doFilter(ServletRequest var1, ServletResponse var2) throws IOException, ServletException;
}

