/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.servlet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class ErrorPageErrorHandler
extends ErrorHandler
implements ErrorHandler.ErrorPageMapper {
    public static final String GLOBAL_ERROR_PAGE = "org.eclipse.jetty.server.error_page.global";
    private static final Logger LOG = Log.getLogger(ErrorPageErrorHandler.class);
    protected ServletContext _servletContext;
    private final Map<String, String> _errorPages = new HashMap<String, String>();
    private final List<ErrorCodeRange> _errorPageList = new ArrayList<ErrorCodeRange>();

    @Override
    public String getErrorPage(HttpServletRequest request) {
        String error_page = null;
        PageLookupTechnique pageSource = null;
        Class<?> matchedThrowable = null;
        Throwable th = (Throwable)request.getAttribute("javax.servlet.error.exception");
        while (error_page == null && th != null) {
            pageSource = PageLookupTechnique.THROWABLE;
            Class<?> exClass = th.getClass();
            error_page = this._errorPages.get(exClass.getName());
            while (error_page == null && (exClass = exClass.getSuperclass()) != null) {
                error_page = this._errorPages.get(exClass.getName());
            }
            if (error_page != null) {
                matchedThrowable = exClass;
            }
            th = th instanceof ServletException ? ((ServletException)th).getRootCause() : null;
        }
        Integer errorStatusCode = null;
        if (error_page == null) {
            pageSource = PageLookupTechnique.STATUS_CODE;
            errorStatusCode = (Integer)request.getAttribute("javax.servlet.error.status_code");
            if (errorStatusCode != null && (error_page = this._errorPages.get(Integer.toString(errorStatusCode))) == null) {
                for (ErrorCodeRange errCode : this._errorPageList) {
                    if (!errCode.isInRange(errorStatusCode)) continue;
                    error_page = errCode.getUri();
                    break;
                }
            }
        }
        if (error_page == null) {
            pageSource = PageLookupTechnique.GLOBAL;
            error_page = this._errorPages.get(GLOBAL_ERROR_PAGE);
        }
        if (LOG.isDebugEnabled()) {
            StringBuilder dbg = new StringBuilder();
            dbg.append("getErrorPage(");
            dbg.append(request.getMethod()).append(' ');
            dbg.append(request.getRequestURI());
            dbg.append(") => error_page=").append(error_page);
            switch (pageSource) {
                case THROWABLE: {
                    dbg.append(" (using matched Throwable ");
                    dbg.append(matchedThrowable.getName());
                    dbg.append(" / actually thrown as ");
                    Throwable originalThrowable = (Throwable)request.getAttribute("javax.servlet.error.exception");
                    dbg.append(originalThrowable.getClass().getName());
                    dbg.append(')');
                    LOG.debug(dbg.toString(), th);
                    break;
                }
                case STATUS_CODE: {
                    dbg.append(" (from status code ");
                    dbg.append(errorStatusCode);
                    dbg.append(')');
                    LOG.debug(dbg.toString(), new Object[0]);
                    break;
                }
                case GLOBAL: {
                    dbg.append(" (from global default)");
                    LOG.debug(dbg.toString(), new Object[0]);
                }
            }
        }
        return error_page;
    }

    public Map<String, String> getErrorPages() {
        return this._errorPages;
    }

    public void setErrorPages(Map<String, String> errorPages) {
        this._errorPages.clear();
        if (errorPages != null) {
            this._errorPages.putAll(errorPages);
        }
    }

    public void addErrorPage(Class<? extends Throwable> exception, String uri) {
        this._errorPages.put(exception.getName(), uri);
    }

    public void addErrorPage(String exceptionClassName, String uri) {
        this._errorPages.put(exceptionClassName, uri);
    }

    public void addErrorPage(int code, String uri) {
        this._errorPages.put(Integer.toString(code), uri);
    }

    public void addErrorPage(int from, int to, String uri) {
        this._errorPageList.add(new ErrorCodeRange(from, to, uri));
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        this._servletContext = ContextHandler.getCurrentContext();
    }

    private static class ErrorCodeRange {
        private int _from;
        private int _to;
        private String _uri;

        ErrorCodeRange(int from, int to, String uri) throws IllegalArgumentException {
            if (from > to) {
                throw new IllegalArgumentException("from>to");
            }
            this._from = from;
            this._to = to;
            this._uri = uri;
        }

        boolean isInRange(int value) {
            return this._from <= value && value <= this._to;
        }

        String getUri() {
            return this._uri;
        }

        public String toString() {
            return "from: " + this._from + ",to: " + this._to + ",uri: " + this._uri;
        }
    }

    private static enum PageLookupTechnique {
        THROWABLE,
        STATUS_CODE,
        GLOBAL;

    }
}

