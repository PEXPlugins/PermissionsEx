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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ListenableFuture;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.db.DatabaseTypeUtils;
import com.j256.ormlite.jdbc.DataSourceConnectionSource;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.permissionsex.backend.AbstractDataStore;
import ninja.leaping.permissionsex.backend.DataStore;
import ninja.leaping.permissionsex.backend.sql.tables.SqlSubject;
import ninja.leaping.permissionsex.data.ImmutableOptionSubjectData;
import ninja.leaping.permissionsex.exception.PermissionsLoadingException;
import ninja.leaping.permissionsex.rank.RankLadder;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import static ninja.leaping.permissionsex.util.Translations._;

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
    protected ImmutableOptionSubjectData getDataInternal(String type, String identifier) throws PermissionsLoadingException {
        try (Connection conn = sql.getConnection()) {
            SqlSubject subject = subjectDao.queryBuilder().where().eq("type", type).eq("identifier", identifier).queryForFirst();
        } catch (SQLException e) {
            //throw new PermissionsLoadingException(_("Unable to get data for user %s:%s"), e, type, identifier);
        }
        return null;
    }

    @Override
    protected ListenableFuture<ImmutableOptionSubjectData> setDataInternal(String type, String identifier, ImmutableOptionSubjectData data) {
        return null;
    }

    @Override
    protected RankLadder getRankLadderInternal(String ladder) {
        return null;
    }

    @Override
    protected ListenableFuture<RankLadder> setRankLadderInternal(String ladder, RankLadder newLadder) {
        return null;
    }

    @Override
    protected void initializeInternal() throws PermissionsLoadingException {
        sql = getManager().getDataSourceForURL(connectionUrl);
        try {
            subjectDao = DaoManager.createDao(new DataSourceConnectionSource(sql, DatabaseTypeUtils.createDatabaseType(connectionUrl)), SqlSubject.class);
        } catch (SQLException e) {
            throw new PermissionsLoadingException(_("Error creating subject DAO"), e);
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
            return Iterables.transform(subjectDao.queryBuilder().selectColumns("identifier").where().eq("type", type).queryRaw(), new Function<String[], String>() {
                @Nullable
                @Override
                public String apply(String[] input) {
                    return input[0];
                }
            });
        } catch (SQLException e) {
            return ImmutableList.of();
        }
    }

    @Override
    public Set<String> getRegisteredTypes() {
        try {
            return ImmutableSet.copyOf(Iterables.transform(subjectDao.queryBuilder().selectColumns("type").distinct().queryRaw(), new Function<String[], String>() {
                @Nullable
                @Override
                public String apply(@Nullable String[] input) {
                    return input[0];
                }
            }));
        } catch (SQLException e) {
            return ImmutableSet.of();
        }
    }

    @Override
    public Iterable<Map.Entry<Map.Entry<String, String>, ImmutableOptionSubjectData>> getAll() {
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
    protected <T> T performBulkOperationSync(final Function<DataStore, T> function) throws Exception {
        return subjectDao.callBatchTasks(new Callable<T>() {
            @Override
            public T call() throws Exception {
                return function.apply(SqlDataStore.this);
            }
        });
    }

}
