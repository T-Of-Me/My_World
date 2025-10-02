/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.QuietServletException;
import org.eclipse.jetty.server.UserIdentity;

public interface Authentication {
    public static final Authentication UNAUTHENTICATED = new Authentication(){

        public String toString() {
            return "UNAUTHENTICATED";
        }
    };
    public static final Authentication NOT_CHECKED = new Authentication(){

        public String toString() {
            return "NOT CHECKED";
        }
    };
    public static final Authentication SEND_CONTINUE = new Challenge(){

        public String toString() {
            return "CHALLENGE";
        }
    };
    public static final Authentication SEND_FAILURE = new Failure(){

        public String toString() {
            return "FAILURE";
        }
    };
    public static final Authentication SEND_SUCCESS = new SendSuccess(){

        public String toString() {
            return "SEND_SUCCESS";
        }
    };

    public static interface SendSuccess
    extends ResponseSent {
    }

    public static interface Failure
    extends ResponseSent {
    }

    public static interface Challenge
    extends ResponseSent {
    }

    public static interface ResponseSent
    extends Authentication {
    }

    public static interface Deferred
    extends Authentication {
        public Authentication authenticate(ServletRequest var1);

        public Authentication authenticate(ServletRequest var1, ServletResponse var2);

        public Authentication login(String var1, Object var2, ServletRequest var3);
    }

    public static interface Wrapped
    extends Authentication {
        public HttpServletRequest getHttpServletRequest();

        public HttpServletResponse getHttpServletResponse();
    }

    public static interface User
    extends Authentication {
        public String getAuthMethod();

        public UserIdentity getUserIdentity();

        public boolean isUserInRole(UserIdentity.Scope var1, String var2);

        public void logout();
    }

    public static class Failed
    extends QuietServletException {
        public Failed(String message) {
            super(message);
        }
    }
}

