/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.security.authentication;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpSession;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpHeaderValue;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.security.authentication.DeferredAuthentication;
import org.eclipse.jetty.security.authentication.LoginAuthenticator;
import org.eclipse.jetty.security.authentication.SessionAuthentication;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class FormAuthenticator
extends LoginAuthenticator {
    private static final Logger LOG = Log.getLogger(FormAuthenticator.class);
    public static final String __FORM_LOGIN_PAGE = "org.eclipse.jetty.security.form_login_page";
    public static final String __FORM_ERROR_PAGE = "org.eclipse.jetty.security.form_error_page";
    public static final String __FORM_DISPATCH = "org.eclipse.jetty.security.dispatch";
    public static final String __J_URI = "org.eclipse.jetty.security.form_URI";
    public static final String __J_POST = "org.eclipse.jetty.security.form_POST";
    public static final String __J_METHOD = "org.eclipse.jetty.security.form_METHOD";
    public static final String __J_SECURITY_CHECK = "/j_security_check";
    public static final String __J_USERNAME = "j_username";
    public static final String __J_PASSWORD = "j_password";
    private String _formErrorPage;
    private String _formErrorPath;
    private String _formLoginPage;
    private String _formLoginPath;
    private boolean _dispatch;
    private boolean _alwaysSaveUri;

    public FormAuthenticator() {
    }

    public FormAuthenticator(String login, String error, boolean dispatch) {
        this();
        if (login != null) {
            this.setLoginPage(login);
        }
        if (error != null) {
            this.setErrorPage(error);
        }
        this._dispatch = dispatch;
    }

    public void setAlwaysSaveUri(boolean alwaysSave) {
        this._alwaysSaveUri = alwaysSave;
    }

    public boolean getAlwaysSaveUri() {
        return this._alwaysSaveUri;
    }

    @Override
    public void setConfiguration(Authenticator.AuthConfiguration configuration) {
        String dispatch;
        String error;
        super.setConfiguration(configuration);
        String login = configuration.getInitParameter(__FORM_LOGIN_PAGE);
        if (login != null) {
            this.setLoginPage(login);
        }
        if ((error = configuration.getInitParameter(__FORM_ERROR_PAGE)) != null) {
            this.setErrorPage(error);
        }
        this._dispatch = (dispatch = configuration.getInitParameter(__FORM_DISPATCH)) == null ? this._dispatch : Boolean.valueOf(dispatch);
    }

    @Override
    public String getAuthMethod() {
        return "FORM";
    }

    private void setLoginPage(String path) {
        if (!path.startsWith("/")) {
            LOG.warn("form-login-page must start with /", new Object[0]);
            path = "/" + path;
        }
        this._formLoginPage = path;
        this._formLoginPath = path;
        if (this._formLoginPath.indexOf(63) > 0) {
            this._formLoginPath = this._formLoginPath.substring(0, this._formLoginPath.indexOf(63));
        }
    }

    private void setErrorPage(String path) {
        if (path == null || path.trim().length() == 0) {
            this._formErrorPath = null;
            this._formErrorPage = null;
        } else {
            if (!path.startsWith("/")) {
                LOG.warn("form-error-page must start with /", new Object[0]);
                path = "/" + path;
            }
            this._formErrorPage = path;
            this._formErrorPath = path;
            if (this._formErrorPath.indexOf(63) > 0) {
                this._formErrorPath = this._formErrorPath.substring(0, this._formErrorPath.indexOf(63));
            }
        }
    }

    @Override
    public UserIdentity login(String username, Object password, ServletRequest request) {
        UserIdentity user = super.login(username, password, request);
        if (user != null) {
            HttpSession session = ((HttpServletRequest)request).getSession(true);
            SessionAuthentication cached = new SessionAuthentication(this.getAuthMethod(), user, password);
            session.setAttribute("org.eclipse.jetty.security.UserIdentity", cached);
        }
        return user;
    }

    @Override
    public void prepareRequest(ServletRequest request) {
        HttpServletRequest httpRequest = (HttpServletRequest)request;
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute("org.eclipse.jetty.security.UserIdentity") == null) {
            return;
        }
        String juri = (String)session.getAttribute(__J_URI);
        if (juri == null || juri.length() == 0) {
            return;
        }
        String method = (String)session.getAttribute(__J_METHOD);
        if (method == null || method.length() == 0) {
            return;
        }
        StringBuffer buf = httpRequest.getRequestURL();
        if (httpRequest.getQueryString() != null) {
            buf.append("?").append(httpRequest.getQueryString());
        }
        if (!juri.equals(buf.toString())) {
            return;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Restoring original method {} for {} with method {}", method, juri, httpRequest.getMethod());
        }
        Request base_request = Request.getBaseRequest(request);
        base_request.setMethod(method);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public Authentication validateRequest(ServletRequest req, ServletResponse res, boolean mandatory) throws ServerAuthException {
        HttpSession session;
        String uri;
        Response base_response;
        Request base_request;
        HttpServletResponse response;
        HttpServletRequest request;
        block39: {
            request = (HttpServletRequest)req;
            response = (HttpServletResponse)res;
            base_request = Request.getBaseRequest(request);
            base_response = base_request.getResponse();
            uri = request.getRequestURI();
            if (uri == null) {
                uri = "/";
            }
            if (!(mandatory |= this.isJSecurityCheck(uri))) {
                return new DeferredAuthentication(this);
            }
            if (this.isLoginOrErrorPage(URIUtil.addPaths(request.getServletPath(), request.getPathInfo())) && !DeferredAuthentication.isDeferred(response)) {
                return new DeferredAuthentication(this);
            }
            session = null;
            try {
                session = request.getSession(true);
            }
            catch (Exception e) {
                if (!LOG.isDebugEnabled()) break block39;
                LOG.debug(e);
            }
        }
        if (session == null) {
            return Authentication.UNAUTHENTICATED;
        }
        try {
            if (this.isJSecurityCheck(uri)) {
                String username = request.getParameter(__J_USERNAME);
                String password = request.getParameter(__J_PASSWORD);
                UserIdentity user = this.login(username, password, request);
                LOG.debug("jsecuritycheck {} {}", username, user);
                session = request.getSession(true);
                if (user != null) {
                    FormAuthentication form_auth;
                    String nuri;
                    HttpSession httpSession = session;
                    synchronized (httpSession) {
                        nuri = (String)session.getAttribute(__J_URI);
                        if ((nuri == null || nuri.length() == 0) && (nuri = request.getContextPath()).length() == 0) {
                            nuri = "/";
                        }
                        form_auth = new FormAuthentication(this.getAuthMethod(), user);
                    }
                    LOG.debug("authenticated {}->{}", form_auth, nuri);
                    response.setContentLength(0);
                    int redirectCode = base_request.getHttpVersion().getVersion() < HttpVersion.HTTP_1_1.getVersion() ? 302 : 303;
                    base_response.sendRedirect(redirectCode, response.encodeRedirectURL(nuri));
                    return form_auth;
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Form authentication FAILED for " + StringUtil.printable(username), new Object[0]);
                }
                if (this._formErrorPage == null) {
                    LOG.debug("auth failed {}->403", username);
                    if (response != null) {
                        response.sendError(403);
                    }
                } else if (this._dispatch) {
                    LOG.debug("auth failed {}=={}", username, this._formErrorPage);
                    RequestDispatcher dispatcher = request.getRequestDispatcher(this._formErrorPage);
                    response.setHeader(HttpHeader.CACHE_CONTROL.asString(), HttpHeaderValue.NO_CACHE.asString());
                    response.setDateHeader(HttpHeader.EXPIRES.asString(), 1L);
                    dispatcher.forward(new FormRequest(request), new FormResponse(response));
                } else {
                    LOG.debug("auth failed {}->{}", username, this._formErrorPage);
                    int redirectCode = base_request.getHttpVersion().getVersion() < HttpVersion.HTTP_1_1.getVersion() ? 302 : 303;
                    base_response.sendRedirect(redirectCode, response.encodeRedirectURL(URIUtil.addPaths(request.getContextPath(), this._formErrorPage)));
                }
                return Authentication.SEND_FAILURE;
            }
            Authentication authentication = (Authentication)session.getAttribute("org.eclipse.jetty.security.UserIdentity");
            if (authentication != null) {
                if (authentication instanceof Authentication.User && this._loginService != null && !this._loginService.validate(((Authentication.User)authentication).getUserIdentity())) {
                    LOG.debug("auth revoked {}", authentication);
                    session.removeAttribute("org.eclipse.jetty.security.UserIdentity");
                } else {
                    HttpSession password = session;
                    synchronized (password) {
                        String j_uri = (String)session.getAttribute(__J_URI);
                        if (j_uri != null) {
                            LOG.debug("auth retry {}->{}", authentication, j_uri);
                            StringBuffer buf = request.getRequestURL();
                            if (request.getQueryString() != null) {
                                buf.append("?").append(request.getQueryString());
                            }
                            if (j_uri.equals(buf.toString())) {
                                MultiMap j_post = (MultiMap)session.getAttribute(__J_POST);
                                if (j_post != null) {
                                    LOG.debug("auth rePOST {}->{}", authentication, j_uri);
                                    base_request.setContentParameters(j_post);
                                }
                                session.removeAttribute(__J_URI);
                                session.removeAttribute(__J_METHOD);
                                session.removeAttribute(__J_POST);
                            }
                        }
                    }
                    LOG.debug("auth {}", authentication);
                    return authentication;
                }
            }
            if (DeferredAuthentication.isDeferred(response)) {
                LOG.debug("auth deferred {}", session.getId());
                return Authentication.UNAUTHENTICATED;
            }
            HttpSession password = session;
            synchronized (password) {
                if (session.getAttribute(__J_URI) == null || this._alwaysSaveUri) {
                    StringBuffer buf = request.getRequestURL();
                    if (request.getQueryString() != null) {
                        buf.append("?").append(request.getQueryString());
                    }
                    session.setAttribute(__J_URI, buf.toString());
                    session.setAttribute(__J_METHOD, request.getMethod());
                    if (MimeTypes.Type.FORM_ENCODED.is(req.getContentType()) && HttpMethod.POST.is(request.getMethod())) {
                        MultiMap<String> formParameters = new MultiMap<String>();
                        base_request.extractFormParameters(formParameters);
                        session.setAttribute(__J_POST, formParameters);
                    }
                }
            }
            if (this._dispatch) {
                LOG.debug("challenge {}=={}", session.getId(), this._formLoginPage);
                RequestDispatcher dispatcher = request.getRequestDispatcher(this._formLoginPage);
                response.setHeader(HttpHeader.CACHE_CONTROL.asString(), HttpHeaderValue.NO_CACHE.asString());
                response.setDateHeader(HttpHeader.EXPIRES.asString(), 1L);
                dispatcher.forward(new FormRequest(request), new FormResponse(response));
            } else {
                LOG.debug("challenge {}->{}", session.getId(), this._formLoginPage);
                int redirectCode = base_request.getHttpVersion().getVersion() < HttpVersion.HTTP_1_1.getVersion() ? 302 : 303;
                base_response.sendRedirect(redirectCode, response.encodeRedirectURL(URIUtil.addPaths(request.getContextPath(), this._formLoginPage)));
            }
            return Authentication.SEND_CONTINUE;
        }
        catch (IOException | ServletException e) {
            throw new ServerAuthException(e);
        }
    }

    public boolean isJSecurityCheck(String uri) {
        int jsc = uri.indexOf(__J_SECURITY_CHECK);
        if (jsc < 0) {
            return false;
        }
        int e = jsc + __J_SECURITY_CHECK.length();
        if (e == uri.length()) {
            return true;
        }
        char c = uri.charAt(e);
        return c == ';' || c == '#' || c == '/' || c == '?';
    }

    public boolean isLoginOrErrorPage(String pathInContext) {
        return pathInContext != null && (pathInContext.equals(this._formErrorPath) || pathInContext.equals(this._formLoginPath));
    }

    @Override
    public boolean secureResponse(ServletRequest req, ServletResponse res, boolean mandatory, Authentication.User validatedUser) throws ServerAuthException {
        return true;
    }

    public static class FormAuthentication
    extends UserAuthentication
    implements Authentication.ResponseSent {
        public FormAuthentication(String method, UserIdentity userIdentity) {
            super(method, userIdentity);
        }

        @Override
        public String toString() {
            return "Form" + super.toString();
        }
    }

    protected static class FormResponse
    extends HttpServletResponseWrapper {
        public FormResponse(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void addDateHeader(String name, long date) {
            if (this.notIgnored(name)) {
                super.addDateHeader(name, date);
            }
        }

        @Override
        public void addHeader(String name, String value) {
            if (this.notIgnored(name)) {
                super.addHeader(name, value);
            }
        }

        @Override
        public void setDateHeader(String name, long date) {
            if (this.notIgnored(name)) {
                super.setDateHeader(name, date);
            }
        }

        @Override
        public void setHeader(String name, String value) {
            if (this.notIgnored(name)) {
                super.setHeader(name, value);
            }
        }

        private boolean notIgnored(String name) {
            return !HttpHeader.CACHE_CONTROL.is(name) && !HttpHeader.PRAGMA.is(name) && !HttpHeader.ETAG.is(name) && !HttpHeader.EXPIRES.is(name) && !HttpHeader.LAST_MODIFIED.is(name) && !HttpHeader.AGE.is(name);
        }
    }

    protected static class FormRequest
    extends HttpServletRequestWrapper {
        public FormRequest(HttpServletRequest request) {
            super(request);
        }

        @Override
        public long getDateHeader(String name) {
            if (name.toLowerCase(Locale.ENGLISH).startsWith("if-")) {
                return -1L;
            }
            return super.getDateHeader(name);
        }

        @Override
        public String getHeader(String name) {
            if (name.toLowerCase(Locale.ENGLISH).startsWith("if-")) {
                return null;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            return Collections.enumeration(Collections.list(super.getHeaderNames()));
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (name.toLowerCase(Locale.ENGLISH).startsWith("if-")) {
                return Collections.enumeration(Collections.emptyList());
            }
            return super.getHeaders(name);
        }
    }
}

