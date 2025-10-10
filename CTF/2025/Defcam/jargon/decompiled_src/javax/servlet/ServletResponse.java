/*
 * Decompiled with CFR 0.152.
 */
package javax.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import javax.servlet.ServletOutputStream;

public interface ServletResponse {
    public String getCharacterEncoding();

    public String getContentType();

    public ServletOutputStream getOutputStream() throws IOException;

    public PrintWriter getWriter() throws IOException;

    public void setCharacterEncoding(String var1);

    public void setContentLength(int var1);

    public void setContentLengthLong(long var1);

    public void setContentType(String var1);

    public void setBufferSize(int var1);

    public int getBufferSize();

    public void flushBuffer() throws IOException;

    public void resetBuffer();

    public boolean isCommitted();

    public void reset();

    public void setLocale(Locale var1);

    public Locale getLocale();
}

