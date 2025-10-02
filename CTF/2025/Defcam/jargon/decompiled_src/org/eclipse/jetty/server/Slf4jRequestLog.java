/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server;

import java.io.IOException;
import org.eclipse.jetty.server.AbstractNCSARequestLog;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.log.Slf4jLog;

@ManagedObject(value="NCSA standard format request log to slf4j bridge")
public class Slf4jRequestLog
extends AbstractNCSARequestLog {
    private Slf4jLog logger;
    private String loggerName = "org.eclipse.jetty.server.RequestLog";

    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }

    public String getLoggerName() {
        return this.loggerName;
    }

    @Override
    protected boolean isEnabled() {
        return this.logger != null;
    }

    @Override
    public void write(String requestEntry) throws IOException {
        this.logger.info(requestEntry, new Object[0]);
    }

    @Override
    protected synchronized void doStart() throws Exception {
        this.logger = new Slf4jLog(this.loggerName);
        super.doStart();
    }
}

