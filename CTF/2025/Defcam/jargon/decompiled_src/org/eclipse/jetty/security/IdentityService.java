/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.security;

import java.security.Principal;
import javax.security.auth.Subject;
import org.eclipse.jetty.security.RunAsToken;
import org.eclipse.jetty.server.UserIdentity;

public interface IdentityService {
    public static final String[] NO_ROLES = new String[0];

    public Object associate(UserIdentity var1);

    public void disassociate(Object var1);

    public Object setRunAs(UserIdentity var1, RunAsToken var2);

    public void unsetRunAs(Object var1);

    public UserIdentity newUserIdentity(Subject var1, Principal var2, String[] var3);

    public RunAsToken newRunAsToken(String var1);

    public UserIdentity getSystemUserIdentity();
}

