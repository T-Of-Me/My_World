/*
 * Decompiled with CFR 0.152.
 */
package org.sqlite.javax;

import java.sql.SQLException;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;
import org.sqlite.javax.SQLitePooledConnection;

public class SQLiteConnectionPoolDataSource
extends SQLiteDataSource
implements ConnectionPoolDataSource {
    public SQLiteConnectionPoolDataSource() {
    }

    public SQLiteConnectionPoolDataSource(SQLiteConfig config) {
        super(config);
    }

    @Override
    public PooledConnection getPooledConnection() throws SQLException {
        return this.getPooledConnection(null, null);
    }

    @Override
    public PooledConnection getPooledConnection(String user, String password) throws SQLException {
        return new SQLitePooledConnection(this.getConnection(user, password));
    }
}

