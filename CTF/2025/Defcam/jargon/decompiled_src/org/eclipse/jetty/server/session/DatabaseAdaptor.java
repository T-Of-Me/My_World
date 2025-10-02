/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.session;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class DatabaseAdaptor {
    static final Logger LOG = Log.getLogger("org.eclipse.jetty.server.session");
    String _dbName;
    boolean _isLower;
    boolean _isUpper;
    protected String _blobType;
    protected String _longType;
    private String _driverClassName;
    private String _connectionUrl;
    private Driver _driver;
    private DataSource _datasource;
    private String _jndiName;

    public void adaptTo(DatabaseMetaData dbMeta) throws SQLException {
        this._dbName = dbMeta.getDatabaseProductName().toLowerCase(Locale.ENGLISH);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Using database {}", this._dbName);
        }
        this._isLower = dbMeta.storesLowerCaseIdentifiers();
        this._isUpper = dbMeta.storesUpperCaseIdentifiers();
    }

    public void setBlobType(String blobType) {
        this._blobType = blobType;
    }

    public String getBlobType() {
        if (this._blobType != null) {
            return this._blobType;
        }
        if (this._dbName.startsWith("postgres")) {
            return "bytea";
        }
        return "blob";
    }

    public void setLongType(String longType) {
        this._longType = longType;
    }

    public String getLongType() {
        if (this._longType != null) {
            return this._longType;
        }
        if (this._dbName == null) {
            throw new IllegalStateException("DbAdaptor missing metadata");
        }
        if (this._dbName.startsWith("oracle")) {
            return "number(20)";
        }
        return "bigint";
    }

    public String convertIdentifier(String identifier) {
        if (identifier == null) {
            return null;
        }
        if (this._dbName == null) {
            throw new IllegalStateException("DbAdaptor missing metadata");
        }
        if (this._isLower) {
            return identifier.toLowerCase(Locale.ENGLISH);
        }
        if (this._isUpper) {
            return identifier.toUpperCase(Locale.ENGLISH);
        }
        return identifier;
    }

    public String getDBName() {
        return this._dbName;
    }

    public InputStream getBlobInputStream(ResultSet result, String columnName) throws SQLException {
        if (this._dbName == null) {
            throw new IllegalStateException("DbAdaptor missing metadata");
        }
        if (this._dbName.startsWith("postgres")) {
            byte[] bytes = result.getBytes(columnName);
            return new ByteArrayInputStream(bytes);
        }
        Blob blob = result.getBlob(columnName);
        return blob.getBinaryStream();
    }

    public boolean isEmptyStringNull() {
        if (this._dbName == null) {
            throw new IllegalStateException("DbAdaptor missing metadata");
        }
        return this._dbName.startsWith("oracle");
    }

    public boolean isRowIdReserved() {
        if (this._dbName == null) {
            throw new IllegalStateException("DbAdaptor missing metadata");
        }
        return this._dbName != null && this._dbName.startsWith("oracle");
    }

    public void setDriverInfo(String driverClassName, String connectionUrl) {
        this._driverClassName = driverClassName;
        this._connectionUrl = connectionUrl;
    }

    public void setDriverInfo(Driver driverClass, String connectionUrl) {
        this._driver = driverClass;
        this._connectionUrl = connectionUrl;
    }

    public void setDatasource(DataSource ds) {
        this._datasource = ds;
    }

    public void setDatasourceName(String jndi) {
        this._jndiName = jndi;
    }

    public String getDatasourceName() {
        return this._jndiName;
    }

    public DataSource getDatasource() {
        return this._datasource;
    }

    public String getDriverClassName() {
        return this._driverClassName;
    }

    public Driver getDriver() {
        return this._driver;
    }

    public String getConnectionUrl() {
        return this._connectionUrl;
    }

    public void initialize() throws Exception {
        if (this._datasource != null) {
            return;
        }
        if (this._jndiName != null) {
            InitialContext ic = new InitialContext();
            this._datasource = (DataSource)ic.lookup(this._jndiName);
        } else if (this._driver != null && this._connectionUrl != null) {
            DriverManager.registerDriver(this._driver);
        } else if (this._driverClassName != null && this._connectionUrl != null) {
            Class.forName(this._driverClassName);
        } else {
            try {
                InitialContext ic = new InitialContext();
                this._datasource = (DataSource)ic.lookup("jdbc/sessions");
            }
            catch (NamingException e) {
                throw new IllegalStateException("No database configured for sessions");
            }
        }
    }

    protected Connection getConnection() throws SQLException {
        if (this._datasource != null) {
            return this._datasource.getConnection();
        }
        return DriverManager.getConnection(this._connectionUrl);
    }

    public String toString() {
        return String.format("%s[jndi=%s,driver=%s]", super.toString(), this._jndiName, this._driverClassName);
    }
}

