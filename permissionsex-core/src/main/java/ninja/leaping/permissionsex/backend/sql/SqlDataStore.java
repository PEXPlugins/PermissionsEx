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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.db.DatabaseTypeUtils;
import com.j256.ormlite.jdbc.DataSourceConnectionSource;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.permissionsex.backend.AbstractDataStore;
import ninja.leaping.permissionsex.backend.DataStore;
import ninja.leaping.permissionsex.backend.sql.tables.SqlSubject;
import ninja.leaping.permissionsex.data.ContextInheritance;
import ninja.leaping.permissionsex.data.ImmutableSubjectData;
import ninja.leaping.permissionsex.exception.PermissionsLoadingException;
import ninja.leaping.permissionsex.rank.RankLadder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static ninja.leaping.permissionsex.util.Translations.t;

/**
 * DataSource for SQL data
 */
public final class SqlDataStore extends AbstractDataStore {
    public static final Factory FACTORY = new Factory("sql", SqlDataStore.class);

    protected SqlDataStore() {
        super(FACTORY);
    }

    @Setting("url")
    private String connectionUrl;

    private DataSource sql;
    private Dao<SqlSubject, String> subjectDao;

    @Override
    protected ImmutableSubjectData getDataInternal(String type, String identifier) throws PermissionsLoadingException {
        try (Connection conn = sql.getConnection()) {
            SqlSubject subject = subjectDao.queryBuilder().where().eq("type", type).eq("identifier", identifier).queryForFirst();
        } catch (SQLException e) {
            //throw new PermissionsLoadingException(_("Unable to get data for user %s:%s"), e, type, identifier);
        }
        return null;
    }

    @Override
    protected CompletableFuture<ImmutableSubjectData> setDataInternal(String type, String identifier, ImmutableSubjectData data) {
        return null;
    }

    @Override
    protected RankLadder getRankLadderInternal(String ladder) {
        return null;
    }

    @Override
    protected CompletableFuture<RankLadder> setRankLadderInternal(String ladder, RankLadder newLadder) {
        return null;
    }

    @Override
    protected void initializeInternal() throws PermissionsLoadingException {
        sql = getManager().getDataSourceForURL(connectionUrl);
        try {
            subjectDao = DaoManager.createDao(new DataSourceConnectionSource(sql, DatabaseTypeUtils.createDatabaseType(connectionUrl)), SqlSubject.class);
        } catch (SQLException e) {
            throw new PermissionsLoadingException(t("Error creating subject DAO"), e);
        }

    }

    @Override
    public void close() {

    }

    @Override
    public boolean isRegistered(String type, String identifier) {
        return false;
    }

    @Override
    public Iterable<String> getAllIdentifiers(String type) {
        try {
            return Iterables.transform(subjectDao.queryBuilder().selectColumns("identifier").where().eq("type", type).queryRaw(), input -> input[0]);
        } catch (SQLException e) {
            return ImmutableList.of();
        }
    }

    @Override
    public Set<String> getRegisteredTypes() {
        try {
            return ImmutableSet.copyOf(Iterables.transform(subjectDao.queryBuilder().selectColumns("type").distinct().queryRaw(), input -> input[0]));
        } catch (SQLException e) {
            return ImmutableSet.of();
        }
    }

    @Override
    public Iterable<Map.Entry<Map.Entry<String, String>, ImmutableSubjectData>> getAll() {
        return ImmutableList.of();
    }

    @Override
    public Iterable<String> getAllRankLadders() {
        return null;
    }

    @Override
    public boolean hasRankLadder(String ladder) {
        return false;
    }

    @Override
    public ContextInheritance getContextInheritanceInternal() {
        return null;
    }

    @Override
    public CompletableFuture<ContextInheritance> setContextInheritanceInternal(ContextInheritance inheritance) {
        return null;
    }

    @Override
    protected <T> T performBulkOperationSync(final Function<DataStore, T> function) throws Exception {
        return subjectDao.callBatchTasks(() -> function.apply(SqlDataStore.this));
    }

}
