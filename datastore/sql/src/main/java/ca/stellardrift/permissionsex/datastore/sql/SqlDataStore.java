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

import ca.stellardrift.permissionsex.datastore.DataStoreContext;
import ca.stellardrift.permissionsex.impl.config.FilePermissionsExConfiguration;
import ca.stellardrift.permissionsex.datastore.DataStoreFactory;
import ca.stellardrift.permissionsex.datastore.ProtoDataStore;
import ca.stellardrift.permissionsex.datastore.sql.dao.H2SqlDao;
import ca.stellardrift.permissionsex.datastore.sql.dao.MySqlDao;
import ca.stellardrift.permissionsex.datastore.sql.dao.SchemaMigration;
import ca.stellardrift.permissionsex.impl.backend.AbstractDataStore;
import ca.stellardrift.permissionsex.datastore.DataStore;
import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.context.ContextInheritance;
import ca.stellardrift.permissionsex.impl.util.PCollections;
import ca.stellardrift.permissionsex.subject.ImmutableSubjectData;
import ca.stellardrift.permissionsex.exception.PermissionsLoadingException;
import ca.stellardrift.permissionsex.rank.RankLadder;
import ca.stellardrift.permissionsex.subject.SubjectRef;
import com.google.auto.service.AutoService;
import com.google.common.annotations.VisibleForTesting;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.pcollections.PMap;
import org.pcollections.PSet;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;
import org.spongepowered.configurate.util.CheckedFunction;
import org.spongepowered.configurate.util.UnmodifiableCollections;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
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
import java.util.stream.Stream;

import static ca.stellardrift.permissionsex.datastore.sql.SchemaMigrations.VERSION_LATEST;

/**
 * DataSource for SQL data.
 */
public final class SqlDataStore extends AbstractDataStore<SqlDataStore, SqlDataStore.Config> {
    private static final Pattern BRACES_PATTERN = Pattern.compile("\\{\\}");
    private boolean autoInitialize = true;

