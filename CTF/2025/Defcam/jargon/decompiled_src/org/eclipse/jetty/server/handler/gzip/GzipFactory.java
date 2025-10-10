/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.handler.gzip;

import java.util.zip.Deflater;
import org.eclipse.jetty.server.Request;

public interface GzipFactory {
    public Deflater getDeflater(Request var1, long var2);

    public boolean isMimeTypeGzipable(String var1);

    public void recycle(Deflater var1);
}

