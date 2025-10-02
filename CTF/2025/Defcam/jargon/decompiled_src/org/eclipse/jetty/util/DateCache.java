/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateCache {
    public static final String DEFAULT_FORMAT = "EEE MMM dd HH:mm:ss zzz yyyy";
    private final String _formatString;
    private final String _tzFormatString;
    private final SimpleDateFormat _tzFormat;
    private final Locale _locale;
    private volatile Tick _tick;

    public DateCache() {
        this(DEFAULT_FORMAT);
    }

    public DateCache(String format) {
        this(format, null, TimeZone.getDefault());
    }

    public DateCache(String format, Locale l) {
        this(format, l, TimeZone.getDefault());
    }

    public DateCache(String format, Locale l, String tz) {
        this(format, l, TimeZone.getTimeZone(tz));
    }

    public DateCache(String format, Locale l, TimeZone tz) {
        this._formatString = format;
        this._locale = l;
        int zIndex = this._formatString.indexOf("ZZZ");
        if (zIndex >= 0) {
            String ss1 = this._formatString.substring(0, zIndex);
            String ss2 = this._formatString.substring(zIndex + 3);
            int tzOffset = tz.getRawOffset();
            StringBuilder sb = new StringBuilder(this._formatString.length() + 10);
            sb.append(ss1);
            sb.append("'");
            if (tzOffset >= 0) {
                sb.append('+');
            } else {
                tzOffset = -tzOffset;
                sb.append('-');
            }
            int raw = tzOffset / 60000;
            int hr = raw / 60;
            int min = raw % 60;
            if (hr < 10) {
                sb.append('0');
            }
            sb.append(hr);
            if (min < 10) {
                sb.append('0');
            }
            sb.append(min);
            sb.append('\'');
            sb.append(ss2);
            this._tzFormatString = sb.toString();
        } else {
            this._tzFormatString = this._formatString;
        }
        this._tzFormat = this._locale != null ? new SimpleDateFormat(this._tzFormatString, this._locale) : new SimpleDateFormat(this._tzFormatString);
        this._tzFormat.setTimeZone(tz);
        this._tick = null;
    }

    public TimeZone getTimeZone() {
        return this._tzFormat.getTimeZone();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public String format(Date inDate) {
        long seconds = inDate.getTime() / 1000L;
        Tick tick = this._tick;
        if (tick == null || seconds != tick._seconds) {
            DateCache dateCache = this;
            synchronized (dateCache) {
                return this._tzFormat.format(inDate);
            }
        }
        return tick._string;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public String format(long inDate) {
        long seconds = inDate / 1000L;
        Tick tick = this._tick;
        if (tick == null || seconds != tick._seconds) {
            Date d = new Date(inDate);
            DateCache dateCache = this;
            synchronized (dateCache) {
                return this._tzFormat.format(d);
            }
        }
        return tick._string;
    }

    public String formatNow(long now) {
        long seconds = now / 1000L;
        Tick tick = this._tick;
        if (tick != null && tick._seconds == seconds) {
            return tick._string;
        }
        return this.formatTick((long)now)._string;
    }

    public String now() {
        return this.formatNow(System.currentTimeMillis());
    }

    public Tick tick() {
        return this.formatTick(System.currentTimeMillis());
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    protected Tick formatTick(long now) {
        long seconds = now / 1000L;
        DateCache dateCache = this;
        synchronized (dateCache) {
            if (this._tick == null || this._tick._seconds != seconds) {
                String s = this._tzFormat.format(new Date(now));
                this._tick = new Tick(seconds, s);
                return this._tick;
            }
            return this._tick;
        }
    }

    public String getFormatString() {
        return this._formatString;
    }

    public static class Tick {
        final long _seconds;
        final String _string;

        public Tick(long seconds, String string) {
            this._seconds = seconds;
            this._string = string;
        }
    }
}

