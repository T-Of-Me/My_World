/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.http.pathmap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import org.eclipse.jetty.http.pathmap.MappedResource;
import org.eclipse.jetty.http.pathmap.PathSpec;
import org.eclipse.jetty.http.pathmap.PathSpecGroup;
import org.eclipse.jetty.http.pathmap.RegexPathSpec;
import org.eclipse.jetty.http.pathmap.ServletPathSpec;
import org.eclipse.jetty.util.ArrayTernaryTrie;
import org.eclipse.jetty.util.Trie;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.component.Dumpable;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

@ManagedObject(value="Path Mappings")
public class PathMappings<E>
implements Iterable<MappedResource<E>>,
Dumpable {
    private static final Logger LOG = Log.getLogger(PathMappings.class);
    private final Set<MappedResource<E>> _mappings = new TreeSet<MappedResource<E>>();
    private Trie<MappedResource<E>> _exactMap = new ArrayTernaryTrie<MappedResource<E>>(false);
    private Trie<MappedResource<E>> _prefixMap = new ArrayTernaryTrie<MappedResource<E>>(false);
    private Trie<MappedResource<E>> _suffixMap = new ArrayTernaryTrie<MappedResource<E>>(false);

    @Override
    public String dump() {
        return ContainerLifeCycle.dump(this);
    }

    @Override
    public void dump(Appendable out, String indent) throws IOException {
        out.append("PathMappings[size=").append(Integer.toString(this._mappings.size())).append("]\n");
        ContainerLifeCycle.dump(out, indent, this._mappings);
    }

    @ManagedAttribute(value="mappings", readonly=true)
    public List<MappedResource<E>> getMappings() {
        return new ArrayList<MappedResource<E>>(this._mappings);
    }

    public int size() {
        return this._mappings.size();
    }

    public void reset() {
        this._mappings.clear();
        this._prefixMap.clear();
        this._suffixMap.clear();
    }

    public void removeIf(Predicate<MappedResource<E>> predicate) {
        this._mappings.removeIf(predicate);
    }

    public List<MappedResource<E>> getMatches(String path) {
        boolean isRootPath = "/".equals(path);
        ArrayList<MappedResource<MappedResource<E>>> ret = new ArrayList<MappedResource<MappedResource<E>>>();
        block4: for (MappedResource<E> mr : this._mappings) {
            switch (mr.getPathSpec().group) {
                case ROOT: {
                    if (!isRootPath) continue block4;
                    ret.add(mr);
                    continue block4;
                }
                case DEFAULT: {
                    if (!isRootPath && !mr.getPathSpec().matches(path)) continue block4;
                    ret.add(mr);
                    continue block4;
                }
            }
            if (!mr.getPathSpec().matches(path)) continue;
            ret.add(mr);
        }
        return ret;
    }

    public MappedResource<E> getMatch(String path) {
        PathSpecGroup last_group = null;
        for (MappedResource<E> mr : this._mappings) {
            PathSpecGroup group = mr.getPathSpec().getGroup();
            if (group != last_group) {
                switch (group) {
                    case EXACT: {
                        MappedResource<E> candidate;
                        int i = path.length();
                        Trie<MappedResource<E>> exact_map = this._exactMap;
                        while (i >= 0 && (candidate = exact_map.getBest(path, 0, i)) != null) {
                            if (candidate.getPathSpec().matches(path)) {
                                return candidate;
                            }
                            i = candidate.getPathSpec().getPrefix().length() - 1;
                        }
                        break;
                    }
                    case PREFIX_GLOB: {
                        MappedResource<E> candidate;
                        int i = path.length();
                        Trie<MappedResource<E>> prefix_map = this._prefixMap;
                        while (i >= 0 && (candidate = prefix_map.getBest(path, 0, i)) != null) {
                            if (candidate.getPathSpec().matches(path)) {
                                return candidate;
                            }
                            i = candidate.getPathSpec().getPrefix().length() - 1;
                        }
                        break;
                    }
                    case SUFFIX_GLOB: {
                        MappedResource<E> candidate;
                        int i = 0;
                        Trie<MappedResource<E>> suffix_map = this._suffixMap;
                        while ((i = path.indexOf(46, i + 1)) > 0) {
                            candidate = suffix_map.get(path, i + 1, path.length() - i - 1);
                            if (candidate == null || !candidate.getPathSpec().matches(path)) continue;
                            return candidate;
                        }
                        break;
                    }
                }
            }
            if (mr.getPathSpec().matches(path)) {
                return mr;
            }
            last_group = group;
        }
        return null;
    }

    @Override
    public Iterator<MappedResource<E>> iterator() {
        return this._mappings.iterator();
    }

    public static PathSpec asPathSpec(String pathSpecString) {
        if (pathSpecString == null || pathSpecString.length() < 1) {
            throw new RuntimeException("Path Spec String must start with '^', '/', or '*.': got [" + pathSpecString + "]");
        }
        return pathSpecString.charAt(0) == '^' ? new RegexPathSpec(pathSpecString) : new ServletPathSpec(pathSpecString);
    }

    public boolean put(String pathSpecString, E resource) {
        return this.put(PathMappings.asPathSpec(pathSpecString), resource);
    }

    public boolean put(PathSpec pathSpec, E resource) {
        MappedResource<E> entry = new MappedResource<E>(pathSpec, resource);
        switch (pathSpec.group) {
            case EXACT: {
                String exact = pathSpec.getPrefix();
                while (exact != null && !this._exactMap.put(exact, entry)) {
                    this._exactMap = new ArrayTernaryTrie<MappedResource<E>>((ArrayTernaryTrie)this._exactMap, 1.5);
                }
                break;
            }
            case PREFIX_GLOB: {
                String prefix = pathSpec.getPrefix();
                while (prefix != null && !this._prefixMap.put(prefix, entry)) {
                    this._prefixMap = new ArrayTernaryTrie<MappedResource<E>>((ArrayTernaryTrie)this._prefixMap, 1.5);
                }
                break;
            }
            case SUFFIX_GLOB: {
                String suffix = pathSpec.getSuffix();
                while (suffix != null && !this._suffixMap.put(suffix, entry)) {
                    this._suffixMap = new ArrayTernaryTrie<MappedResource<E>>((ArrayTernaryTrie)this._prefixMap, 1.5);
                }
                break;
            }
        }
        boolean added = this._mappings.add(entry);
        if (LOG.isDebugEnabled()) {
            LOG.debug("{} {} to {}", added ? "Added" : "Ignored", entry, this);
        }
        return added;
    }

    public boolean remove(PathSpec pathSpec) {
        switch (pathSpec.group) {
            case EXACT: {
                this._exactMap.remove(pathSpec.getPrefix());
                break;
            }
            case PREFIX_GLOB: {
                this._prefixMap.remove(pathSpec.getPrefix());
                break;
            }
            case SUFFIX_GLOB: {
                this._suffixMap.remove(pathSpec.getSuffix());
            }
        }
        Iterator<MappedResource<E>> iter = this._mappings.iterator();
        boolean removed = false;
        while (iter.hasNext()) {
            if (!iter.next().getPathSpec().equals(pathSpec)) continue;
            removed = true;
            iter.remove();
            break;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("{} {} to {}", removed ? "Removed" : "Ignored", pathSpec, this);
        }
        return removed;
    }

    public String toString() {
        return String.format("%s[size=%d]", this.getClass().getSimpleName(), this._mappings.size());
    }
}

