/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MultiMap<V>
extends HashMap<String, List<V>> {
    public MultiMap() {
    }

    public MultiMap(Map<String, List<V>> map) {
        super(map);
    }

    public MultiMap(MultiMap<V> map) {
        super(map);
    }

    public List<V> getValues(String name) {
        List vals = (List)super.get(name);
        if (vals == null || vals.isEmpty()) {
            return null;
        }
        return vals;
    }

    public V getValue(String name, int i) {
        List<V> vals = this.getValues(name);
        if (vals == null) {
            return null;
        }
        if (i == 0 && vals.isEmpty()) {
            return null;
        }
        return vals.get(i);
    }

    public String getString(String name) {
        List vals = (List)this.get(name);
        if (vals == null || vals.isEmpty()) {
            return null;
        }
        if (vals.size() == 1) {
            return vals.get(0).toString();
        }
        StringBuilder values = new StringBuilder(128);
        for (Object e : vals) {
            if (e == null) continue;
            if (values.length() > 0) {
                values.append(',');
            }
            values.append(e.toString());
        }
        return values.toString();
    }

    @Override
    public List<V> put(String name, V value) {
        if (value == null) {
            return super.put(name, null);
        }
        ArrayList<V> vals = new ArrayList<V>();
        vals.add(value);
        return this.put(name, vals);
    }

    public void putAllValues(Map<String, V> input) {
        for (Map.Entry<String, V> entry : input.entrySet()) {
            this.put(entry.getKey(), entry.getValue());
        }
    }

    public List<V> putValues(String name, List<V> values) {
        return super.put(name, values);
    }

    @SafeVarargs
    public final List<V> putValues(String name, V ... values) {
        ArrayList<V> list = new ArrayList<V>();
        list.addAll(Arrays.asList(values));
        return super.put(name, list);
    }

    public void add(String name, V value) {
        ArrayList<V> lo = (ArrayList<V>)this.get(name);
        if (lo == null) {
            lo = new ArrayList<V>();
        }
        lo.add(value);
        super.put(name, lo);
    }

    public void addValues(String name, List<V> values) {
        ArrayList<V> lo = (ArrayList<V>)this.get(name);
        if (lo == null) {
            lo = new ArrayList<V>();
        }
        lo.addAll(values);
        this.put(name, lo);
    }

    public void addValues(String name, V[] values) {
        ArrayList<V> lo = (ArrayList<V>)this.get(name);
        if (lo == null) {
            lo = new ArrayList<V>();
        }
        lo.addAll(Arrays.asList(values));
        this.put(name, lo);
    }

    public boolean addAllValues(MultiMap<V> map) {
        boolean merged = false;
        if (map == null || map.isEmpty()) {
            return merged;
        }
        for (Map.Entry entry : map.entrySet()) {
            String name = (String)entry.getKey();
            List values = (List)entry.getValue();
            if (this.containsKey(name)) {
                merged = true;
            }
            this.addValues(name, values);
        }
        return merged;
    }

    public boolean removeValue(String name, V value) {
        List lo = (List)this.get(name);
        if (lo == null || lo.isEmpty()) {
            return false;
        }
        boolean ret = lo.remove(value);
        if (lo.isEmpty()) {
            this.remove(name);
        } else {
            this.put(name, lo);
        }
        return ret;
    }

    public boolean containsSimpleValue(V value) {
        for (List vals : this.values()) {
            if (vals.size() != 1 || !vals.contains(value)) continue;
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        Iterator iter = this.entrySet().iterator();
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean delim = false;
        while (iter.hasNext()) {
            Map.Entry e = iter.next();
            if (delim) {
                sb.append(", ");
            }
            String key = (String)e.getKey();
            List vals = (List)e.getValue();
            sb.append(key);
            sb.append('=');
            if (vals.size() == 1) {
                sb.append(vals.get(0));
            } else {
                sb.append(vals);
            }
            delim = true;
        }
        sb.append('}');
        return sb.toString();
    }

    public Map<String, String[]> toStringArrayMap() {
        HashMap<String, String[]> map = new HashMap<String, String[]>(this.size() * 3 / 2){

            @Override
            public String toString() {
                StringBuilder b = new StringBuilder();
                b.append('{');
                for (String k : super.keySet()) {
                    if (b.length() > 1) {
                        b.append(',');
                    }
                    b.append(k);
                    b.append('=');
                    b.append(Arrays.asList((Object[])super.get(k)));
                }
                b.append('}');
                return b.toString();
            }
        };
        for (Map.Entry entry : this.entrySet()) {
            String[] a = null;
            if (entry.getValue() != null) {
                a = new String[((List)entry.getValue()).size()];
                a = ((List)entry.getValue()).toArray(a);
            }
            map.put((String)entry.getKey(), a);
        }
        return map;
    }
}

