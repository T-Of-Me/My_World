/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.component;

import java.io.IOException;
import java.util.Collection;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.component.Dumpable;

public class DumpableCollection
implements Dumpable {
    private final String _name;
    private final Collection<?> _collection;

    public DumpableCollection(String name, Collection<?> collection) {
        this._name = name;
        this._collection = collection;
    }

    @Override
    public String dump() {
        return ContainerLifeCycle.dump(this);
    }

    @Override
    public void dump(Appendable out, String indent) throws IOException {
        out.append(this._name).append("\n");
        if (this._collection != null) {
            ContainerLifeCycle.dump(out, indent, this._collection);
        }
    }
}

