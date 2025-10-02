/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.http;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpScheme;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.TypeUtil;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.UrlEncoded;

public class HttpURI {
    private String _scheme;
    private String _user;
    private String _host;
    private int _port;
    private String _path;
    private String _param;
    private String _query;
    private String _fragment;
    String _uri;
    String _decodedPath;

    public static HttpURI createHttpURI(String scheme, String host, int port, String path, String param, String query, String fragment) {
        if (port == 80 && HttpScheme.HTTP.is(scheme)) {
            port = 0;
        }
        if (port == 443 && HttpScheme.HTTPS.is(scheme)) {
            port = 0;
        }
        return new HttpURI(scheme, host, port, path, param, query, fragment);
    }

    public HttpURI() {
    }

    public HttpURI(String scheme, String host, int port, String path, String param, String query, String fragment) {
        this._scheme = scheme;
        this._host = host;
        this._port = port;
        this._path = path;
        this._param = param;
        this._query = query;
        this._fragment = fragment;
    }

    public HttpURI(HttpURI uri) {
        this(uri._scheme, uri._host, uri._port, uri._path, uri._param, uri._query, uri._fragment);
        this._uri = uri._uri;
    }

    public HttpURI(String uri) {
        this._port = -1;
        this.parse(State.START, uri, 0, uri.length());
    }

    public HttpURI(URI uri) {
        int p;
        this._uri = null;
        this._scheme = uri.getScheme();
        this._host = uri.getHost();
        if (this._host == null && uri.getRawSchemeSpecificPart().startsWith("//")) {
            this._host = "";
        }
        this._port = uri.getPort();
        this._user = uri.getUserInfo();
        this._path = uri.getRawPath();
        this._decodedPath = uri.getPath();
        if (this._decodedPath != null && (p = this._decodedPath.lastIndexOf(59)) >= 0) {
            this._param = this._decodedPath.substring(p + 1);
        }
        this._query = uri.getRawQuery();
        this._fragment = uri.getFragment();
        this._decodedPath = null;
    }

    public HttpURI(String scheme, String host, int port, String pathQuery) {
        this._uri = null;
        this._scheme = scheme;
        this._host = host;
        this._port = port;
        this.parse(State.PATH, pathQuery, 0, pathQuery.length());
    }

    public void parse(String uri) {
        this.clear();
        this._uri = uri;
        this.parse(State.START, uri, 0, uri.length());
    }

    public void parseRequestTarget(String method, String uri) {
        this.clear();
        this._uri = uri;
        if (HttpMethod.CONNECT.is(method)) {
            this._path = uri;
        } else {
            this.parse(uri.startsWith("/") ? State.PATH : State.START, uri, 0, uri.length());
        }
    }

    @Deprecated
    public void parseConnect(String uri) {
        this.clear();
        this._uri = uri;
        this._path = uri;
    }

    public void parse(String uri, int offset, int length) {
        this.clear();
        int end = offset + length;
        this._uri = uri.substring(offset, end);
        this.parse(State.START, uri, offset, end);
    }

