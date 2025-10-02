/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server;

import org.eclipse.jetty.http.HttpCompliance;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.AbstractConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnection;
import org.eclipse.jetty.util.annotation.Name;

public class HttpConnectionFactory
extends AbstractConnectionFactory
implements HttpConfiguration.ConnectionFactory {
    private final HttpConfiguration _config;
    private HttpCompliance _httpCompliance;
    private boolean _recordHttpComplianceViolations = false;

    public HttpConnectionFactory() {
        this(new HttpConfiguration());
    }

    public HttpConnectionFactory(@Name(value="config") HttpConfiguration config) {
        this(config, null);
    }

    public HttpConnectionFactory(@Name(value="config") HttpConfiguration config, @Name(value="compliance") HttpCompliance compliance) {
        super(HttpVersion.HTTP_1_1.asString());
        this._config = config;
        HttpCompliance httpCompliance = this._httpCompliance = compliance == null ? HttpCompliance.RFC7230 : compliance;
        if (config == null) {
            throw new IllegalArgumentException("Null HttpConfiguration");
        }
        this.addBean(this._config);
    }

    @Override
    public HttpConfiguration getHttpConfiguration() {
        return this._config;
    }

    public HttpCompliance getHttpCompliance() {
        return this._httpCompliance;
    }

    public boolean isRecordHttpComplianceViolations() {
        return this._recordHttpComplianceViolations;
    }

    public void setHttpCompliance(HttpCompliance httpCompliance) {
        this._httpCompliance = httpCompliance;
    }

    @Override
    public Connection newConnection(Connector connector, EndPoint endPoint) {
        HttpConnection conn = new HttpConnection(this._config, connector, endPoint, this._httpCompliance, this.isRecordHttpComplianceViolations());
        return this.configure(conn, connector, endPoint);
    }

    public void setRecordHttpComplianceViolations(boolean recordHttpComplianceViolations) {
        this._recordHttpComplianceViolations = recordHttpComplianceViolations;
    }
}

