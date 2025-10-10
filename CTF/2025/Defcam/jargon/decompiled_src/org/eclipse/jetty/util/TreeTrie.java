/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.jetty.util.AbstractTrie;

public class TreeTrie<V>
extends AbstractTrie<V> {
    private static final int[] __lookup = new int[]{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 31, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 26, -1, 27, 30, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 28, 29, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1};
    private static final int INDEX = 32;
    private final TreeTrie<V>[] _nextIndex;
    private final List<TreeTrie<V>> _nextOther = new ArrayList<TreeTrie<V>>();
    private final char _c;
    private String _key;
    private V _value;

    public TreeTrie() {
        super(true);
        this._nextIndex = new TreeTrie[32];
        this._c = '\u0000';
    }

    private TreeTrie(char c) {
        super(true);
        this._nextIndex = new TreeTrie[32];
        this._c = c;
    }

    @Override
    public void clear() {
        Arrays.fill(this._nextIndex, null);
        this._nextOther.clear();
        this._key = null;
        this._value = null;
    }

    @Override
    public boolean put(String s, V v) {
        TreeTrie<V> t = this;
        int limit = s.length();
        for (int k = 0; k < limit; ++k) {
            int index;
            char c = s.charAt(k);
            int n = index = c >= '\u0000' && c < '\u007f' ? __lookup[c] : -1;
            if (index >= 0) {
                if (t._nextIndex[index] == null) {
                    t._nextIndex[index] = new TreeTrie<V>(c);
                }
                t = t._nextIndex[index];
                continue;
            }
            TreeTrie<V> n2 = null;
            int i = t._nextOther.size();
            while (i-- > 0) {
                n2 = t._nextOther.get(i);
                if (n2._c == c) break;
                n2 = null;
            }
            if (n2 == null) {
                n2 = new TreeTrie<V>(c);
                t._nextOther.add(n2);
            }
            t = n2;
        }
        t._key = v == null ? null : s;
        t._value = v;
        return true;
    }

    @Override
    public V get(String s, int offset, int len) {
        TreeTrie<V> t = this;
        for (int i = 0; i < len; ++i) {
            int index;
            char c = s.charAt(offset + i);
            int n = index = c >= '\u0000' && c < '\u007f' ? __lookup[c] : -1;
            if (index >= 0) {
                if (t._nextIndex[index] == null) {
                    return null;
                }
                t = t._nextIndex[index];
                continue;
            }
            TreeTrie<V> n2 = null;
            int j = t._nextOther.size();
            while (j-- > 0) {
                n2 = t._nextOther.get(j);
                if (n2._c == c) break;
                n2 = null;
            }
            if (n2 == null) {
                return null;
            }
            t = n2;
        }
        return t._value;
    }

    @Override
    public V get(ByteBuffer b, int offset, int len) {
        TreeTrie<V> t = this;
        for (int i = 0; i < len; ++i) {
            int index;
            byte c = b.get(offset + i);
            int n = index = c >= 0 && c < 127 ? __lookup[c] : -1;
            if (index >= 0) {
                if (t._nextIndex[index] == null) {
                    return null;
                }
                t = t._nextIndex[index];
                continue;
            }
            TreeTrie<V> n2 = null;
            int j = t._nextOther.size();
            while (j-- > 0) {
                n2 = t._nextOther.get(j);
                if (n2._c == c) break;
                n2 = null;
            }
            if (n2 == null) {
                return null;
            }
            t = n2;
        }
        return t._value;
    }

    @Override
    public V getBest(byte[] b, int offset, int len) {
        TreeTrie<V> t = this;
        for (int i = 0; i < len; ++i) {
            int index;
            byte c = b[offset + i];
            int n = index = c >= 0 && c < 127 ? __lookup[c] : -1;
            if (index >= 0) {
                if (t._nextIndex[index] == null) break;
                t = t._nextIndex[index];
            } else {
                TreeTrie<V> n2 = null;
                int j = t._nextOther.size();
                while (j-- > 0) {
                    n2 = t._nextOther.get(j);
                    if (n2._c == c) break;
                    n2 = null;
                }
                if (n2 == null) break;
                t = n2;
            }
            if (t._key == null) continue;
            V best = t.getBest(b, offset + i + 1, len - i - 1);
            if (best == null) break;
            return best;
        }
        return t._value;
    }

    @Override
    public V getBest(String s, int offset, int len) {
        TreeTrie<V> t = this;
        for (int i = 0; i < len; ++i) {
            int index;
            byte c = (byte)(0xFF & s.charAt(offset + i));
            int n = index = c >= 0 && c < 127 ? __lookup[c] : -1;
            if (index >= 0) {
                if (t._nextIndex[index] == null) break;
                t = t._nextIndex[index];
            } else {
                TreeTrie<V> n2 = null;
                int j = t._nextOther.size();
                while (j-- > 0) {
                    n2 = t._nextOther.get(j);
                    if (n2._c == c) break;
                    n2 = null;
                }
                if (n2 == null) break;
                t = n2;
            }
            if (t._key == null) continue;
            V best = t.getBest(s, offset + i + 1, len - i - 1);
            if (best == null) break;
            return best;
        }
        return t._value;
    }

    @Override
    public V getBest(ByteBuffer b, int offset, int len) {
        if (b.hasArray()) {
            return this.getBest(b.array(), b.arrayOffset() + b.position() + offset, len);
        }
        return this.getBestByteBuffer(b, offset, len);
    }

    private V getBestByteBuffer(ByteBuffer b, int offset, int len) {
        TreeTrie<V> t = this;
        int pos = b.position() + offset;
        for (int i = 0; i < len; ++i) {
            byte c;
            int index;
            int n = index = (c = b.get(pos++)) >= 0 && c < 127 ? __lookup[c] : -1;
            if (index >= 0) {
                if (t._nextIndex[index] == null) break;
                t = t._nextIndex[index];
            } else {
                TreeTrie<V> n2 = null;
                int j = t._nextOther.size();
                while (j-- > 0) {
                    n2 = t._nextOther.get(j);
                    if (n2._c == c) break;
                    n2 = null;
                }
                if (n2 == null) break;
                t = n2;
            }
            if (t._key == null) continue;
            V best = t.getBest(b, offset + i + 1, len - i - 1);
            if (best == null) break;
            return best;
        }
        return t._value;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        TreeTrie.toString(buf, this);
        if (buf.length() == 0) {
            return "{}";
        }
        buf.setCharAt(0, '{');
        buf.append('}');
        return buf.toString();
    }

    private static <V> void toString(Appendable out, TreeTrie<V> t) {
        if (t != null) {
            int i;
            if (t._value != null) {
                try {
                    out.append(',');
                    out.append(t._key);
                    out.append('=');
                    out.append(t._value.toString());
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            for (i = 0; i < 32; ++i) {
                if (t._nextIndex[i] == null) continue;
                TreeTrie.toString(out, t._nextIndex[i]);
            }
            i = t._nextOther.size();
            while (i-- > 0) {
                TreeTrie.toString(out, t._nextOther.get(i));
            }
        }
    }

    @Override
    public Set<String> keySet() {
        HashSet<String> keys = new HashSet<String>();
        TreeTrie.keySet(keys, this);
        return keys;
    }

    private static <V> void keySet(Set<String> set, TreeTrie<V> t) {
        if (t != null) {
            int i;
            if (t._key != null) {
                set.add(t._key);
            }
            for (i = 0; i < 32; ++i) {
                if (t._nextIndex[i] == null) continue;
                TreeTrie.keySet(set, t._nextIndex[i]);
            }
            i = t._nextOther.size();
            while (i-- > 0) {
                TreeTrie.keySet(set, t._nextOther.get(i));
            }
        }
    }

    @Override
    public boolean isFull() {
        return false;
    }
}

