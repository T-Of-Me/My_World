/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util;

import java.util.Set;
import java.util.function.Predicate;
import org.eclipse.jetty.util.IncludeExcludeSet;

public class IncludeExclude<ITEM>
extends IncludeExcludeSet<ITEM, ITEM> {
    public IncludeExclude() {
    }

    public <SET extends Set<ITEM>> IncludeExclude(Class<SET> setClass) {
        super(setClass);
    }

    public <SET extends Set<ITEM>> IncludeExclude(Set<ITEM> includeSet, Predicate<ITEM> includePredicate, Set<ITEM> excludeSet, Predicate<ITEM> excludePredicate) {
        super(includeSet, includePredicate, excludeSet, excludePredicate);
    }
}

