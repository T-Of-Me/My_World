/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Deprecated
public class ConcurrentHashSet<E>
extends AbstractSet<E>
implements Set<E> {
    private final Map<E, Boolean> _map = new ConcurrentHashMap<E, Boolean>();
    private transient Set<E> _keys = this._map.keySet();

    @Override
    public boolean add(E e) {
        return this._map.put(e, Boolean.TRUE) == null;
    }

    @Override
    public void clear() {
        this._map.clear();
    }

    @Override
    public boolean contains(Object o) {
        return this._map.containsKey(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return this._keys.containsAll(c);
    }

    @Override
    public boolean equals(Object o) {
        return o == this || this._keys.equals(o);
    }

    @Override
    public int hashCode() {
        return this._keys.hashCode();
    }

    @Override
    public boolean isEmpty() {
        return this._map.isEmpty();
    }

    @Override
    public Iterator<E> iterator() {
        return this._keys.iterator();
    }

    @Override
    public boolean remove(Object o) {
        return this._map.remove(o) != null;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return this._keys.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return this._keys.retainAll(c);
    }

    @Override
    public int size() {
        return this._map.size();
    }

    @Override
    public Object[] toArray() {
        return this._keys.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return this._keys.toArray(a);
    }

    @Override
    public String toString() {
        return this._keys.toString();
    }
}

