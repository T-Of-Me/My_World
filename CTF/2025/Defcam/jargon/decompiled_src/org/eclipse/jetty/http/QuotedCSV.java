/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.http;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class QuotedCSV
implements Iterable<String> {
    protected final List<String> _values = new ArrayList<String>();
    protected final boolean _keepQuotes;

    public QuotedCSV(String ... values) {
        this(true, values);
    }

    public QuotedCSV(boolean keepQuotes, String ... values) {
        this._keepQuotes = keepQuotes;
        for (String v : values) {
            this.addValue(v);
        }
    }

    /*
     * Unable to fully structure code
     */
    public void addValue(String value) {
        if (value == null) {
            return;
        }
        buffer = new StringBuffer();
        l = value.length();
        state = State.VALUE;
        quoted = false;
        sloshed = false;
        nws_length = 0;
        last_length = 0;
        value_length = -1;
        param_name = -1;
        param_value = -1;
        block25: for (i = 0; i <= l; ++i) {
            block38: {
                block39: {
                    v0 = c = i == l ? '\u0000' : value.charAt(i);
                    if (!quoted || c == '\u0000') break block38;
                    if (!sloshed) break block39;
                    sloshed = false;
                    ** GOTO lbl-1000
                }
                switch (c) {
                    case '\\': {
                        sloshed = true;
                        if (!this._keepQuotes) {
                            break;
                        }
                        ** GOTO lbl29
                    }
                    case '\"': {
                        quoted = false;
                        if (!this._keepQuotes) break;
                    }
lbl29:
                    // 3 sources

                    default: lbl-1000:
                    // 2 sources

                    {
                        buffer.append(c);
                        nws_length = buffer.length();
                        break;
                    }
                }
                continue;
            }
            switch (c) {
                case '\t': 
                case ' ': {
                    if (buffer.length() <= last_length) continue block25;
                    buffer.append(c);
                    continue block25;
                }
                case '\"': {
                    quoted = true;
                    if (this._keepQuotes) {
                        if (state == State.PARAM_VALUE && param_value < 0) {
                            param_value = nws_length;
                        }
                        buffer.append(c);
                    } else if (state == State.PARAM_VALUE && param_value < 0) {
                        param_value = nws_length;
                    }
                    nws_length = buffer.length();
                    continue block25;
                }
                case ';': {
                    buffer.setLength(nws_length);
                    if (state == State.VALUE) {
                        this.parsedValue(buffer);
                        value_length = buffer.length();
                    } else {
                        this.parsedParam(buffer, value_length, param_name, param_value);
                    }
                    nws_length = buffer.length();
                    param_value = -1;
                    param_name = -1;
                    buffer.append(c);
                    last_length = ++nws_length;
                    state = State.PARAM_NAME;
                    continue block25;
                }
                case '\u0000': 
                case ',': {
                    if (nws_length > 0) {
                        buffer.setLength(nws_length);
                        switch (1.$SwitchMap$org$eclipse$jetty$http$QuotedCSV$State[state.ordinal()]) {
                            case 1: {
                                this.parsedValue(buffer);
                                value_length = buffer.length();
                                break;
                            }
                            case 2: 
                            case 3: {
                                this.parsedParam(buffer, value_length, param_name, param_value);
                            }
                        }
                        this._values.add(buffer.toString());
                    }
                    buffer.setLength(0);
                    last_length = 0;
                    nws_length = 0;
                    param_value = -1;
                    param_name = -1;
                    value_length = -1;
                    state = State.VALUE;
                    continue block25;
                }
                case '=': {
                    switch (1.$SwitchMap$org$eclipse$jetty$http$QuotedCSV$State[state.ordinal()]) {
                        case 1: {
                            param_name = 0;
                            value_length = 0;
                            buffer.setLength(nws_length);
                            param = buffer.toString();
                            buffer.setLength(0);
                            this.parsedValue(buffer);
                            value_length = buffer.length();
                            buffer.append(param);
                            buffer.append(c);
                            last_length = ++nws_length;
                            state = State.PARAM_VALUE;
                            continue block25;
                        }
                        case 2: {
                            buffer.setLength(nws_length);
                            buffer.append(c);
                            last_length = ++nws_length;
                            state = State.PARAM_VALUE;
                            continue block25;
                        }
                        case 3: {
                            if (param_value < 0) {
                                param_value = nws_length;
                            }
                            buffer.append(c);
                            nws_length = buffer.length();
                            continue block25;
                        }
                    }
                    continue block25;
                }
                default: {
                    switch (1.$SwitchMap$org$eclipse$jetty$http$QuotedCSV$State[state.ordinal()]) {
                        case 1: {
                            buffer.append(c);
                            nws_length = buffer.length();
                            continue block25;
                        }
                        case 2: {
                            if (param_name < 0) {
                                param_name = nws_length;
                            }
                            buffer.append(c);
                            nws_length = buffer.length();
                            continue block25;
                        }
                        case 3: {
                            if (param_value < 0) {
                                param_value = nws_length;
                            }
                            buffer.append(c);
                            nws_length = buffer.length();
                            continue block25;
                        }
                    }
                }
            }
        }
    }

    protected void parsedValue(StringBuffer buffer) {
    }

    protected void parsedParam(StringBuffer buffer, int valueLength, int paramName, int paramValue) {
    }

    public int size() {
        return this._values.size();
    }

    public boolean isEmpty() {
        return this._values.isEmpty();
    }

    public List<String> getValues() {
        return this._values;
    }

    @Override
    public Iterator<String> iterator() {
        return this._values.iterator();
    }

    public static String unquote(String s) {
        char c;
        int i;
        int l = s.length();
        if (s == null || l == 0) {
            return s;
        }
        for (i = 0; i < l && (c = s.charAt(i)) != '\"'; ++i) {
        }
        if (i == l) {
            return s;
        }
        boolean quoted = true;
        boolean sloshed = false;
        StringBuffer buffer = new StringBuffer();
        buffer.append(s, 0, i);
        ++i;
        while (i < l) {
            char c2 = s.charAt(i);
            if (quoted) {
                if (sloshed) {
                    buffer.append(c2);
                    sloshed = false;
                } else if (c2 == '\"') {
                    quoted = false;
                } else if (c2 == '\\') {
                    sloshed = true;
                } else {
                    buffer.append(c2);
                }
            } else if (c2 == '\"') {
                quoted = true;
            } else {
                buffer.append(c2);
            }
            ++i;
        }
        return buffer.toString();
    }

    public String toString() {
        ArrayList<String> list = new ArrayList<String>();
        for (String s : this) {
            list.add(s);
        }
        return ((Object)list).toString();
    }

    private static enum State {
        VALUE,
        PARAM_NAME,
        PARAM_VALUE;

    }
}

