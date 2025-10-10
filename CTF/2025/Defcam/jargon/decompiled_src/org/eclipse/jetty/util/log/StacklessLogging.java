/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.log;

import java.util.HashSet;
import java.util.Set;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.log.StdErrLog;

public class StacklessLogging
implements AutoCloseable {
    private final Set<StdErrLog> squelched = new HashSet<StdErrLog>();

    public StacklessLogging(Class<?> ... classesToSquelch) {
        for (Class<?> clazz : classesToSquelch) {
            StdErrLog stdErrLog;
            Logger log = Log.getLogger(clazz);
            if (!(log instanceof StdErrLog) || log.isDebugEnabled() || (stdErrLog = (StdErrLog)log).isHideStacks()) continue;
            stdErrLog.setHideStacks(true);
            this.squelched.add(stdErrLog);
        }
    }

    public StacklessLogging(Logger ... logs) {
        for (Logger log : logs) {
            StdErrLog stdErrLog;
            if (!(log instanceof StdErrLog) || log.isDebugEnabled() || (stdErrLog = (StdErrLog)log).isHideStacks()) continue;
            stdErrLog.setHideStacks(true);
            this.squelched.add(stdErrLog);
        }
    }

    @Override
    public void close() {
        for (StdErrLog log : this.squelched) {
            log.setHideStacks(false);
        }
    }
}

