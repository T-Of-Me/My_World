/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.security;

import javax.servlet.ServletRequest;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.server.UserIdentity;

public interface LoginService {
    public String getName();

    public UserIdentity login(String var1, Object var2, ServletRequest var3);

    public boolean validate(UserIdentity var1);

    public IdentityService getIdentityService();

    public void setIdentityService(IdentityService var1);

    public void logout(UserIdentity var1);
}

