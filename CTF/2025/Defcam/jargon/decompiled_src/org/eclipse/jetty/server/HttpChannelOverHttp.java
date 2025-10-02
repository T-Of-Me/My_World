/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jetty.http.BadMessageException;
import org.eclipse.jetty.http.HostPortHttpField;
import org.eclipse.jetty.http.HttpCompliance;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpGenerator;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpHeaderValue;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpParser;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpChannelState;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnection;
import org.eclipse.jetty.server.HttpInput;
import org.eclipse.jetty.server.HttpInputOverHTTP;
import org.eclipse.jetty.server.HttpTransport;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class HttpChannelOverHttp
extends HttpChannel
implements HttpParser.RequestHandler,
HttpParser.ComplianceHandler {
    private static final Logger LOG = Log.getLogger(HttpChannelOverHttp.class);
    private static final HttpField PREAMBLE_UPGRADE_H2C = new HttpField(HttpHeader.UPGRADE, "h2c");
    private static final String ATTR_COMPLIANCE_VIOLATIONS = "org.eclipse.jetty.http.compliance.violations";
    private final HttpFields _fields = new HttpFields();
    private final MetaData.Request _metadata = new MetaData.Request(this._fields);
    private final HttpConnection _httpConnection;
    private HttpField _connection;
    private HttpField _upgrade = null;
    private boolean _delayedForContent;
    private boolean _unknownExpectation = false;
    private boolean _expect100Continue = false;
    private boolean _expect102Processing = false;
    private List<String> _complianceViolations;
    private HttpFields _trailers;

    public HttpChannelOverHttp(HttpConnection httpConnection, Connector connector, HttpConfiguration config, EndPoint endPoint, HttpTransport transport) {
        super(connector, config, endPoint, transport);
        this._httpConnection = httpConnection;
        this._metadata.setURI(new HttpURI());
    }

    @Override
    protected HttpInput newHttpInput(HttpChannelState state) {
        return new HttpInputOverHTTP(state);
    }

    @Override
    public void recycle() {
        super.recycle();
        this._unknownExpectation = false;
        this._expect100Continue = false;
        this._expect102Processing = false;
        this._metadata.recycle();
        this._connection = null;
        this._fields.clear();
        this._upgrade = null;
        this._trailers = null;
    }

    @Override
    public boolean isExpecting100Continue() {
        return this._expect100Continue;
    }

    @Override
    public boolean isExpecting102Processing() {
        return this._expect102Processing;
    }

    @Override
    public boolean startRequest(String method, String uri, HttpVersion version) {
        this._metadata.setMethod(method);
        this._metadata.getURI().parseRequestTarget(method, uri);
        this._metadata.setHttpVersion(version);
        this._unknownExpectation = false;
        this._expect100Continue = false;
        this._expect102Processing = false;
        return false;
    }

    @Override
    public void parsedHeader(HttpField field) {
        HttpHeader header = field.getHeader();
        String value = field.getValue();
        if (header != null) {
            block0 : switch (header) {
                case CONNECTION: {
                    this._connection = field;
                    break;
                }
                case HOST: {
                    if (this._metadata.getURI().isAbsolute() || !(field instanceof HostPortHttpField)) break;
                    HostPortHttpField hp = (HostPortHttpField)field;
                    this._metadata.getURI().setAuthority(hp.getHost(), hp.getPort());
                    break;
                }
                case EXPECT: {
                    if (this._metadata.getHttpVersion() != HttpVersion.HTTP_1_1) break;
                    HttpHeaderValue expect = HttpHeaderValue.CACHE.get(value);
                    switch (expect == null ? HttpHeaderValue.UNKNOWN : expect) {
                        case CONTINUE: {
                            this._expect100Continue = true;
                            break block0;
                        }
                        case PROCESSING: {
                            this._expect102Processing = true;
                            break block0;
                        }
                    }
                    String[] values = field.getValues();
                    block14: for (int i = 0; values != null && i < values.length; ++i) {
                        expect = HttpHeaderValue.CACHE.get(values[i].trim());
                        if (expect == null) {
                            this._unknownExpectation = true;
                            continue;
                        }
                        switch (expect) {
                            case CONTINUE: {
                                this._expect100Continue = true;
                                continue block14;
                            }
                            case PROCESSING: {
                                this._expect102Processing = true;
                                continue block14;
                            }
                            default: {
                                this._unknownExpectation = true;
                            }
                        }
                    }
                    break;
                }
                case UPGRADE: {
                    this._upgrade = field;
                    break;
                }
            }
        }
        this._fields.add(field);
    }

    @Override
    public void parsedTrailer(HttpField field) {
        if (this._trailers == null) {
            this._trailers = new HttpFields();
        }
        this._trailers.add(field);
    }

    @Override
    public void continue100(int available) throws IOException {
        if (this.isExpecting100Continue()) {
            this._expect100Continue = false;
            if (available == 0) {
                if (this.getResponse().isCommitted()) {
                    throw new IOException("Committed before 100 Continues");
                }
                boolean committed = this.sendResponse(HttpGenerator.CONTINUE_100_INFO, null, false);
                if (!committed) {
                    throw new IOException("Concurrent commit while trying to send 100-Continue");
                }
            }
        }
    }

    @Override
    public void earlyEOF() {
        this._httpConnection.getGenerator().setPersistent(false);
        if (this._metadata.getMethod() == null) {
            this._httpConnection.close();
        } else if (this.onEarlyEOF() || this._delayedForContent) {
            this._delayedForContent = false;
            this.handle();
        }
    }

    @Override
    public boolean content(ByteBuffer content) {
        HttpInput.Content c = this._httpConnection.newContent(content);
        boolean handle = this.onContent(c) || this._delayedForContent;
        this._delayedForContent = false;
        return handle;
    }

    @Override
    public void asyncReadFillInterested() {
        this._httpConnection.asyncReadFillInterested();
    }

    @Override
    public void badMessage(int status, String reason) {
        this._httpConnection.getGenerator().setPersistent(false);
        try {
            this.onRequest(this._metadata);
            this.getRequest().getHttpInput().earlyEOF();
        }
        catch (Exception e) {
            LOG.ignore(e);
        }
        this.onBadMessage(status, reason);
    }

    @Override
    public boolean headerComplete() {
        boolean persistent;
        if (this._complianceViolations != null && !this._complianceViolations.isEmpty()) {
            this.getRequest().setAttribute(ATTR_COMPLIANCE_VIOLATIONS, this._complianceViolations);
            this._complianceViolations = null;
        }
        switch (this._metadata.getHttpVersion()) {
            case HTTP_0_9: {
                persistent = false;
                break;
            }
            case HTTP_1_0: {
                persistent = this.getHttpConfiguration().isPersistentConnectionsEnabled() ? (this._connection != null ? (this._connection.contains(HttpHeaderValue.KEEP_ALIVE.asString()) ? true : this._fields.contains(HttpHeader.CONNECTION, HttpHeaderValue.KEEP_ALIVE.asString())) : false) : false;
                if (!persistent) {
                    persistent = HttpMethod.CONNECT.is(this._metadata.getMethod());
                }
                if (!persistent) break;
                this.getResponse().getHttpFields().add(HttpHeader.CONNECTION, HttpHeaderValue.KEEP_ALIVE);
                break;
            }
            case HTTP_1_1: {
                if (this._unknownExpectation) {
                    this.badMessage(417, null);
                    return false;
                }
                persistent = this.getHttpConfiguration().isPersistentConnectionsEnabled() ? (this._connection != null ? (this._connection.contains(HttpHeaderValue.CLOSE.asString()) ? false : !this._fields.contains(HttpHeader.CONNECTION, HttpHeaderValue.CLOSE.asString())) : true) : false;
                if (!persistent) {
                    persistent = HttpMethod.CONNECT.is(this._metadata.getMethod());
                }
                if (!persistent) {
                    this.getResponse().getHttpFields().add(HttpHeader.CONNECTION, HttpHeaderValue.CLOSE);
                }
                if (this._upgrade == null || !this.upgrade()) break;
                return true;
            }
            case HTTP_2: {
                this._upgrade = PREAMBLE_UPGRADE_H2C;
                if (HttpMethod.PRI.is(this._metadata.getMethod()) && "*".equals(this._metadata.getURI().toString()) && this._fields.size() == 0 && this.upgrade()) {
                    return true;
                }
                this.badMessage(426, null);
                this._httpConnection.getParser().close();
                return false;
            }
            default: {
                throw new IllegalStateException("unsupported version " + (Object)((Object)this._metadata.getHttpVersion()));
            }
        }
        if (!persistent) {
            this._httpConnection.getGenerator().setPersistent(false);
        }
        this.onRequest(this._metadata);
        this._delayedForContent = this.getHttpConfiguration().isDelayDispatchUntilContent() && (this._httpConnection.getParser().getContentLength() > 0L || this._httpConnection.getParser().isChunking()) && !this.isExpecting100Continue() && !this.isCommitted() && this._httpConnection.isRequestBufferEmpty();
        return !this._delayedForContent;
    }

    private boolean upgrade() throws BadMessageException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("upgrade {} {}", this, this._upgrade);
        }
        if (!(this._upgrade == PREAMBLE_UPGRADE_H2C || this._connection != null && this._connection.contains("upgrade"))) {
            throw new BadMessageException(400);
        }
        ConnectionFactory.Upgrading factory = null;
        for (ConnectionFactory f : this.getConnector().getConnectionFactories()) {
            if (!(f instanceof ConnectionFactory.Upgrading) || !f.getProtocols().contains(this._upgrade.getValue())) continue;
            factory = (ConnectionFactory.Upgrading)f;
            break;
        }
        if (factory == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No factory for {} in {}", this._upgrade, this.getConnector());
            }
            return false;
        }
        HttpFields response101 = new HttpFields();
        Connection upgrade_connection = factory.upgradeConnection(this.getConnector(), this.getEndPoint(), this._metadata, response101);
        if (upgrade_connection == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Upgrade ignored for {} by {}", this._upgrade, factory);
            }
            return false;
        }
        try {
            if (this._upgrade != PREAMBLE_UPGRADE_H2C) {
                this.sendResponse(new MetaData.Response(HttpVersion.HTTP_1_1, 101, response101, 0L), null, true);
            }
        }
        catch (IOException e) {
            throw new BadMessageException(500, null, e);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Upgrade from {} to {}", this.getEndPoint().getConnection(), upgrade_connection);
        }
        this.getRequest().setAttribute("org.eclipse.jetty.server.HttpConnection.UPGRADE", upgrade_connection);
        this.getResponse().setStatus(101);
        this.getHttpTransport().onCompleted();
        return true;
    }

    @Override
    protected void handleException(Throwable x) {
        this._httpConnection.getGenerator().setPersistent(false);
        super.handleException(x);
    }

    @Override
    public void abort(Throwable failure) {
        super.abort(failure);
        this._httpConnection.getGenerator().setPersistent(false);
    }

    @Override
    public boolean contentComplete() {
        boolean handle = this.onContentComplete() || this._delayedForContent;
        this._delayedForContent = false;
        return handle;
    }

    @Override
    public boolean messageComplete() {
        if (this._trailers != null) {
            this.onTrailers(this._trailers);
        }
        return this.onRequestComplete();
    }

    @Override
    public int getHeaderCacheSize() {
        return this.getHttpConfiguration().getHeaderCacheSize();
    }

    @Override
    public void onComplianceViolation(HttpCompliance compliance, HttpCompliance required, String reason) {
        if (this._httpConnection.isRecordHttpComplianceViolations()) {
            if (this._complianceViolations == null) {
                this._complianceViolations = new ArrayList<String>();
            }
            String violation = String.format("%s<%s: %s for %s", new Object[]{compliance, required, reason, this.getHttpTransport()});
            this._complianceViolations.add(violation);
            if (LOG.isDebugEnabled()) {
                LOG.debug(violation, new Object[0]);
            }
        }
    }
}

