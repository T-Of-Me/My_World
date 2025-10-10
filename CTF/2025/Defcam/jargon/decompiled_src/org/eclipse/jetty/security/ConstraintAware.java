/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.security;

import java.util.List;
import java.util.Set;
import org.eclipse.jetty.security.ConstraintMapping;

public interface ConstraintAware {
    public List<ConstraintMapping> getConstraintMappings();

    public Set<String> getRoles();

    public void setConstraintMappings(List<ConstraintMapping> var1, Set<String> var2);

    public void addConstraintMapping(ConstraintMapping var1);

    public void addRole(String var1);

    public void setDenyUncoveredHttpMethods(boolean var1);

    public boolean isDenyUncoveredHttpMethods();

    public boolean checkPathsWithUncoveredHttpMethods();
}

