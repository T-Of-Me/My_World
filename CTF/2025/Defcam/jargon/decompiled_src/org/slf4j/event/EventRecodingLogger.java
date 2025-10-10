/*
 * Decompiled with CFR 0.152.
 */
package org.slf4j.event;

import java.util.Queue;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.event.SubstituteLoggingEvent;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.helpers.SubstituteLogger;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class EventRecodingLogger
implements Logger {
    String name;
    SubstituteLogger logger;
    Queue<SubstituteLoggingEvent> eventQueue;
    static final boolean RECORD_ALL_EVENTS = true;

    public EventRecodingLogger(SubstituteLogger logger, Queue<SubstituteLoggingEvent> eventQueue) {
        this.logger = logger;
        this.name = logger.getName();
        this.eventQueue = eventQueue;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public boolean isTraceEnabled() {
        return true;
    }

    @Override
    public void trace(String msg) {
        this.recordEvent_0Args(Level.TRACE, null, msg, null);
    }

    @Override
    public void trace(String format, Object arg) {
        this.recordEvent_1Args(Level.TRACE, null, format, arg);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        this.recordEvent2Args(Level.TRACE, null, format, arg1, arg2);
    }

    @Override
    public void trace(String format, Object ... arguments) {
        this.recordEventArgArray(Level.TRACE, null, format, arguments);
    }

    @Override
    public void trace(String msg, Throwable t) {
        this.recordEvent_0Args(Level.TRACE, null, msg, t);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return true;
    }

    @Override
    public void trace(Marker marker, String msg) {
        this.recordEvent_0Args(Level.TRACE, marker, msg, null);
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        this.recordEvent_1Args(Level.TRACE, marker, format, arg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        this.recordEvent2Args(Level.TRACE, marker, format, arg1, arg2);
    }

    @Override
    public void trace(Marker marker, String format, Object ... argArray) {
        this.recordEventArgArray(Level.TRACE, marker, format, argArray);
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        this.recordEvent_0Args(Level.TRACE, marker, msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public void debug(String msg) {
        this.recordEvent_0Args(Level.DEBUG, null, msg, null);
    }

    @Override
    public void debug(String format, Object arg) {
        this.recordEvent_1Args(Level.DEBUG, null, format, arg);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        this.recordEvent2Args(Level.DEBUG, null, format, arg1, arg2);
    }

    @Override
    public void debug(String format, Object ... arguments) {
        this.recordEventArgArray(Level.DEBUG, null, format, arguments);
    }

    @Override
    public void debug(String msg, Throwable t) {
        this.recordEvent_0Args(Level.DEBUG, null, msg, t);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return true;
    }

    @Override
    public void debug(Marker marker, String msg) {
        this.recordEvent_0Args(Level.DEBUG, marker, msg, null);
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        this.recordEvent_1Args(Level.DEBUG, marker, format, arg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        this.recordEvent2Args(Level.DEBUG, marker, format, arg1, arg2);
    }

    @Override
    public void debug(Marker marker, String format, Object ... arguments) {
        this.recordEventArgArray(Level.DEBUG, marker, format, arguments);
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        this.recordEvent_0Args(Level.DEBUG, marker, msg, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public void info(String msg) {
        this.recordEvent_0Args(Level.INFO, null, msg, null);
    }

    @Override
    public void info(String format, Object arg) {
        this.recordEvent_1Args(Level.INFO, null, format, arg);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        this.recordEvent2Args(Level.INFO, null, format, arg1, arg2);
    }

    @Override
    public void info(String format, Object ... arguments) {
        this.recordEventArgArray(Level.INFO, null, format, arguments);
    }

    @Override
    public void info(String msg, Throwable t) {
        this.recordEvent_0Args(Level.INFO, null, msg, t);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return true;
    }

    @Override
    public void info(Marker marker, String msg) {
        this.recordEvent_0Args(Level.INFO, marker, msg, null);
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        this.recordEvent_1Args(Level.INFO, marker, format, arg);
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        this.recordEvent2Args(Level.INFO, marker, format, arg1, arg2);
    }

    @Override
    public void info(Marker marker, String format, Object ... arguments) {
        this.recordEventArgArray(Level.INFO, marker, format, arguments);
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        this.recordEvent_0Args(Level.INFO, marker, msg, t);
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public void warn(String msg) {
        this.recordEvent_0Args(Level.WARN, null, msg, null);
    }

    @Override
    public void warn(String format, Object arg) {
        this.recordEvent_1Args(Level.WARN, null, format, arg);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        this.recordEvent2Args(Level.WARN, null, format, arg1, arg2);
    }

    @Override
    public void warn(String format, Object ... arguments) {
        this.recordEventArgArray(Level.WARN, null, format, arguments);
    }

    @Override
    public void warn(String msg, Throwable t) {
        this.recordEvent_0Args(Level.WARN, null, msg, t);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return true;
    }

    @Override
    public void warn(Marker marker, String msg) {
        this.recordEvent_0Args(Level.WARN, marker, msg, null);
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        this.recordEvent_1Args(Level.WARN, marker, format, arg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        this.recordEvent2Args(Level.WARN, marker, format, arg1, arg2);
    }

    @Override
    public void warn(Marker marker, String format, Object ... arguments) {
        this.recordEventArgArray(Level.WARN, marker, format, arguments);
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        this.recordEvent_0Args(Level.WARN, marker, msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    public void error(String msg) {
        this.recordEvent_0Args(Level.ERROR, null, msg, null);
    }

    @Override
    public void error(String format, Object arg) {
        this.recordEvent_1Args(Level.ERROR, null, format, arg);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        this.recordEvent2Args(Level.ERROR, null, format, arg1, arg2);
    }

    @Override
    public void error(String format, Object ... arguments) {
        this.recordEventArgArray(Level.ERROR, null, format, arguments);
    }

    @Override
    public void error(String msg, Throwable t) {
        this.recordEvent_0Args(Level.ERROR, null, msg, t);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return true;
    }

    @Override
    public void error(Marker marker, String msg) {
        this.recordEvent_0Args(Level.ERROR, marker, msg, null);
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        this.recordEvent_1Args(Level.ERROR, marker, format, arg);
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        this.recordEvent2Args(Level.ERROR, marker, format, arg1, arg2);
    }

    @Override
    public void error(Marker marker, String format, Object ... arguments) {
        this.recordEventArgArray(Level.ERROR, marker, format, arguments);
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        this.recordEvent_0Args(Level.ERROR, marker, msg, t);
    }

    private void recordEvent_0Args(Level level, Marker marker, String msg, Throwable t) {
        this.recordEvent(level, marker, msg, null, t);
    }

    private void recordEvent_1Args(Level level, Marker marker, String msg, Object arg1) {
        this.recordEvent(level, marker, msg, new Object[]{arg1}, null);
    }

    private void recordEvent2Args(Level level, Marker marker, String msg, Object arg1, Object arg2) {
        if (arg2 instanceof Throwable) {
            this.recordEvent(level, marker, msg, new Object[]{arg1}, (Throwable)arg2);
        } else {
            this.recordEvent(level, marker, msg, new Object[]{arg1, arg2}, null);
        }
    }

    private void recordEventArgArray(Level level, Marker marker, String msg, Object[] args) {
        Throwable throwableCandidate = MessageFormatter.getThrowableCandidate(args);
        if (throwableCandidate != null) {
            Object[] trimmedCopy = MessageFormatter.trimmedCopy(args);
            this.recordEvent(level, marker, msg, trimmedCopy, throwableCandidate);
        } else {
            this.recordEvent(level, marker, msg, args, null);
        }
    }

    private void recordEvent(Level level, Marker marker, String msg, Object[] args, Throwable throwable) {
        SubstituteLoggingEvent loggingEvent = new SubstituteLoggingEvent();
        loggingEvent.setTimeStamp(System.currentTimeMillis());
        loggingEvent.setLevel(level);
        loggingEvent.setLogger(this.logger);
        loggingEvent.setLoggerName(this.name);
        loggingEvent.setMarker(marker);
        loggingEvent.setMessage(msg);
        loggingEvent.setThreadName(Thread.currentThread().getName());
        loggingEvent.setArgumentArray(args);
        loggingEvent.setThrowable(throwable);
        this.eventQueue.add(loggingEvent);
    }
}

