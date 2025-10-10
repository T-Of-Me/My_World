/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util;

import java.nio.ByteBuffer;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.eclipse.jetty.util.AbstractTrie;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.Trie;

public class ArrayTernaryTrie<V>
extends AbstractTrie<V> {
    private static int LO = 1;
    private static int EQ = 2;
    private static int HI = 3;
    private static final int ROW_SIZE = 4;
    private final char[] _tree;
    private final String[] _key;
    private final V[] _value;
    private char _rows;

    public ArrayTernaryTrie() {
        this(128);
    }

    public ArrayTernaryTrie(boolean insensitive) {
        this(insensitive, 128);
    }

    public ArrayTernaryTrie(int capacity) {
        this(true, capacity);
    }

    public ArrayTernaryTrie(boolean insensitive, int capacity) {
        super(insensitive);
        this._value = new Object[capacity];
        this._tree = new char[capacity * 4];
        this._key = new String[capacity];
    }

    public ArrayTernaryTrie(ArrayTernaryTrie<V> trie, double factor) {
        super(trie.isCaseInsensitive());
        int capacity = (int)((double)trie._value.length * factor);
        this._rows = trie._rows;
        this._value = Arrays.copyOf(trie._value, capacity);
        this._tree = Arrays.copyOf(trie._tree, capacity * 4);
        this._key = Arrays.copyOf(trie._key, capacity);
    }

    @Override
    public void clear() {
        this._rows = '\u0000';
        Arrays.fill(this._value, null);
        Arrays.fill(this._tree, '\u0000');
        Arrays.fill(this._key, null);
    }

    @Override
    public boolean put(String s, V v) {
        char t = '\u0000';
        int limit = s.length();
        int last = 0;
        for (int k = 0; k < limit; ++k) {
            int diff;
            char c = s.charAt(k);
            if (this.isCaseInsensitive() && c < '\u0080') {
                c = StringUtil.lowercases[c];
            }
            do {
                char n;
                int row = 4 * t;
                if (t == this._rows) {
                    this._rows = (char)(this._rows + '\u0001');
                    if (this._rows >= this._key.length) {
                        this._rows = (char)(this._rows - '\u0001');
                        return false;
                    }
                    this._tree[row] = c;
                }
                if ((diff = (n = this._tree[row]) - c) == 0) {
                    last = row + EQ;
                    t = this._tree[last];
                } else if (diff < 0) {
                    last = row + LO;
                    t = this._tree[last];
                } else {
                    last = row + HI;
                    t = this._tree[last];
                }
                if (t != '\u0000') continue;
                t = this._rows;
                this._tree[last] = t;
            } while (diff != 0);
        }
        if (t == this._rows) {
            this._rows = (char)(this._rows + '\u0001');
            if (this._rows >= this._key.length) {
                this._rows = (char)(this._rows - '\u0001');
                return false;
            }
        }
        this._key[t] = v == null ? null : s;
        this._value[t] = v;
        return true;
    }

    @Override
    public V get(String s, int offset, int len) {
        int t = 0;
        int i = 0;
        block0: while (i < len) {
            int diff;
            int row;
            char c = s.charAt(offset + i++);
            if (this.isCaseInsensitive() && c < '\u0080') {
                c = StringUtil.lowercases[c];
            }
            do {
                char n;
                if ((diff = (n = this._tree[row = 4 * t]) - c) != 0) continue;
                t = this._tree[row + EQ];
                if (t != 0) continue block0;
                return null;
            } while ((t = this._tree[row + ArrayTernaryTrie.hilo(diff)]) != 0);
            return null;
        }
        return this._value[t];
    }

    @Override
    public V get(ByteBuffer b, int offset, int len) {
        int t = 0;
        offset += b.position();
        int i = 0;
        block0: while (i < len) {
            int diff;
            int row;
            byte c = (byte)(b.get(offset + i++) & 0x7F);
            if (this.isCaseInsensitive()) {
                c = (byte)StringUtil.lowercases[c];
            }
            do {
                char n;
                if ((diff = (n = this._tree[row = 4 * t]) - c) != 0) continue;
                t = this._tree[row + EQ];
                if (t != 0) continue block0;
                return null;
            } while ((t = this._tree[row + ArrayTernaryTrie.hilo(diff)]) != 0);
            return null;
        }
        return this._value[t];
    }

    @Override
    public V getBest(String s) {
        return this.getBest(0, s, 0, s.length());
    }

    @Override
    public V getBest(String s, int offset, int length) {
        return this.getBest(0, s, offset, length);
    }

    private V getBest(int t, String s, int offset, int len) {
        int node = t;
        int end = offset + len;
        block0: while (offset < end) {
            int diff;
            int row;
            char c = s.charAt(offset++);
            --len;
            if (this.isCaseInsensitive() && c < '\u0080') {
                c = StringUtil.lowercases[c];
            }
            do {
                char n;
                if ((diff = (n = this._tree[row = 4 * t]) - c) != 0) continue;
                t = this._tree[row + EQ];
                if (t == 0) break block0;
                if (this._key[t] == null) continue block0;
                node = t;
                V better = this.getBest(t, s, offset, len);
                if (better == null) continue block0;
                return better;
            } while ((t = this._tree[row + ArrayTernaryTrie.hilo(diff)]) != 0);
            break;
        }
        return this._value[node];
    }

    @Override
    public V getBest(ByteBuffer b, int offset, int len) {
        if (b.hasArray()) {
            return this.getBest(0, b.array(), b.arrayOffset() + b.position() + offset, len);
        }
        return this.getBest(0, b, offset, len);
    }

    private V getBest(int t, byte[] b, int offset, int len) {
        int node = t;
        int end = offset + len;
        block0: while (offset < end) {
            int diff;
            int row;
            byte c = (byte)(b[offset++] & 0x7F);
            --len;
            if (this.isCaseInsensitive()) {
                c = (byte)StringUtil.lowercases[c];
            }
            do {
                char n;
                if ((diff = (n = this._tree[row = 4 * t]) - c) != 0) continue;
                t = this._tree[row + EQ];
                if (t == 0) break block0;
                if (this._key[t] == null) continue block0;
                node = t;
                V better = this.getBest(t, b, offset, len);
                if (better == null) continue block0;
                return better;
            } while ((t = this._tree[row + ArrayTernaryTrie.hilo(diff)]) != 0);
            break;
        }
        return this._value[node];
    }

    private V getBest(int t, ByteBuffer b, int offset, int len) {
        int node = t;
        int o = offset + b.position();
        block0: for (int i = 0; i < len; ++i) {
            int diff;
            int row;
            byte c = (byte)(b.get(o + i) & 0x7F);
            if (this.isCaseInsensitive()) {
                c = (byte)StringUtil.lowercases[c];
            }
            do {
                char n;
                if ((diff = (n = this._tree[row = 4 * t]) - c) != 0) continue;
                t = this._tree[row + EQ];
                if (t == 0) break block0;
                if (this._key[t] == null) continue block0;
                node = t;
                V best = this.getBest(t, b, offset + i + 1, len - i - 1);
                if (best == null) continue block0;
                return best;
            } while ((t = this._tree[row + ArrayTernaryTrie.hilo(diff)]) != 0);
            break;
        }
        return this._value[node];
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (int r = 0; r <= this._rows; ++r) {
            if (this._key[r] == null || this._value[r] == null) continue;
            buf.append(',');
            buf.append(this._key[r]);
            buf.append('=');
            buf.append(this._value[r].toString());
        }
        if (buf.length() == 0) {
            return "{}";
        }
        buf.setCharAt(0, '{');
        buf.append('}');
        return buf.toString();
    }

    @Override
    public Set<String> keySet() {
        HashSet<String> keys = new HashSet<String>();
        for (int r = 0; r <= this._rows; ++r) {
            if (this._key[r] == null || this._value[r] == null) continue;
            keys.add(this._key[r]);
        }
        return keys;
    }

    public int size() {
        int s = 0;
        for (int r = 0; r <= this._rows; ++r) {
            if (this._key[r] == null || this._value[r] == null) continue;
            ++s;
        }
        return s;
    }

    public boolean isEmpty() {
        for (int r = 0; r <= this._rows; ++r) {
            if (this._key[r] == null || this._value[r] == null) continue;
            return false;
        }
        return true;
    }

    public Set<Map.Entry<String, V>> entrySet() {
        HashSet<Map.Entry<String, V>> entries = new HashSet<Map.Entry<String, V>>();
        for (int r = 0; r <= this._rows; ++r) {
            if (this._key[r] == null || this._value[r] == null) continue;
            entries.add(new AbstractMap.SimpleEntry<String, V>(this._key[r], this._value[r]));
        }
        return entries;
    }

    @Override
    public boolean isFull() {
        return this._rows + '\u0001' == this._key.length;
    }

    public static int hilo(int diff) {
        return 1 + (diff | Integer.MAX_VALUE) / 0x3FFFFFFF;
    }

    public void dump() {
        for (int r = 0; r < this._rows; ++r) {
            char c = this._tree[r * 4 + 0];
            System.err.printf("%4d [%s,%d,%d,%d] '%s':%s%n", r, c < ' ' || c > '\u007f' ? "" + c : "'" + c + "'", (int)this._tree[r * 4 + LO], (int)this._tree[r * 4 + EQ], (int)this._tree[r * 4 + HI], this._key[r], this._value[r]);
        }
    }

    public static class Growing<V>
    implements Trie<V> {
        private final int _growby;
        private ArrayTernaryTrie<V> _trie;

        public Growing() {
            this(1024, 1024);
        }

        public Growing(int capacity, int growby) {
            this._growby = growby;
            this._trie = new ArrayTernaryTrie(capacity);
        }

        public Growing(boolean insensitive, int capacity, int growby) {
            this._growby = growby;
            this._trie = new ArrayTernaryTrie(insensitive, capacity);
        }

        @Override
        public boolean put(V v) {
            return this.put(v.toString(), v);
        }

        public int hashCode() {
            return this._trie.hashCode();
        }

        @Override
        public V remove(String s) {
            return this._trie.remove(s);
        }

        @Override
        public V get(String s) {
            return this._trie.get(s);
        }

        @Override
        public V get(ByteBuffer b) {
            return this._trie.get(b);
        }

        @Override
        public V getBest(byte[] b, int offset, int len) {
            return this._trie.getBest(b, offset, len);
        }

        @Override
        public boolean isCaseInsensitive() {
            return this._trie.isCaseInsensitive();
        }

        public boolean equals(Object obj) {
            return this._trie.equals(obj);
        }

        @Override
        public void clear() {
            this._trie.clear();
        }

        @Override
        public boolean put(String s, V v) {
            boolean added = this._trie.put(s, v);
            while (!added && this._growby > 0) {
                ArrayTernaryTrie<V> bigger = new ArrayTernaryTrie<V>(((ArrayTernaryTrie)this._trie)._key.length + this._growby);
                for (Map.Entry<String, V> entry : this._trie.entrySet()) {
                    bigger.put(entry.getKey(), entry.getValue());
                }
                this._trie = bigger;
                added = this._trie.put(s, v);
            }
            return added;
        }

        @Override
        public V get(String s, int offset, int len) {
            return this._trie.get(s, offset, len);
        }

        @Override
        public V get(ByteBuffer b, int offset, int len) {
            return this._trie.get(b, offset, len);
        }

        @Override
        public V getBest(String s) {
            return this._trie.getBest(s);
        }

        @Override
        public V getBest(String s, int offset, int length) {
            return this._trie.getBest(s, offset, length);
        }

        @Override
        public V getBest(ByteBuffer b, int offset, int len) {
            return this._trie.getBest(b, offset, len);
        }

        public String toString() {
            return this._trie.toString();
        }

        @Override
        public Set<String> keySet() {
            return this._trie.keySet();
        }

        @Override
        public boolean isFull() {
            return false;
        }

        public void dump() {
            this._trie.dump();
        }

        public boolean isEmpty() {
            return this._trie.isEmpty();
        }

        public int size() {
            return this._trie.size();
        }
    }
}

