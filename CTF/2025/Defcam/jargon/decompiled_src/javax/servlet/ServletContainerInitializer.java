/*
 * Decompiled with CFR 0.152.
 */
package javax.servlet;

import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

public interface ServletContainerInitializer {
    public void onStartup(Set<Class<?>> var1, ServletContext var2) throws ServletException;
}

