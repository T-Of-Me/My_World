/*
 * Decompiled with CFR 0.152.
 */
package javax.servlet;

import java.io.IOException;
import java.util.EventListener;

public interface ReadListener
extends EventListener {
    public void onDataAvailable() throws IOException;

    public void onAllDataRead() throws IOException;

    public void onError(Throwable var1);
}

