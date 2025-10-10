/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

public class RolloverFileOutputStream
extends FilterOutputStream {
    private static Timer __rollover;
    static final String YYYY_MM_DD = "yyyy_mm_dd";
    static final String ROLLOVER_FILE_DATE_FORMAT = "yyyy_MM_dd";
    static final String ROLLOVER_FILE_BACKUP_FORMAT = "HHmmssSSS";
    static final int ROLLOVER_FILE_RETAIN_DAYS = 31;
    private RollTask _rollTask;
    private SimpleDateFormat _fileBackupFormat;
    private SimpleDateFormat _fileDateFormat;
    private String _filename;
    private File _file;
    private boolean _append;
    private int _retainDays;

    public RolloverFileOutputStream(String filename) throws IOException {
        this(filename, true, 31);
    }

    public RolloverFileOutputStream(String filename, boolean append) throws IOException {
        this(filename, append, 31);
    }

    public RolloverFileOutputStream(String filename, boolean append, int retainDays) throws IOException {
        this(filename, append, retainDays, TimeZone.getDefault());
    }

    public RolloverFileOutputStream(String filename, boolean append, int retainDays, TimeZone zone) throws IOException {
        this(filename, append, retainDays, zone, null, null, ZonedDateTime.now(zone.toZoneId()));
    }

    public RolloverFileOutputStream(String filename, boolean append, int retainDays, TimeZone zone, String dateFormat, String backupFormat) throws IOException {
        this(filename, append, retainDays, zone, dateFormat, backupFormat, ZonedDateTime.now(zone.toZoneId()));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    RolloverFileOutputStream(String filename, boolean append, int retainDays, TimeZone zone, String dateFormat, String backupFormat, ZonedDateTime now) throws IOException {
        super(null);
        if (dateFormat == null) {
            dateFormat = ROLLOVER_FILE_DATE_FORMAT;
        }
        this._fileDateFormat = new SimpleDateFormat(dateFormat);
        if (backupFormat == null) {
            backupFormat = ROLLOVER_FILE_BACKUP_FORMAT;
        }
        this._fileBackupFormat = new SimpleDateFormat(backupFormat);
        this._fileBackupFormat.setTimeZone(zone);
        this._fileDateFormat.setTimeZone(zone);
        if (filename != null && (filename = filename.trim()).length() == 0) {
            filename = null;
        }
        if (filename == null) {
            throw new IllegalArgumentException("Invalid filename");
        }
        this._filename = filename;
        this._append = append;
        this._retainDays = retainDays;
        Class<RolloverFileOutputStream> clazz = RolloverFileOutputStream.class;
        synchronized (RolloverFileOutputStream.class) {
            if (__rollover == null) {
                __rollover = new Timer(RolloverFileOutputStream.class.getName(), true);
            }
            this.setFile(now);
            this.scheduleNextRollover(now);
            // ** MonitorExit[var8_8] (shouldn't be in output)
            return;
        }
    }

    public static ZonedDateTime toMidnight(ZonedDateTime now) {
        return now.toLocalDate().atStartOfDay(now.getZone()).plus(1L, ChronoUnit.DAYS);
    }

    private void scheduleNextRollover(ZonedDateTime now) {
        this._rollTask = new RollTask();
        ZonedDateTime midnight = RolloverFileOutputStream.toMidnight(now);
        long delay = midnight.toInstant().toEpochMilli() - now.toInstant().toEpochMilli();
        __rollover.schedule((TimerTask)this._rollTask, delay);
    }

    public String getFilename() {
        return this._filename;
    }

    public String getDatedFilename() {
        if (this._file == null) {
            return null;
        }
        return this._file.toString();
    }

    public int getRetainDays() {
        return this._retainDays;
    }

    synchronized void setFile(ZonedDateTime now) throws IOException {
        File file = new File(this._filename);
        this._filename = file.getCanonicalPath();
        file = new File(this._filename);
        File dir = new File(file.getParent());
        if (!dir.isDirectory() || !dir.canWrite()) {
            throw new IOException("Cannot write log directory " + dir);
        }
        String filename = file.getName();
        int i = filename.toLowerCase(Locale.ENGLISH).indexOf(YYYY_MM_DD);
        if (i >= 0) {
            file = new File(dir, filename.substring(0, i) + this._fileDateFormat.format(new Date(now.toInstant().toEpochMilli())) + filename.substring(i + YYYY_MM_DD.length()));
        }
        if (file.exists() && !file.canWrite()) {
            throw new IOException("Cannot write log file " + file);
        }
        if (this.out == null || !file.equals(this._file)) {
            this._file = file;
            if (!this._append && file.exists()) {
                file.renameTo(new File(file.toString() + "." + this._fileBackupFormat.format(new Date(now.toInstant().toEpochMilli()))));
            }
            OutputStream oldOut = this.out;
            this.out = new FileOutputStream(file.toString(), this._append);
            if (oldOut != null) {
                oldOut.close();
            }
        }
    }

    void removeOldFiles(ZonedDateTime now) {
        if (this._retainDays > 0) {
            long expired = now.minus(this._retainDays, ChronoUnit.DAYS).toInstant().toEpochMilli();
            File file = new File(this._filename);
            File dir = new File(file.getParent());
            String fn = file.getName();
            int s = fn.toLowerCase(Locale.ENGLISH).indexOf(YYYY_MM_DD);
            if (s < 0) {
                return;
            }
            String prefix = fn.substring(0, s);
            String suffix = fn.substring(s + YYYY_MM_DD.length());
            String[] logList = dir.list();
            for (int i = 0; i < logList.length; ++i) {
                File f;
                fn = logList[i];
                if (!fn.startsWith(prefix) || fn.indexOf(suffix, prefix.length()) < 0 || (f = new File(dir, fn)).lastModified() >= expired) continue;
                f.delete();
            }
        }
    }

    @Override
    public void write(byte[] buf) throws IOException {
        this.out.write(buf);
    }

    @Override
    public void write(byte[] buf, int off, int len) throws IOException {
        this.out.write(buf, off, len);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void close() throws IOException {
        Class<RolloverFileOutputStream> clazz = RolloverFileOutputStream.class;
        synchronized (RolloverFileOutputStream.class) {
            try {
                super.close();
            }
            finally {
                this.out = null;
                this._file = null;
            }
            if (this._rollTask != null) {
                this._rollTask.cancel();
            }
            // ** MonitorExit[var1_1] (shouldn't be in output)
            return;
        }
    }

    private class RollTask
    extends TimerTask {
        private RollTask() {
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public void run() {
            try {
                Class<RolloverFileOutputStream> clazz = RolloverFileOutputStream.class;
                synchronized (RolloverFileOutputStream.class) {
                    ZonedDateTime now = ZonedDateTime.now(RolloverFileOutputStream.this._fileDateFormat.getTimeZone().toZoneId());
                    RolloverFileOutputStream.this.setFile(now);
                    RolloverFileOutputStream.this.scheduleNextRollover(now);
                    RolloverFileOutputStream.this.removeOldFiles(now);
                    // ** MonitorExit[var1_1] (shouldn't be in output)
                }
            }
            catch (Throwable t) {
                t.printStackTrace(System.err);
            }
            {
                return;
            }
        }
    }
}

