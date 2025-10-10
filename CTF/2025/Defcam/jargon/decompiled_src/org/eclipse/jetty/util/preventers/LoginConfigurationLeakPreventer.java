/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.preventers;

import org.eclipse.jetty.util.preventers.AbstractLeakPreventer;

public class LoginConfigurationLeakPreventer
extends AbstractLeakPreventer {
    @Override
    public void prevent(ClassLoader loader) {
        try {
            Class.forName("javax.security.auth.login.Configuration", true, loader);
        }
        catch (ClassNotFoundException e) {
            LOG.warn(e);
        }
    }
}

