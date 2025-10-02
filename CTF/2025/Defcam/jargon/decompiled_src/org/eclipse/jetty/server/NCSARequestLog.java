/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.TimeZone;
import org.eclipse.jetty.server.AbstractNCSARequestLog;
import org.eclipse.jetty.util.RolloverFileOutputStream;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;

@ManagedObject(value="NCSA standard format request log")
public class NCSARequestLog
extends AbstractNCSARequestLog {
    private String _filename;
    private boolean _append;
    private int _retainDays;
    private boolean _closeOut;
    private String _filenameDateFormat = null;
    private transient OutputStream _out;
    private transient OutputStream _fileOut;
    private transient Writer _writer;

    public NCSARequestLog() {
        this.setExtended(true);
        this._append = true;
        this._retainDays = 31;
    }

    public NCSARequestLog(String filename) {
        this.setExtended(true);
        this._append = true;
        this._retainDays = 31;
        this.setFilename(filename);
    }

    public void setFilename(String filename) {
        if (filename != null && (filename = filename.trim()).length() == 0) {
            filename = null;
        }
        this._filename = filename;
    }

    @ManagedAttribute(value="file of log")
    public String getFilename() {
        return this._filename;
    }

    public String getDatedFilename() {
        if (this._fileOut instanceof RolloverFileOutputStream) {
            return ((RolloverFileOutputStream)this._fileOut).getDatedFilename();
        }
        return null;
    }

    @Override
    protected boolean isEnabled() {
        return this._fileOut != null;
    }

    public void setRetainDays(int retainDays) {
        this._retainDays = retainDays;
    }

    @ManagedAttribute(value="number of days that log files are kept")
    public int getRetainDays() {
        return this._retainDays;
    }

    public void setAppend(boolean append) {
        this._append = append;
    }

    @ManagedAttribute(value="existing log files are appends to the new one")
    public boolean isAppend() {
        return this._append;
    }

    public void setFilenameDateFormat(String logFileDateFormat) {
        this._filenameDateFormat = logFileDateFormat;
    }

    public String getFilenameDateFormat() {
        return this._filenameDateFormat;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void write(String requestEntry) throws IOException {
        NCSARequestLog nCSARequestLog = this;
        synchronized (nCSARequestLog) {
            if (this._writer == null) {
                return;
            }
            this._writer.write(requestEntry);
            this._writer.write(StringUtil.__LINE_SEPARATOR);
            this._writer.flush();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    protected synchronized void doStart() throws Exception {
        if (this._filename != null) {
            this._fileOut = new RolloverFileOutputStream(this._filename, this._append, this._retainDays, TimeZone.getTimeZone(this.getLogTimeZone()), this._filenameDateFormat, null);
            this._closeOut = true;
            LOG.info("Opened " + this.getDatedFilename(), new Object[0]);
        } else {
            this._fileOut = System.err;
        }
        this._out = this._fileOut;
        NCSARequestLog nCSARequestLog = this;
        synchronized (nCSARequestLog) {
            this._writer = new OutputStreamWriter(this._out);
        }
        super.doStart();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    protected void doStop() throws Exception {
        NCSARequestLog nCSARequestLog = this;
        synchronized (nCSARequestLog) {
            super.doStop();
            try {
                if (this._writer != null) {
                    this._writer.flush();
                }
            }
            catch (IOException e) {
                LOG.ignore(e);
            }
            if (this._out != null && this._closeOut) {
                try {
                    this._out.close();
                }
                catch (IOException e) {
                    LOG.ignore(e);
                }
            }
            this._out = null;
            this._fileOut = null;
            this._closeOut = false;
            this._writer = null;
        }
    }
}