    private void parse(State state, String uri, int offset, int end) {
        boolean encoded = false;
        int mark = offset;
        int path_mark = 0;
        block67: for (int i = offset; i < end; ++i) {
            char c = uri.charAt(i);
            switch (state) {
                case START: {
                    switch (c) {
                        case '/': {
                            mark = i;
                            state = State.HOST_OR_PATH;
                            continue block67;
                        }
                        case ';': {
                            mark = i + 1;
                            state = State.PARAM;
                            continue block67;
                        }
                        case '?': {
                            this._path = "";
                            mark = i + 1;
                            state = State.QUERY;
                            continue block67;
                        }
                        case '#': {
                            mark = i + 1;
                            state = State.FRAGMENT;
                            continue block67;
                        }
                        case '*': {
                            this._path = "*";
                            state = State.ASTERISK;
                            continue block67;
                        }
                    }
                    mark = i;
                    if (this._scheme == null) {
                        state = State.SCHEME_OR_PATH;
                        continue block67;
                    }
                    path_mark = i;
                    state = State.PATH;
                    continue block67;
                }
                case SCHEME_OR_PATH: {
                    switch (c) {
                        case ':': {
                            this._scheme = uri.substring(mark, i);
                            state = State.START;
                            break;
                        }
                        case '/': {
                            state = State.PATH;
                            break;
                        }
                        case ';': {
                            mark = i + 1;
                            state = State.PARAM;
                            break;
                        }
                        case '?': {
                            this._path = uri.substring(mark, i);
                            mark = i + 1;
                            state = State.QUERY;
                            break;
                        }
                        case '%': {
                            encoded = true;
                            state = State.PATH;
                            break;
                        }
                        case '#': {
                            this._path = uri.substring(mark, i);
                            state = State.FRAGMENT;
                        }
                    }
                    continue block67;
                }
                case HOST_OR_PATH: {
                    switch (c) {
                        case '/': {
                            this._host = "";
                            mark = i + 1;
                            state = State.HOST;
                            continue block67;
                        }
                        case '#': 
                        case ';': 
                        case '?': 
                        case '@': {
                            --i;
                            path_mark = mark;
                            state = State.PATH;
                            continue block67;
                        }
                    }
                    path_mark = mark;
                    state = State.PATH;
                    continue block67;
                }
                case HOST: {
                    switch (c) {
                        case '/': {
                            this._host = uri.substring(mark, i);
                            path_mark = mark = i;
                            state = State.PATH;
                            break;
                        }
                        case ':': {
                            if (i > mark) {
                                this._host = uri.substring(mark, i);
                            }
                            mark = i + 1;
                            state = State.PORT;
                            break;
                        }
                        case '@': {
                            if (this._user != null) {
                                throw new IllegalArgumentException("Bad authority");
                            }
                            this._user = uri.substring(mark, i);
                            mark = i + 1;
                            break;
                        }
                        case '[': {
                            state = State.IPV6;
                        }
                    }
                    continue block67;
                }
                case IPV6: {
                    switch (c) {
                        case '/': {
                            throw new IllegalArgumentException("No closing ']' for ipv6 in " + uri);
                        }
                        case ']': {
                            c = uri.charAt(++i);
                            this._host = uri.substring(mark, i);
                            if (c == ':') {
                                mark = i + 1;
                                state = State.PORT;
                                break;
                            }
                            path_mark = mark = i;
                            state = State.PATH;
                        }
                    }
                    continue block67;
                }
                case PORT: {
                    if (c == '@') {
                        if (this._user != null) {
                            throw new IllegalArgumentException("Bad authority");
                        }
                        this._user = this._host + ":" + uri.substring(mark, i);
                        mark = i + 1;
                        state = State.HOST;
                        continue block67;
                    }
                    if (c != '/') continue block67;
                    this._port = TypeUtil.parseInt(uri, mark, i - mark, 10);
                    path_mark = mark = i;
                    state = State.PATH;
                    continue block67;
                }
                case PATH: {
                    switch (c) {
                        case ';': {
                            mark = i + 1;
                            state = State.PARAM;
                            break;
                        }
                        case '?': {
                            this._path = uri.substring(path_mark, i);
                            mark = i + 1;
                            state = State.QUERY;
                            break;
                        }
                        case '#': {
                            this._path = uri.substring(path_mark, i);
                            mark = i + 1;
                            state = State.FRAGMENT;
                            break;
                        }
                        case '%': {
                            encoded = true;
                        }
                    }
                    continue block67;
                }
                case PARAM: {
                    switch (c) {
                        case '?': {
                            this._path = uri.substring(path_mark, i);
                            this._param = uri.substring(mark, i);
                            mark = i + 1;
                            state = State.QUERY;
                            break;
                        }
                        case '#': {
                            this._path = uri.substring(path_mark, i);
                            this._param = uri.substring(mark, i);
                            mark = i + 1;
                            state = State.FRAGMENT;
                            break;
                        }
                        case '/': {
                            encoded = true;
                            state = State.PATH;
                            break;
                        }
                        case ';': {
                            mark = i + 1;
                        }
                    }
                    continue block67;
                }
                case QUERY: {
                    if (c != '#') continue block67;
                    this._query = uri.substring(mark, i);
                    mark = i + 1;
                    state = State.FRAGMENT;
                    continue block67;
                }
                case ASTERISK: {
                    throw new IllegalArgumentException("Bad character '*'");
                }
                case FRAGMENT: {
                    this._fragment = uri.substring(mark, end);
                    i = end;
                }
            }
        }
        switch (state) {
            case START: {
                break;
            }
            case SCHEME_OR_PATH: {
                this._path = uri.substring(mark, end);
                break;
            }
            case HOST_OR_PATH: {
                this._path = uri.substring(mark, end);
                break;
            }
            case HOST: {
                if (end <= mark) break;
                this._host = uri.substring(mark, end);
                break;
            }
            case IPV6: {
                throw new IllegalArgumentException("No closing ']' for ipv6 in " + uri);
            }
            case PORT: {
                this._port = TypeUtil.parseInt(uri, mark, end - mark, 10);
                break;
            }
            case ASTERISK: {
                break;
            }
            case FRAGMENT: {
                this._fragment = uri.substring(mark, end);
                break;
            }
            case PARAM: {
                this._path = uri.substring(path_mark, end);
                this._param = uri.substring(mark, end);
                break;
            }
            case PATH: {
                this._path = uri.substring(path_mark, end);
                break;
            }
            case QUERY: {
                this._query = uri.substring(mark, end);
            }
        }
        if (!encoded) {
            this._decodedPath = this._param == null ? this._path : this._path.substring(0, this._path.length() - this._param.length() - 1);
        }
    }

