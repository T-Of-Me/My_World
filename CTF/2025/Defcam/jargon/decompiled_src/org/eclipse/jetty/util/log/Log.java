/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.log;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.eclipse.jetty.util.Loader;
import org.eclipse.jetty.util.Uptime;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.log.LoggerLog;
import org.eclipse.jetty.util.log.StdErrLog;

public class Log {
    public static final String EXCEPTION = "EXCEPTION ";
    public static final String IGNORED = "IGNORED EXCEPTION ";
    protected static final Properties __props;
    public static String __logClass;
    public static boolean __ignored;
    private static final ConcurrentMap<String, Logger> __loggers;
    private static Logger LOG;
    private static boolean __initialized;

    static void loadProperties(String resourceName, Properties props) {
        URL testProps = Loader.getResource(resourceName);
        if (testProps != null) {
            try (InputStream in = testProps.openStream();){
                Properties p = new Properties();
                p.load(in);
                for (Object key : p.keySet()) {
                    Object value = p.get(key);
                    if (value == null) continue;
                    props.put(key, value);
                }
            }
            catch (IOException e) {
                System.err.println("[WARN] Error loading logging config: " + testProps);
                e.printStackTrace(System.err);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void initialized() {
        Class<Log> clazz = Log.class;
        synchronized (Log.class) {
            if (__initialized) {
                // ** MonitorExit[var0] (shouldn't be in output)
                return;
            }
            __initialized = true;
            Boolean announce = Boolean.parseBoolean(__props.getProperty("org.eclipse.jetty.util.log.announce", "true"));
            try {
                Class log_class;
                Class clazz2 = log_class = __logClass == null ? null : Loader.loadClass(__logClass);
                if (LOG == null || log_class != null && !LOG.getClass().equals(log_class)) {
                    LOG = (Logger)log_class.newInstance();
                    if (announce.booleanValue()) {
                        LOG.debug("Logging to {} via {}", LOG, log_class.getName());
                    }
                }
            }
            catch (Throwable e) {
                Log.initStandardLogging(e);
            }
            if (announce.booleanValue() && LOG != null) {
                LOG.info(String.format("Logging initialized @%dms to %s", Uptime.getUptime(), LOG.getClass().getName()), new Object[0]);
            }
            // ** MonitorExit[var0] (shouldn't be in output)
            return;
        }
    }

    private static void initStandardLogging(Throwable e) {
        if (e != null && __ignored) {
            e.printStackTrace(System.err);
        }
        if (LOG == null) {
            Class<StdErrLog> log_class = StdErrLog.class;
            LOG = new StdErrLog();
            Boolean announce = Boolean.parseBoolean(__props.getProperty("org.eclipse.jetty.util.log.announce", "true"));
            if (announce.booleanValue()) {
                LOG.debug("Logging to {} via {}", LOG, log_class.getName());
            }
        }
    }

    public static Logger getLog() {
        Log.initialized();
        return LOG;
    }

    public static void setLog(Logger log) {
        LOG = log;
        __logClass = null;
    }

    public static Logger getRootLogger() {
        Log.initialized();
        return LOG;
    }

    static boolean isIgnored() {
        return __ignored;
    }

    public static void setLogToParent(String name) {
        ClassLoader loader = Log.class.getClassLoader();
        if (loader != null && loader.getParent() != null) {
            try {
                Class<?> uberlog = loader.getParent().loadClass("org.eclipse.jetty.util.log.Log");
                Method getLogger = uberlog.getMethod("getLogger", String.class);
                Object logger = getLogger.invoke(null, name);
                Log.setLog(new LoggerLog(logger));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.setLog(Log.getLogger(name));
        }
    }

    public static Logger getLogger(Class<?> clazz) {
        return Log.getLogger(clazz.getName());
    }

    public static Logger getLogger(String name) {
        Log.initialized();
        if (name == null) {
            return LOG;
        }
        Logger logger = (Logger)__loggers.get(name);
        if (logger == null) {
            logger = LOG.getLogger(name);
        }
        return logger;
    }

    static ConcurrentMap<String, Logger> getMutableLoggers() {
        return __loggers;
    }

    @ManagedAttribute(value="list of all instantiated loggers")
    public static Map<String, Logger> getLoggers() {
        return Collections.unmodifiableMap(__loggers);
    }

    public static Properties getProperties() {
        return __props;
    }

    static {
        __loggers = new ConcurrentHashMap<String, Logger>();
        __props = new Properties();
        AccessController.doPrivileged(new PrivilegedAction<Object>(){

            @Override
            public Object run() {
                Log.loadProperties("jetty-logging.properties", __props);
                String osName = System.getProperty("os.name");
                if (osName != null && osName.length() > 0) {
                    osName = osName.toLowerCase(Locale.ENGLISH).replace(' ', '-');
                    Log.loadProperties("jetty-logging-" + osName + ".properties", __props);
                }
                Enumeration<?> systemKeyEnum = System.getProperties().propertyNames();
                while (systemKeyEnum.hasMoreElements()) {
                    String key = (String)systemKeyEnum.nextElement();
                    String val = System.getProperty(key);
                    if (val == null) continue;
                    __props.setProperty(key, val);
                }
                __logClass = __props.getProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.Slf4jLog");
                __ignored = Boolean.parseBoolean(__props.getProperty("org.eclipse.jetty.util.log.IGNORED", "false"));
                return null;
            }
        });
        __initialized = false;
    }
}

