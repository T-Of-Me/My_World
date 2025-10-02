/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.component;

import java.io.IOException;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.annotation.ManagedOperation;

@ManagedObject(value="Dumpable Object")
public interface Dumpable {
    @ManagedOperation(value="Dump the nested Object state as a String", impact="INFO")
    public String dump();

    public void dump(Appendable var1, String var2) throws IOException;
}

