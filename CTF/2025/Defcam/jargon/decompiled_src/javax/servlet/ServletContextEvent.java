/*
 * Decompiled with CFR 0.152.
 */
package javax.servlet;

import java.util.EventObject;
import javax.servlet.ServletContext;

public class ServletContextEvent
extends EventObject {
    private static final long serialVersionUID = -7501701636134222423L;

    public ServletContextEvent(ServletContext source) {
        super(source);
    }

    public ServletContext getServletContext() {
        return (ServletContext)super.getSource();
    }
}

