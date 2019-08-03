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
package ninja.leaping.permissionsex.backend.sql.dao;

import ninja.leaping.permissionsex.backend.sql.SqlDao;
import ninja.leaping.permissionsex.backend.sql.SqlDataStore;

import java.sql.SQLException;

public class H2SqlDao extends SqlDao {

    public H2SqlDao(SqlDataStore ds) throws SQLException {
        super(ds);
    }

    @Override
    protected String getInsertGlobalParameterQueryUpdating() {
        return "MERGE INTO {}global (`key`, `value`) KEY(`key`) VALUES (?, ?)";
    }

    @Override
    protected String getInsertOptionUpdatingQuery() {
        return "MERGE INTO {}options (`segment`, `key`, `value`) VALUES (?, ?, ?)";
    }

    @Override
    protected String getInsertPermissionUpdatingQuery() {
        return "MERGE INTO {}permissions (`segment`, `key`, `value`) VALUES (?, ?, ?)";
    }
}
