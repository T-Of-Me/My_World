/*
 * Decompiled with CFR 0.152.
 */
package javax.servlet.http;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ResourceBundle;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.NoBodyOutputStream;

class NoBodyResponse
extends HttpServletResponseWrapper {
    private static final ResourceBundle lStrings = ResourceBundle.getBundle("javax.servlet.http.LocalStrings");
    private NoBodyOutputStream noBody = new NoBodyOutputStream();
    private PrintWriter writer;
    private boolean didSetContentLength;
    private boolean usingOutputStream;

    NoBodyResponse(HttpServletResponse r) {
        super(r);
    }

    void setContentLength() {
        if (!this.didSetContentLength) {
            if (this.writer != null) {
                this.writer.flush();
            }
            this.setContentLength(this.noBody.getContentLength());
        }
    }

    @Override
    public void setContentLength(int len) {
        super.setContentLength(len);
        this.didSetContentLength = true;
    }

    @Override
    public void setContentLengthLong(long len) {
        super.setContentLengthLong(len);
        this.didSetContentLength = true;
    }

    @Override
    public void setHeader(String name, String value) {
        super.setHeader(name, value);
        this.checkHeader(name);
    }

    @Override
    public void addHeader(String name, String value) {
        super.addHeader(name, value);
        this.checkHeader(name);
    }

    @Override
    public void setIntHeader(String name, int value) {
        super.setIntHeader(name, value);
        this.checkHeader(name);
    }

    @Override
    public void addIntHeader(String name, int value) {
        super.addIntHeader(name, value);
        this.checkHeader(name);
    }

    private void checkHeader(String name) {
        if ("content-length".equalsIgnoreCase(name)) {
            this.didSetContentLength = true;
        }
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (this.writer != null) {
            throw new IllegalStateException(lStrings.getString("err.ise.getOutputStream"));
        }
        this.usingOutputStream = true;
        return this.noBody;
    }

    @Override
    public PrintWriter getWriter() throws UnsupportedEncodingException {
        if (this.usingOutputStream) {
            throw new IllegalStateException(lStrings.getString("err.ise.getWriter"));
        }
        if (this.writer == null) {
            OutputStreamWriter w = new OutputStreamWriter((OutputStream)this.noBody, this.getCharacterEncoding());
            this.writer = new PrintWriter(w);
        }
        return this.writer;
    }
}

