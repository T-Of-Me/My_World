/*
 * Decompiled with CFR 0.152.
 */
package javax.servlet;

import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ReadListener;

public abstract class ServletInputStream
extends InputStream {
    protected ServletInputStream() {
    }

    public int readLine(byte[] b, int off, int len) throws IOException {
        int c;
        if (len <= 0) {
            return 0;
        }
        int count = 0;
        while ((c = this.read()) != -1) {
            b[off++] = (byte)c;
            if (c != 10 && ++count != len) continue;
        }
        return count > 0 ? count : -1;
    }

    public abstract boolean isFinished();

    public abstract boolean isReady();

    public abstract void setReadListener(ReadListener var1);
}

