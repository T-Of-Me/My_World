/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util;

import java.nio.ByteBuffer;
import java.util.Set;

public interface Trie<V> {
    public boolean put(String var1, V var2);

    public boolean put(V var1);

    public V remove(String var1);

    public V get(String var1);

    public V get(String var1, int var2, int var3);

    public V get(ByteBuffer var1);

    public V get(ByteBuffer var1, int var2, int var3);

    public V getBest(String var1);

    public V getBest(String var1, int var2, int var3);

    public V getBest(byte[] var1, int var2, int var3);

    public V getBest(ByteBuffer var1, int var2, int var3);

    public Set<String> keySet();

    public boolean isFull();

    public boolean isCaseInsensitive();

    public void clear();
}

