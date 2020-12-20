/*
 * PermissionsEx
 * Copyright (C) zml and PermissionsEx contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.stellardrift.permissionsex.datastore.sql;

import ca.stellardrift.permissionsex.config.FilePermissionsExConfiguration;
import ca.stellardrift.permissionsex.datastore.DataStoreFactory;
import ca.stellardrift.permissionsex.datastore.StoreProperties;
import ca.stellardrift.permissionsex.datastore.sql.dao.H2SqlDao;
import ca.stellardrift.permissionsex.datastore.sql.dao.MySqlDao;
import ca.stellardrift.permissionsex.datastore.sql.dao.SchemaMigration;
import ca.stellardrift.permissionsex.backend.AbstractDataStore;
import ca.stellardrift.permissionsex.datastore.DataStore;
import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.context.ContextInheritance;
import ca.stellardrift.permissionsex.subject.ImmutableSubjectData;
import ca.stellardrift.permissionsex.exception.PermissionsLoadingException;
import ca.stellardrift.permissionsex.rank.RankLadder;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableMap;
import org.pcollections.HashTreePMap;
import org.pcollections.TreePVector;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;
import org.spongepowered.configurate.util.CheckedFunction;
import org.spongepowered.configurate.util.UnmodifiableCollections;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.regex.Pattern;

import static ca.stellardrift.permissionsex.datastore.sql.SchemaMigrations.VERSION_LATEST;

/**
 * DataSource for SQL data.
 */
public final class SqlDataStore extends AbstractDataStore<SqlDataStore, SqlDataStore.Config> {
    private static final Pattern BRACES_PATTERN = Pattern.compile("\\{\\}");
    private boolean autoInitialize = true;

    SqlDataStore(final StoreProperties<Config> properties) {
        super(properties);
    }

    @AutoService(DataStoreFactory.class)
    public static final class Factory extends AbstractDataStore.Factory<SqlDataStore, Config> {

        static String ID = "sql";

        public Factory() {
            super(ID, Config.class, SqlDataStore::new);
        }
    }

    @ConfigSerializable
    static class Config {
        @Setting("url")
        private String connectionUrl;
        @Setting("prefix")
        private String prefix = "pex";
        private transient String realPrefix;
        @Setting("aliases")
        private Map<String, String> legacyAliases;

        String prefix() {
            if (this.realPrefix == null) {
                if (this.prefix != null && !this.prefix.isEmpty() && !this.prefix.endsWith("_")) {
                    this.realPrefix = this.prefix + "_";
                } else if (this.prefix == null) {
                    this.realPrefix = "";
                } else {
                    this.realPrefix = this.prefix;
                }
            }
            return this.realPrefix;
        }
    }

    // For testing
    static SqlDataStore create(final String ident) {
        try {
            return (SqlDataStore) DataStoreFactory.forType(Factory.ID).create(ident, BasicConfigurationNode.root(FilePermissionsExConfiguration.PEX_OPTIONS));
        } catch (PermissionsLoadingException e) {
            throw new RuntimeException(e);
        }
    }

    private final ConcurrentMap<String, String> queryPrefixCache = new ConcurrentHashMap<>();
    private final ThreadLocal<SqlDao> heldDao = new ThreadLocal<>();
    private final Map<String, CheckedFunction<SqlDataStore, SqlDao, SQLException>> daoImplementations = ImmutableMap.of("mysql", MySqlDao::new, "h2", H2SqlDao::new);
    private CheckedFunction<SqlDataStore, SqlDao, SQLException> daoFactory;
    private DataSource sql;

    SqlDao getDao() throws SQLException {
        SqlDao dao = heldDao.get();
        if (dao != null) {
            return dao;
        }
        return daoFactory.apply(this);
    }

