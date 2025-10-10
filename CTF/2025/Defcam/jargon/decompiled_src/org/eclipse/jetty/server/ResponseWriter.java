/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Formatter;
import java.util.Locale;
import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.io.RuntimeIOException;
import org.eclipse.jetty.server.HttpWriter;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class ResponseWriter
extends PrintWriter {
    private static final Logger LOG = Log.getLogger(ResponseWriter.class);
    private static final String __lineSeparator = System.getProperty("line.separator");
    private static final String __trueln = "true" + __lineSeparator;
    private static final String __falseln = "false" + __lineSeparator;
    private final HttpWriter _httpWriter;
    private final Locale _locale;
    private final String _encoding;
    private IOException _ioException;
    private boolean _isClosed = false;
    private Formatter _formatter;

    public ResponseWriter(HttpWriter httpWriter, Locale locale, String encoding) {
        super((Writer)httpWriter, false);
        this._httpWriter = httpWriter;
        this._locale = locale;
        this._encoding = encoding;
    }

    public boolean isFor(Locale locale, String encoding) {
        if (this._locale == null && locale != null) {
            return false;
        }
        if (this._encoding == null && encoding != null) {
            return false;
        }
        return this._encoding.equalsIgnoreCase(encoding) && this._locale.equals(locale);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    protected void reopen() {
        Object object = this.lock;
        synchronized (object) {
            this._isClosed = false;
            this.clearError();
            this.out = this._httpWriter;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    protected void clearError() {
        Object object = this.lock;
        synchronized (object) {
            this._ioException = null;
            super.clearError();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public boolean checkError() {
        Object object = this.lock;
        synchronized (object) {
            return this._ioException != null || super.checkError();
        }
    }

    private void setError(Throwable th) {
        super.setError();
        if (th instanceof IOException) {
            this._ioException = (IOException)th;
        } else {
            this._ioException = new IOException(String.valueOf(th));
            this._ioException.initCause(th);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug(th);
        }
    }

    @Override
    protected void setError() {
        this.setError(new IOException());
    }

    private void isOpen() throws IOException {
        if (this._ioException != null) {
            throw new RuntimeIOException(this._ioException);
        }
        if (this._isClosed) {
            throw new EofException("Stream closed");
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void flush() {
        try {
            Object object = this.lock;
            synchronized (object) {
                this.isOpen();
                this.out.flush();
            }
        }
        catch (IOException ex) {
            this.setError(ex);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void close() {
        try {
            Object object = this.lock;
            synchronized (object) {
                this.out.close();
                this._isClosed = true;
            }
        }
        catch (IOException ex) {
            this.setError(ex);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void write(int c) {
        try {
            Object object = this.lock;
            synchronized (object) {
                this.isOpen();
                this.out.write(c);
            }
        }
        catch (InterruptedIOException ex) {
            LOG.debug(ex);
            Thread.currentThread().interrupt();
        }
        catch (IOException ex) {
            this.setError(ex);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void write(char[] buf, int off, int len) {
        try {
            Object object = this.lock;
            synchronized (object) {
                this.isOpen();
                this.out.write(buf, off, len);
            }
        }
        catch (InterruptedIOException ex) {
            LOG.debug(ex);
            Thread.currentThread().interrupt();
        }
        catch (IOException ex) {
            this.setError(ex);
        }
    }

    @Override
    public void write(char[] buf) {
        this.write(buf, 0, buf.length);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void write(String s, int off, int len) {
        try {
            Object object = this.lock;
            synchronized (object) {
                this.isOpen();
                this.out.write(s, off, len);
            }
        }
        catch (InterruptedIOException ex) {
            LOG.debug(ex);
            Thread.currentThread().interrupt();
        }
        catch (IOException ex) {
            this.setError(ex);
        }
    }

    @Override
    public void write(String s) {
        this.write(s, 0, s.length());
    }

    @Override
    public void print(boolean b) {
        this.write(b ? "true" : "false");
    }

    @Override
    public void print(char c) {
        this.write(c);
    }

    @Override
    public void print(int i) {
        this.write(String.valueOf(i));
    }

    @Override
    public void print(long l) {
        this.write(String.valueOf(l));
    }

    @Override
    public void print(float f) {
        this.write(String.valueOf(f));
    }

    @Override
    public void print(double d) {
        this.write(String.valueOf(d));
    }

    @Override
    public void print(char[] s) {
        this.write(s);
    }

    @Override
    public void print(String s) {
        if (s == null) {
            s = "null";
        }
        this.write(s);
    }

    @Override
    public void print(Object obj) {
        this.write(String.valueOf(obj));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void println() {
        try {
            Object object = this.lock;
            synchronized (object) {
                this.isOpen();
                this.out.write(__lineSeparator);
            }
        }
        catch (InterruptedIOException ex) {
            LOG.debug(ex);
            Thread.currentThread().interrupt();
        }
        catch (IOException ex) {
            this.setError(ex);
        }
    }

    @Override
    public void println(boolean b) {
        this.println(b ? __trueln : __falseln);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void println(char c) {
        try {
            Object object = this.lock;
            synchronized (object) {
                this.isOpen();
                this.out.write(c);
            }
        }
        catch (InterruptedIOException ex) {
            LOG.debug(ex);
            Thread.currentThread().interrupt();
        }
        catch (IOException ex) {
            this.setError(ex);
        }
    }

    @Override
    public void println(int x) {
        this.println(String.valueOf(x));
    }

    @Override
    public void println(long x) {
        this.println(String.valueOf(x));
    }

    @Override
    public void println(float x) {
        this.println(String.valueOf(x));
    }

    @Override
    public void println(double x) {
        this.println(String.valueOf(x));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void println(char[] s) {
        try {
            Object object = this.lock;
            synchronized (object) {
                this.isOpen();
                this.out.write(s, 0, s.length);
                this.out.write(__lineSeparator);
            }
        }
        catch (InterruptedIOException ex) {
            LOG.debug(ex);
            Thread.currentThread().interrupt();
        }
        catch (IOException ex) {
            this.setError(ex);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void println(String s) {
        if (s == null) {
            s = "null";
        }
        try {
            Object object = this.lock;
            synchronized (object) {
                this.isOpen();
                this.out.write(s, 0, s.length());
                this.out.write(__lineSeparator);
            }
        }
        catch (InterruptedIOException ex) {
            LOG.debug(ex);
            Thread.currentThread().interrupt();
        }
        catch (IOException ex) {
            this.setError(ex);
        }
    }

    @Override
    public void println(Object x) {
        this.println(String.valueOf(x));
    }

    @Override
    public PrintWriter printf(String format, Object ... args) {
        return this.format(this._locale, format, args);
    }

    @Override
    public PrintWriter printf(Locale l, String format, Object ... args) {
        return this.format(l, format, args);
    }

    @Override
    public PrintWriter format(String format, Object ... args) {
        return this.format(this._locale, format, args);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public PrintWriter format(Locale l, String format, Object ... args) {
        try {
            Object object = this.lock;
            synchronized (object) {
                this.isOpen();
                if (this._formatter == null || this._formatter.locale() != l) {
                    this._formatter = new Formatter(this, l);
                }
                this._formatter.format(l, format, args);
            }
        }
        catch (InterruptedIOException ex) {
            LOG.debug(ex);
            Thread.currentThread().interrupt();
        }
        catch (IOException ex) {
            this.setError(ex);
        }
        return this;
    }
}

