/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.security;

import org.eclipse.jetty.util.security.Credential;

public interface CredentialProvider {
    public Credential getCredential(String var1);

    public String getPrefix();
}

