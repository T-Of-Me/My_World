/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class PathWatcher
extends AbstractLifeCycle
implements Runnable {
    private static final boolean IS_WINDOWS;
    private static final Logger LOG;
    private static final Logger NOISY_LOG;
    private static final WatchEvent.Kind<?>[] WATCH_EVENT_KINDS;
    private WatchService watchService;
    private WatchEvent.Modifier[] watchModifiers;
    private boolean nativeWatchService;
    private Map<WatchKey, Config> keys = new HashMap<WatchKey, Config>();
    private List<EventListener> listeners = new CopyOnWriteArrayList<EventListener>();
    private List<Config> configs = new ArrayList<Config>();
    private long updateQuietTimeDuration = 1000L;
    private TimeUnit updateQuietTimeUnit = TimeUnit.MILLISECONDS;
    private Thread thread;
    private boolean _notifyExistingOnStart = true;
    private Map<Path, PathPendingEvents> pendingEvents = new LinkedHashMap<Path, PathPendingEvents>();

    protected static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return event;
    }

    public void watch(Path file) {
        Path abs = file;
        if (!abs.isAbsolute()) {
            abs = file.toAbsolutePath();
        }
        Config config = null;
        Path parent = abs.getParent();
        for (Config c : this.configs) {
            if (!c.getPath().equals(parent)) continue;
            config = c;
            break;
        }
        if (config == null) {
            config = new Config(abs.getParent());
            config.addIncludeGlobRelative("");
            config.addIncludeGlobRelative(file.getFileName().toString());
            this.watch(config);
        } else {
            config.addIncludeGlobRelative(file.getFileName().toString());
        }
    }

    public void watch(Config config) {
        this.configs.add(config);
    }

    protected void prepareConfig(Config baseDir) throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Watching directory {}", baseDir);
        }
        Files.walkFileTree(baseDir.getPath(), new DepthLimitedFileVisitor(this, baseDir));
    }

    public void addListener(EventListener listener) {
        this.listeners.add(listener);
    }

    private void appendConfigId(StringBuilder s) {
        ArrayList<Path> dirs = new ArrayList<Path>();
        for (Config config : this.keys.values()) {
            dirs.add(config.dir);
        }
        Collections.sort(dirs);
        s.append("[");
        if (dirs.size() > 0) {
            s.append(dirs.get(0));
            if (dirs.size() > 1) {
                s.append(" (+").append(dirs.size() - 1).append(")");
            }
        } else {
            s.append("<null>");
        }
        s.append("]");
    }

    @Override
    protected void doStart() throws Exception {
        this.createWatchService();
        this.setUpdateQuietTime(this.getUpdateQuietTimeMillis(), TimeUnit.MILLISECONDS);
        for (Config c : this.configs) {
            this.prepareConfig(c);
        }
        StringBuilder threadId = new StringBuilder();
        threadId.append("PathWatcher-Thread");
        this.appendConfigId(threadId);
        this.thread = new Thread((Runnable)this, threadId.toString());
        this.thread.setDaemon(true);
        this.thread.start();
        super.doStart();
    }

    @Override
    protected void doStop() throws Exception {
        if (this.watchService != null) {
            this.watchService.close();
        }
        this.watchService = null;
        this.thread = null;
        this.keys.clear();
        this.pendingEvents.clear();
        super.doStop();
    }

    public void reset() {
        if (!this.isStopped()) {
            throw new IllegalStateException("PathWatcher must be stopped before reset.");
        }
        this.configs.clear();
        this.listeners.clear();
    }

    private void createWatchService() throws IOException {
        this.watchService = FileSystems.getDefault().newWatchService();
        WatchEvent.Modifier[] modifiers = null;
        boolean nativeService = true;
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Class<?> pollingWatchServiceClass = Class.forName("sun.nio.fs.PollingWatchService", false, cl);
            if (pollingWatchServiceClass.isAssignableFrom(this.watchService.getClass())) {
                nativeService = false;
                LOG.info("Using Non-Native Java {}", pollingWatchServiceClass.getName());
                Class<?> c = Class.forName("com.sun.nio.file.SensitivityWatchEventModifier");
                Field f = c.getField("HIGH");
                modifiers = new WatchEvent.Modifier[]{(WatchEvent.Modifier)f.get(c)};
            }
        }
        catch (Throwable t) {
            LOG.ignore(t);
        }
        this.watchModifiers = modifiers;
        this.nativeWatchService = nativeService;
    }

    protected boolean isNotifiable() {
        return this.isStarted() || !this.isStarted() && this.isNotifyExistingOnStart();
    }

    public Iterator<EventListener> getListeners() {
        return this.listeners.iterator();
    }

    public long getUpdateQuietTimeMillis() {
        return TimeUnit.MILLISECONDS.convert(this.updateQuietTimeDuration, this.updateQuietTimeUnit);
    }

    protected void notifyOnPathWatchEvents(List<PathWatchEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        for (EventListener listener : this.listeners) {
            if (listener instanceof EventListListener) {
                try {
                    ((EventListListener)listener).onPathWatchEvents(events);
                }
                catch (Throwable t) {
                    LOG.warn(t);
                }
                continue;
            }
            Listener l = (Listener)listener;
            for (PathWatchEvent event : events) {
                try {
                    l.onPathWatchEvent(event);
                }
                catch (Throwable t) {
                    LOG.warn(t);
                }
            }
        }
    }

    protected void register(Path dir, Config root) throws IOException {
        LOG.debug("Registering watch on {}", dir);
        if (this.watchModifiers != null) {
            WatchKey key = dir.register(this.watchService, WATCH_EVENT_KINDS, this.watchModifiers);
            this.keys.put(key, root.asSubConfig(dir));
        } else {
            WatchKey key = dir.register(this.watchService, WATCH_EVENT_KINDS);
            this.keys.put(key, root.asSubConfig(dir));
        }
    }

    public boolean removeListener(Listener listener) {
        return this.listeners.remove(listener);
    }

    @Override
    public void run() {
        ArrayList<PathWatchEvent> notifiableEvents = new ArrayList<PathWatchEvent>();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Starting java.nio file watching with {}", this.watchService);
        }
        while (this.watchService != null && this.thread == Thread.currentThread()) {
            WatchKey key = null;
            try {
                if (this.pendingEvents.isEmpty()) {
                    if (NOISY_LOG.isDebugEnabled()) {
                        NOISY_LOG.debug("Waiting for take()", new Object[0]);
                    }
                    key = this.watchService.take();
                } else {
                    if (NOISY_LOG.isDebugEnabled()) {
                        NOISY_LOG.debug("Waiting for poll({}, {})", new Object[]{this.updateQuietTimeDuration, this.updateQuietTimeUnit});
                    }
                    if ((key = this.watchService.poll(this.updateQuietTimeDuration, this.updateQuietTimeUnit)) == null) {
                        long now = System.currentTimeMillis();
                        for (Path path : new HashSet<Path>(this.pendingEvents.keySet())) {
                            PathPendingEvents pending = this.pendingEvents.get(path);
                            if (!pending.isQuiet(now, this.updateQuietTimeDuration, this.updateQuietTimeUnit)) continue;
                            for (PathWatchEvent p : pending.getEvents()) {
                                notifiableEvents.add(p);
                            }
                            this.pendingEvents.remove(path);
                        }
                    }
                }
            }
            catch (ClosedWatchServiceException e) {
                return;
            }
            catch (InterruptedException e) {
                if (this.isRunning()) {
                    LOG.warn(e);
                } else {
                    LOG.ignore(e);
                }
                return;
            }
            if (key != null) {
                Config config = this.keys.get(key);
                if (config == null) {
                    if (!LOG.isDebugEnabled()) continue;
                    LOG.debug("WatchKey not recognized: {}", key);
                    continue;
                }
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    WatchEvent<Path> ev = PathWatcher.cast(event);
                    Path name = (Path)ev.context();
                    Path child = config.dir.resolve(name);
                    if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                        if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
                            try {
                                this.prepareConfig(config.asSubConfig(child));
                            }
                            catch (IOException e) {
                                LOG.warn(e);
                            }
                            continue;
                        }
                        if (!config.matches(child)) continue;
                        this.addToPendingList(child, new PathWatchEvent(child, ev));
                        continue;
                    }
                    if (!config.matches(child)) continue;
                    this.addToPendingList(child, new PathWatchEvent(child, ev));
                }
            }
            this.notifyOnPathWatchEvents(notifiableEvents);
            notifiableEvents.clear();
            if (key == null || key.reset()) continue;
            this.keys.remove(key);
            if (!this.keys.isEmpty()) continue;
            return;
        }
    }

    public void addToPendingList(Path path, PathWatchEvent event) {
        PathPendingEvents pending = this.pendingEvents.get(path);
        if (pending == null) {
            this.pendingEvents.put(path, new PathPendingEvents(path, event));
        } else {
            pending.addEvent(event);
        }
    }

    public void setNotifyExistingOnStart(boolean notify) {
        this._notifyExistingOnStart = notify;
    }

    public boolean isNotifyExistingOnStart() {
        return this._notifyExistingOnStart;
    }

    public void setUpdateQuietTime(long duration, TimeUnit unit) {
        long desiredMillis = unit.toMillis(duration);
        if (this.watchService != null && !this.nativeWatchService && desiredMillis < 5000L) {
            LOG.warn("Quiet Time is too low for non-native WatchService [{}]: {} < 5000 ms (defaulting to 5000 ms)", this.watchService.getClass().getName(), desiredMillis);
            this.updateQuietTimeDuration = 5000L;
            this.updateQuietTimeUnit = TimeUnit.MILLISECONDS;
            return;
        }
        if (IS_WINDOWS && desiredMillis < 1000L) {
            LOG.warn("Quiet Time is too low for Microsoft Windows: {} < 1000 ms (defaulting to 1000 ms)", desiredMillis);
            this.updateQuietTimeDuration = 1000L;
            this.updateQuietTimeUnit = TimeUnit.MILLISECONDS;
            return;
        }
        this.updateQuietTimeDuration = duration;
        this.updateQuietTimeUnit = unit;
    }

    public String toString() {
        StringBuilder s = new StringBuilder(this.getClass().getName());
        this.appendConfigId(s);
        return s.toString();
    }

    static {
        String os = System.getProperty("os.name");
        if (os == null) {
            IS_WINDOWS = false;
        } else {
            String osl = os.toLowerCase(Locale.ENGLISH);
            IS_WINDOWS = osl.contains("windows");
        }
        LOG = Log.getLogger(PathWatcher.class);
        NOISY_LOG = Log.getLogger(PathWatcher.class.getName() + ".Noisy");
        WATCH_EVENT_KINDS = new WatchEvent.Kind[]{StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY};
    }

    public static enum PathWatchEventType {
        ADDED,
        DELETED,
        MODIFIED,
        UNKNOWN;

    }

    public static class PathPendingEvents {
        private Path _path;
        private List<PathWatchEvent> _events;
        private long _timestamp;
        private long _lastFileSize = -1L;

        public PathPendingEvents(Path path) {
            this._path = path;
        }

        public PathPendingEvents(Path path, PathWatchEvent event) {
            this(path);
            this.addEvent(event);
        }

        public void addEvent(PathWatchEvent event) {
            long now;
            this._timestamp = now = System.currentTimeMillis();
            if (this._events == null) {
                this._events = new ArrayList<PathWatchEvent>();
                this._events.add(event);
            } else {
                PathWatchEvent existingType = null;
                for (PathWatchEvent e : this._events) {
                    if (e.getType() != event.getType()) continue;
                    existingType = e;
                    break;
                }
                if (existingType == null) {
                    this._events.add(event);
                } else {
                    existingType.incrementCount(event.getCount());
                }
            }
        }

        public List<PathWatchEvent> getEvents() {
            return this._events;
        }

        public long getTimestamp() {
            return this._timestamp;
        }

        public boolean isQuiet(long now, long expiredDuration, TimeUnit expiredUnit) {
            long pastdue = this._timestamp + expiredUnit.toMillis(expiredDuration);
            this._timestamp = now;
            long fileSize = this._path.toFile().length();
            boolean fileSizeChanged = this._lastFileSize != fileSize;
            this._lastFileSize = fileSize;
            return now > pastdue && !fileSizeChanged;
        }
    }

    public static class PathWatchEvent {
        private final Path path;
        private final PathWatchEventType type;
        private int count = 0;

        public PathWatchEvent(Path path, PathWatchEventType type) {
            this.path = path;
            this.count = 1;
            this.type = type;
        }

        public PathWatchEvent(Path path, WatchEvent<Path> event) {
            this.path = path;
            this.count = event.count();
            this.type = event.kind() == StandardWatchEventKinds.ENTRY_CREATE ? PathWatchEventType.ADDED : (event.kind() == StandardWatchEventKinds.ENTRY_DELETE ? PathWatchEventType.DELETED : (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY ? PathWatchEventType.MODIFIED : PathWatchEventType.UNKNOWN));
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (this.getClass() != obj.getClass()) {
                return false;
            }
            PathWatchEvent other = (PathWatchEvent)obj;
            if (this.path == null ? other.path != null : !this.path.equals(other.path)) {
                return false;
            }
            return this.type == other.type;
        }

        public Path getPath() {
            return this.path;
        }

        public PathWatchEventType getType() {
            return this.type;
        }

        public void incrementCount(int num) {
            this.count += num;
        }

        public int getCount() {
            return this.count;
        }

        public int hashCode() {
            int prime = 31;
            int result = 1;
            result = 31 * result + (this.path == null ? 0 : this.path.hashCode());
            result = 31 * result + (this.type == null ? 0 : this.type.hashCode());
            return result;
        }

        public String toString() {
            return String.format("PathWatchEvent[%s|%s]", new Object[]{this.type, this.path});
        }
    }

    public static interface EventListListener
    extends EventListener {
        public void onPathWatchEvents(List<PathWatchEvent> var1);
    }

    public static interface Listener
    extends EventListener {
        public void onPathWatchEvent(PathWatchEvent var1);
    }

    public static class DepthLimitedFileVisitor
    extends SimpleFileVisitor<Path> {
        private Config base;
        private PathWatcher watcher;

        public DepthLimitedFileVisitor(PathWatcher watcher, Config base) {
            this.base = base;
            this.watcher = watcher;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            if (!this.base.isExcluded(dir)) {
                if (this.base.isIncluded(dir) && this.watcher.isNotifiable()) {
                    PathWatchEvent event = new PathWatchEvent(dir, PathWatchEventType.ADDED);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Pending {}", event);
                    }
                    this.watcher.addToPendingList(dir, event);
                }
                if (this.base.getPath().equals(dir) && (this.base.isRecurseDepthUnlimited() || this.base.getRecurseDepth() >= 0) || this.base.shouldRecurseDirectory(dir)) {
                    this.watcher.register(dir, this.base);
                }
            }
            if (this.base.getPath().equals(dir) && (this.base.isRecurseDepthUnlimited() || this.base.getRecurseDepth() >= 0) || this.base.shouldRecurseDirectory(dir)) {
                return FileVisitResult.CONTINUE;
            }
            return FileVisitResult.SKIP_SUBTREE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (this.base.matches(file) && this.watcher.isNotifiable()) {
                PathWatchEvent event = new PathWatchEvent(file, PathWatchEventType.ADDED);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Pending {}", event);
                }
                this.watcher.addToPendingList(file, event);
            }
            return FileVisitResult.CONTINUE;
        }
    }

    public static class Config {
        public static final int UNLIMITED_DEPTH = -9999;
        private static final String PATTERN_SEP;
        protected final Path dir;
        protected int recurseDepth = 0;
        protected List<PathMatcher> includes;
        protected List<PathMatcher> excludes;
        protected boolean excludeHidden = false;

        public Config(Path path) {
            this.dir = path;
            this.includes = new ArrayList<PathMatcher>();
            this.excludes = new ArrayList<PathMatcher>();
        }

        public void addExclude(PathMatcher matcher) {
            this.excludes.add(matcher);
        }

        public void addExclude(String syntaxAndPattern) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Adding exclude: [{}]", syntaxAndPattern);
            }
            this.addExclude(this.dir.getFileSystem().getPathMatcher(syntaxAndPattern));
        }

        public void addExcludeGlobRelative(String pattern) {
            this.addExclude(this.toGlobPattern(this.dir, pattern));
        }

        public void addExcludeHidden() {
            if (!this.excludeHidden) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Adding hidden files and directories to exclusions", new Object[0]);
                }
                this.excludeHidden = true;
                this.addExclude("regex:^.*" + PATTERN_SEP + "\\..*$");
                this.addExclude("regex:^.*" + PATTERN_SEP + "\\..*" + PATTERN_SEP + ".*$");
            }
        }

        public void addExcludes(List<String> syntaxAndPatterns) {
            for (String syntaxAndPattern : syntaxAndPatterns) {
                this.addExclude(syntaxAndPattern);
            }
        }

        public void addInclude(PathMatcher matcher) {
            this.includes.add(matcher);
        }

        public void addInclude(String syntaxAndPattern) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Adding include: [{}]", syntaxAndPattern);
            }
            this.addInclude(this.dir.getFileSystem().getPathMatcher(syntaxAndPattern));
        }

        public void addIncludeGlobRelative(String pattern) {
            this.addInclude(this.toGlobPattern(this.dir, pattern));
        }

        public void addIncludes(List<String> syntaxAndPatterns) {
            for (String syntaxAndPattern : syntaxAndPatterns) {
                this.addInclude(syntaxAndPattern);
            }
        }

        public Config asSubConfig(Path dir) {
            Config subconfig = new Config(dir);
            subconfig.includes = this.includes;
            subconfig.excludes = this.excludes;
            subconfig.recurseDepth = dir == this.dir ? this.recurseDepth : (this.recurseDepth == -9999 ? -9999 : this.recurseDepth - (dir.getNameCount() - this.dir.getNameCount()));
            return subconfig;
        }

        public int getRecurseDepth() {
            return this.recurseDepth;
        }

        public boolean isRecurseDepthUnlimited() {
            return this.recurseDepth == -9999;
        }

        public Path getPath() {
            return this.dir;
        }

        private boolean hasMatch(Path path, List<PathMatcher> matchers) {
            for (PathMatcher matcher : matchers) {
                if (!matcher.matches(path)) continue;
                return true;
            }
            return false;
        }

        public boolean isExcluded(Path dir) throws IOException {
            if (this.excludeHidden && Files.isHidden(dir)) {
                if (NOISY_LOG.isDebugEnabled()) {
                    NOISY_LOG.debug("isExcluded [Hidden] on {}", dir);
                }
                return true;
            }
            if (this.excludes.isEmpty()) {
                return false;
            }
            boolean matched = this.hasMatch(dir, this.excludes);
            if (NOISY_LOG.isDebugEnabled()) {
                NOISY_LOG.debug("isExcluded [{}] on {}", matched, dir);
            }
            return matched;
        }

        public boolean isIncluded(Path dir) {
            if (this.includes.isEmpty()) {
                if (NOISY_LOG.isDebugEnabled()) {
                    NOISY_LOG.debug("isIncluded [All] on {}", dir);
                }
                return true;
            }
            boolean matched = this.hasMatch(dir, this.includes);
            if (NOISY_LOG.isDebugEnabled()) {
                NOISY_LOG.debug("isIncluded [{}] on {}", matched, dir);
            }
            return matched;
        }

        public boolean matches(Path path) {
            try {
                return !this.isExcluded(path) && this.isIncluded(path);
            }
            catch (IOException e) {
                LOG.warn("Unable to match path: " + path, e);
                return false;
            }
        }

        public void setRecurseDepth(int depth) {
            this.recurseDepth = depth;
        }

        public boolean shouldRecurseDirectory(Path child) {
            if (!child.startsWith(this.dir)) {
                return false;
            }
            if (this.isRecurseDepthUnlimited()) {
                return true;
            }
            int childDepth = this.dir.relativize(child).getNameCount();
            return childDepth <= this.recurseDepth;
        }

        private String toGlobPattern(Path path, String subPattern) {
            StringBuilder s = new StringBuilder();
            s.append("glob:");
            boolean needDelim = false;
            Path root = path.getRoot();
            if (root != null) {
                if (NOISY_LOG.isDebugEnabled()) {
                    NOISY_LOG.debug("Path: {} -> Root: {}", path, root);
                }
                for (Object c : (Object)root.toString().toCharArray()) {
                    if (c == 92) {
                        s.append(PATTERN_SEP);
                        continue;
                    }
                    s.append((char)c);
                }
            } else {
                needDelim = true;
            }
            for (Path segment : path) {
                if (needDelim) {
                    s.append(PATTERN_SEP);
                }
                s.append(segment);
                needDelim = true;
            }
            if (subPattern != null && subPattern.length() > 0) {
                if (needDelim) {
                    s.append(PATTERN_SEP);
                }
                for (Object c : (Object)subPattern.toCharArray()) {
                    if (c == 47) {
                        s.append(PATTERN_SEP);
                        continue;
                    }
                    s.append((char)c);
                }
            }
            return s.toString();
        }

        public String toString() {
            StringBuilder s = new StringBuilder();
            s.append(this.dir);
            if (this.recurseDepth > 0) {
                s.append(" [depth=").append(this.recurseDepth).append("]");
            }
            return s.toString();
        }

        static {
            String sep = File.separator;
            if (File.separatorChar == '\\') {
                sep = "\\\\";
            }
            PATTERN_SEP = sep;
        }
    }
}

