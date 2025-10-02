/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server;

import java.util.ArrayList;
import java.util.Arrays;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Response;

class RequestLogCollection
implements RequestLog {
    private final ArrayList<RequestLog> delegates;

    public RequestLogCollection(RequestLog ... requestLogs) {
        this.delegates = new ArrayList<RequestLog>(Arrays.asList(requestLogs));
    }

    public void add(RequestLog requestLog) {
        this.delegates.add(requestLog);
    }

    @Override
    public void log(Request request, Response response) {
        for (RequestLog delegate : this.delegates) {
            delegate.log(request, response);
        }
    }
}

