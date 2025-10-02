/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.http;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateParser {
    private static final TimeZone __GMT = TimeZone.getTimeZone("GMT");
    static final String[] __dateReceiveFmt;
    private static final ThreadLocal<DateParser> __dateParser;
    final SimpleDateFormat[] _dateReceive = new SimpleDateFormat[__dateReceiveFmt.length];

    public static long parseDate(String date) {
        return __dateParser.get().parse(date);
    }

    private long parse(String dateVal) {
        for (int i = 0; i < this._dateReceive.length; ++i) {
            if (this._dateReceive[i] == null) {
                this._dateReceive[i] = new SimpleDateFormat(__dateReceiveFmt[i], Locale.US);
                this._dateReceive[i].setTimeZone(__GMT);
            }
            try {
                Date date = (Date)this._dateReceive[i].parseObject(dateVal);
                return date.getTime();
            }
            catch (Exception exception) {
                continue;
            }
        }
        if (dateVal.endsWith(" GMT")) {
            String val = dateVal.substring(0, dateVal.length() - 4);
            for (SimpleDateFormat element : this._dateReceive) {
                try {
                    Date date = (Date)element.parseObject(val);
                    return date.getTime();
                }
                catch (Exception exception) {
                }
            }
        }
        return -1L;
    }

    static {
        __GMT.setID("GMT");
        __dateReceiveFmt = new String[]{"EEE, dd MMM yyyy HH:mm:ss zzz", "EEE, dd-MMM-yy HH:mm:ss", "EEE MMM dd HH:mm:ss yyyy", "EEE, dd MMM yyyy HH:mm:ss", "EEE dd MMM yyyy HH:mm:ss zzz", "EEE dd MMM yyyy HH:mm:ss", "EEE MMM dd yyyy HH:mm:ss zzz", "EEE MMM dd yyyy HH:mm:ss", "EEE MMM-dd-yyyy HH:mm:ss zzz", "EEE MMM-dd-yyyy HH:mm:ss", "dd MMM yyyy HH:mm:ss zzz", "dd MMM yyyy HH:mm:ss", "dd-MMM-yy HH:mm:ss zzz", "dd-MMM-yy HH:mm:ss", "MMM dd HH:mm:ss yyyy zzz", "MMM dd HH:mm:ss yyyy", "EEE MMM dd HH:mm:ss yyyy zzz", "EEE, MMM dd HH:mm:ss yyyy zzz", "EEE, MMM dd HH:mm:ss yyyy", "EEE, dd-MMM-yy HH:mm:ss zzz", "EEE dd-MMM-yy HH:mm:ss zzz", "EEE dd-MMM-yy HH:mm:ss"};
        __dateParser = new ThreadLocal<DateParser>(){

            @Override
            protected DateParser initialValue() {
                return new DateParser();
            }
        };
    }
}

