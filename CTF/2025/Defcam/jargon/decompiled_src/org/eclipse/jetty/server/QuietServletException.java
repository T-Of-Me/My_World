/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server;

import javax.servlet.ServletException;
import org.eclipse.jetty.io.QuietException;

public class QuietServletException
extends ServletException
implements QuietException {
    public QuietServletException() {
    }

    public QuietServletException(String message, Throwable rootCause) {
        super(message, rootCause);
    }

    public QuietServletException(String message) {
        super(message);
    }

    public QuietServletException(Throwable rootCause) {
        super(rootCause);
    }
}

