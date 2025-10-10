/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.http;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.eclipse.jetty.http.DateGenerator;
import org.eclipse.jetty.http.DateParser;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpHeaderValue;
import org.eclipse.jetty.http.QuotedCSV;
import org.eclipse.jetty.http.QuotedQualityCSV;
import org.eclipse.jetty.util.ArrayTernaryTrie;
import org.eclipse.jetty.util.QuotedStringTokenizer;
import org.eclipse.jetty.util.Trie;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class HttpFields
implements Iterable<HttpField> {
    @Deprecated
    public static final String __separators = ", \t";
    private static final Logger LOG = Log.getLogger(HttpFields.class);
    private HttpField[] _fields;
    private int _size;
    @Deprecated
    private static final Float __one = new Float("1.0");
    @Deprecated
    private static final Float __zero = new Float("0.0");
    @Deprecated
    private static final Trie<Float> __qualities = new ArrayTernaryTrie<Float>();

    public HttpFields() {
        this._fields = new HttpField[20];
    }

    public HttpFields(int capacity) {
        this._fields = new HttpField[capacity];
    }

    public HttpFields(HttpFields fields) {
        this._fields = Arrays.copyOf(fields._fields, fields._fields.length + 10);
        this._size = fields._size;
    }

    public int size() {
        return this._size;
    }

    @Override
    public Iterator<HttpField> iterator() {
        return new Itr();
    }

    public Stream<HttpField> stream() {
        return StreamSupport.stream(Arrays.spliterator(this._fields, 0, this._size), false);
    }

    public Set<String> getFieldNamesCollection() {
        HashSet<String> set = new HashSet<String>(this._size);
        for (HttpField f : this) {
            if (f == null) continue;
            set.add(f.getName());
        }
        return set;
    }

    public Enumeration<String> getFieldNames() {
        return Collections.enumeration(this.getFieldNamesCollection());
    }

    public HttpField getField(int index) {
        if (index >= this._size) {
            throw new NoSuchElementException();
        }
        return this._fields[index];
    }

    public HttpField getField(HttpHeader header) {
        for (int i = 0; i < this._size; ++i) {
            HttpField f = this._fields[i];
            if (f.getHeader() != header) continue;
            return f;
        }
        return null;
    }

    public HttpField getField(String name) {
        for (int i = 0; i < this._size; ++i) {
            HttpField f = this._fields[i];
            if (!f.getName().equalsIgnoreCase(name)) continue;
            return f;
        }
        return null;
    }

    public boolean contains(HttpField field) {
        int i = this._size;
        while (i-- > 0) {
            HttpField f = this._fields[i];
            if (!f.isSameName(field) || !f.equals(field) && !f.contains(field.getValue())) continue;
            return true;
        }
        return false;
    }

    public boolean contains(HttpHeader header, String value) {
        int i = this._size;
        while (i-- > 0) {
            HttpField f = this._fields[i];
            if (f.getHeader() != header || !f.contains(value)) continue;
            return true;
        }
        return false;
    }

    public boolean contains(String name, String value) {
        int i = this._size;
        while (i-- > 0) {
            HttpField f = this._fields[i];
            if (!f.getName().equalsIgnoreCase(name) || !f.contains(value)) continue;
            return true;
        }
        return false;
    }

    public boolean contains(HttpHeader header) {
        int i = this._size;
        while (i-- > 0) {
            HttpField f = this._fields[i];
            if (f.getHeader() != header) continue;
            return true;
        }
        return false;
    }

    public boolean containsKey(String name) {
        int i = this._size;
        while (i-- > 0) {
            HttpField f = this._fields[i];
            if (!f.getName().equalsIgnoreCase(name)) continue;
            return true;
        }
        return false;
    }

    @Deprecated
    public String getStringField(HttpHeader header) {
        return this.get(header);
    }

    public String get(HttpHeader header) {
        for (int i = 0; i < this._size; ++i) {
            HttpField f = this._fields[i];
            if (f.getHeader() != header) continue;
            return f.getValue();
        }
        return null;
    }

    @Deprecated
    public String getStringField(String name) {
        return this.get(name);
    }

    public String get(String header) {
        for (int i = 0; i < this._size; ++i) {
            HttpField f = this._fields[i];
            if (!f.getName().equalsIgnoreCase(header)) continue;
            return f.getValue();
        }
        return null;
    }

    public List<String> getValuesList(HttpHeader header) {
        ArrayList<String> list = new ArrayList<String>();
        for (HttpField f : this) {
            if (f.getHeader() != header) continue;
            list.add(f.getValue());
        }
        return list;
    }

    public List<String> getValuesList(String name) {
        ArrayList<String> list = new ArrayList<String>();
        for (HttpField f : this) {
            if (!f.getName().equalsIgnoreCase(name)) continue;
            list.add(f.getValue());
        }
        return list;
    }

    public boolean addCSV(HttpHeader header, String ... values) {
        QuotedCSV existing = null;
        for (HttpField f : this) {
            if (f.getHeader() != header) continue;
            if (existing == null) {
                existing = new QuotedCSV(false, new String[0]);
            }
            existing.addValue(f.getValue());
        }
        String value = this.addCSV(existing, values);
        if (value != null) {
            this.add(header, value);
            return true;
        }
        return false;
    }

    public boolean addCSV(String name, String ... values) {
        QuotedCSV existing = null;
        for (HttpField f : this) {
            if (!f.getName().equalsIgnoreCase(name)) continue;
            if (existing == null) {
                existing = new QuotedCSV(false, new String[0]);
            }
            existing.addValue(f.getValue());
        }
        String value = this.addCSV(existing, values);
        if (value != null) {
            this.add(name, value);
            return true;
        }
        return false;
    }

    protected String addCSV(QuotedCSV existing, String ... values) {
        boolean add = true;
        if (existing != null && !existing.isEmpty()) {
            add = false;
            int i = values.length;
            while (i-- > 0) {
                String unquoted = QuotedCSV.unquote(values[i]);
                if (existing.getValues().contains(unquoted)) {
                    values[i] = null;
                    continue;
                }
                add = true;
            }
        }
        if (add) {
            StringBuilder value = new StringBuilder();
            for (String v : values) {
                if (v == null) continue;
                if (value.length() > 0) {
                    value.append(", ");
                }
                value.append(v);
            }
            if (value.length() > 0) {
                return value.toString();
            }
        }
        return null;
    }

    public List<String> getCSV(HttpHeader header, boolean keepQuotes) {
        QuotedCSV values = null;
        for (HttpField f : this) {
            if (f.getHeader() != header) continue;
            if (values == null) {
                values = new QuotedCSV(keepQuotes, new String[0]);
            }
            values.addValue(f.getValue());
        }
        return values == null ? Collections.emptyList() : values.getValues();
    }

    public List<String> getCSV(String name, boolean keepQuotes) {
        QuotedCSV values = null;
        for (HttpField f : this) {
            if (!f.getName().equalsIgnoreCase(name)) continue;
            if (values == null) {
                values = new QuotedCSV(keepQuotes, new String[0]);
            }
            values.addValue(f.getValue());
        }
        return values == null ? Collections.emptyList() : values.getValues();
    }

    public List<String> getQualityCSV(HttpHeader header) {
        QuotedQualityCSV values = null;
        for (HttpField f : this) {
            if (f.getHeader() != header) continue;
            if (values == null) {
                values = new QuotedQualityCSV();
            }
            values.addValue(f.getValue());
        }
        return values == null ? Collections.emptyList() : values.getValues();
    }

    public List<String> getQualityCSV(String name) {
        QuotedQualityCSV values = null;
        for (HttpField f : this) {
            if (!f.getName().equalsIgnoreCase(name)) continue;
            if (values == null) {
                values = new QuotedQualityCSV();
            }
            values.addValue(f.getValue());
        }
        return values == null ? Collections.emptyList() : values.getValues();
    }

    public Enumeration<String> getValues(final String name) {
        for (int i = 0; i < this._size; ++i) {
            final HttpField f = this._fields[i];
            if (!f.getName().equalsIgnoreCase(name) || f.getValue() == null) continue;
            final int first = i;
            return new Enumeration<String>(){
                HttpField field;
                int i;
                {
                    this.field = f;
                    this.i = first + 1;
                }

                @Override
                public boolean hasMoreElements() {
                    if (this.field == null) {
                        while (this.i < HttpFields.this._size) {
                            this.field = HttpFields.this._fields[this.i++];
                            if (!this.field.getName().equalsIgnoreCase(name) || this.field.getValue() == null) continue;
                            return true;
                        }
                        this.field = null;
                        return false;
                    }
                    return true;
                }

                @Override
                public String nextElement() throws NoSuchElementException {
                    if (this.hasMoreElements()) {
                        String value = this.field.getValue();
                        this.field = null;
                        return value;
                    }
                    throw new NoSuchElementException();
                }
            };
        }
        List empty = Collections.emptyList();
        return Collections.enumeration(empty);
    }

    @Deprecated
    public Enumeration<String> getValues(String name, final String separators) {
        final Enumeration<String> e = this.getValues(name);
        if (e == null) {
            return null;
        }
        return new Enumeration<String>(){
            QuotedStringTokenizer tok = null;

            @Override
            public boolean hasMoreElements() {
                if (this.tok != null && this.tok.hasMoreElements()) {
                    return true;
                }
                while (e.hasMoreElements()) {
                    String value = (String)e.nextElement();
                    if (value == null) continue;
                    this.tok = new QuotedStringTokenizer(value, separators, false, false);
                    if (!this.tok.hasMoreElements()) continue;
                    return true;
                }
                this.tok = null;
                return false;
            }

            @Override
            public String nextElement() throws NoSuchElementException {
                if (!this.hasMoreElements()) {
                    throw new NoSuchElementException();
                }
                String next = (String)this.tok.nextElement();
                if (next != null) {
                    next = next.trim();
                }
                return next;
            }
        };
    }

    public void put(HttpField field) {
        boolean put = false;
        int i = this._size;
        while (i-- > 0) {
            HttpField f = this._fields[i];
            if (!f.isSameName(field)) continue;
            if (put) {
                System.arraycopy(this._fields, i + 1, this._fields, i, --this._size - i);
                continue;
            }
            this._fields[i] = field;
            put = true;
        }
        if (!put) {
            this.add(field);
        }
    }

    public void put(String name, String value) {
        if (value == null) {
            this.remove(name);
        } else {
            this.put(new HttpField(name, value));
        }
    }

    public void put(HttpHeader header, HttpHeaderValue value) {
        this.put(header, value.toString());
    }

    public void put(HttpHeader header, String value) {
        if (value == null) {
            this.remove(header);
        } else {
            this.put(new HttpField(header, value));
        }
    }

    public void put(String name, List<String> list) {
        this.remove(name);
        for (String v : list) {
            if (v == null) continue;
            this.add(name, v);
        }
    }

    public void add(String name, String value) {
        if (value == null) {
            return;
        }
        HttpField field = new HttpField(name, value);
        this.add(field);
    }

    public void add(HttpHeader header, HttpHeaderValue value) {
        this.add(header, value.toString());
    }

    public void add(HttpHeader header, String value) {
        if (value == null) {
            throw new IllegalArgumentException("null value");
        }
        HttpField field = new HttpField(header, value);
        this.add(field);
    }

    public HttpField remove(HttpHeader name) {
        HttpField removed = null;
        int i = this._size;
        while (i-- > 0) {
            HttpField f = this._fields[i];
            if (f.getHeader() != name) continue;
            removed = f;
            System.arraycopy(this._fields, i + 1, this._fields, i, --this._size - i);
        }
        return removed;
    }

    public HttpField remove(String name) {
        HttpField removed = null;
        int i = this._size;
        while (i-- > 0) {
            HttpField f = this._fields[i];
            if (!f.getName().equalsIgnoreCase(name)) continue;
            removed = f;
            System.arraycopy(this._fields, i + 1, this._fields, i, --this._size - i);
        }
        return removed;
    }

    public long getLongField(String name) throws NumberFormatException {
        HttpField field = this.getField(name);
        return field == null ? -1L : field.getLongValue();
    }

    public long getDateField(String name) {
        HttpField field = this.getField(name);
        if (field == null) {
            return -1L;
        }
        String val = HttpFields.valueParameters(field.getValue(), null);
        if (val == null) {
            return -1L;
        }
        long date = DateParser.parseDate(val);
        if (date == -1L) {
            throw new IllegalArgumentException("Cannot convert date: " + val);
        }
        return date;
    }

    public void putLongField(HttpHeader name, long value) {
        String v = Long.toString(value);
        this.put(name, v);
    }

    public void putLongField(String name, long value) {
        String v = Long.toString(value);
        this.put(name, v);
    }

    public void putDateField(HttpHeader name, long date) {
        String d = DateGenerator.formatDate(date);
        this.put(name, d);
    }

    public void putDateField(String name, long date) {
        String d = DateGenerator.formatDate(date);
        this.put(name, d);
    }

    public void addDateField(String name, long date) {
        String d = DateGenerator.formatDate(date);
        this.add(name, d);
    }

    public int hashCode() {
        int hash = 0;
        for (HttpField field : this._fields) {
            hash += field.hashCode();
        }
        return hash;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HttpFields)) {
            return false;
        }
        HttpFields that = (HttpFields)o;
        if (this.size() != that.size()) {
            return false;
        }
        block0: for (HttpField fi : this) {
            for (HttpField fa : that) {
                if (!fi.equals(fa)) continue;
                continue block0;
            }
            return false;
        }
        return true;
    }

    public String toString() {
        try {
            StringBuilder buffer = new StringBuilder();
            for (HttpField field : this) {
                if (field == null) continue;
                String tmp = field.getName();
                if (tmp != null) {
                    buffer.append(tmp);
                }
                buffer.append(": ");
                tmp = field.getValue();
                if (tmp != null) {
                    buffer.append(tmp);
                }
                buffer.append("\r\n");
            }
            buffer.append("\r\n");
            return buffer.toString();
        }
        catch (Exception e) {
            LOG.warn(e);
            return e.toString();
        }
    }

    public void clear() {
        this._size = 0;
    }

    public void add(HttpField field) {
        if (field != null) {
            if (this._size == this._fields.length) {
                this._fields = Arrays.copyOf(this._fields, this._size * 2);
            }
            this._fields[this._size++] = field;
        }
    }

    public void addAll(HttpFields fields) {
        for (int i = 0; i < fields._size; ++i) {
            this.add(fields._fields[i]);
        }
    }

    public void add(HttpFields fields) {
        if (fields == null) {
            return;
        }
        Enumeration<String> e = fields.getFieldNames();
        while (e.hasMoreElements()) {
            String name = e.nextElement();
            Enumeration<String> values = fields.getValues(name);
            while (values.hasMoreElements()) {
                this.add(name, values.nextElement());
            }
        }
    }

    public static String stripParameters(String value) {
        if (value == null) {
            return null;
        }
        int i = value.indexOf(59);
        if (i < 0) {
            return value;
        }
        return value.substring(0, i).trim();
    }

    public static String valueParameters(String value, Map<String, String> parameters) {
        if (value == null) {
            return null;
        }
        int i = value.indexOf(59);
        if (i < 0) {
            return value;
        }
        if (parameters == null) {
            return value.substring(0, i).trim();
        }
        QuotedStringTokenizer tok1 = new QuotedStringTokenizer(value.substring(i), ";", false, true);
        while (((StringTokenizer)tok1).hasMoreTokens()) {
            String token = ((StringTokenizer)tok1).nextToken();
            QuotedStringTokenizer tok2 = new QuotedStringTokenizer(token, "= ");
            if (!((StringTokenizer)tok2).hasMoreTokens()) continue;
            String paramName = ((StringTokenizer)tok2).nextToken();
            String paramVal = null;
            if (((StringTokenizer)tok2).hasMoreTokens()) {
                paramVal = ((StringTokenizer)tok2).nextToken();
            }
            parameters.put(paramName, paramVal);
        }
        return value.substring(0, i).trim();
    }

    @Deprecated
    public static Float getQuality(String value) {
        Float q;
        Float q2;
        if (value == null) {
            return __zero;
        }
        int qe = value.indexOf(";");
        if (qe++ < 0 || qe == value.length()) {
            return __one;
        }
        if (value.charAt(qe++) == 'q' && (q2 = __qualities.get(value, ++qe, value.length() - qe)) != null) {
            return q2;
        }
        HashMap<String, String> params = new HashMap<String, String>(4);
        HttpFields.valueParameters(value, params);
        String qs = (String)params.get("q");
        if (qs == null) {
            qs = "*";
        }
        if ((q = __qualities.get(qs)) == null) {
            try {
                q = new Float(qs);
            }
            catch (Exception e) {
                q = __one;
            }
        }
        return q;
    }

    @Deprecated
    public static List<String> qualityList(Enumeration<String> e) {
        if (e == null || !e.hasMoreElements()) {
            return Collections.emptyList();
        }
        QuotedQualityCSV values = new QuotedQualityCSV();
        while (e.hasMoreElements()) {
            values.addValue(e.nextElement());
        }
        return values.getValues();
    }

    static {
        __qualities.put("*", __one);
        __qualities.put("1.0", __one);
        __qualities.put("1", __one);
        __qualities.put("0.9", new Float("0.9"));
        __qualities.put("0.8", new Float("0.8"));
        __qualities.put("0.7", new Float("0.7"));
        __qualities.put("0.66", new Float("0.66"));
        __qualities.put("0.6", new Float("0.6"));
        __qualities.put("0.5", new Float("0.5"));
        __qualities.put("0.4", new Float("0.4"));
        __qualities.put("0.33", new Float("0.33"));
        __qualities.put("0.3", new Float("0.3"));
        __qualities.put("0.2", new Float("0.2"));
        __qualities.put("0.1", new Float("0.1"));
        __qualities.put("0", __zero);
        __qualities.put("0.0", __zero);
    }

    private class Itr
    implements Iterator<HttpField> {
        int _cursor;
        int _last = -1;

        private Itr() {
        }

        @Override
        public boolean hasNext() {
            return this._cursor != HttpFields.this._size;
        }

        @Override
        public HttpField next() {
            int i = this._cursor;
            if (i >= HttpFields.this._size) {
                throw new NoSuchElementException();
            }
            this._cursor = i + 1;
            this._last = i;
            return HttpFields.this._fields[this._last];
        }

        @Override
        public void remove() {
            if (this._last < 0) {
                throw new IllegalStateException();
            }
            System.arraycopy(HttpFields.this._fields, this._last + 1, HttpFields.this._fields, this._last, --HttpFields.this._size - this._last);
            this._cursor = this._last;
            this._last = -1;
        }
    }
}

