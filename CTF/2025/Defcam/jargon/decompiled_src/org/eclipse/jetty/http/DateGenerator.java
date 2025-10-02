/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.http;

import java.util.GregorianCalendar;
import java.util.TimeZone;
import org.eclipse.jetty.util.StringUtil;

public class DateGenerator {
    private static final TimeZone __GMT = TimeZone.getTimeZone("GMT");
    static final String[] DAYS;
    static final String[] MONTHS;
    private static final ThreadLocal<DateGenerator> __dateGenerator;
    public static final String __01Jan1970;
    private final StringBuilder buf = new StringBuilder(32);
    private final GregorianCalendar gc = new GregorianCalendar(__GMT);

    public static String formatDate(long date) {
        return __dateGenerator.get().doFormatDate(date);
    }

    public static void formatCookieDate(StringBuilder buf, long date) {
        __dateGenerator.get().doFormatCookieDate(buf, date);
    }

    public static String formatCookieDate(long date) {
        StringBuilder buf = new StringBuilder(28);
        DateGenerator.formatCookieDate(buf, date);
        return buf.toString();
    }

    public String doFormatDate(long date) {
        this.buf.setLength(0);
        this.gc.setTimeInMillis(date);
        int day_of_week = this.gc.get(7);
        int day_of_month = this.gc.get(5);
        int month = this.gc.get(2);
        int year = this.gc.get(1);
        int century = year / 100;
        year %= 100;
        int hours = this.gc.get(11);
        int minutes = this.gc.get(12);
        int seconds = this.gc.get(13);
        this.buf.append(DAYS[day_of_week]);
        this.buf.append(',');
        this.buf.append(' ');
        StringUtil.append2digits(this.buf, day_of_month);
        this.buf.append(' ');
        this.buf.append(MONTHS[month]);
        this.buf.append(' ');
        StringUtil.append2digits(this.buf, century);
        StringUtil.append2digits(this.buf, year);
        this.buf.append(' ');
        StringUtil.append2digits(this.buf, hours);
        this.buf.append(':');
        StringUtil.append2digits(this.buf, minutes);
        this.buf.append(':');
        StringUtil.append2digits(this.buf, seconds);
        this.buf.append(" GMT");
        return this.buf.toString();
    }

    public void doFormatCookieDate(StringBuilder buf, long date) {
        this.gc.setTimeInMillis(date);
        int day_of_week = this.gc.get(7);
        int day_of_month = this.gc.get(5);
        int month = this.gc.get(2);
        int year = this.gc.get(1);
        year %= 10000;
        int epoch = (int)(date / 1000L % 86400L);
        int seconds = epoch % 60;
        int minutes = (epoch /= 60) % 60;
        int hours = epoch / 60;
        buf.append(DAYS[day_of_week]);
        buf.append(',');
        buf.append(' ');
        StringUtil.append2digits(buf, day_of_month);
        buf.append('-');
        buf.append(MONTHS[month]);
        buf.append('-');
        StringUtil.append2digits(buf, year / 100);
        StringUtil.append2digits(buf, year % 100);
        buf.append(' ');
        StringUtil.append2digits(buf, hours);
        buf.append(':');
        StringUtil.append2digits(buf, minutes);
        buf.append(':');
        StringUtil.append2digits(buf, seconds);
        buf.append(" GMT");
    }

    static {
        __GMT.setID("GMT");
        DAYS = new String[]{"Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        MONTHS = new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec", "Jan"};
        __dateGenerator = new ThreadLocal<DateGenerator>(){

            @Override
            protected DateGenerator initialValue() {
                return new DateGenerator();
            }
        };
        __01Jan1970 = DateGenerator.formatDate(0L);
    }
}

