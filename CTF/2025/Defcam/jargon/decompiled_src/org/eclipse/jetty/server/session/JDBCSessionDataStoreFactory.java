/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.session;

import org.eclipse.jetty.server.session.AbstractSessionDataStoreFactory;
import org.eclipse.jetty.server.session.DatabaseAdaptor;
import org.eclipse.jetty.server.session.JDBCSessionDataStore;
import org.eclipse.jetty.server.session.SessionDataStore;
import org.eclipse.jetty.server.session.SessionHandler;

public class JDBCSessionDataStoreFactory
extends AbstractSessionDataStoreFactory {
    DatabaseAdaptor _adaptor;
    JDBCSessionDataStore.SessionTableSchema _schema;

    @Override
    public SessionDataStore getSessionDataStore(SessionHandler handler) {
        JDBCSessionDataStore ds = new JDBCSessionDataStore();
        ds.setDatabaseAdaptor(this._adaptor);
        ds.setSessionTableSchema(this._schema);
        ds.setGracePeriodSec(this.getGracePeriodSec());
        ds.setSavePeriodSec(this.getSavePeriodSec());
        return ds;
    }

    public void setDatabaseAdaptor(DatabaseAdaptor adaptor) {
        this._adaptor = adaptor;
    }

    public void setSessionTableSchema(JDBCSessionDataStore.SessionTableSchema schema) {
        this._schema = schema;
    }
}

