/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.log;

import org.eclipse.jetty.util.log.AbstractLogger;
import org.eclipse.jetty.util.log.JettyAwareLogger;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LocationAwareLogger;

public class Slf4jLog
extends AbstractLogger {
    private final org.slf4j.Logger _logger;

    public Slf4jLog() throws Exception {
        this("org.eclipse.jetty.util.log");
    }

    public Slf4jLog(String name) {
        org.slf4j.Logger logger = LoggerFactory.getLogger(name);
        this._logger = logger instanceof LocationAwareLogger ? new JettyAwareLogger((LocationAwareLogger)logger) : logger;
    }

    @Override
    public String getName() {
        return this._logger.getName();
    }

    @Override
    public void warn(String msg, Object ... args) {
        this._logger.warn(msg, args);
    }

    @Override
    public void warn(Throwable thrown) {
        this.warn("", thrown);
    }

    @Override
    public void warn(String msg, Throwable thrown) {
        this._logger.warn(msg, thrown);
    }

    @Override
    public void info(String msg, Object ... args) {
        this._logger.info(msg, args);
    }

    @Override
    public void info(Throwable thrown) {
        this.info("", thrown);
    }

    @Override
    public void info(String msg, Throwable thrown) {
        this._logger.info(msg, thrown);
    }

    @Override
    public void debug(String msg, Object ... args) {
        this._logger.debug(msg, args);
    }

    @Override
    public void debug(String msg, long arg) {
        if (this.isDebugEnabled()) {
            this._logger.debug(msg, new Object[]{new Long(arg)});
        }
    }

    @Override
    public void debug(Throwable thrown) {
        this.debug("", thrown);
    }

    @Override
    public void debug(String msg, Throwable thrown) {
        this._logger.debug(msg, thrown);
    }

    @Override
    public boolean isDebugEnabled() {
        return this._logger.isDebugEnabled();
    }

    @Override
    public void setDebugEnabled(boolean enabled) {
        this.warn("setDebugEnabled not implemented", null, null);
    }

    @Override
    protected Logger newLogger(String fullname) {
        return new Slf4jLog(fullname);
    }

    @Override
    public void ignore(Throwable ignored) {
        if (Log.isIgnored()) {
            this.debug("IGNORED EXCEPTION ", ignored);
        }
    }

    public String toString() {
        return this._logger.toString();
    }
}

