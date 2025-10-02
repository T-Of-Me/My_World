/*
 * Decompiled with CFR 0.152.
 */
package javax.servlet.http;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

class NoBodyOutputStream
extends ServletOutputStream {
    private static final String LSTRING_FILE = "javax.servlet.http.LocalStrings";
    private static ResourceBundle lStrings = ResourceBundle.getBundle("javax.servlet.http.LocalStrings");
    private int contentLength = 0;

    NoBodyOutputStream() {
    }

    int getContentLength() {
        return this.contentLength;
    }

    @Override
    public void write(int b) {
        ++this.contentLength;
    }

    @Override
    public void write(byte[] buf, int offset, int len) throws IOException {
        if (buf == null) {
            throw new NullPointerException(lStrings.getString("err.io.nullArray"));
        }
        if (offset < 0 || len < 0 || offset + len > buf.length) {
            String msg = lStrings.getString("err.io.indexOutOfBounds");
            Object[] msgArgs = new Object[]{offset, len, buf.length};
            msg = MessageFormat.format(msg, msgArgs);
            throw new IndexOutOfBoundsException(msg);
        }
        this.contentLength += len;
    }

    @Override
    public boolean isReady() {
        return false;
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
    }
}

