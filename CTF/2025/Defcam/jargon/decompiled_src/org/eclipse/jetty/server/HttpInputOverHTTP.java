/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server;

import java.io.IOException;
import org.eclipse.jetty.server.HttpChannelState;
import org.eclipse.jetty.server.HttpConnection;
import org.eclipse.jetty.server.HttpInput;

public class HttpInputOverHTTP
extends HttpInput {
    public HttpInputOverHTTP(HttpChannelState state) {
        super(state);
    }

    @Override
    protected void produceContent() throws IOException {
        ((HttpConnection)this.getHttpChannelState().getHttpChannel().getEndPoint().getConnection()).fillAndParseForContent();
    }

    @Override
    protected void blockForContent() throws IOException {
        ((HttpConnection)this.getHttpChannelState().getHttpChannel().getEndPoint().getConnection()).blockingReadFillInterested();
        try {
            super.blockForContent();
        }
        catch (Throwable e) {
            ((HttpConnection)this.getHttpChannelState().getHttpChannel().getEndPoint().getConnection()).blockingReadException(e);
        }
    }
}

