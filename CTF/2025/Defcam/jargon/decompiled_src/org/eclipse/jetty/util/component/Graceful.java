/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.component;

import java.util.concurrent.Future;

public interface Graceful {
    public Future<Void> shutdown();
}

