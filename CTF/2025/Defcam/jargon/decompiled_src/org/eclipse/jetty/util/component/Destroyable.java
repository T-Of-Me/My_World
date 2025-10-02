/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.component;

import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.annotation.ManagedOperation;

@ManagedObject
public interface Destroyable {
    @ManagedOperation(value="Destroys this component", impact="ACTION")
    public void destroy();
}

