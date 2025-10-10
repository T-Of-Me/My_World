/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.session;

import org.eclipse.jetty.server.session.SessionContext;

public class UnwriteableSessionDataException
extends Exception {
    private String _id;
    private SessionContext _sessionContext;

    public UnwriteableSessionDataException(String id, SessionContext contextId, Throwable t) {
        super("Unwriteable session " + id + " for " + contextId, t);
        this._id = id;
    }

    public String getId() {
        return this._id;
    }

    public SessionContext getSessionContext() {
        return this._sessionContext;
    }
}

