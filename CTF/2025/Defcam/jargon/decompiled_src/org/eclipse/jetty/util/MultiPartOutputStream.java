/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MultiPartOutputStream
extends FilterOutputStream {
    private static final byte[] __CRLF = new byte[]{13, 10};
    private static final byte[] __DASHDASH = new byte[]{45, 45};
    public static final String MULTIPART_MIXED = "multipart/mixed";
    public static final String MULTIPART_X_MIXED_REPLACE = "multipart/x-mixed-replace";
    private final String boundary;
    private final byte[] boundaryBytes;
    private boolean inPart = false;

    public MultiPartOutputStream(OutputStream out) throws IOException {
        super(out);
        this.boundary = "jetty" + System.identityHashCode(this) + Long.toString(System.currentTimeMillis(), 36);
        this.boundaryBytes = this.boundary.getBytes(StandardCharsets.ISO_8859_1);
    }

    public MultiPartOutputStream(OutputStream out, String boundary) throws IOException {
        super(out);
        this.boundary = boundary;
        this.boundaryBytes = boundary.getBytes(StandardCharsets.ISO_8859_1);
    }

    @Override
    public void close() throws IOException {
        try {
            if (this.inPart) {
                this.out.write(__CRLF);
            }
            this.out.write(__DASHDASH);
            this.out.write(this.boundaryBytes);
            this.out.write(__DASHDASH);
            this.out.write(__CRLF);
            this.inPart = false;
        }
        finally {
            super.close();
        }
    }

    public String getBoundary() {
        return this.boundary;
    }

    public OutputStream getOut() {
        return this.out;
    }

    public void startPart(String contentType) throws IOException {
        if (this.inPart) {
            this.out.write(__CRLF);
        }
        this.inPart = true;
        this.out.write(__DASHDASH);
        this.out.write(this.boundaryBytes);
        this.out.write(__CRLF);
        if (contentType != null) {
            this.out.write(("Content-Type: " + contentType).getBytes(StandardCharsets.ISO_8859_1));
        }
        this.out.write(__CRLF);
        this.out.write(__CRLF);
    }

    public void startPart(String contentType, String[] headers) throws IOException {
        if (this.inPart) {
            this.out.write(__CRLF);
        }
        this.inPart = true;
        this.out.write(__DASHDASH);
        this.out.write(this.boundaryBytes);
        this.out.write(__CRLF);
        if (contentType != null) {
            this.out.write(("Content-Type: " + contentType).getBytes(StandardCharsets.ISO_8859_1));
        }
        this.out.write(__CRLF);
        for (int i = 0; headers != null && i < headers.length; ++i) {
            this.out.write(headers[i].getBytes(StandardCharsets.ISO_8859_1));
            this.out.write(__CRLF);
        }
        this.out.write(__CRLF);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        this.out.write(b, off, len);
    }
}

