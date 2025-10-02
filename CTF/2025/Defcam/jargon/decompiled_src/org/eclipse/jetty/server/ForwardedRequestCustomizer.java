/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server;

import java.net.InetSocketAddress;
import org.eclipse.jetty.http.HostPortHttpField;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpScheme;
import org.eclipse.jetty.http.QuotedCSV;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.StringUtil;

public class ForwardedRequestCustomizer
implements HttpConfiguration.Customizer {
    private HostPortHttpField _forcedHost;
    private String _forwardedHeader = HttpHeader.FORWARDED.toString();
    private String _forwardedHostHeader = HttpHeader.X_FORWARDED_HOST.toString();
    private String _forwardedServerHeader = HttpHeader.X_FORWARDED_SERVER.toString();
    private String _forwardedForHeader = HttpHeader.X_FORWARDED_FOR.toString();
    private String _forwardedProtoHeader = HttpHeader.X_FORWARDED_PROTO.toString();
    private String _forwardedHttpsHeader = "X-Proxied-Https";
    private String _forwardedCipherSuiteHeader = "Proxy-auth-cert";
    private String _forwardedSslSessionIdHeader = "Proxy-ssl-id";
    private boolean _proxyAsAuthority = false;
    private boolean _sslIsSecure = true;

    public boolean getProxyAsAuthority() {
        return this._proxyAsAuthority;
    }

    public void setProxyAsAuthority(boolean proxyAsAuthority) {
        this._proxyAsAuthority = proxyAsAuthority;
    }

    public void setForwardedOnly(boolean rfc7239only) {
        if (rfc7239only) {
            if (this._forwardedHeader == null) {
                this._forwardedHeader = HttpHeader.FORWARDED.toString();
            }
            this._forwardedHostHeader = null;
            this._forwardedHostHeader = null;
            this._forwardedServerHeader = null;
            this._forwardedForHeader = null;
            this._forwardedProtoHeader = null;
            this._forwardedHttpsHeader = null;
        } else {
            if (this._forwardedHostHeader == null) {
                this._forwardedHostHeader = HttpHeader.X_FORWARDED_HOST.toString();
            }
            if (this._forwardedServerHeader == null) {
                this._forwardedServerHeader = HttpHeader.X_FORWARDED_SERVER.toString();
            }
            if (this._forwardedForHeader == null) {
                this._forwardedForHeader = HttpHeader.X_FORWARDED_FOR.toString();
            }
            if (this._forwardedProtoHeader == null) {
                this._forwardedProtoHeader = HttpHeader.X_FORWARDED_PROTO.toString();
            }
            if (this._forwardedHttpsHeader == null) {
                this._forwardedHttpsHeader = "X-Proxied-Https";
            }
        }
    }

    public String getForcedHost() {
        return this._forcedHost.getValue();
    }

    public void setForcedHost(String hostAndPort) {
        this._forcedHost = new HostPortHttpField(hostAndPort);
    }

    public String getForwardedHeader() {
        return this._forwardedHeader;
    }

    public void setForwardedHeader(String forwardedHeader) {
        this._forwardedHeader = forwardedHeader;
    }

    public String getForwardedHostHeader() {
        return this._forwardedHostHeader;
    }

    public void setForwardedHostHeader(String forwardedHostHeader) {
        this._forwardedHostHeader = forwardedHostHeader;
    }

    public String getForwardedServerHeader() {
        return this._forwardedServerHeader;
    }

    public void setForwardedServerHeader(String forwardedServerHeader) {
        this._forwardedServerHeader = forwardedServerHeader;
    }

    public String getForwardedForHeader() {
        return this._forwardedForHeader;
    }

    public void setForwardedForHeader(String forwardedRemoteAddressHeader) {
        this._forwardedForHeader = forwardedRemoteAddressHeader;
    }

    public String getForwardedProtoHeader() {
        return this._forwardedProtoHeader;
    }

    public void setForwardedProtoHeader(String forwardedProtoHeader) {
        this._forwardedProtoHeader = forwardedProtoHeader;
    }

    public String getForwardedCipherSuiteHeader() {
        return this._forwardedCipherSuiteHeader;
    }

    public void setForwardedCipherSuiteHeader(String forwardedCipherSuite) {
        this._forwardedCipherSuiteHeader = forwardedCipherSuite;
    }

    public String getForwardedSslSessionIdHeader() {
        return this._forwardedSslSessionIdHeader;
    }

    public void setForwardedSslSessionIdHeader(String forwardedSslSessionId) {
        this._forwardedSslSessionIdHeader = forwardedSslSessionId;
    }

    public String getForwardedHttpsHeader() {
        return this._forwardedHttpsHeader;
    }

    public void setForwardedHttpsHeader(String forwardedHttpsHeader) {
        this._forwardedHttpsHeader = forwardedHttpsHeader;
    }

    public boolean isSslIsSecure() {
        return this._sslIsSecure;
    }

    public void setSslIsSecure(boolean sslIsSecure) {
        this._sslIsSecure = sslIsSecure;
    }

    @Override
    public void customize(Connector connector, HttpConfiguration config, Request request) {
        HostPortHttpField auth;
        HttpFields httpFields = request.getHttpFields();
        QuotedCSV rfc7239 = null;
        String forwardedHost = null;
        String forwardedServer = null;
        String forwardedFor = null;
        String forwardedProto = null;
        String forwardedHttps = null;
        for (HttpField field : httpFields) {
            String name = field.getName();
            if (this.getForwardedCipherSuiteHeader() != null && this.getForwardedCipherSuiteHeader().equalsIgnoreCase(name)) {
                request.setAttribute("javax.servlet.request.cipher_suite", field.getValue());
                if (this.isSslIsSecure()) {
                    request.setSecure(true);
                    request.setScheme(config.getSecureScheme());
                }
            }
            if (this.getForwardedSslSessionIdHeader() != null && this.getForwardedSslSessionIdHeader().equalsIgnoreCase(name)) {
                request.setAttribute("javax.servlet.request.ssl_session_id", field.getValue());
                if (this.isSslIsSecure()) {
                    request.setSecure(true);
                    request.setScheme(config.getSecureScheme());
                }
            }
            if (forwardedHost == null && this._forwardedHostHeader != null && this._forwardedHostHeader.equalsIgnoreCase(name)) {
                forwardedHost = this.getLeftMost(field.getValue());
            }
            if (forwardedServer == null && this._forwardedServerHeader != null && this._forwardedServerHeader.equalsIgnoreCase(name)) {
                forwardedServer = this.getLeftMost(field.getValue());
            }
            if (forwardedFor == null && this._forwardedForHeader != null && this._forwardedForHeader.equalsIgnoreCase(name)) {
                forwardedFor = this.getLeftMost(field.getValue());
            }
            if (forwardedProto == null && this._forwardedProtoHeader != null && this._forwardedProtoHeader.equalsIgnoreCase(name)) {
                forwardedProto = this.getLeftMost(field.getValue());
            }
            if (forwardedHttps == null && this._forwardedHttpsHeader != null && this._forwardedHttpsHeader.equalsIgnoreCase(name)) {
                forwardedHttps = this.getLeftMost(field.getValue());
            }
            if (this._forwardedHeader == null || !this._forwardedHeader.equalsIgnoreCase(name)) continue;
            if (rfc7239 == null) {
                rfc7239 = new RFC7239();
            }
            rfc7239.addValue(field.getValue());
        }
        if (this._forcedHost != null) {
            httpFields.put(this._forcedHost);
            request.setAuthority(this._forcedHost.getHost(), this._forcedHost.getPort());
        } else if (rfc7239 != null && ((RFC7239)rfc7239)._host != null) {
            auth = ((RFC7239)rfc7239)._host;
            httpFields.put(auth);
            request.setAuthority(auth.getHost(), auth.getPort());
        } else if (forwardedHost != null) {
            auth = new HostPortHttpField(forwardedHost);
            httpFields.put(auth);
            request.setAuthority(auth.getHost(), auth.getPort());
        } else if (this._proxyAsAuthority) {
            if (rfc7239 != null && ((RFC7239)rfc7239)._by != null) {
                auth = ((RFC7239)rfc7239)._by;
                httpFields.put(auth);
                request.setAuthority(auth.getHost(), auth.getPort());
            } else if (forwardedServer != null) {
                request.setAuthority(forwardedServer, request.getServerPort());
            }
        }
        if (rfc7239 != null && ((RFC7239)rfc7239)._for != null) {
            request.setRemoteAddr(InetSocketAddress.createUnresolved(((RFC7239)rfc7239)._for.getHost(), ((RFC7239)rfc7239)._for.getPort()));
        } else if (forwardedFor != null) {
            request.setRemoteAddr(InetSocketAddress.createUnresolved(forwardedFor, request.getRemotePort()));
        }
        if (rfc7239 != null && ((RFC7239)rfc7239)._proto != null) {
            request.setScheme(((RFC7239)rfc7239)._proto);
            if (((RFC7239)rfc7239)._proto.equals(config.getSecureScheme())) {
                request.setSecure(true);
            }
        } else if (forwardedProto != null) {
            request.setScheme(forwardedProto);
            if (forwardedProto.equals(config.getSecureScheme())) {
                request.setSecure(true);
            }
        } else if (forwardedHttps != null && ("on".equalsIgnoreCase(forwardedHttps) || "true".equalsIgnoreCase(forwardedHttps))) {
            request.setScheme(HttpScheme.HTTPS.asString());
            if (HttpScheme.HTTPS.asString().equals(config.getSecureScheme())) {
                request.setSecure(true);
            }
        }
    }

    protected String getLeftMost(String headerValue) {
        if (headerValue == null) {
            return null;
        }
        int commaIndex = headerValue.indexOf(44);
        if (commaIndex == -1) {
            return headerValue;
        }
        return headerValue.substring(0, commaIndex).trim();
    }

    public String toString() {
        return String.format("%s@%x", this.getClass().getSimpleName(), this.hashCode());
    }

    @Deprecated
    public String getHostHeader() {
        return this._forcedHost.getValue();
    }

    @Deprecated
    public void setHostHeader(String hostHeader) {
        this._forcedHost = new HostPortHttpField(hostHeader);
    }

    private final class RFC7239
    extends QuotedCSV {
        HostPortHttpField _by;
        HostPortHttpField _for;
        HostPortHttpField _host;
        String _proto;

        private RFC7239() {
            super(false, new String[0]);
        }

        @Override
        protected void parsedParam(StringBuffer buffer, int valueLength, int paramName, int paramValue) {
            if (valueLength == 0 && paramValue > paramName) {
                String name = StringUtil.asciiToLowerCase(buffer.substring(paramName, paramValue - 1));
                String value = buffer.substring(paramValue);
                switch (name) {
                    case "by": {
                        if (this._by != null || value.startsWith("_") || "unknown".equals(value)) break;
                        this._by = new HostPortHttpField(value);
                        break;
                    }
                    case "for": {
                        if (this._for != null || value.startsWith("_") || "unknown".equals(value)) break;
                        this._for = new HostPortHttpField(value);
                        break;
                    }
                    case "host": {
                        if (this._host != null) break;
                        this._host = new HostPortHttpField(value);
                        break;
                    }
                    case "proto": {
                        if (this._proto != null) break;
                        this._proto = value;
                    }
                }
            }
        }
    }
}

