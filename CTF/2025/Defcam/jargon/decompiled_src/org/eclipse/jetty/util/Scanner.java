/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class Scanner
extends AbstractLifeCycle {
    private static final Logger LOG = Log.getLogger(Scanner.class);
    private static int __scannerId = 0;
    private int _scanInterval;
    private int _scanCount = 0;
    private final List<Listener> _listeners = new ArrayList<Listener>();
    private final Map<String, TimeNSize> _prevScan = new HashMap<String, TimeNSize>();
    private final Map<String, TimeNSize> _currentScan = new HashMap<String, TimeNSize>();
    private FilenameFilter _filter;
    private final List<File> _scanDirs = new ArrayList<File>();
    private volatile boolean _running = false;
    private boolean _reportExisting = true;
    private boolean _reportDirs = true;
    private Timer _timer;
    private TimerTask _task;
    private int _scanDepth = 0;
    private final Map<String, Notification> _notifications = new HashMap<String, Notification>();

    public synchronized int getScanInterval() {
        return this._scanInterval;
    }

    public synchronized void setScanInterval(int scanInterval) {
        this._scanInterval = scanInterval;
        this.schedule();
    }

    public void setScanDirs(List<File> dirs) {
        this._scanDirs.clear();
        this._scanDirs.addAll(dirs);
    }

    public synchronized void addScanDir(File dir) {
        this._scanDirs.add(dir);
    }

    public List<File> getScanDirs() {
        return Collections.unmodifiableList(this._scanDirs);
    }

    public void setRecursive(boolean recursive) {
        this._scanDepth = recursive ? -1 : 0;
    }

    public boolean getRecursive() {
        return this._scanDepth == -1;
    }

    public int getScanDepth() {
        return this._scanDepth;
    }

    public void setScanDepth(int scanDepth) {
        this._scanDepth = scanDepth;
    }

    public void setFilenameFilter(FilenameFilter filter) {
        this._filter = filter;
    }

    public FilenameFilter getFilenameFilter() {
        return this._filter;
    }

    public void setReportExistingFilesOnStartup(boolean reportExisting) {
        this._reportExisting = reportExisting;
    }

    public boolean getReportExistingFilesOnStartup() {
        return this._reportExisting;
    }

    public void setReportDirs(boolean dirs) {
        this._reportDirs = dirs;
    }

    public boolean getReportDirs() {
        return this._reportDirs;
    }

    public synchronized void addListener(Listener listener) {
        if (listener == null) {
            return;
        }
        this._listeners.add(listener);
    }

    public synchronized void removeListener(Listener listener) {
        if (listener == null) {
            return;
        }
        this._listeners.remove(listener);
    }

    @Override
    public synchronized void doStart() {
        if (this._running) {
            return;
        }
        this._running = true;
        if (this._reportExisting) {
            this.scan();
            this.scan();
        } else {
            this.scanFiles();
            this._prevScan.putAll(this._currentScan);
        }
        this.schedule();
    }

    public TimerTask newTimerTask() {
        return new TimerTask(){

            @Override
            public void run() {
                Scanner.this.scan();
            }
        };
    }

    public Timer newTimer() {
        return new Timer("Scanner-" + __scannerId++, true);
    }

    public void schedule() {
        if (this._running) {
            if (this._timer != null) {
                this._timer.cancel();
            }
            if (this._task != null) {
                this._task.cancel();
            }
            if (this.getScanInterval() > 0) {
                this._timer = this.newTimer();
                this._task = this.newTimerTask();
                this._timer.schedule(this._task, 1010L * (long)this.getScanInterval(), 1010L * (long)this.getScanInterval());
            }
        }
    }

    @Override
    public synchronized void doStop() {
        if (this._running) {
            this._running = false;
            if (this._timer != null) {
                this._timer.cancel();
            }
            if (this._task != null) {
                this._task.cancel();
            }
            this._task = null;
            this._timer = null;
        }
    }

    public boolean exists(String path) {
        for (File dir : this._scanDirs) {
            if (!new File(dir, path).exists()) continue;
            return true;
        }
        return false;
    }

    public synchronized void scan() {
        this.reportScanStart(++this._scanCount);
        this.scanFiles();
        this.reportDifferences(this._currentScan, this._prevScan);
        this._prevScan.clear();
        this._prevScan.putAll(this._currentScan);
        this.reportScanEnd(this._scanCount);
        for (Listener l : this._listeners) {
            try {
                if (!(l instanceof ScanListener)) continue;
                ((ScanListener)l).scan();
            }
            catch (Exception e) {
                LOG.warn(e);
            }
            catch (Error e) {
                LOG.warn(e);
            }
        }
    }

    public synchronized void scanFiles() {
        if (this._scanDirs == null) {
            return;
        }
        this._currentScan.clear();
        for (File dir : this._scanDirs) {
            if (dir == null || !dir.exists()) continue;
            try {
                this.scanFile(dir.getCanonicalFile(), this._currentScan, 0);
            }
            catch (IOException e) {
                LOG.warn("Error scanning files.", e);
            }
        }
    }

    public synchronized void reportDifferences(Map<String, TimeNSize> currentScan, Map<String, TimeNSize> oldScan) {
        HashSet<String> oldScanKeys = new HashSet<String>(oldScan.keySet());
        for (Map.Entry<String, TimeNSize> entry : currentScan.entrySet()) {
            Notification old;
            String file = entry.getKey();
            if (!oldScanKeys.contains(file)) {
                old = this._notifications.put(file, Notification.ADDED);
                if (old == null) continue;
                switch (old) {
                    case REMOVED: 
                    case CHANGED: {
                        this._notifications.put(file, Notification.CHANGED);
                    }
                }
                continue;
            }
            if (oldScan.get(file).equals(currentScan.get(file)) || (old = this._notifications.put(file, Notification.CHANGED)) == null) continue;
            switch (old) {
                case ADDED: {
                    this._notifications.put(file, Notification.ADDED);
                }
            }
        }
        for (String file : oldScan.keySet()) {
            Notification old;
            if (currentScan.containsKey(file) || (old = this._notifications.put(file, Notification.REMOVED)) == null) continue;
            switch (old) {
                case ADDED: {
                    this._notifications.remove(file);
                }
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("scanned " + this._scanDirs + ": " + this._notifications, new Object[0]);
        }
        ArrayList<String> bulkChanges = new ArrayList<String>();
        Iterator<Map.Entry<String, Notification>> iter = this._notifications.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Notification> entry = iter.next();
            String file = entry.getKey();
            if (oldScan.containsKey(file) ? !oldScan.get(file).equals(currentScan.get(file)) : currentScan.containsKey(file)) continue;
            Notification notification = entry.getValue();
            iter.remove();
            bulkChanges.add(file);
            switch (notification) {
                case ADDED: {
                    this.reportAddition(file);
                    break;
                }
                case CHANGED: {
                    this.reportChange(file);
                    break;
                }
                case REMOVED: {
                    this.reportRemoval(file);
                }
            }
        }
        if (!bulkChanges.isEmpty()) {
            this.reportBulkChanges(bulkChanges);
        }
    }

    private void scanFile(File f, Map<String, TimeNSize> scanInfoMap, int depth) {
        try {
            if (!f.exists()) {
                return;
            }
            if (f.isFile() || depth > 0 && this._reportDirs && f.isDirectory()) {
                if (this._filter == null || this._filter != null && this._filter.accept(f.getParentFile(), f.getName())) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("scan accepted {}", f);
                    }
                    String name = f.getCanonicalPath();
                    scanInfoMap.put(name, new TimeNSize(f.lastModified(), f.isDirectory() ? 0L : f.length()));
                } else if (LOG.isDebugEnabled()) {
                    LOG.debug("scan rejected {}", f);
                }
            }
            if (f.isDirectory() && (depth < this._scanDepth || this._scanDepth == -1 || this._scanDirs.contains(f))) {
                File[] files = f.listFiles();
                if (files != null) {
                    for (int i = 0; i < files.length; ++i) {
                        this.scanFile(files[i], scanInfoMap, depth + 1);
                    }
                } else {
                    LOG.warn("Error listing files in directory {}", f);
                }
            }
        }
        catch (IOException e) {
            LOG.warn("Error scanning watched files", e);
        }
    }

    private void warn(Object listener, String filename, Throwable th) {
        LOG.warn(listener + " failed on '" + filename, th);
    }

    private void reportAddition(String filename) {
        for (Listener l : this._listeners) {
            try {
                if (!(l instanceof DiscreteListener)) continue;
                ((DiscreteListener)l).fileAdded(filename);
            }
            catch (Exception e) {
                this.warn(l, filename, e);
            }
            catch (Error e) {
                this.warn(l, filename, e);
            }
        }
    }

    private void reportRemoval(String filename) {
        for (Listener l : this._listeners) {
            try {
                if (!(l instanceof DiscreteListener)) continue;
                ((DiscreteListener)l).fileRemoved(filename);
            }
            catch (Exception e) {
                this.warn(l, filename, e);
            }
            catch (Error e) {
                this.warn(l, filename, e);
            }
        }
    }

    private void reportChange(String filename) {
        for (Listener l : this._listeners) {
            try {
                if (!(l instanceof DiscreteListener)) continue;
                ((DiscreteListener)l).fileChanged(filename);
            }
            catch (Exception e) {
                this.warn(l, filename, e);
            }
            catch (Error e) {
                this.warn(l, filename, e);
            }
        }
    }

    private void reportBulkChanges(List<String> filenames) {
        for (Listener l : this._listeners) {
            try {
                if (!(l instanceof BulkListener)) continue;
                ((BulkListener)l).filesChanged(filenames);
            }
            catch (Exception e) {
                this.warn(l, filenames.toString(), e);
            }
            catch (Error e) {
                this.warn(l, filenames.toString(), e);
            }
        }
    }

    private void reportScanStart(int cycle) {
        for (Listener listener : this._listeners) {
            try {
                if (!(listener instanceof ScanCycleListener)) continue;
                ((ScanCycleListener)listener).scanStarted(cycle);
            }
            catch (Exception e) {
                LOG.warn(listener + " failed on scan start for cycle " + cycle, e);
            }
        }
    }

    private void reportScanEnd(int cycle) {
        for (Listener listener : this._listeners) {
            try {
                if (!(listener instanceof ScanCycleListener)) continue;
                ((ScanCycleListener)listener).scanEnded(cycle);
            }
            catch (Exception e) {
                LOG.warn(listener + " failed on scan end for cycle " + cycle, e);
            }
        }
    }

    public static interface ScanCycleListener
    extends Listener {
        public void scanStarted(int var1) throws Exception;

        public void scanEnded(int var1) throws Exception;
    }

    public static interface BulkListener
    extends Listener {
        public void filesChanged(List<String> var1) throws Exception;
    }

    public static interface DiscreteListener
    extends Listener {
        public void fileChanged(String var1) throws Exception;

        public void fileAdded(String var1) throws Exception;

        public void fileRemoved(String var1) throws Exception;
    }

    public static interface ScanListener
    extends Listener {
        public void scan();
    }

    public static interface Listener {
    }

    static class TimeNSize {
        final long _lastModified;
        final long _size;

        public TimeNSize(long lastModified, long size) {
            this._lastModified = lastModified;
            this._size = size;
        }

        public int hashCode() {
            return (int)this._lastModified ^ (int)this._size;
        }

        public boolean equals(Object o) {
            if (o instanceof TimeNSize) {
                TimeNSize tns = (TimeNSize)o;
                return tns._lastModified == this._lastModified && tns._size == this._size;
            }
            return false;
        }

        public String toString() {
            return "[lm=" + this._lastModified + ",s=" + this._size + "]";
        }
    }

    public static enum Notification {
        ADDED,
        CHANGED,
        REMOVED;

    }
}

