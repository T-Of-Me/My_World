/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.log;

import java.util.Properties;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public abstract class AbstractLogger
implements Logger {
    public static final int LEVEL_DEFAULT = -1;
    public static final int LEVEL_ALL = 0;
    public static final int LEVEL_DEBUG = 1;
    public static final int LEVEL_INFO = 2;
    public static final int LEVEL_WARN = 3;
    public static final int LEVEL_OFF = 10;

    @Override
    public final Logger getLogger(String name) {
        if (AbstractLogger.isBlank(name)) {
            return this;
        }
        String basename = this.getName();
        String fullname = AbstractLogger.isBlank(basename) || Log.getRootLogger() == this ? name : basename + "." + name;
        Logger logger = Log.getLoggers().get(fullname);
        if (logger == null) {
            Logger newlog = this.newLogger(fullname);
            logger = Log.getMutableLoggers().putIfAbsent(fullname, newlog);
            if (logger == null) {
                logger = newlog;
            }
        }
        return logger;
    }

    protected abstract Logger newLogger(String var1);

    private static boolean isBlank(String name) {
        if (name == null) {
            return true;
        }
        int size = name.length();
        for (int i = 0; i < size; ++i) {
            char c = name.charAt(i);
            if (Character.isWhitespace(c)) continue;
            return false;
        }
        return true;
    }

    public static int lookupLoggingLevel(Properties props, String name) {
        if (props == null || props.isEmpty() || name == null) {
            return -1;
        }
        String nameSegment = name;
        while (nameSegment != null && nameSegment.length() > 0) {
            String levelStr = props.getProperty(nameSegment + ".LEVEL");
            int level = AbstractLogger.getLevelId(nameSegment + ".LEVEL", levelStr);
            if (level != -1) {
                return level;
            }
            int idx = nameSegment.lastIndexOf(46);
            if (idx >= 0) {
                nameSegment = nameSegment.substring(0, idx);
                continue;
            }
            nameSegment = null;
        }
        return -1;
    }

    public static String getLoggingProperty(Properties props, String name, String property) {
        String nameSegment = name;
        while (nameSegment != null && nameSegment.length() > 0) {
            String s = props.getProperty(nameSegment + "." + property);
            if (s != null) {
                return s;
            }
            int idx = nameSegment.lastIndexOf(46);
            nameSegment = idx >= 0 ? nameSegment.substring(0, idx) : null;
        }
        return null;
    }

    protected static int getLevelId(String levelSegment, String levelName) {
        if (levelName == null) {
            return -1;
        }
        String levelStr = levelName.trim();
        if ("ALL".equalsIgnoreCase(levelStr)) {
            return 0;
        }
        if ("DEBUG".equalsIgnoreCase(levelStr)) {
            return 1;
        }
        if ("INFO".equalsIgnoreCase(levelStr)) {
            return 2;
        }
        if ("WARN".equalsIgnoreCase(levelStr)) {
            return 3;
        }
        if ("OFF".equalsIgnoreCase(levelStr)) {
            return 10;
        }
        System.err.println("Unknown StdErrLog level [" + levelSegment + "]=[" + levelStr + "], expecting only [ALL, DEBUG, INFO, WARN, OFF] as values.");
        return -1;
    }

    protected static String condensePackageString(String classname) {
        if (classname == null || classname.isEmpty()) {
            return "";
        }
        String allowed = classname.replaceAll("[^\\w.]", "");
        int len = allowed.length();
        while (allowed.charAt(--len) == '.') {
        }
        String[] parts = allowed.substring(0, len + 1).split("\\.");
        StringBuilder dense = new StringBuilder();
        for (int i = 0; i < parts.length - 1; ++i) {
            String part = parts[i].trim();
            if (part.isEmpty()) continue;
            dense.append(part.charAt(0));
        }
        if (dense.length() > 0) {
            dense.append('.');
        }
        dense.append(parts[parts.length - 1]);
        return dense.toString();
    }

    @Override
    public void debug(String msg, long arg) {
        if (this.isDebugEnabled()) {
            this.debug(msg, new Object[]{new Long(arg)});
        }
    }
}

