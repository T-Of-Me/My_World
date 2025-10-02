/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.preventers;

import org.eclipse.jetty.util.preventers.AbstractLeakPreventer;

public class LDAPLeakPreventer
extends AbstractLeakPreventer {
    @Override
    public void prevent(ClassLoader loader) {
        try {
            Class.forName("com.sun.jndi.LdapPoolManager", true, loader);
        }
        catch (ClassNotFoundException e) {
            LOG.ignore(e);
        }
    }
}

