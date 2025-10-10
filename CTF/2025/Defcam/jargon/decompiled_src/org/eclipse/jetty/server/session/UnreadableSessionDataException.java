/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.session;

import org.eclipse.jetty.server.session.SessionContext;

public class UnreadableSessionDataException
extends Exception {
    private static final long serialVersionUID = 1806303483488966566L;
    private String _id;
    private SessionContext _sessionContext;

    public String getId() {
        return this._id;
    }

    public SessionContext getSessionContext() {
        return this._sessionContext;
    }

    public UnreadableSessionDataException(String id, SessionContext contextId, Throwable t) {
        super("Unreadable session " + id + " for " + contextId, t);
        this._sessionContext = contextId;
        this._id = id;
    }
}

