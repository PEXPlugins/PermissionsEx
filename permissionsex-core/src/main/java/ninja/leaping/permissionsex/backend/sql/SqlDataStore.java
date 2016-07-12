/**
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
package ninja.leaping.permissionsex.backend.sql;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.permissionsex.backend.AbstractDataStore;
import ninja.leaping.permissionsex.backend.ConversionUtils;
import ninja.leaping.permissionsex.backend.DataStore;
import ninja.leaping.permissionsex.backend.sql.dao.H2SqlDao;
import ninja.leaping.permissionsex.backend.sql.dao.MySqlDao;
import ninja.leaping.permissionsex.backend.sql.dao.SchemaMigration;
import ninja.leaping.permissionsex.data.ContextInheritance;
import ninja.leaping.permissionsex.data.ImmutableSubjectData;
import ninja.leaping.permissionsex.exception.PermissionsLoadingException;
import ninja.leaping.permissionsex.rank.RankLadder;
import ninja.leaping.permissionsex.util.ThrowingFunction;
import ninja.leaping.permissionsex.util.Util;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.regex.Pattern;

import static ninja.leaping.permissionsex.backend.sql.SchemaMigrations.VERSION_LATEST;
import static ninja.leaping.permissionsex.util.Translations.t;

/**
 * DataSource for SQL data
 */
public final class SqlDataStore extends AbstractDataStore {
    public static final Factory FACTORY = new Factory("sql", SqlDataStore.class);
    private static final Pattern BRACES_PATTERN = Pattern.compile("\\{\\}");
    private boolean autoInitialize = true;

    protected SqlDataStore() {
        super(FACTORY);
    }

    @Setting("url")
    private String connectionUrl;
    @Setting("prefix")
    private String prefix = "pex";
    private String realPrefix;
    @Setting("aliases")
    private Map<String, String> legacyAliases;

    private final ConcurrentMap<String, String> queryPrefixCache = new ConcurrentHashMap<>();
    private final ThreadLocal<SqlDao> heldDao = new ThreadLocal<>();
    private final Map<String, ThrowingFunction<SqlDataStore, SqlDao, SQLException>> daoImplementations = ImmutableMap.of("mysql", MySqlDao::new, "h2", H2SqlDao::new);
    private ThrowingFunction<SqlDataStore, SqlDao, SQLException> daoFactory;
    private DataSource sql;

    SqlDao getDao() throws SQLException {
        SqlDao dao = heldDao.get();
        if (dao != null) {
            return dao;
        }
        return daoFactory.apply(this);
    }

    @Override
    protected void initializeInternal() throws PermissionsLoadingException {
        try {
            if (this.connectionUrl == null) {
                throw new PermissionsLoadingException(t("The connection URL must be set for this backend"));
            }

            sql = getManager().getDataSourceForURL(connectionUrl);
            if (this.prefix != null && !this.prefix.isEmpty() && !this.prefix.endsWith("_")) {
                this.realPrefix = this.prefix + "_";
            } else if (this.prefix == null) {
                this.realPrefix = "";
            } else {
                this.realPrefix = this.prefix;
            }

            // Provide database-implementation specific DAO
            try (Connection conn = sql.getConnection()) {
                final String database = conn.getMetaData().getDatabaseProductName().toLowerCase();
                this.daoFactory = daoImplementations.get(database);
                if (this.daoFactory == null) {
                    throw new PermissionsLoadingException(t("Database implementation %s is not supported!", database));
                }
            }
        } catch (SQLException e) {
            throw new PermissionsLoadingException(t("Could not connect to SQL database!"), e);
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
                initializeTables();
            } catch (SQLException e) {
                throw new PermissionsLoadingException(t("Error initializing tables in SQL database!"), e);
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
                    getManager().getLogger().info(t("Updated database schema from version %s to %s", initialVersion, finalVersion));
                }
            }
        }
    }

    public void setConnectionUrl(String connectionUrl) {
        this.connectionUrl = connectionUrl;
    }

    DataSource getDataSource() {
        return this.sql;
    }

    public String getTableName(String raw) {
        return getTableName(raw, false);
    }

    public String getTableName(String raw, boolean legacyOnly) {
        if (this.legacyAliases != null && this.legacyAliases.containsKey(raw)) {
            return this.legacyAliases.get(raw);
        } else if (legacyOnly) {
            return raw;
        } else {
            return this.realPrefix + raw;
        }
    }

    String insertPrefix(String query) {
        return queryPrefixCache.computeIfAbsent(query, qu -> BRACES_PATTERN.matcher(qu).replaceAll(this.realPrefix));
    }

    @Override
    protected CompletableFuture<ImmutableSubjectData> getDataInternal(String type, String identifier) {
        return runAsync(() -> {
            try (SqlDao dao = getDao()) {
                Optional<SqlSubjectRef> ref = dao.getSubjectRef(type, identifier);
                if (ref.isPresent()) {
                    return getDataForRef(dao, ref.get());
                } else {
                    return new SqlSubjectData(SqlSubjectRef.unresolved(type, identifier));
                }
            } catch (SQLException e) {
                throw new PermissionsLoadingException(t("Error loading permissions for %s %s", type, identifier), e);
            }
        });
    }

    private SqlSubjectData getDataForRef(SqlDao dao, SqlSubjectRef ref) throws SQLException {
        List<SqlDataSegment> segments = dao.getSegments(ref);
        Map<Set<Entry<String, String>>, SqlDataSegment> contexts = new HashMap<>();
        for (SqlDataSegment segment : segments) {
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
                    SqlSubjectRef ref = dao.getOrCreateSubjectRef(type, identifier);
                    SqlSubjectData newData = getDataForRef(dao, ref);
                    newData = ConversionUtils.transfer(data, newData);
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
            return ImmutableSet.of();
        }
    }

    @Override
    public Set<String> getRegisteredTypes() {
        try (SqlDao dao = getDao()) {
            return dao.getRegisteredTypes();
        } catch (SQLException e) {
            return ImmutableSet.of();
        }

    }

    @Override
    public Iterable<Entry<Entry<String, String>, ImmutableSubjectData>> getAll() {
        try (SqlDao dao = getDao()) {
            ImmutableSet.Builder<Entry<Entry<String, String>, ImmutableSubjectData>> builder = ImmutableSet.builder();
            for (SqlSubjectRef ref : dao.getAllSubjectRefs()) {
                builder.add(Maps.immutableEntry(ref, getDataForRef(dao, ref)));
            }
            return builder.build();
        } catch (SQLException e) {
            return ImmutableSet.of();
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
            return ImmutableSet.of();
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
                    sqlInheritance = new SqlContextInheritance(inheritance.getAllParents(), Util.appendImmutable(null, (dao_, inheritance_) -> {
                        for (Entry<Entry<String, String>, List<Entry<String, String>>> ent : inheritance_.getAllParents().entrySet()) {
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
                    heldDao.set(null);
                }
                try {
                    dao.close();
                } catch (SQLException ignore) {
                }
            }
        }
    }

    @Override
    public void close() {
        this.queryPrefixCache.clear();
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setAutoInitialize(boolean autoInitialize) {
        this.autoInitialize = autoInitialize;
    }
}
