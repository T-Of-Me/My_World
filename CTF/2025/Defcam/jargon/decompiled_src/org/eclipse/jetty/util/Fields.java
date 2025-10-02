/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class Fields
implements Iterable<Field> {
    private final boolean caseSensitive;
    private final Map<String, Field> fields;

    public Fields() {
        this(false);
    }

    public Fields(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
        this.fields = new LinkedHashMap<String, Field>();
    }

    public Fields(Fields original, boolean immutable) {
        this.caseSensitive = original.caseSensitive;
        LinkedHashMap<String, Field> copy = new LinkedHashMap<String, Field>();
        copy.putAll(original.fields);
        this.fields = immutable ? Collections.unmodifiableMap(copy) : copy;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        Fields that = (Fields)obj;
        if (this.getSize() != that.getSize()) {
            return false;
        }
        if (this.caseSensitive != that.caseSensitive) {
            return false;
        }
        for (Map.Entry<String, Field> entry : this.fields.entrySet()) {
            String name = entry.getKey();
            Field value = entry.getValue();
            if (value.equals(that.get(name), this.caseSensitive)) continue;
            return false;
        }
        return true;
    }

    public int hashCode() {
        return this.fields.hashCode();
    }

    public Set<String> getNames() {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        for (Field field : this.fields.values()) {
            result.add(field.getName());
        }
        return result;
    }

    private String normalizeName(String name) {
        return this.caseSensitive ? name : name.toLowerCase(Locale.ENGLISH);
    }

    public Field get(String name) {
        return this.fields.get(this.normalizeName(name));
    }

    public void put(String name, String value) {
        Field field = new Field(name, value);
        this.fields.put(this.normalizeName(name), field);
    }

    public void put(Field field) {
        if (field != null) {
            this.fields.put(this.normalizeName(field.getName()), field);
        }
    }

    public void add(String name, String value) {
        String key = this.normalizeName(name);
        Field field = this.fields.get(key);
        if (field == null) {
            field = new Field(name, value);
            this.fields.put(key, field);
        } else {
            field = new Field(field.getName(), (List)field.getValues(), new String[]{value});
            this.fields.put(key, field);
        }
    }

    public Field remove(String name) {
        return this.fields.remove(this.normalizeName(name));
    }

    public void clear() {
        this.fields.clear();
    }

    public boolean isEmpty() {
        return this.fields.isEmpty();
    }

    public int getSize() {
        return this.fields.size();
    }

    @Override
    public Iterator<Field> iterator() {
        return this.fields.values().iterator();
    }

    public String toString() {
        return this.fields.toString();
    }

    public static class Field {
        private final String name;
        private final List<String> values;

        public Field(String name, String value) {
            this(name, Collections.singletonList(value), new String[0]);
        }

        private Field(String name, List<String> values, String ... moreValues) {
            this.name = name;
            ArrayList<String> list = new ArrayList<String>(values.size() + moreValues.length);
            list.addAll(values);
            list.addAll(Arrays.asList(moreValues));
            this.values = Collections.unmodifiableList(list);
        }

        public boolean equals(Field that, boolean caseSensitive) {
            if (this == that) {
                return true;
            }
            if (that == null) {
                return false;
            }
            if (caseSensitive) {
                return this.equals(that);
            }
            return this.name.equalsIgnoreCase(that.name) && this.values.equals(that.values);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || this.getClass() != obj.getClass()) {
                return false;
            }
            Field that = (Field)obj;
            return this.name.equals(that.name) && this.values.equals(that.values);
        }

        public int hashCode() {
            int result = this.name.hashCode();
            result = 31 * result + this.values.hashCode();
            return result;
        }

        public String getName() {
            return this.name;
        }

        public String getValue() {
            return this.values.get(0);
        }

        public Integer getValueAsInt() {
            String value = this.getValue();
            return value == null ? null : Integer.valueOf(value);
        }

        public List<String> getValues() {
            return this.values;
        }

        public boolean hasMultipleValues() {
            return this.values.size() > 1;
        }

        public String toString() {
            return String.format("%s=%s", this.name, this.values);
        }
    }
}

