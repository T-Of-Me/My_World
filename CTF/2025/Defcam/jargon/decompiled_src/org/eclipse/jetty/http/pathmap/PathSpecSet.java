/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.http.pathmap;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.function.Predicate;
import org.eclipse.jetty.http.pathmap.MappedResource;
import org.eclipse.jetty.http.pathmap.PathMappings;
import org.eclipse.jetty.http.pathmap.PathSpec;

public class PathSpecSet
extends AbstractSet<String>
implements Predicate<String> {
    private final PathMappings<Boolean> specs = new PathMappings();

    @Override
    public boolean test(String s) {
        return this.specs.getMatch(s) != null;
    }

    @Override
    public int size() {
        return this.specs.size();
    }

    private PathSpec asPathSpec(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof PathSpec) {
            return (PathSpec)o;
        }
        if (o instanceof String) {
            return PathMappings.asPathSpec((String)o);
        }
        return PathMappings.asPathSpec(o.toString());
    }

    @Override
    public boolean add(String s) {
        return this.specs.put(PathMappings.asPathSpec(s), Boolean.TRUE);
    }

    @Override
    public boolean remove(Object o) {
        return this.specs.remove(this.asPathSpec(o));
    }

    @Override
    public void clear() {
        this.specs.reset();
    }

    @Override
    public Iterator<String> iterator() {
        final Iterator<MappedResource<Boolean>> iterator = this.specs.iterator();
        return new Iterator<String>(){

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public String next() {
                return ((MappedResource)iterator.next()).getPathSpec().getDeclaration();
            }
        };
    }
}

