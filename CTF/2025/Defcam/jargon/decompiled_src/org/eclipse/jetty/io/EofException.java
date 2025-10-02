/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.io;

import java.io.EOFException;
import org.eclipse.jetty.io.QuietException;

public class EofException
extends EOFException
implements QuietException {
    public EofException() {
    }

    public EofException(String reason) {
        super(reason);
    }

    public EofException(Throwable th) {
        if (th != null) {
            this.initCause(th);
        }
    }
}

