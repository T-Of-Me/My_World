/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server;

import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;
import org.eclipse.jetty.util.LazyList;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class InclusiveByteRange {
    private static final Logger LOG = Log.getLogger(InclusiveByteRange.class);
    long first = 0L;
    long last = 0L;

    public InclusiveByteRange(long first, long last) {
        this.first = first;
        this.last = last;
    }

    public long getFirst() {
        return this.first;
    }

    public long getLast() {
        return this.last;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public static List<InclusiveByteRange> satisfiableRanges(Enumeration<String> headers, long size) {
        Object satRanges = null;
        block4: while (headers.hasMoreElements()) {
            String header = headers.nextElement();
            StringTokenizer tok = new StringTokenizer(header, "=,", false);
            String t = null;
            try {
                while (true) {
                    if (!tok.hasMoreTokens()) continue block4;
                    try {
                        long last;
                        long first;
                        block13: {
                            t = tok.nextToken().trim();
                            first = -1L;
                            last = -1L;
                            int d = t.indexOf(45);
                            if (d < 0 || t.indexOf("-", d + 1) >= 0) {
                                if ("bytes".equals(t)) continue;
                                LOG.warn("Bad range format: {}", t);
                                continue block4;
                            }
                            if (d == 0) {
                                if (d + 1 < t.length()) {
                                    last = Long.parseLong(t.substring(d + 1).trim());
                                    break block13;
                                } else {
                                    LOG.warn("Bad range format: {}", t);
                                    continue;
                                }
                            }
                            if (d + 1 < t.length()) {
                                first = Long.parseLong(t.substring(0, d).trim());
                                last = Long.parseLong(t.substring(d + 1).trim());
                            } else {
                                first = Long.parseLong(t.substring(0, d).trim());
                            }
                        }
                        if (first == -1L && last == -1L || first != -1L && last != -1L && first > last) continue block4;
                        if (first >= size) continue;
                        InclusiveByteRange range = new InclusiveByteRange(first, last);
                        satRanges = LazyList.add(satRanges, range);
                    }
                    catch (NumberFormatException e) {
                        LOG.warn("Bad range format: {}", t);
                        LOG.ignore(e);
                    }
                }
            }
            catch (Exception e) {
                LOG.warn("Bad range format: {}", t);
                LOG.ignore(e);
                continue;
            }
            break;
        }
        return LazyList.getList(satRanges, true);
    }

    public long getFirst(long size) {
        if (this.first < 0L) {
            long tf = size - this.last;
            if (tf < 0L) {
                tf = 0L;
            }
            return tf;
        }
        return this.first;
    }

    public long getLast(long size) {
        if (this.first < 0L) {
            return size - 1L;
        }
        if (this.last < 0L || this.last >= size) {
            return size - 1L;
        }
        return this.last;
    }

    public long getSize(long size) {
        return this.getLast(size) - this.getFirst(size) + 1L;
    }

    public String toHeaderRangeString(long size) {
        StringBuilder sb = new StringBuilder(40);
        sb.append("bytes ");
        sb.append(this.getFirst(size));
        sb.append('-');
        sb.append(this.getLast(size));
        sb.append("/");
        sb.append(size);
        return sb.toString();
    }

    public static String to416HeaderRangeString(long size) {
        StringBuilder sb = new StringBuilder(40);
        sb.append("bytes */");
        sb.append(size);
        return sb.toString();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(60);
        sb.append(Long.toString(this.first));
        sb.append(":");
        sb.append(Long.toString(this.last));
        return sb.toString();
    }
}

