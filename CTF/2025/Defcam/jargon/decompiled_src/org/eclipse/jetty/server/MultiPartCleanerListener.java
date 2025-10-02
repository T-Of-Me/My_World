/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server;

import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.MultiException;
import org.eclipse.jetty.util.MultiPartInputStreamParser;

public class MultiPartCleanerListener
implements ServletRequestListener {
    public static final MultiPartCleanerListener INSTANCE = new MultiPartCleanerListener();

    protected MultiPartCleanerListener() {
    }

    @Override
    public void requestDestroyed(ServletRequestEvent sre) {
        ContextHandler.Context context;
        MultiPartInputStreamParser mpis = (MultiPartInputStreamParser)sre.getServletRequest().getAttribute("org.eclipse.jetty.multiPartInputStream");
        if (mpis != null && (context = (ContextHandler.Context)sre.getServletRequest().getAttribute("org.eclipse.jetty.multiPartContext")) == sre.getServletContext()) {
            try {
                mpis.deleteParts();
            }
            catch (MultiException e) {
                sre.getServletContext().log("Errors deleting multipart tmp files", e);
            }
        }
    }

    @Override
    public void requestInitialized(ServletRequestEvent sre) {
    }
}

