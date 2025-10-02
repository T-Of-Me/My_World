/*
 * Decompiled with CFR 0.152.
 */
package javax.servlet;

import java.io.IOException;
import java.util.EventListener;

public interface WriteListener
extends EventListener {
    public void onWritePossible() throws IOException;

    public void onError(Throwable var1);
}

