/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server;

import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import org.eclipse.jetty.http.BadMessageException;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpScheme;
import org.eclipse.jetty.http.PreEncodedHttpField;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.ssl.SslConnection;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.ProxyConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.TypeUtil;
import org.eclipse.jetty.util.annotation.Name;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.ssl.X509;

public class SecureRequestCustomizer
implements HttpConfiguration.Customizer {
    private static final Logger LOG = Log.getLogger(SecureRequestCustomizer.class);
    public static final String CACHED_INFO_ATTR = CachedInfo.class.getName();
    private String sslSessionAttribute = "org.eclipse.jetty.servlet.request.ssl_session";
    private boolean _sniHostCheck;
    private long _stsMaxAge = -1L;
    private boolean _stsIncludeSubDomains;
    private HttpField _stsField;

    public SecureRequestCustomizer() {
        this(true);
    }

    public SecureRequestCustomizer(@Name(value="sniHostCheck") boolean sniHostCheck) {
        this(sniHostCheck, -1L, false);
    }

    public SecureRequestCustomizer(@Name(value="sniHostCheck") boolean sniHostCheck, @Name(value="stsMaxAgeSeconds") long stsMaxAgeSeconds, @Name(value="stsIncludeSubdomains") boolean stsIncludeSubdomains) {
        this._sniHostCheck = sniHostCheck;
        this._stsMaxAge = stsMaxAgeSeconds;
        this._stsIncludeSubDomains = stsIncludeSubdomains;
        this.formatSTS();
    }

    public boolean isSniHostCheck() {
        return this._sniHostCheck;
    }

    public void setSniHostCheck(boolean sniHostCheck) {
        this._sniHostCheck = sniHostCheck;
    }

    public long getStsMaxAge() {
        return this._stsMaxAge;
    }

    public void setStsMaxAge(long stsMaxAgeSeconds) {
        this._stsMaxAge = stsMaxAgeSeconds;
        this.formatSTS();
    }

    public void setStsMaxAge(long period, TimeUnit units) {
        this._stsMaxAge = units.toSeconds(period);
        this.formatSTS();
    }

    public boolean isStsIncludeSubDomains() {
        return this._stsIncludeSubDomains;
    }

    public void setStsIncludeSubDomains(boolean stsIncludeSubDomains) {
        this._stsIncludeSubDomains = stsIncludeSubDomains;
        this.formatSTS();
    }

    private void formatSTS() {
        this._stsField = this._stsMaxAge < 0L ? null : new PreEncodedHttpField(HttpHeader.STRICT_TRANSPORT_SECURITY, String.format("max-age=%d%s", this._stsMaxAge, this._stsIncludeSubDomains ? "; includeSubDomains" : ""));
    }

    @Override
    public void customize(Connector connector, HttpConfiguration channelConfig, Request request) {
        EndPoint endp = request.getHttpChannel().getEndPoint();
        if (endp instanceof SslConnection.DecryptedEndPoint) {
            SslConnection.DecryptedEndPoint ssl_endp = (SslConnection.DecryptedEndPoint)endp;
            SslConnection sslConnection = ssl_endp.getSslConnection();
            SSLEngine sslEngine = sslConnection.getSSLEngine();
            this.customize(sslEngine, request);
            if (request.getHttpURI().getScheme() == null) {
                request.setScheme(HttpScheme.HTTPS.asString());
            }
        } else if (endp instanceof ProxyConnectionFactory.ProxyEndPoint) {
            ProxyConnectionFactory.ProxyEndPoint proxy = (ProxyConnectionFactory.ProxyEndPoint)endp;
            if (request.getHttpURI().getScheme() == null && proxy.getAttribute("TLS_VERSION") != null) {
                request.setScheme(HttpScheme.HTTPS.asString());
            }
        }
        if (HttpScheme.HTTPS.is(request.getScheme())) {
            this.customizeSecure(request);
        }
    }

    protected void customizeSecure(Request request) {
        request.setSecure(true);
        if (this._stsField != null) {
            request.getResponse().getHttpFields().add(this._stsField);
        }
    }

    protected void customize(SSLEngine sslEngine, Request request) {
        SSLSession sslSession = sslEngine.getSession();
        if (this._sniHostCheck) {
            String name = request.getServerName();
            X509 x509 = (X509)sslSession.getValue("org.eclipse.jetty.util.ssl.snix509");
            if (x509 != null && !x509.matches(name)) {
                LOG.warn("Host {} does not match SNI {}", name, x509);
                throw new BadMessageException(400, "Host does not match SNI");
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Host {} matched SNI {}", name, x509);
            }
        }
        try {
            String idStr;
            X509Certificate[] certs;
            Integer keySize;
            String cipherSuite = sslSession.getCipherSuite();
            CachedInfo cachedInfo = (CachedInfo)sslSession.getValue(CACHED_INFO_ATTR);
            if (cachedInfo != null) {
                keySize = cachedInfo.getKeySize();
                certs = cachedInfo.getCerts();
                idStr = cachedInfo.getIdStr();
            } else {
                keySize = SslContextFactory.deduceKeyLength(cipherSuite);
                certs = SslContextFactory.getCertChain(sslSession);
                byte[] bytes = sslSession.getId();
                idStr = TypeUtil.toHexString(bytes);
                cachedInfo = new CachedInfo(keySize, certs, idStr);
                sslSession.putValue(CACHED_INFO_ATTR, cachedInfo);
            }
            if (certs != null) {
                request.setAttribute("javax.servlet.request.X509Certificate", certs);
            }
            request.setAttribute("javax.servlet.request.cipher_suite", cipherSuite);
            request.setAttribute("javax.servlet.request.key_size", keySize);
            request.setAttribute("javax.servlet.request.ssl_session_id", idStr);
            String sessionAttribute = this.getSslSessionAttribute();
            if (sessionAttribute != null && !sessionAttribute.isEmpty()) {
                request.setAttribute(sessionAttribute, sslSession);
            }
        }
        catch (Exception e) {
            LOG.warn("EXCEPTION ", e);
        }
    }

    public void setSslSessionAttribute(String attribute) {
        this.sslSessionAttribute = attribute;
    }

    public String getSslSessionAttribute() {
        return this.sslSessionAttribute;
    }

    public String toString() {
        return String.format("%s@%x", this.getClass().getSimpleName(), this.hashCode());
    }

    private static class CachedInfo {
        private final X509Certificate[] _certs;
        private final Integer _keySize;
        private final String _idStr;

        CachedInfo(Integer keySize, X509Certificate[] certs, String idStr) {
            this._keySize = keySize;
            this._certs = certs;
            this._idStr = idStr;
        }

        X509Certificate[] getCerts() {
            return this._certs;
        }

        Integer getKeySize() {
            return this._keySize;
        }

        String getIdStr() {
            return this._idStr;
        }
    }
}

