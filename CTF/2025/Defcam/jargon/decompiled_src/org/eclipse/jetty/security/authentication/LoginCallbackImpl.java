/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.security.authentication;

import java.security.Principal;
import javax.security.auth.Subject;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.authentication.LoginCallback;

public class LoginCallbackImpl
implements LoginCallback {
    private final Subject subject;
    private final String userName;
    private Object credential;
    private boolean success;
    private Principal userPrincipal;
    private String[] roles = IdentityService.NO_ROLES;

    public LoginCallbackImpl(Subject subject, String userName, Object credential) {
        this.subject = subject;
        this.userName = userName;
        this.credential = credential;
    }

    @Override
    public Subject getSubject() {
        return this.subject;
    }

    @Override
    public String getUserName() {
        return this.userName;
    }

    @Override
    public Object getCredential() {
        return this.credential;
    }

    @Override
    public boolean isSuccess() {
        return this.success;
    }

    @Override
    public void setSuccess(boolean success) {
        this.success = success;
    }

    @Override
    public Principal getUserPrincipal() {
        return this.userPrincipal;
    }

    @Override
    public void setUserPrincipal(Principal userPrincipal) {
        this.userPrincipal = userPrincipal;
    }

    @Override
    public String[] getRoles() {
        return this.roles;
    }

    @Override
    public void setRoles(String[] groups) {
        this.roles = groups;
    }

    @Override
    public void clearPassword() {
        if (this.credential != null) {
            this.credential = null;
        }
    }
}

