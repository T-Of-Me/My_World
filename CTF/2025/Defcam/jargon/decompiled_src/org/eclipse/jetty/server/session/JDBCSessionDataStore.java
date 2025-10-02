/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.session;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.jetty.server.session.AbstractSessionDataStore;
import org.eclipse.jetty.server.session.DatabaseAdaptor;
import org.eclipse.jetty.server.session.SessionContext;
import org.eclipse.jetty.server.session.SessionData;
import org.eclipse.jetty.server.session.UnreadableSessionDataException;
import org.eclipse.jetty.util.ClassLoadingObjectInputStream;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

@ManagedObject
public class JDBCSessionDataStore
extends AbstractSessionDataStore {
    static final Logger LOG = Log.getLogger("org.eclipse.jetty.server.session");
    protected boolean _initialized = false;
    private DatabaseAdaptor _dbAdaptor;
    private SessionTableSchema _sessionTableSchema;
    private boolean _schemaProvided;

    @Override
    protected void doStart() throws Exception {
        if (this._dbAdaptor == null) {
            throw new IllegalStateException("No jdbc config");
        }
        this.initialize();
        super.doStart();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        this._initialized = false;
        if (!this._schemaProvided) {
            this._sessionTableSchema = null;
        }
    }

    public void initialize() throws Exception {
        if (!this._initialized) {
            this._initialized = true;
            if (this._sessionTableSchema == null) {
                this._sessionTableSchema = new SessionTableSchema();
                this.addBean((Object)this._sessionTableSchema, true);
            }
            this._dbAdaptor.initialize();
            this._sessionTableSchema.setDatabaseAdaptor(this._dbAdaptor);
            this._sessionTableSchema.prepareTables();
        }
    }

    @Override
    public SessionData load(final String id) throws Exception {
        final AtomicReference reference = new AtomicReference();
        final AtomicReference exception = new AtomicReference();
        Runnable r = new Runnable(){

            @Override
            public void run() {
                try (Connection connection = JDBCSessionDataStore.this._dbAdaptor.getConnection();
                     PreparedStatement statement = JDBCSessionDataStore.this._sessionTableSchema.getLoadStatement(connection, id, JDBCSessionDataStore.this._context);
                     ResultSet result = statement.executeQuery();){
                    SessionData data = null;
                    if (result.next()) {
                        data = JDBCSessionDataStore.this.newSessionData(id, result.getLong(JDBCSessionDataStore.this._sessionTableSchema.getCreateTimeColumn()), result.getLong(JDBCSessionDataStore.this._sessionTableSchema.getAccessTimeColumn()), result.getLong(JDBCSessionDataStore.this._sessionTableSchema.getLastAccessTimeColumn()), result.getLong(JDBCSessionDataStore.this._sessionTableSchema.getMaxIntervalColumn()));
                        data.setCookieSet(result.getLong(JDBCSessionDataStore.this._sessionTableSchema.getCookieTimeColumn()));
                        data.setLastNode(result.getString(JDBCSessionDataStore.this._sessionTableSchema.getLastNodeColumn()));
                        data.setLastSaved(result.getLong(JDBCSessionDataStore.this._sessionTableSchema.getLastSavedTimeColumn()));
                        data.setExpiry(result.getLong(JDBCSessionDataStore.this._sessionTableSchema.getExpiryTimeColumn()));
                        data.setContextPath(result.getString(JDBCSessionDataStore.this._sessionTableSchema.getContextPathColumn()));
                        data.setVhost(result.getString(JDBCSessionDataStore.this._sessionTableSchema.getVirtualHostColumn()));
                        try (InputStream is = JDBCSessionDataStore.this._dbAdaptor.getBlobInputStream(result, JDBCSessionDataStore.this._sessionTableSchema.getMapColumn());
                             ClassLoadingObjectInputStream ois = new ClassLoadingObjectInputStream(is);){
                            Object o = ois.readObject();
                            data.putAllAttributes((Map)o);
                        }
                        catch (Exception e) {
                            throw new UnreadableSessionDataException(id, JDBCSessionDataStore.this._context, e);
                        }
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("LOADED session {}", data);
                        }
                    } else if (LOG.isDebugEnabled()) {
                        LOG.debug("No session {}", id);
                    }
                    reference.set(data);
                }
                catch (Exception e) {
                    exception.set(e);
                }
            }
        };
        this._context.run(r);
        if (exception.get() != null) {
            throw (Exception)exception.get();
        }
        return (SessionData)reference.get();
    }

    /*
     * Exception decompiling
     */
    @Override
    public boolean delete(String id) throws Exception {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doJarVersionTypes(Driver.java:257)
         *     at org.benf.cfr.reader.Driver.doJar(Driver.java:139)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:76)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    @Override
    public void doStore(String id, SessionData data, long lastSaveTime) throws Exception {
        if (data == null || id == null) {
            return;
        }
        if (lastSaveTime <= 0L) {
            this.doInsert(id, data);
        } else {
            this.doUpdate(id, data);
        }
    }

    private void doInsert(String id, SessionData data) throws Exception {
        String s = this._sessionTableSchema.getInsertSessionStatementAsString();
        try (Connection connection = this._dbAdaptor.getConnection();){
            connection.setAutoCommit(true);
            try (PreparedStatement statement = connection.prepareStatement(s);){
                statement.setString(1, id);
                statement.setString(2, this._context.getCanonicalContextPath());
                statement.setString(3, this._context.getVhost());
                statement.setString(4, data.getLastNode());
                statement.setLong(5, data.getAccessed());
                statement.setLong(6, data.getLastAccessed());
                statement.setLong(7, data.getCreated());
                statement.setLong(8, data.getCookieSet());
                statement.setLong(9, data.getLastSaved());
                statement.setLong(10, data.getExpiry());
                statement.setLong(11, data.getMaxInactiveMs());
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(data.getAllAttributes());
                oos.flush();
                byte[] bytes = baos.toByteArray();
                ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                statement.setBinaryStream(12, (InputStream)bais, bytes.length);
                statement.executeUpdate();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Inserted session " + data, new Object[0]);
                }
            }
        }
    }

    private void doUpdate(String id, SessionData data) throws Exception {
        try (Connection connection = this._dbAdaptor.getConnection();){
            connection.setAutoCommit(true);
            try (PreparedStatement statement = this._sessionTableSchema.getUpdateSessionStatement(connection, this._context.getCanonicalContextPath());){
                statement.setString(1, data.getLastNode());
                statement.setLong(2, data.getAccessed());
                statement.setLong(3, data.getLastAccessed());
                statement.setLong(4, data.getLastSaved());
                statement.setLong(5, data.getExpiry());
                statement.setLong(6, data.getMaxInactiveMs());
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(data.getAllAttributes());
                oos.flush();
                byte[] bytes = baos.toByteArray();
                ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                statement.setBinaryStream(7, (InputStream)bais, bytes.length);
                if ((this._context.getCanonicalContextPath() == null || "".equals(this._context.getCanonicalContextPath())) && this._dbAdaptor.isEmptyStringNull()) {
                    statement.setString(8, id);
                    statement.setString(9, this._context.getVhost());
                } else {
                    statement.setString(8, id);
                    statement.setString(9, this._context.getCanonicalContextPath());
                    statement.setString(10, this._context.getVhost());
                }
                statement.executeUpdate();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Updated session " + data, new Object[0]);
                }
            }
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    @Override
    public Set<String> doGetExpired(Set<String> candidates) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Getting expired sessions " + System.currentTimeMillis(), new Object[0]);
        }
        long now = System.currentTimeMillis();
        HashSet<String> expiredSessionKeys = new HashSet<String>();
        try (Connection connection = this._dbAdaptor.getConnection();){
            Object object;
            connection.setAutoCommit(true);
            long upperBound = now;
            if (LOG.isDebugEnabled()) {
                LOG.debug("{}- Pass 1: Searching for sessions for context {} managed by me {} and expired before {}", this._context.getCanonicalContextPath(), this._context.getWorkerName(), upperBound);
            }
            PreparedStatement statement = this._sessionTableSchema.getExpiredSessionsStatement(connection, this._context.getCanonicalContextPath(), this._context.getVhost(), upperBound);
            HashSet<String> hashSet = null;
            try {
                ResultSet result2 = statement.executeQuery();
                object = null;
                try {
                    while (result2.next()) {
                        String sessionId2 = result2.getString(this._sessionTableSchema.getIdColumn());
                        long exp = result2.getLong(this._sessionTableSchema.getExpiryTimeColumn());
                        expiredSessionKeys.add(sessionId2);
                        if (!LOG.isDebugEnabled()) continue;
                        LOG.debug(this._context.getCanonicalContextPath() + "- Found expired sessionId=" + sessionId2, new Object[0]);
                    }
                }
                catch (Throwable sessionId2) {
                    object = sessionId2;
                    throw sessionId2;
                }
                finally {
                    if (result2 != null) {
                        if (object != null) {
                            try {
                                result2.close();
                            }
                            catch (Throwable sessionId2) {
                                ((Throwable)object).addSuppressed(sessionId2);
                            }
                        } else {
                            result2.close();
                        }
                    }
                }
            }
            catch (Throwable result2) {
                hashSet = result2;
                throw result2;
            }
            finally {
                if (statement != null) {
                    if (hashSet != null) {
                        try {
                            statement.close();
                        }
                        catch (Throwable result2) {
                            ((Throwable)((Object)hashSet)).addSuppressed(result2);
                        }
                    } else {
                        statement.close();
                    }
                }
            }
            PreparedStatement selectExpiredSessions = this._sessionTableSchema.getAllAncientExpiredSessionsStatement(connection);
            hashSet = null;
            try {
                upperBound = this._lastExpiryCheckTime <= 0L ? now - 3L * (1000L * (long)this._gracePeriodSec) : this._lastExpiryCheckTime - 1000L * (long)this._gracePeriodSec;
                if (LOG.isDebugEnabled()) {
                    LOG.debug("{}- Pass 2: Searching for sessions expired before {}", this._context.getWorkerName(), upperBound);
                }
                selectExpiredSessions.setLong(1, upperBound);
                ResultSet result3 = selectExpiredSessions.executeQuery();
                object = null;
                try {
                    while (result3.next()) {
                        String sessionId3 = result3.getString(this._sessionTableSchema.getIdColumn());
                        String ctxtpth = result3.getString(this._sessionTableSchema.getContextPathColumn());
                        String vh = result3.getString(this._sessionTableSchema.getVirtualHostColumn());
                        expiredSessionKeys.add(sessionId3);
                        if (!LOG.isDebugEnabled()) continue;
                        LOG.debug("{}- Found expired sessionId=", this._context.getWorkerName(), sessionId3);
                    }
                }
                catch (Throwable sessionId3) {
                    object = sessionId3;
                    throw sessionId3;
                }
                finally {
                    if (result3 != null) {
                        if (object != null) {
                            try {
                                result3.close();
                            }
                            catch (Throwable sessionId3) {
                                ((Throwable)object).addSuppressed(sessionId3);
                            }
                        } else {
                            result3.close();
                        }
                    }
                }
            }
            catch (Throwable result3) {
                hashSet = result3;
                throw result3;
            }
            finally {
                if (selectExpiredSessions != null) {
                    if (hashSet != null) {
                        try {
                            selectExpiredSessions.close();
                        }
                        catch (Throwable result3) {
                            ((Throwable)((Object)hashSet)).addSuppressed(result3);
                        }
                    } else {
                        selectExpiredSessions.close();
                    }
                }
            }
            HashSet<String> notExpiredInDB = new HashSet<String>();
            for (String k : candidates) {
                if (expiredSessionKeys.contains(k)) continue;
                notExpiredInDB.add(k);
            }
            if (!notExpiredInDB.isEmpty()) {
                try (PreparedStatement checkSessionExists = this._sessionTableSchema.getCheckSessionExistsStatement(connection, this._context.getCanonicalContextPath());){
                    for (String k : notExpiredInDB) {
                        this._sessionTableSchema.fillCheckSessionExistsStatement(checkSessionExists, k, this._context);
                        try {
                            ResultSet result4 = checkSessionExists.executeQuery();
                            Throwable throwable2 = null;
                            try {
                                if (result4.next()) continue;
                                expiredSessionKeys.add(k);
                            }
                            catch (Throwable throwable3) {
                                throwable2 = throwable3;
                                throw throwable3;
                            }
                            finally {
                                if (result4 == null) continue;
                                if (throwable2 != null) {
                                    try {
                                        result4.close();
                                    }
                                    catch (Throwable throwable4) {
                                        throwable2.addSuppressed(throwable4);
                                    }
                                    continue;
                                }
                                result4.close();
                            }
                        }
                        catch (Exception e) {
                            LOG.warn("Problem checking if potentially expired session {} exists in db", k, e);
                        }
                    }
                }
            }
            hashSet = expiredSessionKeys;
            return hashSet;
        }
        catch (Exception e) {
            LOG.warn(e);
            return expiredSessionKeys;
        }
    }

    public void setDatabaseAdaptor(DatabaseAdaptor dbAdaptor) {
        this.checkStarted();
        this.updateBean(this._dbAdaptor, dbAdaptor);
        this._dbAdaptor = dbAdaptor;
    }

    public void setSessionTableSchema(SessionTableSchema schema) {
        this.checkStarted();
        this.updateBean(this._sessionTableSchema, schema);
        this._sessionTableSchema = schema;
        this._schemaProvided = true;
    }

    @Override
    @ManagedAttribute(value="does this store serialize sessions", readonly=true)
    public boolean isPassivating() {
        return true;
    }

    /*
     * Exception decompiling
     */
    @Override
    public boolean exists(String id) throws Exception {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Tried to end blocks [1[TRYBLOCK], 2[TRYBLOCK]], but top level block is 56[SIMPLE_IF_TAKEN]
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.processEndingBlocks(Op04StructuredStatement.java:435)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:484)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doJarVersionTypes(Driver.java:257)
         *     at org.benf.cfr.reader.Driver.doJar(Driver.java:139)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:76)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    public static class SessionTableSchema {
        public static final int MAX_INTERVAL_NOT_SET = -999;
        protected DatabaseAdaptor _dbAdaptor;
        protected String _schemaName = null;
        protected String _tableName = "JettySessions";
        protected String _idColumn = "sessionId";
        protected String _contextPathColumn = "contextPath";
        protected String _virtualHostColumn = "virtualHost";
        protected String _lastNodeColumn = "lastNode";
        protected String _accessTimeColumn = "accessTime";
        protected String _lastAccessTimeColumn = "lastAccessTime";
        protected String _createTimeColumn = "createTime";
        protected String _cookieTimeColumn = "cookieTime";
        protected String _lastSavedTimeColumn = "lastSavedTime";
        protected String _expiryTimeColumn = "expiryTime";
        protected String _maxIntervalColumn = "maxInterval";
        protected String _mapColumn = "map";

        protected void setDatabaseAdaptor(DatabaseAdaptor dbadaptor) {
            this._dbAdaptor = dbadaptor;
        }

        public String getSchemaName() {
            return this._schemaName;
        }

        public void setSchemaName(String schemaName) {
            this.checkNotNull(schemaName);
            this._schemaName = schemaName;
        }

        public String getTableName() {
            return this._tableName;
        }

        public void setTableName(String tableName) {
            this.checkNotNull(tableName);
            this._tableName = tableName;
        }

        private String getSchemaTableName() {
            return (this.getSchemaName() != null ? this.getSchemaName() + "." : "") + this.getTableName();
        }

        public String getIdColumn() {
            return this._idColumn;
        }

        public void setIdColumn(String idColumn) {
            this.checkNotNull(idColumn);
            this._idColumn = idColumn;
        }

        public String getContextPathColumn() {
            return this._contextPathColumn;
        }

        public void setContextPathColumn(String contextPathColumn) {
            this.checkNotNull(contextPathColumn);
            this._contextPathColumn = contextPathColumn;
        }

        public String getVirtualHostColumn() {
            return this._virtualHostColumn;
        }

        public void setVirtualHostColumn(String virtualHostColumn) {
            this.checkNotNull(virtualHostColumn);
            this._virtualHostColumn = virtualHostColumn;
        }

        public String getLastNodeColumn() {
            return this._lastNodeColumn;
        }

        public void setLastNodeColumn(String lastNodeColumn) {
            this.checkNotNull(lastNodeColumn);
            this._lastNodeColumn = lastNodeColumn;
        }

        public String getAccessTimeColumn() {
            return this._accessTimeColumn;
        }

        public void setAccessTimeColumn(String accessTimeColumn) {
            this.checkNotNull(accessTimeColumn);
            this._accessTimeColumn = accessTimeColumn;
        }

        public String getLastAccessTimeColumn() {
            return this._lastAccessTimeColumn;
        }

        public void setLastAccessTimeColumn(String lastAccessTimeColumn) {
            this.checkNotNull(lastAccessTimeColumn);
            this._lastAccessTimeColumn = lastAccessTimeColumn;
        }

        public String getCreateTimeColumn() {
            return this._createTimeColumn;
        }

        public void setCreateTimeColumn(String createTimeColumn) {
            this.checkNotNull(createTimeColumn);
            this._createTimeColumn = createTimeColumn;
        }

        public String getCookieTimeColumn() {
            return this._cookieTimeColumn;
        }

        public void setCookieTimeColumn(String cookieTimeColumn) {
            this.checkNotNull(cookieTimeColumn);
            this._cookieTimeColumn = cookieTimeColumn;
        }

        public String getLastSavedTimeColumn() {
            return this._lastSavedTimeColumn;
        }

        public void setLastSavedTimeColumn(String lastSavedTimeColumn) {
            this.checkNotNull(lastSavedTimeColumn);
            this._lastSavedTimeColumn = lastSavedTimeColumn;
        }

        public String getExpiryTimeColumn() {
            return this._expiryTimeColumn;
        }

        public void setExpiryTimeColumn(String expiryTimeColumn) {
            this.checkNotNull(expiryTimeColumn);
            this._expiryTimeColumn = expiryTimeColumn;
        }

        public String getMaxIntervalColumn() {
            return this._maxIntervalColumn;
        }

        public void setMaxIntervalColumn(String maxIntervalColumn) {
            this.checkNotNull(maxIntervalColumn);
            this._maxIntervalColumn = maxIntervalColumn;
        }

        public String getMapColumn() {
            return this._mapColumn;
        }

        public void setMapColumn(String mapColumn) {
            this.checkNotNull(mapColumn);
            this._mapColumn = mapColumn;
        }

        public String getCreateStatementAsString() {
            if (this._dbAdaptor == null) {
                throw new IllegalStateException("No DBAdaptor");
            }
            String blobType = this._dbAdaptor.getBlobType();
            String longType = this._dbAdaptor.getLongType();
            return "create table " + this._tableName + " (" + this._idColumn + " varchar(120), " + this._contextPathColumn + " varchar(60), " + this._virtualHostColumn + " varchar(60), " + this._lastNodeColumn + " varchar(60), " + this._accessTimeColumn + " " + longType + ", " + this._lastAccessTimeColumn + " " + longType + ", " + this._createTimeColumn + " " + longType + ", " + this._cookieTimeColumn + " " + longType + ", " + this._lastSavedTimeColumn + " " + longType + ", " + this._expiryTimeColumn + " " + longType + ", " + this._maxIntervalColumn + " " + longType + ", " + this._mapColumn + " " + blobType + ", primary key(" + this._idColumn + ", " + this._contextPathColumn + "," + this._virtualHostColumn + "))";
        }

        public String getCreateIndexOverExpiryStatementAsString(String indexName) {
            return "create index " + indexName + " on " + this.getSchemaTableName() + " (" + this.getExpiryTimeColumn() + ")";
        }

        public String getCreateIndexOverSessionStatementAsString(String indexName) {
            return "create index " + indexName + " on " + this.getSchemaTableName() + " (" + this.getIdColumn() + ", " + this.getContextPathColumn() + ")";
        }

        public String getAlterTableForMaxIntervalAsString() {
            if (this._dbAdaptor == null) {
                throw new IllegalStateException("No DBAdaptor");
            }
            String longType = this._dbAdaptor.getLongType();
            String stem = "alter table " + this.getSchemaTableName() + " add " + this.getMaxIntervalColumn() + " " + longType;
            if (this._dbAdaptor.getDBName().contains("oracle")) {
                return stem + " default " + -999 + " not null";
            }
            return stem + " not null default " + -999;
        }

        private void checkNotNull(String s) {
            if (s == null) {
                throw new IllegalArgumentException(s);
            }
        }

        public String getInsertSessionStatementAsString() {
            return "insert into " + this.getSchemaTableName() + " (" + this.getIdColumn() + ", " + this.getContextPathColumn() + ", " + this.getVirtualHostColumn() + ", " + this.getLastNodeColumn() + ", " + this.getAccessTimeColumn() + ", " + this.getLastAccessTimeColumn() + ", " + this.getCreateTimeColumn() + ", " + this.getCookieTimeColumn() + ", " + this.getLastSavedTimeColumn() + ", " + this.getExpiryTimeColumn() + ", " + this.getMaxIntervalColumn() + ", " + this.getMapColumn() + ")  values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        }

        public PreparedStatement getUpdateSessionStatement(Connection connection, String canonicalContextPath) throws SQLException {
            String s = "update " + this.getSchemaTableName() + " set " + this.getLastNodeColumn() + " = ?, " + this.getAccessTimeColumn() + " = ?, " + this.getLastAccessTimeColumn() + " = ?, " + this.getLastSavedTimeColumn() + " = ?, " + this.getExpiryTimeColumn() + " = ?, " + this.getMaxIntervalColumn() + " = ?, " + this.getMapColumn() + " = ? where ";
            if ((canonicalContextPath == null || "".equals(canonicalContextPath)) && this._dbAdaptor.isEmptyStringNull()) {
                s = s + this.getIdColumn() + " = ? and " + this.getContextPathColumn() + " is null and " + this.getVirtualHostColumn() + " = ?";
                return connection.prepareStatement(s);
            }
            return connection.prepareStatement(s + this.getIdColumn() + " = ? and " + this.getContextPathColumn() + " = ? and " + this.getVirtualHostColumn() + " = ?");
        }

        public PreparedStatement getExpiredSessionsStatement(Connection connection, String canonicalContextPath, String vhost, long expiry) throws SQLException {
            if (this._dbAdaptor == null) {
                throw new IllegalStateException("No DB adaptor");
            }
            if ((canonicalContextPath == null || "".equals(canonicalContextPath)) && this._dbAdaptor.isEmptyStringNull()) {
                PreparedStatement statement = connection.prepareStatement("select " + this.getIdColumn() + ", " + this.getExpiryTimeColumn() + " from " + this.getSchemaTableName() + " where " + this.getContextPathColumn() + " is null and " + this.getVirtualHostColumn() + " = ? and " + this.getExpiryTimeColumn() + " >0 and " + this.getExpiryTimeColumn() + " <= ?");
                statement.setString(1, vhost);
                statement.setLong(2, expiry);
                return statement;
            }
            PreparedStatement statement = connection.prepareStatement("select " + this.getIdColumn() + ", " + this.getExpiryTimeColumn() + " from " + this.getSchemaTableName() + " where " + this.getContextPathColumn() + " = ? and " + this.getVirtualHostColumn() + " = ? and " + this.getExpiryTimeColumn() + " >0 and " + this.getExpiryTimeColumn() + " <= ?");
            statement.setString(1, canonicalContextPath);
            statement.setString(2, vhost);
            statement.setLong(3, expiry);
            return statement;
        }

        public PreparedStatement getMyExpiredSessionsStatement(Connection connection, SessionContext sessionContext, long expiry) throws SQLException {
            if (this._dbAdaptor == null) {
                throw new IllegalStateException("No DB adaptor");
            }
            if ((sessionContext.getCanonicalContextPath() == null || "".equals(sessionContext.getCanonicalContextPath())) && this._dbAdaptor.isEmptyStringNull()) {
                PreparedStatement statement = connection.prepareStatement("select " + this.getIdColumn() + ", " + this.getExpiryTimeColumn() + " from " + this.getSchemaTableName() + " where " + this.getLastNodeColumn() + " = ?  and " + this.getContextPathColumn() + " is null and " + this.getVirtualHostColumn() + " = ? and " + this.getExpiryTimeColumn() + " >0 and " + this.getExpiryTimeColumn() + " <= ?");
                statement.setString(1, sessionContext.getWorkerName());
                statement.setString(2, sessionContext.getVhost());
                statement.setLong(3, expiry);
                return statement;
            }
            PreparedStatement statement = connection.prepareStatement("select " + this.getIdColumn() + ", " + this.getExpiryTimeColumn() + " from " + this.getSchemaTableName() + " where " + this.getLastNodeColumn() + " = ? and " + this.getContextPathColumn() + " = ? and " + this.getVirtualHostColumn() + " = ? and " + this.getExpiryTimeColumn() + " >0 and " + this.getExpiryTimeColumn() + " <= ?");
            statement.setString(1, sessionContext.getWorkerName());
            statement.setString(2, sessionContext.getCanonicalContextPath());
            statement.setString(3, sessionContext.getVhost());
            statement.setLong(4, expiry);
            return statement;
        }

        public PreparedStatement getAllAncientExpiredSessionsStatement(Connection connection) throws SQLException {
            if (this._dbAdaptor == null) {
                throw new IllegalStateException("No DB adaptor");
            }
            PreparedStatement statement = connection.prepareStatement("select " + this.getIdColumn() + ", " + this.getContextPathColumn() + ", " + this.getVirtualHostColumn() + " from " + this.getSchemaTableName() + " where " + this.getExpiryTimeColumn() + " >0 and " + this.getExpiryTimeColumn() + " <= ?");
            return statement;
        }

        public PreparedStatement getCheckSessionExistsStatement(Connection connection, String canonicalContextPath) throws SQLException {
            if (this._dbAdaptor == null) {
                throw new IllegalStateException("No DB adaptor");
            }
            if ((canonicalContextPath == null || "".equals(canonicalContextPath)) && this._dbAdaptor.isEmptyStringNull()) {
                PreparedStatement statement = connection.prepareStatement("select " + this.getIdColumn() + ", " + this.getExpiryTimeColumn() + " from " + this.getSchemaTableName() + " where " + this.getIdColumn() + " = ? and " + this.getContextPathColumn() + " is null and " + this.getVirtualHostColumn() + " = ?");
                return statement;
            }
            PreparedStatement statement = connection.prepareStatement("select " + this.getIdColumn() + ", " + this.getExpiryTimeColumn() + " from " + this.getSchemaTableName() + " where " + this.getIdColumn() + " = ? and " + this.getContextPathColumn() + " = ? and " + this.getVirtualHostColumn() + " = ?");
            return statement;
        }

        public void fillCheckSessionExistsStatement(PreparedStatement statement, String id, SessionContext contextId) throws SQLException {
            statement.clearParameters();
            ParameterMetaData metaData = statement.getParameterMetaData();
            if (metaData.getParameterCount() < 3) {
                statement.setString(1, id);
                statement.setString(2, contextId.getVhost());
            } else {
                statement.setString(1, id);
                statement.setString(2, contextId.getCanonicalContextPath());
                statement.setString(3, contextId.getVhost());
            }
        }

        public PreparedStatement getLoadStatement(Connection connection, String id, SessionContext contextId) throws SQLException {
            if (this._dbAdaptor == null) {
                throw new IllegalStateException("No DB adaptor");
            }
            if ((contextId.getCanonicalContextPath() == null || "".equals(contextId.getCanonicalContextPath())) && this._dbAdaptor.isEmptyStringNull()) {
                PreparedStatement statement = connection.prepareStatement("select * from " + this.getSchemaTableName() + " where " + this.getIdColumn() + " = ? and " + this.getContextPathColumn() + " is null and " + this.getVirtualHostColumn() + " = ?");
                statement.setString(1, id);
                statement.setString(2, contextId.getVhost());
                return statement;
            }
            PreparedStatement statement = connection.prepareStatement("select * from " + this.getSchemaTableName() + " where " + this.getIdColumn() + " = ? and " + this.getContextPathColumn() + " = ? and " + this.getVirtualHostColumn() + " = ?");
            statement.setString(1, id);
            statement.setString(2, contextId.getCanonicalContextPath());
            statement.setString(3, contextId.getVhost());
            return statement;
        }

        public PreparedStatement getUpdateStatement(Connection connection, String id, SessionContext contextId) throws SQLException {
            if (this._dbAdaptor == null) {
                throw new IllegalStateException("No DB adaptor");
            }
            String s = "update " + this.getSchemaTableName() + " set " + this.getLastNodeColumn() + " = ?, " + this.getAccessTimeColumn() + " = ?, " + this.getLastAccessTimeColumn() + " = ?, " + this.getLastSavedTimeColumn() + " = ?, " + this.getExpiryTimeColumn() + " = ?, " + this.getMaxIntervalColumn() + " = ?, " + this.getMapColumn() + " = ? where ";
            if ((contextId.getCanonicalContextPath() == null || "".equals(contextId.getCanonicalContextPath())) && this._dbAdaptor.isEmptyStringNull()) {
                PreparedStatement statement = connection.prepareStatement(s + this.getIdColumn() + " = ? and " + this.getContextPathColumn() + " is null and " + this.getVirtualHostColumn() + " = ?");
                statement.setString(1, id);
                statement.setString(2, contextId.getVhost());
                return statement;
            }
            PreparedStatement statement = connection.prepareStatement(s + this.getIdColumn() + " = ? and " + this.getContextPathColumn() + " = ? and " + this.getVirtualHostColumn() + " = ?");
            statement.setString(1, id);
            statement.setString(2, contextId.getCanonicalContextPath());
            statement.setString(3, contextId.getVhost());
            return statement;
        }

        public PreparedStatement getDeleteStatement(Connection connection, String id, SessionContext contextId) throws Exception {
            if (this._dbAdaptor == null) {
                throw new IllegalStateException("No DB adaptor");
            }
            if ((contextId.getCanonicalContextPath() == null || "".equals(contextId.getCanonicalContextPath())) && this._dbAdaptor.isEmptyStringNull()) {
                PreparedStatement statement = connection.prepareStatement("delete from " + this.getSchemaTableName() + " where " + this.getIdColumn() + " = ? and " + this.getContextPathColumn() + " = ? and " + this.getVirtualHostColumn() + " = ?");
                statement.setString(1, id);
                statement.setString(2, contextId.getVhost());
                return statement;
            }
            PreparedStatement statement = connection.prepareStatement("delete from " + this.getSchemaTableName() + " where " + this.getIdColumn() + " = ? and " + this.getContextPathColumn() + " = ? and " + this.getVirtualHostColumn() + " = ?");
            statement.setString(1, id);
            statement.setString(2, contextId.getCanonicalContextPath());
            statement.setString(3, contextId.getVhost());
            return statement;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        public void prepareTables() throws SQLException {
            try (Connection connection = this._dbAdaptor.getConnection();
                 Statement statement = connection.createStatement();){
                String schemaName;
                String tableName;
                DatabaseMetaData metaData;
                block60: {
                    connection.setAutoCommit(true);
                    metaData = connection.getMetaData();
                    this._dbAdaptor.adaptTo(metaData);
                    tableName = this._dbAdaptor.convertIdentifier(this.getTableName());
                    schemaName = this._dbAdaptor.convertIdentifier(this.getSchemaName());
                    try (ResultSet result = metaData.getTables(null, schemaName, tableName, null);){
                        if (!result.next()) {
                            statement.executeUpdate(this.getCreateStatementAsString());
                            break block60;
                        }
                        ResultSet colResult = null;
                        try {
                            colResult = metaData.getColumns(null, schemaName, tableName, this._dbAdaptor.convertIdentifier(this.getMaxIntervalColumn()));
                        }
                        catch (SQLException s) {
                            LOG.warn("Problem checking if " + this.getTableName() + " table contains " + this.getMaxIntervalColumn() + " column. Ensure table contains column definition: \"" + this.getMaxIntervalColumn() + " long not null default -999\"", new Object[0]);
                            throw s;
                        }
                        try {
                            if (colResult.next()) break block60;
                            try {
                                statement.executeUpdate(this.getAlterTableForMaxIntervalAsString());
                            }
                            catch (SQLException s) {
                                LOG.warn("Problem adding " + this.getMaxIntervalColumn() + " column. Ensure table contains column definition: \"" + this.getMaxIntervalColumn() + " long not null default -999\"", new Object[0]);
                                throw s;
                            }
                        }
                        finally {
                            colResult.close();
                        }
                    }
                }
                String index1 = "idx_" + this.getTableName() + "_expiry";
                String index2 = "idx_" + this.getTableName() + "_session";
                boolean index1Exists = false;
                boolean index2Exists = false;
                try (ResultSet result = metaData.getIndexInfo(null, schemaName, tableName, false, true);){
                    while (result.next()) {
                        String idxName = result.getString("INDEX_NAME");
                        if (index1.equalsIgnoreCase(idxName)) {
                            index1Exists = true;
                            continue;
                        }
                        if (!index2.equalsIgnoreCase(idxName)) continue;
                        index2Exists = true;
                    }
                }
                if (!index1Exists) {
                    statement.executeUpdate(this.getCreateIndexOverExpiryStatementAsString(index1));
                }
                if (!index2Exists) {
                    statement.executeUpdate(this.getCreateIndexOverSessionStatementAsString(index2));
                }
            }
        }

        public String toString() {
            return String.format("%s[%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s]", super.toString(), this._schemaName, this._tableName, this._idColumn, this._contextPathColumn, this._virtualHostColumn, this._cookieTimeColumn, this._createTimeColumn, this._expiryTimeColumn, this._accessTimeColumn, this._lastAccessTimeColumn, this._lastNodeColumn, this._lastSavedTimeColumn, this._maxIntervalColumn);
        }
    }
}

