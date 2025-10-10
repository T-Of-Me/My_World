/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.security.authentication;

import java.security.Principal;
import javax.security.auth.Subject;

public interface LoginCallback {
    public Subject getSubject();

    public String getUserName();

    public Object getCredential();

    public boolean isSuccess();

    public void setSuccess(boolean var1);

    public Principal getUserPrincipal();

    public void setUserPrincipal(Principal var1);

    public String[] getRoles();

    public void setRoles(String[] var1);

    public void clearPassword();
}