    public String getScheme() {
        return this._scheme;
    }

    public String getHost() {
        if (this._host != null && this._host.length() == 0) {
            return null;
        }
        return this._host;
    }

    public int getPort() {
        return this._port;
    }

    public String getPath() {
        return this._path;
    }

    public String getDecodedPath() {
        if (this._decodedPath == null && this._path != null) {
            this._decodedPath = URIUtil.decodePath(this._path);
        }
        return this._decodedPath;
    }

    public String getParam() {
        return this._param;
    }

    public String getQuery() {
        return this._query;
    }

    public boolean hasQuery() {
        return this._query != null && this._query.length() > 0;
    }

    public String getFragment() {
        return this._fragment;
    }

    public void decodeQueryTo(MultiMap<String> parameters) {
        if (this._query == this._fragment) {
            return;
        }
        UrlEncoded.decodeUtf8To(this._query, parameters);
    }

    public void decodeQueryTo(MultiMap<String> parameters, String encoding) throws UnsupportedEncodingException {
        this.decodeQueryTo(parameters, Charset.forName(encoding));
    }

    public void decodeQueryTo(MultiMap<String> parameters, Charset encoding) throws UnsupportedEncodingException {
        if (this._query == this._fragment) {
            return;
        }
        if (encoding == null || StandardCharsets.UTF_8.equals(encoding)) {
            UrlEncoded.decodeUtf8To(this._query, parameters);
        } else {
            UrlEncoded.decodeTo(this._query, parameters, encoding);
        }
    }

    public void clear() {
        this._uri = null;
        this._scheme = null;
        this._host = null;
        this._port = -1;
        this._path = null;
        this._param = null;
        this._query = null;
        this._fragment = null;
        this._decodedPath = null;
    }

    public boolean isAbsolute() {
        return this._scheme != null && this._scheme.length() > 0;
    }

    public String toString() {
        if (this._uri == null) {
            StringBuilder out = new StringBuilder();
            if (this._scheme != null) {
                out.append(this._scheme).append(':');
            }
            if (this._host != null) {
                out.append("//");
                if (this._user != null) {
                    out.append(this._user).append('@');
                }
                out.append(this._host);
            }
            if (this._port > 0) {
                out.append(':').append(this._port);
            }
            if (this._path != null) {
                out.append(this._path);
            }
            if (this._query != null) {
                out.append('?').append(this._query);
            }
            if (this._fragment != null) {
                out.append('#').append(this._fragment);
            }
            this._uri = out.length() > 0 ? out.toString() : "";
        }
        return this._uri;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof HttpURI)) {
            return false;
        }
        return this.toString().equals(o.toString());
    }

    public void setScheme(String scheme) {
        this._scheme = scheme;
        this._uri = null;
    }

    public void setAuthority(String host, int port) {
        this._host = host;
        this._port = port;
        this._uri = null;
    }

    public void setPath(String path) {
        this._uri = null;
        this._path = path;
        this._decodedPath = null;
    }

    public void setPathQuery(String path) {
        this._uri = null;
        this._path = null;
        this._decodedPath = null;
        this._param = null;
        this._fragment = null;
        if (path != null) {
            this.parse(State.PATH, path, 0, path.length());
        }
    }

    public void setQuery(String query) {
        this._query = query;
        this._uri = null;
    }

    public URI toURI() throws URISyntaxException {
        return new URI(this._scheme, null, this._host, this._port, this._path, this._query == null ? null : UrlEncoded.decodeString(this._query), this._fragment);
    }

    public String getPathQuery() {
        if (this._query == null) {
            return this._path;
        }
        return this._path + "?" + this._query;
    }

    public String getAuthority() {
        if (this._port > 0) {
            return this._host + ":" + this._port;
        }
        return this._host;
    }

    public String getUser() {
        return this._user;
    }

    private static enum State {
        START,
        HOST_OR_PATH,
        SCHEME_OR_PATH,
        HOST,
        IPV6,
        PORT,
        PATH,
        PARAM,
        QUERY,
        FRAGMENT,
        ASTERISK;

    }
}