    @Override
    protected boolean initializeInternal() throws PermissionsLoadingException {
        try {
            sql = getManager().dataSourceForUrl(config().connectionUrl);

            // Provide database-implementation specific DAO
            try (Connection conn = sql.getConnection()) {
                final String database = conn.getMetaData().getDatabaseProductName().toLowerCase();
                this.daoFactory = daoImplementations.get(database);
                if (this.daoFactory == null) {
                    throw new PermissionsLoadingException(Messages.DB_IMPL_NOT_SUPPORTED.tr(database));
                }
            }
        } catch (SQLException e) {
            throw new PermissionsLoadingException(Messages.DB_CONNECTION_ERROR.tr(), e);
        }

        /*try (SqlDao conn = getDao()) {
            conn.prepareStatement("ALTER TABLE `{permissions}` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci").execute();
            conn.prepareStatement("ALTER TABLE `{permissions_entity}` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci").execute();
            conn.prepareStatement("ALTER TABLE `{permissions_inheritance}` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci").execute();
        } catch (SQLException e) {
            // Ignore, this MySQL version just doesn't support it.
        }*/


        if (autoInitialize) {
            try {
                return initializeTables();
            } catch (SQLException e) {
                throw new PermissionsLoadingException(Messages.ERROR_INITIALIZE_TABLES.tr(), e);
            }
        } else {
            return true;
        }
    }

    public boolean initializeTables() throws SQLException {
        List<SchemaMigration> migrations = SchemaMigrations.getMigrations();
        // Initialize data, perform migrations
        try (SqlDao dao = getDao()) {
            int initialVersion = dao.getSchemaVersion();
            if (initialVersion == SqlConstants.VERSION_NOT_INITIALIZED) {
                dao.initializeTables();
                dao.setSchemaVersion(VERSION_LATEST);
                return false;
            } else {
                int finalVersion = dao.executeInTransaction(() -> {
                    int highestVersion = initialVersion;
                    for (int i = initialVersion + 1; i < migrations.size(); ++i) {
                        migrations.get(i).migrate(dao);
                        highestVersion = i;
                    }
                    return highestVersion;
                });
                if (initialVersion != finalVersion) {
                    dao.setSchemaVersion(finalVersion);
                    getManager().logger().info(Messages.SCHEMA_UPDATE_SUCCESS.tr(initialVersion, finalVersion));
                }
                return true;
            }
        }
    }

    public void setConnectionUrl(String connectionUrl) {
        config().connectionUrl = connectionUrl;
    }

    DataSource getDataSource() {
        return this.sql;
    }

    public String getTableName(String raw) {
        return getTableName(raw, false);
    }

    public String getTableName(String raw, boolean legacyOnly) {
        if (config().legacyAliases != null && config().legacyAliases.containsKey(raw)) {
            return config().legacyAliases.get(raw);
        } else if (legacyOnly) {
            return raw;
        } else {
            return config().prefix() + raw;
        }
    }

    String insertPrefix(String query) {
        return queryPrefixCache.computeIfAbsent(query, qu -> BRACES_PATTERN.matcher(qu).replaceAll(config().prefix()));
    }

    @Override
    protected CompletableFuture<ImmutableSubjectData> getDataInternal(String type, String identifier) {
        return runAsync(() -> {
            try (SqlDao dao = getDao()) {
                Optional<SubjectRef> ref = dao.getSubjectRef(type, identifier);
                if (ref.isPresent()) {
                    return getDataForRef(dao, ref.get());
                } else {
                    return new SqlSubjectData(SubjectRef.unresolved(type, identifier));
                }
            } catch (SQLException e) {
                throw new PermissionsLoadingException(Messages.ERROR_LOADING.tr(type, identifier));
            }
        });
    }

    private SqlSubjectData getDataForRef(SqlDao dao, SubjectRef ref) throws SQLException {
        List<Segment> segments = dao.getSegments(ref);
        Map<Set<ContextValue<?>>, Segment> contexts = new HashMap<>();
        for (Segment segment : segments) {
            contexts.put(segment.getContexts(), segment);
        }

        return new SqlSubjectData(ref, contexts, null);

    }

