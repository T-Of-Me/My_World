/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.IllegalSelectorException;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.eclipse.jetty.http.CookieCompliance;
import org.eclipse.jetty.http.DateGenerator;
import org.eclipse.jetty.http.HttpContent;
import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpGenerator;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpHeaderValue;
import org.eclipse.jetty.http.HttpScheme;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.http.PreEncodedHttpField;
import org.eclipse.jetty.http.Syntax;
import org.eclipse.jetty.io.RuntimeIOException;
import org.eclipse.jetty.server.EncodingHttpWriter;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.Iso88591HttpWriter;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.ResponseWriter;
import org.eclipse.jetty.server.Utf8HttpWriter;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.util.QuotedStringTokenizer;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class Response
implements HttpServletResponse {
    private static final Logger LOG = Log.getLogger(Response.class);
    private static final String __COOKIE_DELIM = "\",;\\ \t";
    private static final String __01Jan1970_COOKIE = DateGenerator.formatCookieDate(0L).trim();
    private static final int __MIN_BUFFER_SIZE = 1;
    private static final HttpField __EXPIRES_01JAN1970 = new PreEncodedHttpField(HttpHeader.EXPIRES, DateGenerator.__01Jan1970);
    private static final ThreadLocal<StringBuilder> __cookieBuilder = ThreadLocal.withInitial(() -> new StringBuilder(128));
    public static final String SET_INCLUDE_HEADER_PREFIX = "org.eclipse.jetty.server.include.";
    public static final String HTTP_ONLY_COMMENT = "__HTTP_ONLY__";
    private final HttpChannel _channel;
    private final HttpFields _fields = new HttpFields();
    private final AtomicInteger _include = new AtomicInteger();
    private final HttpOutput _out;
    private int _status = 200;
    private String _reason;
    private Locale _locale;
    private MimeTypes.Type _mimeType;
    private String _characterEncoding;
    private EncodingFrom _encodingFrom = EncodingFrom.NOT_SET;
    private String _contentType;
    private OutputType _outputType = OutputType.NONE;
    private ResponseWriter _writer;
    private long _contentLength = -1L;
    private Supplier<HttpFields> trailers;
    private static final EnumSet<EncodingFrom> __localeOverride = EnumSet.of(EncodingFrom.NOT_SET, EncodingFrom.INFERRED);
    private static final EnumSet<EncodingFrom> __explicitCharset = EnumSet.of(EncodingFrom.SET_LOCALE, EncodingFrom.SET_CHARACTER_ENCODING);

    public Response(HttpChannel channel, HttpOutput out) {
        this._channel = channel;
        this._out = out;
    }

    public HttpChannel getHttpChannel() {
        return this._channel;
    }

    protected void recycle() {
        this._status = 200;
        this._reason = null;
        this._locale = null;
        this._mimeType = null;
        this._characterEncoding = null;
        this._contentType = null;
        this._outputType = OutputType.NONE;
        this._contentLength = -1L;
        this._out.recycle();
        this._fields.clear();
        this._encodingFrom = EncodingFrom.NOT_SET;
    }

    public HttpOutput getHttpOutput() {
        return this._out;
    }

    public boolean isIncluding() {
        return this._include.get() > 0;
    }

    public void include() {
        this._include.incrementAndGet();
    }

    public void included() {
        this._include.decrementAndGet();
        if (this._outputType == OutputType.WRITER) {
            this._writer.reopen();
        }
        this._out.reopen();
    }

    public void addCookie(HttpCookie cookie) {
        if (StringUtil.isBlank(cookie.getName())) {
            throw new IllegalArgumentException("Cookie.name cannot be blank/null");
        }
        if (this.getHttpChannel().getHttpConfiguration().isCookieCompliance(CookieCompliance.RFC2965)) {
            this.addSetRFC2965Cookie(cookie.getName(), cookie.getValue(), cookie.getDomain(), cookie.getPath(), cookie.getMaxAge(), cookie.getComment(), cookie.isSecure(), cookie.isHttpOnly(), cookie.getVersion());
        } else {
            this.addSetRFC6265Cookie(cookie.getName(), cookie.getValue(), cookie.getDomain(), cookie.getPath(), cookie.getMaxAge(), cookie.isSecure(), cookie.isHttpOnly());
        }
    }

    @Override
    public void addCookie(Cookie cookie) {
        int i;
        String comment = cookie.getComment();
        boolean httpOnly = false;
        if (comment != null && (i = comment.indexOf(HTTP_ONLY_COMMENT)) >= 0) {
            httpOnly = true;
            if ((comment = comment.replace(HTTP_ONLY_COMMENT, "").trim()).length() == 0) {
                comment = null;
            }
        }
        if (StringUtil.isBlank(cookie.getName())) {
            throw new IllegalArgumentException("Cookie.name cannot be blank/null");
        }
        if (this.getHttpChannel().getHttpConfiguration().isCookieCompliance(CookieCompliance.RFC2965)) {
            this.addSetRFC2965Cookie(cookie.getName(), cookie.getValue(), cookie.getDomain(), cookie.getPath(), cookie.getMaxAge(), comment, cookie.getSecure(), httpOnly || cookie.isHttpOnly(), cookie.getVersion());
        } else {
            this.addSetRFC6265Cookie(cookie.getName(), cookie.getValue(), cookie.getDomain(), cookie.getPath(), cookie.getMaxAge(), cookie.getSecure(), httpOnly || cookie.isHttpOnly());
        }
    }

    public void addSetRFC6265Cookie(String name, String value, String domain, String path, long maxAge, boolean isSecure, boolean isHttpOnly) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Bad cookie name");
        }
        Syntax.requireValidRFC2616Token(name, "RFC6265 Cookie name");
        Syntax.requireValidRFC6265CookieValue(value);
        StringBuilder buf = __cookieBuilder.get();
        buf.setLength(0);
        buf.append(name).append('=').append(value == null ? "" : value);
        if (path != null && path.length() > 0) {
            buf.append(";Path=").append(path);
        }
        if (domain != null && domain.length() > 0) {
            buf.append(";Domain=").append(domain);
        }
        if (maxAge >= 0L) {
            buf.append(";Expires=");
            if (maxAge == 0L) {
                buf.append(__01Jan1970_COOKIE);
            } else {
                DateGenerator.formatCookieDate(buf, System.currentTimeMillis() + 1000L * maxAge);
            }
            buf.append(";Max-Age=");
            buf.append(maxAge);
        }
        if (isSecure) {
            buf.append(";Secure");
        }
        if (isHttpOnly) {
            buf.append(";HttpOnly");
        }
        this._fields.add(HttpHeader.SET_COOKIE, buf.toString());
        this._fields.put(__EXPIRES_01JAN1970);
    }

    public void addSetRFC2965Cookie(String name, String value, String domain, String path, long maxAge, String comment, boolean isSecure, boolean isHttpOnly, int version) {
        boolean quote_path;
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Bad cookie name");
        }
        StringBuilder buf = __cookieBuilder.get();
        buf.setLength(0);
        boolean quote_name = Response.isQuoteNeededForCookie(name);
        Response.quoteOnlyOrAppend(buf, name, quote_name);
        buf.append('=');
        boolean quote_value = Response.isQuoteNeededForCookie(value);
        Response.quoteOnlyOrAppend(buf, value, quote_value);
        boolean has_domain = domain != null && domain.length() > 0;
        boolean quote_domain = has_domain && Response.isQuoteNeededForCookie(domain);
        boolean has_path = path != null && path.length() > 0;
        boolean bl = quote_path = has_path && Response.isQuoteNeededForCookie(path);
        if (version == 0 && (comment != null || quote_name || quote_value || quote_domain || quote_path || QuotedStringTokenizer.isQuoted(name) || QuotedStringTokenizer.isQuoted(value) || QuotedStringTokenizer.isQuoted(path) || QuotedStringTokenizer.isQuoted(domain))) {
            version = 1;
        }
        if (version == 1) {
            buf.append(";Version=1");
        } else if (version > 1) {
            buf.append(";Version=").append(version);
        }
        if (has_path) {
            buf.append(";Path=");
            Response.quoteOnlyOrAppend(buf, path, quote_path);
        }
        if (has_domain) {
            buf.append(";Domain=");
            Response.quoteOnlyOrAppend(buf, domain, quote_domain);
        }
        if (maxAge >= 0L) {
            buf.append(";Expires=");
            if (maxAge == 0L) {
                buf.append(__01Jan1970_COOKIE);
            } else {
                DateGenerator.formatCookieDate(buf, System.currentTimeMillis() + 1000L * maxAge);
            }
            if (version >= 1) {
                buf.append(";Max-Age=");
                buf.append(maxAge);
            }
        }
        if (isSecure) {
            buf.append(";Secure");
        }
        if (isHttpOnly) {
            buf.append(";HttpOnly");
        }
        if (comment != null) {
            buf.append(";Comment=");
            Response.quoteOnlyOrAppend(buf, comment, Response.isQuoteNeededForCookie(comment));
        }
        this._fields.add(HttpHeader.SET_COOKIE, buf.toString());
        this._fields.put(__EXPIRES_01JAN1970);
    }

    private static boolean isQuoteNeededForCookie(String s) {
        if (s == null || s.length() == 0) {
            return true;
        }
        if (QuotedStringTokenizer.isQuoted(s)) {
            return false;
        }
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (__COOKIE_DELIM.indexOf(c) >= 0) {
                return true;
            }
            if (c >= ' ' && c < '\u007f') continue;
            throw new IllegalArgumentException("Illegal character in cookie value");
        }
        return false;
    }

    private static void quoteOnlyOrAppend(StringBuilder buf, String s, boolean quote) {
        if (quote) {
            QuotedStringTokenizer.quoteOnly(buf, s);
        } else {
            buf.append(s);
        }
    }

    @Override
    public boolean containsHeader(String name) {
        return this._fields.containsKey(name);
    }

    @Override
    public String encodeURL(String url) {
        int prefix;
        String sessionURLPrefix;
        Request request = this._channel.getRequest();
        SessionHandler sessionManager = request.getSessionHandler();
        if (sessionManager == null) {
            return url;
        }
        HttpURI uri = null;
        if (sessionManager.isCheckingRemoteSessionIdEncoding() && URIUtil.hasScheme(url)) {
            uri = new HttpURI(url);
            String path = uri.getPath();
            path = path == null ? "" : path;
            int port = uri.getPort();
            if (port < 0) {
                int n = port = HttpScheme.HTTPS.asString().equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
            }
            if (!request.getServerName().equalsIgnoreCase(uri.getHost())) {
                return url;
            }
            if (request.getServerPort() != port) {
                return url;
            }
            if (!path.startsWith(request.getContextPath())) {
                return url;
            }
        }
        if ((sessionURLPrefix = sessionManager.getSessionIdPathParameterNamePrefix()) == null) {
            return url;
        }
        if (url == null) {
            return null;
        }
        if (sessionManager.isUsingCookies() && request.isRequestedSessionIdFromCookie() || !sessionManager.isUsingURLs()) {
            int prefix2 = url.indexOf(sessionURLPrefix);
            if (prefix2 != -1) {
                int suffix = url.indexOf("?", prefix2);
                if (suffix < 0) {
                    suffix = url.indexOf("#", prefix2);
                }
                if (suffix <= prefix2) {
                    return url.substring(0, prefix2);
                }
                return url.substring(0, prefix2) + url.substring(suffix);
            }
            return url;
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            return url;
        }
        if (!sessionManager.isValid(session)) {
            return url;
        }
        String id = sessionManager.getExtendedId(session);
        if (uri == null) {
            uri = new HttpURI(url);
        }
        if ((prefix = url.indexOf(sessionURLPrefix)) != -1) {
            int suffix = url.indexOf("?", prefix);
            if (suffix < 0) {
                suffix = url.indexOf("#", prefix);
            }
            if (suffix <= prefix) {
                return url.substring(0, prefix + sessionURLPrefix.length()) + id;
            }
            return url.substring(0, prefix + sessionURLPrefix.length()) + id + url.substring(suffix);
        }
        int suffix = url.indexOf(63);
        if (suffix < 0) {
            suffix = url.indexOf(35);
        }
        if (suffix < 0) {
            return url + ((HttpScheme.HTTPS.is(uri.getScheme()) || HttpScheme.HTTP.is(uri.getScheme())) && uri.getPath() == null ? "/" : "") + sessionURLPrefix + id;
        }
        return url.substring(0, suffix) + ((HttpScheme.HTTPS.is(uri.getScheme()) || HttpScheme.HTTP.is(uri.getScheme())) && uri.getPath() == null ? "/" : "") + sessionURLPrefix + id + url.substring(suffix);
    }

    @Override
    public String encodeRedirectURL(String url) {
        return this.encodeURL(url);
    }

    @Override
    @Deprecated
    public String encodeUrl(String url) {
        return this.encodeURL(url);
    }

    @Override
    @Deprecated
    public String encodeRedirectUrl(String url) {
        return this.encodeRedirectURL(url);
    }

    @Override
    public void sendError(int sc) throws IOException {
        this.sendError(sc, null);
    }

    @Override
    public void sendError(int code, String message) throws IOException {
        if (this.isIncluding()) {
            return;
        }
        if (this.isCommitted()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Aborting on sendError on committed response {} {}", code, message);
            }
            code = -1;
        } else {
            this.resetBuffer();
        }
        switch (code) {
            case -1: {
                this._channel.abort(new IOException());
                return;
            }
            case 102: {
                this.sendProcessing();
                return;
            }
        }
        this._mimeType = null;
        this._characterEncoding = null;
        this._outputType = OutputType.NONE;
        this.setHeader(HttpHeader.EXPIRES, null);
        this.setHeader(HttpHeader.LAST_MODIFIED, null);
        this.setHeader(HttpHeader.CACHE_CONTROL, null);
        this.setHeader(HttpHeader.CONTENT_TYPE, null);
        this.setHeader(HttpHeader.CONTENT_LENGTH, null);
        this.setStatus(code);
        Request request = this._channel.getRequest();
        Throwable cause = (Throwable)request.getAttribute("javax.servlet.error.exception");
        if (message == null) {
            this._reason = HttpStatus.getMessage(code);
            message = cause == null ? this._reason : cause.toString();
        } else {
            this._reason = message;
        }
        if (code != 204 && code != 304 && code != 206 && code >= 200) {
            ContextHandler.Context context = request.getContext();
            ContextHandler contextHandler = context == null ? this._channel.getState().getContextHandler() : context.getContextHandler();
            request.setAttribute("javax.servlet.error.status_code", code);
            request.setAttribute("javax.servlet.error.message", message);
            request.setAttribute("javax.servlet.error.request_uri", request.getRequestURI());
            request.setAttribute("javax.servlet.error.servlet_name", request.getServletName());
            ErrorHandler error_handler = ErrorHandler.getErrorHandler(this._channel.getServer(), contextHandler);
            if (error_handler != null) {
                error_handler.handle(null, request, request, this);
            }
        }
        if (!request.isAsyncStarted()) {
            this.closeOutput();
        }
    }

    public void sendProcessing() throws IOException {
        if (this._channel.isExpecting102Processing() && !this.isCommitted()) {
            this._channel.sendResponse(HttpGenerator.PROGRESS_102_INFO, null, true);
        }
    }

    public void sendRedirect(int code, String location) throws IOException {
        if (code < 300 || code >= 400) {
            throw new IllegalArgumentException("Not a 3xx redirect code");
        }
        if (this.isIncluding()) {
            return;
        }
        if (location == null) {
            throw new IllegalArgumentException();
        }
        if (!URIUtil.hasScheme(location)) {
            StringBuilder buf = this._channel.getRequest().getRootURL();
            if (location.startsWith("/")) {
                location = URIUtil.canonicalEncodedPath(location);
            } else {
                String path = this._channel.getRequest().getRequestURI();
                String parent = path.endsWith("/") ? path : URIUtil.parentPath(path);
                location = URIUtil.canonicalEncodedPath(URIUtil.addEncodedPaths(parent, location));
                if (!location.startsWith("/")) {
                    buf.append('/');
                }
            }
            if (location == null) {
                throw new IllegalStateException("path cannot be above root");
            }
            buf.append(location);
            location = buf.toString();
        }
        this.resetBuffer();
        this.setHeader(HttpHeader.LOCATION, location);
        this.setStatus(code);
        this.closeOutput();
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        this.sendRedirect(302, location);
    }

    @Override
    public void setDateHeader(String name, long date) {
        if (!this.isIncluding()) {
            this._fields.putDateField(name, date);
        }
    }

    @Override
    public void addDateHeader(String name, long date) {
        if (!this.isIncluding()) {
            this._fields.addDateField(name, date);
        }
    }

    public void setHeader(HttpHeader name, String value) {
        if (HttpHeader.CONTENT_TYPE == name) {
            this.setContentType(value);
        } else {
            if (this.isIncluding()) {
                return;
            }
            this._fields.put(name, value);
            if (HttpHeader.CONTENT_LENGTH == name) {
                this._contentLength = value == null ? -1L : Long.parseLong(value);
            }
        }
    }

    @Override
    public void setHeader(String name, String value) {
        if (HttpHeader.CONTENT_TYPE.is(name)) {
            this.setContentType(value);
        } else {
            if (this.isIncluding()) {
                if (name.startsWith(SET_INCLUDE_HEADER_PREFIX)) {
                    name = name.substring(SET_INCLUDE_HEADER_PREFIX.length());
                } else {
                    return;
                }
            }
            this._fields.put(name, value);
            if (HttpHeader.CONTENT_LENGTH.is(name)) {
                this._contentLength = value == null ? -1L : Long.parseLong(value);
            }
        }
    }

    @Override
    public Collection<String> getHeaderNames() {
        return this._fields.getFieldNamesCollection();
    }

    @Override
    public String getHeader(String name) {
        return this._fields.get(name);
    }

    @Override
    public Collection<String> getHeaders(String name) {
        List<String> i = this._fields.getValuesList(name);
        if (i == null) {
            return Collections.emptyList();
        }
        return i;
    }

    @Override
    public void addHeader(String name, String value) {
        if (this.isIncluding()) {
            if (name.startsWith(SET_INCLUDE_HEADER_PREFIX)) {
                name = name.substring(SET_INCLUDE_HEADER_PREFIX.length());
            } else {
                return;
            }
        }
        if (HttpHeader.CONTENT_TYPE.is(name)) {
            this.setContentType(value);
            return;
        }
        if (HttpHeader.CONTENT_LENGTH.is(name)) {
            this.setHeader(name, value);
            return;
        }
        this._fields.add(name, value);
    }

    @Override
    public void setIntHeader(String name, int value) {
        if (!this.isIncluding()) {
            this._fields.putLongField(name, (long)value);
            if (HttpHeader.CONTENT_LENGTH.is(name)) {
                this._contentLength = value;
            }
        }
    }

    @Override
    public void addIntHeader(String name, int value) {
        if (!this.isIncluding()) {
            this._fields.add(name, Integer.toString(value));
            if (HttpHeader.CONTENT_LENGTH.is(name)) {
                this._contentLength = value;
            }
        }
    }

    @Override
    public void setStatus(int sc) {
        if (sc <= 0) {
            throw new IllegalArgumentException();
        }
        if (!this.isIncluding()) {
            this._status = sc;
            this._reason = null;
        }
    }

    @Override
    @Deprecated
    public void setStatus(int sc, String sm) {
        this.setStatusWithReason(sc, sm);
    }

    public void setStatusWithReason(int sc, String sm) {
        if (sc <= 0) {
            throw new IllegalArgumentException();
        }
        if (!this.isIncluding()) {
            this._status = sc;
            this._reason = sm;
        }
    }

    @Override
    public String getCharacterEncoding() {
        if (this._characterEncoding == null) {
            String encoding = MimeTypes.getCharsetAssumedFromContentType(this._contentType);
            if (encoding != null) {
                return encoding;
            }
            encoding = MimeTypes.getCharsetInferredFromContentType(this._contentType);
            if (encoding != null) {
                return encoding;
            }
            return "iso-8859-1";
        }
        return this._characterEncoding;
    }

    @Override
    public String getContentType() {
        return this._contentType;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (this._outputType == OutputType.WRITER) {
            throw new IllegalStateException("WRITER");
        }
        this._outputType = OutputType.STREAM;
        return this._out;
    }

    public boolean isWriting() {
        return this._outputType == OutputType.WRITER;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (this._outputType == OutputType.STREAM) {
            throw new IllegalStateException("STREAM");
        }
        if (this._outputType == OutputType.NONE) {
            String encoding = this._characterEncoding;
            if (encoding == null) {
                if (this._mimeType != null && this._mimeType.isCharsetAssumed()) {
                    encoding = this._mimeType.getCharsetString();
                } else {
                    encoding = MimeTypes.getCharsetAssumedFromContentType(this._contentType);
                    if (encoding == null) {
                        encoding = MimeTypes.getCharsetInferredFromContentType(this._contentType);
                        if (encoding == null) {
                            encoding = "iso-8859-1";
                        }
                        this.setCharacterEncoding(encoding, EncodingFrom.INFERRED);
                    }
                }
            }
            Locale locale = this.getLocale();
            if (this._writer != null && this._writer.isFor(locale, encoding)) {
                this._writer.reopen();
            } else {
                this._writer = "iso-8859-1".equalsIgnoreCase(encoding) ? new ResponseWriter(new Iso88591HttpWriter(this._out), locale, encoding) : ("utf-8".equalsIgnoreCase(encoding) ? new ResponseWriter(new Utf8HttpWriter(this._out), locale, encoding) : new ResponseWriter(new EncodingHttpWriter(this._out, encoding), locale, encoding));
            }
            this._outputType = OutputType.WRITER;
        }
        return this._writer;
    }

    @Override
    public void setContentLength(int len) {
        if (this.isCommitted() || this.isIncluding()) {
            return;
        }
        if (len > 0) {
            long written = this._out.getWritten();
            if (written > (long)len) {
                throw new IllegalArgumentException("setContentLength(" + len + ") when already written " + written);
            }
            this._contentLength = len;
            this._fields.putLongField(HttpHeader.CONTENT_LENGTH, (long)len);
            if (this.isAllContentWritten(written)) {
                try {
                    this.closeOutput();
                }
                catch (IOException e) {
                    throw new RuntimeIOException(e);
                }
            }
        } else if (len == 0) {
            long written = this._out.getWritten();
            if (written > 0L) {
                throw new IllegalArgumentException("setContentLength(0) when already written " + written);
            }
            this._contentLength = len;
            this._fields.put(HttpHeader.CONTENT_LENGTH, "0");
        } else {
            this._contentLength = len;
            this._fields.remove(HttpHeader.CONTENT_LENGTH);
        }
    }

    public long getContentLength() {
        return this._contentLength;
    }

    public boolean isAllContentWritten(long written) {
        return this._contentLength >= 0L && written >= this._contentLength;
    }

    public boolean isContentComplete(long written) {
        return this._contentLength < 0L || written >= this._contentLength;
    }

    public void closeOutput() throws IOException {
        switch (this._outputType) {
            case WRITER: {
                this._writer.close();
                if (this._out.isClosed()) break;
                this._out.close();
                break;
            }
            case STREAM: {
                if (this._out.isClosed()) break;
                this.getOutputStream().close();
                break;
            }
            default: {
                if (this._out.isClosed()) break;
                this._out.close();
            }
        }
    }

    public long getLongContentLength() {
        return this._contentLength;
    }

    public void setLongContentLength(long len) {
        if (this.isCommitted() || this.isIncluding()) {
            return;
        }
        this._contentLength = len;
        this._fields.putLongField(HttpHeader.CONTENT_LENGTH.toString(), len);
    }

    @Override
    public void setContentLengthLong(long length) {
        this.setLongContentLength(length);
    }

    @Override
    public void setCharacterEncoding(String encoding) {
        this.setCharacterEncoding(encoding, EncodingFrom.SET_CHARACTER_ENCODING);
    }

    private void setCharacterEncoding(String encoding, EncodingFrom from) {
        if (this.isIncluding() || this.isWriting()) {
            return;
        }
        if (this._outputType != OutputType.WRITER && !this.isCommitted()) {
            if (encoding == null) {
                this._encodingFrom = EncodingFrom.NOT_SET;
                if (this._characterEncoding != null) {
                    this._characterEncoding = null;
                    if (this._mimeType != null) {
                        this._mimeType = this._mimeType.getBaseType();
                        this._contentType = this._mimeType.asString();
                        this._fields.put(this._mimeType.getContentTypeField());
                    } else if (this._contentType != null) {
                        this._contentType = MimeTypes.getContentTypeWithoutCharset(this._contentType);
                        this._fields.put(HttpHeader.CONTENT_TYPE, this._contentType);
                    }
                }
            } else {
                this._encodingFrom = from;
                String string = this._characterEncoding = HttpGenerator.__STRICT ? encoding : StringUtil.normalizeCharset(encoding);
                if (this._mimeType != null) {
                    this._contentType = this._mimeType.getBaseType().asString() + ";charset=" + this._characterEncoding;
                    this._mimeType = MimeTypes.CACHE.get(this._contentType);
                    if (this._mimeType == null || HttpGenerator.__STRICT) {
                        this._fields.put(HttpHeader.CONTENT_TYPE, this._contentType);
                    } else {
                        this._fields.put(this._mimeType.getContentTypeField());
                    }
                } else if (this._contentType != null) {
                    this._contentType = MimeTypes.getContentTypeWithoutCharset(this._contentType) + ";charset=" + this._characterEncoding;
                    this._fields.put(HttpHeader.CONTENT_TYPE, this._contentType);
                }
            }
        }
    }

    @Override
    public void setContentType(String contentType) {
        if (this.isCommitted() || this.isIncluding()) {
            return;
        }
        if (contentType == null) {
            if (this.isWriting() && this._characterEncoding != null) {
                throw new IllegalSelectorException();
            }
            if (this._locale == null) {
                this._characterEncoding = null;
            }
            this._mimeType = null;
            this._contentType = null;
            this._fields.remove(HttpHeader.CONTENT_TYPE);
        } else {
            this._contentType = contentType;
            this._mimeType = MimeTypes.CACHE.get(contentType);
            String charset = this._mimeType != null && this._mimeType.getCharset() != null && !this._mimeType.isCharsetAssumed() ? this._mimeType.getCharsetString() : MimeTypes.getCharsetFromContentType(contentType);
            if (charset == null) {
                switch (this._encodingFrom) {
                    case NOT_SET: {
                        break;
                    }
                    case INFERRED: 
                    case SET_CONTENT_TYPE: {
                        if (this.isWriting()) {
                            this._mimeType = null;
                            this._contentType = this._contentType + ";charset=" + this._characterEncoding;
                            break;
                        }
                        this._encodingFrom = EncodingFrom.NOT_SET;
                        this._characterEncoding = null;
                        break;
                    }
                    case SET_LOCALE: 
                    case SET_CHARACTER_ENCODING: {
                        this._contentType = contentType + ";charset=" + this._characterEncoding;
                        this._mimeType = null;
                    }
                }
            } else if (this.isWriting() && !charset.equalsIgnoreCase(this._characterEncoding)) {
                this._mimeType = null;
                this._contentType = MimeTypes.getContentTypeWithoutCharset(this._contentType);
                if (this._characterEncoding != null) {
                    this._contentType = this._contentType + ";charset=" + this._characterEncoding;
                }
            } else {
                this._characterEncoding = charset;
                this._encodingFrom = EncodingFrom.SET_CONTENT_TYPE;
            }
            if (HttpGenerator.__STRICT || this._mimeType == null) {
                this._fields.put(HttpHeader.CONTENT_TYPE, this._contentType);
            } else {
                this._contentType = this._mimeType.asString();
                this._fields.put(this._mimeType.getContentTypeField());
            }
        }
    }

    @Override
    public void setBufferSize(int size) {
        if (this.isCommitted()) {
            throw new IllegalStateException("cannot set buffer size after response is in committed state");
        }
        if (this.getContentCount() > 0L) {
            throw new IllegalStateException("cannot set buffer size after response has " + this.getContentCount() + " bytes already written");
        }
        if (size < 1) {
            size = 1;
        }
        this._out.setBufferSize(size);
    }

    @Override
    public int getBufferSize() {
        return this._out.getBufferSize();
    }

    @Override
    public void flushBuffer() throws IOException {
        if (!this._out.isClosed()) {
            this._out.flush();
        }
    }

    @Override
    public void reset() {
        this.reset(false);
    }

    public void reset(boolean preserveCookies) {
        this.resetForForward();
        this._status = 200;
        this._reason = null;
        this._contentLength = -1L;
        List<HttpField> cookies = preserveCookies ? this._fields.stream().filter(f -> f.getHeader() == HttpHeader.SET_COOKIE).collect(Collectors.toList()) : null;
        this._fields.clear();
        String connection = this._channel.getRequest().getHeader(HttpHeader.CONNECTION.asString());
        if (connection != null) {
            for (String value : StringUtil.csvSplit(null, connection, 0, connection.length())) {
                HttpHeaderValue cb = HttpHeaderValue.CACHE.get(value);
                if (cb == null) continue;
                switch (cb) {
                    case CLOSE: {
                        this._fields.put(HttpHeader.CONNECTION, HttpHeaderValue.CLOSE.toString());
                        break;
                    }
                    case KEEP_ALIVE: {
                        if (!HttpVersion.HTTP_1_0.is(this._channel.getRequest().getProtocol())) break;
                        this._fields.put(HttpHeader.CONNECTION, HttpHeaderValue.KEEP_ALIVE.toString());
                        break;
                    }
                    case TE: {
                        this._fields.put(HttpHeader.CONNECTION, HttpHeaderValue.TE.toString());
                        break;
                    }
                }
            }
        }
        if (preserveCookies) {
            cookies.forEach(this._fields::add);
        } else {
            HttpCookie c;
            SessionHandler sh;
            Request request = this.getHttpChannel().getRequest();
            HttpSession session = request.getSession(false);
            if (session != null && session.isNew() && (sh = request.getSessionHandler()) != null && (c = sh.getSessionCookie(session, request.getContextPath(), request.isSecure())) != null) {
                this.addCookie(c);
            }
        }
    }

    public void resetForForward() {
        this.resetBuffer();
        this._outputType = OutputType.NONE;
    }

    @Override
    public void resetBuffer() {
        this._out.resetBuffer();
    }

    public void setTrailers(Supplier<HttpFields> trailers) {
        this.trailers = trailers;
    }

    public Supplier<HttpFields> getTrailers() {
        return this.trailers;
    }

    protected MetaData.Response newResponseMetaData() {
        MetaData.Response info = new MetaData.Response(this._channel.getRequest().getHttpVersion(), this.getStatus(), this.getReason(), this._fields, this.getLongContentLength());
        info.setTrailerSupplier(this.getTrailers());
        return info;
    }

    public MetaData.Response getCommittedMetaData() {
        MetaData.Response meta = this._channel.getCommittedMetaData();
        if (meta == null) {
            return this.newResponseMetaData();
        }
        return meta;
    }

    @Override
    public boolean isCommitted() {
        return this._channel.isCommitted();
    }

    @Override
    public void setLocale(Locale locale) {
        if (locale == null || this.isCommitted() || this.isIncluding()) {
            return;
        }
        this._locale = locale;
        this._fields.put(HttpHeader.CONTENT_LANGUAGE, locale.toString().replace('_', '-'));
        if (this._outputType != OutputType.NONE) {
            return;
        }
        if (this._channel.getRequest().getContext() == null) {
            return;
        }
        String charset = this._channel.getRequest().getContext().getContextHandler().getLocaleEncoding(locale);
        if (charset != null && charset.length() > 0 && __localeOverride.contains((Object)this._encodingFrom)) {
            this.setCharacterEncoding(charset, EncodingFrom.SET_LOCALE);
        }
    }

    @Override
    public Locale getLocale() {
        if (this._locale == null) {
            return Locale.getDefault();
        }
        return this._locale;
    }

    @Override
    public int getStatus() {
        return this._status;
    }

    public String getReason() {
        return this._reason;
    }

    public HttpFields getHttpFields() {
        return this._fields;
    }

    public long getContentCount() {
        return this._out.getWritten();
    }

    public String toString() {
        return String.format("%s %d %s%n%s", new Object[]{this._channel.getRequest().getHttpVersion(), this._status, this._reason == null ? "" : this._reason, this._fields});
    }

    public void putHeaders(HttpContent content, long contentLength, boolean etag) {
        HttpField et;
        HttpField ce;
        HttpField lm = content.getLastModified();
        if (lm != null) {
            this._fields.put(lm);
        }
        if (contentLength == 0L) {
            this._fields.put(content.getContentLength());
            this._contentLength = content.getContentLengthValue();
        } else if (contentLength > 0L) {
            this._fields.putLongField(HttpHeader.CONTENT_LENGTH, contentLength);
            this._contentLength = contentLength;
        }
        HttpField ct = content.getContentType();
        if (ct != null) {
            if (this._characterEncoding != null && content.getCharacterEncoding() == null && content.getContentTypeValue() != null && __explicitCharset.contains((Object)this._encodingFrom)) {
                this.setContentType(MimeTypes.getContentTypeWithoutCharset(content.getContentTypeValue()));
            } else {
                this._fields.put(ct);
                this._contentType = ct.getValue();
                this._characterEncoding = content.getCharacterEncoding();
                this._mimeType = content.getMimeType();
            }
        }
        if ((ce = content.getContentEncoding()) != null) {
            this._fields.put(ce);
        }
        if (etag && (et = content.getETag()) != null) {
            this._fields.put(et);
        }
    }

    public static void putHeaders(HttpServletResponse response, HttpContent content, long contentLength, boolean etag) {
        String et;
        String ce;
        String ct;
        long lml = content.getResource().lastModified();
        if (lml >= 0L) {
            response.setDateHeader(HttpHeader.LAST_MODIFIED.asString(), lml);
        }
        if (contentLength == 0L) {
            contentLength = content.getContentLengthValue();
        }
        if (contentLength >= 0L) {
            if (contentLength < Integer.MAX_VALUE) {
                response.setContentLength((int)contentLength);
            } else {
                response.setHeader(HttpHeader.CONTENT_LENGTH.asString(), Long.toString(contentLength));
            }
        }
        if ((ct = content.getContentTypeValue()) != null && response.getContentType() == null) {
            response.setContentType(ct);
        }
        if ((ce = content.getContentEncodingValue()) != null) {
            response.setHeader(HttpHeader.CONTENT_ENCODING.asString(), ce);
        }
        if (etag && (et = content.getETagValue()) != null) {
            response.setHeader(HttpHeader.ETAG.asString(), et);
        }
    }

    private static enum EncodingFrom {
        NOT_SET,
        INFERRED,
        SET_LOCALE,
        SET_CONTENT_TYPE,
        SET_CHARACTER_ENCODING;

    }

    public static enum OutputType {
        NONE,
        STREAM,
        WRITER;

    }
}

