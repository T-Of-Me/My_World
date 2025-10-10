/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.preventers;

import java.security.Security;
import org.eclipse.jetty.util.preventers.AbstractLeakPreventer;

public class SecurityProviderLeakPreventer
extends AbstractLeakPreventer {
    @Override
    public void prevent(ClassLoader loader) {
        Security.getProviders();
    }
}

