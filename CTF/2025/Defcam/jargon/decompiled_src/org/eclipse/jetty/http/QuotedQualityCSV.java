/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.http;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import org.eclipse.jetty.http.QuotedCSV;

public class QuotedQualityCSV
extends QuotedCSV
implements Iterable<String> {
    private static final Double ZERO = new Double(0.0);
    private static final Double ONE = new Double(1.0);
    public static Function<String, Integer> MOST_SPECIFIC = new Function<String, Integer>(){

        @Override
        public Integer apply(String s) {
            String[] elements = s.split("/");
            return 1000000 * elements.length + 1000 * elements[0].length() + elements[elements.length - 1].length();
        }
    };
    private final List<Double> _quality = new ArrayList<Double>();
    private boolean _sorted = false;
    private final Function<String, Integer> _secondaryOrdering;

    public QuotedQualityCSV() {
        this(s -> 0);
    }

    public QuotedQualityCSV(String[] preferredOrder) {
        this(s -> {
            for (int i = 0; i < preferredOrder.length; ++i) {
                if (!preferredOrder[i].equals(s)) continue;
                return preferredOrder.length - i;
            }
            if ("*".equals(s)) {
                return preferredOrder.length;
            }
            return Integer.MIN_VALUE;
        });
    }

    public QuotedQualityCSV(Function<String, Integer> secondaryOrdering) {
        super(new String[0]);
        this._secondaryOrdering = secondaryOrdering;
    }

    @Override
    protected void parsedValue(StringBuffer buffer) {
        super.parsedValue(buffer);
        this._quality.add(ONE);
    }

    @Override
    protected void parsedParam(StringBuffer buffer, int valueLength, int paramName, int paramValue) {
        if (paramName < 0) {
            if (buffer.charAt(buffer.length() - 1) == ';') {
                buffer.setLength(buffer.length() - 1);
            }
        } else if (paramValue >= 0 && buffer.charAt(paramName) == 'q' && paramValue > paramName && buffer.length() >= paramName && buffer.charAt(paramName + 1) == '=') {
            Double q;
            try {
                q = this._keepQuotes && buffer.charAt(paramValue) == '\"' ? new Double(buffer.substring(paramValue + 1, buffer.length() - 1)) : new Double(buffer.substring(paramValue));
            }
            catch (Exception e) {
                q = ZERO;
            }
            buffer.setLength(Math.max(0, paramName - 1));
            if (!ONE.equals(q)) {
                this._quality.set(this._quality.size() - 1, q);
            }
        }
    }

    @Override
    public List<String> getValues() {
        if (!this._sorted) {
            this.sort();
        }
        return this._values;
    }

    @Override
    public Iterator<String> iterator() {
        if (!this._sorted) {
            this.sort();
        }
        return this._values.iterator();
    }

    protected void sort() {
        this._sorted = true;
        Double last = ZERO;
        int lastSecondaryOrder = Integer.MIN_VALUE;
        int i = this._values.size();
        while (i-- > 0) {
            String v = (String)this._values.get(i);
            Double q = this._quality.get(i);
            int compare = last.compareTo(q);
            if (compare > 0 || compare == 0 && this._secondaryOrdering.apply(v) < lastSecondaryOrder) {
                this._values.set(i, this._values.get(i + 1));
                this._values.set(i + 1, v);
                this._quality.set(i, this._quality.get(i + 1));
                this._quality.set(i + 1, q);
                last = ZERO;
                lastSecondaryOrder = 0;
                i = this._values.size();
                continue;
            }
            last = q;
            lastSecondaryOrder = this._secondaryOrdering.apply(v);
        }
        int last_element = this._quality.size();
        while (last_element > 0 && this._quality.get(--last_element).equals(ZERO)) {
            this._quality.remove(last_element);
            this._values.remove(last_element);
        }
    }
}