    @Override
    protected CompletableFuture<ImmutableSubjectData> setDataInternal(String type, String identifier, ImmutableSubjectData data) {
        // Cases: update data for sql (easy), update of another type (get SQL data, do update)
        SqlSubjectData sqlData;
        if (data instanceof SqlSubjectData) {
            sqlData = (SqlSubjectData) data;
        } else {
            return runAsync(() -> {
                try (SqlDao dao = getDao()) {
                    SubjectRef ref = dao.getOrCreateSubjectRef(type, identifier);
                    SqlSubjectData newData = getDataForRef(dao, ref);
                    newData = (SqlSubjectData) newData.mergeFrom(data);
                    newData.doUpdates(dao);
                    return newData;
                }
            });
        }
        return runAsync(() -> {
            try (SqlDao dao = getDao()) {
                sqlData.doUpdates(dao);
                return sqlData;
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> isRegistered(String type, String identifier) {
        return runAsync(() -> {
            try (SqlDao dao = getDao()) {
                return dao.getSubjectRef(type, identifier).isPresent();
            }
        });
    }

    @Override
    public Set<String> getAllIdentifiers(String type) {
        try (SqlDao dao = getDao()) {
            return dao.getAllIdentifiers(type);
        } catch (SQLException e) {
            return Collections.emptySet();
        }
    }

    @Override
    public Set<String> getRegisteredTypes() {
        try (SqlDao dao = getDao()) {
            return dao.getRegisteredTypes();
        } catch (SQLException e) {
            return Collections.emptySet();
        }

    }

    @Override
    public CompletableFuture<Set<String>> getDefinedContextKeys() {
        return runAsync(() -> {
            try (SqlDao dao = getDao()) {
                return dao.getUsedContextKeys();
            }
        });
    }

    @Override
    public Iterable<Map.Entry<Map.Entry<String, String>, ImmutableSubjectData>> getAll() {
        try (SqlDao dao = getDao()) {
            Set<Map.Entry<Map.Entry<String, String>, ImmutableSubjectData>> builder = new HashSet<>();
            for (SubjectRef ref : dao.getAllSubjectRefs()) {
                builder.add(UnmodifiableCollections.immutableMapEntry(ref, getDataForRef(dao, ref)));
            }
            return Collections.unmodifiableSet(builder);
        } catch (SQLException e) {
            return Collections.emptySet();
        }
    }

    @Override
    protected CompletableFuture<RankLadder> getRankLadderInternal(String ladder) {
        return runAsync(() -> {
            try (SqlDao dao = getDao()) {
                return dao.getRankLadder(ladder);
            }
        });
    }

    @Override
    protected CompletableFuture<RankLadder> setRankLadderInternal(String ladder, RankLadder newLadder) {
        return runAsync(() -> {
            try (SqlDao dao = getDao()) {
                dao.setRankLadder(ladder, newLadder);
                return dao.getRankLadder(ladder);
            }
        });
    }

    @Override
    public Iterable<String> getAllRankLadders() {
        try (SqlDao dao = getDao()) {
            return dao.getAllRankLadderNames();
        } catch (SQLException e) {
            return Collections.emptySet();
        }
    }

    @Override
    public CompletableFuture<Boolean> hasRankLadder(String ladder) {
        return runAsync(() -> {
            try (SqlDao dao = getDao()) {
                return dao.hasEntriesForRankLadder(ladder);
            }
        });
    }

    @Override
    public CompletableFuture<ContextInheritance> getContextInheritanceInternal() {
        return runAsync(() -> {
            try (SqlDao dao = getDao()) {
                return dao.getContextInheritance();
            }
        });
    }

    @Override
    public CompletableFuture<ContextInheritance> setContextInheritanceInternal(ContextInheritance inheritance) {
        return runAsync(() -> {
            try (SqlDao dao = getDao()) {
                SqlContextInheritance sqlInheritance;
                if (inheritance instanceof SqlContextInheritance) {
                    sqlInheritance = (SqlContextInheritance) inheritance;
                } else {
                    sqlInheritance = new SqlContextInheritance(HashTreePMap.from(inheritance.allParents()), TreePVector.singleton((dao_, inheritance_) -> {
                        for (Map.Entry<ContextValue<?>, List<ContextValue<?>>> ent : inheritance_.allParents().entrySet()) {
                            dao_.setContextInheritance(ent.getKey(), ent.getValue());
                        }
                    }));
                }
                sqlInheritance.doUpdate(dao);
            }
            return inheritance;
        });
    }

    @Override
    protected <T> T performBulkOperationSync(final Function<DataStore, T> function) throws Exception {
        SqlDao dao = null;
        try {
            dao = getDao();
            heldDao.set(dao);
            dao.holdOpen++;
            return function.apply(this);
        } finally {
            if (dao != null) {
                if (--dao.holdOpen == 0) {
                    this.heldDao.remove();
                }
                try {
                    dao.close();
                } catch (SQLException ignore) {
                    // Not much we can do
                }
            }
        }
    }

    @Override
    public void close() {
        this.queryPrefixCache.clear();
        this.heldDao.remove();
    }

    public void setPrefix(String prefix) {
        config().prefix = prefix;
        config().realPrefix = null;
    }

    public void setAutoInitialize(boolean autoInitialize) {
        this.autoInitialize = autoInitialize;
    }
}
