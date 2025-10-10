/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.log;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.regex.Pattern;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class JettyLogHandler
extends Handler {
    public static void config() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL url = cl.getResource("logging.properties");
        if (url != null) {
            System.err.printf("Initializing java.util.logging from %s%n", url);
            try (InputStream in = url.openStream();){
                LogManager.getLogManager().readConfiguration(in);
            }
            catch (IOException e) {
                e.printStackTrace(System.err);
            }
        } else {
            System.err.printf("WARNING: java.util.logging failed to initialize: logging.properties not found%n", new Object[0]);
        }
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.Jdk14Logger");
    }

    public JettyLogHandler() {
        if (Boolean.parseBoolean(Log.__props.getProperty("org.eclipse.jetty.util.log.DEBUG", "false"))) {
            this.setLevel(Level.FINEST);
        }
        if (Boolean.parseBoolean(Log.__props.getProperty("org.eclipse.jetty.util.log.IGNORED", "false"))) {
            this.setLevel(Level.ALL);
        }
        System.err.printf("%s Initialized at level [%s]%n", this.getClass().getName(), this.getLevel().getName());
    }

    private synchronized String formatMessage(LogRecord record) {
        String msg = this.getMessage(record);
        try {
            Object[] params = record.getParameters();
            if (params == null || params.length == 0) {
                return msg;
            }
            if (Pattern.compile("\\{\\d+\\}").matcher(msg).find()) {
                return MessageFormat.format(msg, params);
            }
            return msg;
        }
        catch (Exception ex) {
            return msg;
        }
    }

    private String getMessage(LogRecord record) {
        ResourceBundle bundle = record.getResourceBundle();
        if (bundle != null) {
            try {
                return bundle.getString(record.getMessage());
            }
            catch (MissingResourceException missingResourceException) {
                // empty catch block
            }
        }
        return record.getMessage();
    }

    @Override
    public void publish(LogRecord record) {
        Logger JLOG = this.getJettyLogger(record.getLoggerName());
        int level = record.getLevel().intValue();
        if (level >= Level.OFF.intValue()) {
            return;
        }
        Throwable cause = record.getThrown();
        String msg = this.formatMessage(record);
        if (level >= Level.WARNING.intValue()) {
            if (cause != null) {
                JLOG.warn(msg, cause);
            } else {
                JLOG.warn(msg, new Object[0]);
            }
            return;
        }
        if (level >= Level.INFO.intValue()) {
            if (cause != null) {
                JLOG.info(msg, cause);
            } else {
                JLOG.info(msg, new Object[0]);
            }
            return;
        }
        if (level >= Level.FINEST.intValue()) {
            if (cause != null) {
                JLOG.debug(msg, cause);
            } else {
                JLOG.debug(msg, new Object[0]);
            }
            return;
        }
        if (level >= Level.ALL.intValue()) {
            JLOG.ignore(cause);
            return;
        }
    }

    private Logger getJettyLogger(String loggerName) {
        return Log.getLogger(loggerName);
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
    }
}

