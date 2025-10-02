/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;

public class WriterOutputStream
extends OutputStream {
    protected final Writer _writer;
    protected final Charset _encoding;
    private final byte[] _buf = new byte[1];

    public WriterOutputStream(Writer writer, String encoding) {
        this._writer = writer;
        this._encoding = encoding == null ? null : Charset.forName(encoding);
    }

    public WriterOutputStream(Writer writer) {
        this._writer = writer;
        this._encoding = null;
    }

    @Override
    public void close() throws IOException {
        this._writer.close();
    }

    @Override
    public void flush() throws IOException {
        this._writer.flush();
    }

    @Override
    public void write(byte[] b) throws IOException {
        if (this._encoding == null) {
            this._writer.write(new String(b));
        } else {
            this._writer.write(new String(b, this._encoding));
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (this._encoding == null) {
            this._writer.write(new String(b, off, len));
        } else {
            this._writer.write(new String(b, off, len, this._encoding));
        }
    }

    @Override
    public synchronized void write(int b) throws IOException {
        this._buf[0] = (byte)b;
        this.write(this._buf);
    }
}

