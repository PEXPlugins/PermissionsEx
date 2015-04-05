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
import com.google.common.util.concurrent.ListenableFuture;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.permissionsex.backend.AbstractDataStore;
import ninja.leaping.permissionsex.backend.DataStore;
import ninja.leaping.permissionsex.data.ImmutableOptionSubjectData;
import ninja.leaping.permissionsex.exception.PermissionsLoadingException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * DataSource for SQL data
 */
public class SqlDataStore extends AbstractDataStore {
    public static final Factory FACTORY = new Factory("sql", SqlDataStore.class);

    protected SqlDataStore() {
        super(FACTORY);
    }

    @Setting
    private String connectionUrl;

    private DataSource sql;

    @Override
    protected ImmutableOptionSubjectData getDataInternal(String type, String identifier) throws PermissionsLoadingException {
        try (Connection conn = sql.getConnection()) {

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
    protected void initializeInternal() throws PermissionsLoadingException {
        sql = getManager().getDataSourceForURL(connectionUrl);

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
        return null;
    }

    @Override
    public Iterable<String> getRegisteredTypes() {
        return null;
    }

    @Override
    public Iterable<Map.Entry<Map.Entry<String, String>, ImmutableOptionSubjectData>> getAll() {
        return null;
    }

    @Override
    protected <T> T performBulkOperationSync(Function<DataStore, T> function) throws Exception {
        return function.apply(this);
    }

}