    SqlDataStore(final DataStoreContext context, final ProtoDataStore<Config> properties) {
        super(context, properties);
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

        @Setting
        private @Nullable Boolean autoInitialize = null;

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
    @VisibleForTesting
    static ProtoDataStore<?> create(final String ident, final String jdbcUrl, final String tablePrefix, final boolean autoInitialize) {
        try {
            return DataStoreFactory.forType(Factory.ID)
                .create(ident, BasicConfigurationNode.root(FilePermissionsExConfiguration.PEX_OPTIONS, n -> {
                    n.node("url").raw(jdbcUrl);
                    n.node("prefix").raw(tablePrefix);
                    n.node("auto-initialize").raw(autoInitialize);
                }));
        } catch (PermissionsLoadingException e) {
            throw new RuntimeException(e);
        }
    }

    private final ConcurrentMap<String, String> queryPrefixCache = new ConcurrentHashMap<>();
    private final ThreadLocal<@Nullable SqlDao> heldDao = new ThreadLocal<>();
    private final PMap<String, CheckedFunction<SqlDataStore, SqlDao, SQLException>> daoImplementations = PCollections.<String, CheckedFunction<SqlDataStore, SqlDao, SQLException>>map("mysql", MySqlDao::new)
            .plus("h2", H2SqlDao::new);
    private CheckedFunction<SqlDataStore, SqlDao, SQLException> daoFactory;
    private DataSource sql;

    SqlDao getDao() throws SQLException {
        final @Nullable SqlDao dao = this.heldDao.get();
        if (dao != null) {
            return dao;
        }
        return this.daoFactory.apply(this);
    }

    DataStoreContext ctx() {
        return super.context();
    }

    @Override
    protected void load() throws PermissionsLoadingException {
        try {
            sql = context().dataSourceForUrl(config().connectionUrl);

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


        if ((this.config().autoInitialize == null || this.config().autoInitialize) && autoInitialize) {
            try {
                initializeTables();
            } catch (SQLException e) {
                throw new PermissionsLoadingException(Messages.ERROR_INITIALIZE_TABLES.tr(), e);
            }
        }
    }

    public void initializeTables() throws SQLException {
        List<SchemaMigration> migrations = SchemaMigrations.getMigrations();
        // Initialize data, perform migrations
        try (SqlDao dao = getDao()) {
            int initialVersion = dao.getSchemaVersion();
            if (initialVersion == SqlConstants.VERSION_NOT_INITIALIZED) {
                dao.initializeTables();
                dao.setSchemaVersion(VERSION_LATEST);
                markFirstRun();
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
                    engine().logger().info(Messages.SCHEMA_UPDATE_SUCCESS.tr(initialVersion, finalVersion));
                }
            }
        }
    }

    public void setConnectionUrl(String connectionUrl) {
        config().connectionUrl = connectionUrl;
    }

    DataSource getDataSource() {
        return this.sql;
    }

    String prefix() {
        return this.config().prefix();
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
                final Optional<SqlSubjectRef<?>> ref = dao.getSubjectRef(type, identifier);
                if (ref.isPresent()) {
                    return getDataForRef(dao, ref.get());
                } else {
                    return new SqlSubjectData(SqlSubjectRef.unresolved(this.context(), type, identifier));
                }
            } catch (SQLException e) {
                throw new PermissionsLoadingException(Messages.ERROR_LOADING.tr(type, identifier));
            }
        });
    }

    private SqlSubjectData getDataForRef(SqlDao dao, SqlSubjectRef<?> ref) throws SQLException {
        List<SqlSegment> segments = dao.getSegments(ref);
        PMap<PSet<ContextValue<?>>, SqlSegment> contexts = PCollections.map();
        for (SqlSegment segment : segments) {
            contexts = contexts.plus(segment.contexts(), segment);
        }

        return new SqlSubjectData(ref, contexts, PCollections.vector());

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
                    SqlSubjectRef<?> ref = dao.getOrCreateSubjectRef(type, identifier);
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
    public Stream<String> getAllIdentifiers(String type) {
        try (SqlDao dao = getDao()) {
            return dao.getAllIdentifiers(type).stream(); // TODO
        } catch (SQLException e) {
            return Stream.of();
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
    public Stream<Map.Entry<SubjectRef<?>, ImmutableSubjectData>> getAll() {
        try (SqlDao dao = getDao()) {
            Set<Map.Entry<SubjectRef<?>, ImmutableSubjectData>> builder = new HashSet<>();
            for (SqlSubjectRef<?> ref : dao.getAllSubjectRefs()) {
                builder.add(UnmodifiableCollections.immutableMapEntry(ref, getDataForRef(dao, ref)));
            }
            return builder.stream();
        } catch (SQLException e) {
            return Stream.of();
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
    protected CompletableFuture<RankLadder> setRankLadderInternal(final String ladder, final @Nullable RankLadder newLadder) {
        return runAsync(() -> {
            try (SqlDao dao = getDao()) {
                dao.setRankLadder(ladder, newLadder);
                return dao.getRankLadder(ladder);
            }
        });
    }

    @Override
    public Stream<String> getAllRankLadders() {
        try (SqlDao dao = getDao()) {
            return dao.getAllRankLadderNames().stream();
        } catch (SQLException e) {
            return Stream.of();
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
                    sqlInheritance = new SqlContextInheritance(
                            PCollections.asMap(inheritance.allParents(), (k, v) -> k, (k, v) -> PCollections.asVector(v)),
                            PCollections.vector((dao_, inheritance_) -> {
                                for (Map.Entry<ContextValue<?>, List<ContextValue<?>>> ent : inheritance_.allParents().entrySet()) {
                                    dao_.setContextInheritance(ent.getKey(), PCollections.asVector(ent.getValue()));
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
