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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DAO covering legacy operations. Some things in here might be mildly weird because
 * they have to handle bridging between two models of working with the database.
 */
public class LegacyDao {
    public static final LegacyDao INSTANCE = new LegacyDao();
    private static final Pattern TABLE_PATTERN = Pattern.compile("\\{([^}]+)\\}");

    protected LegacyDao() {
    }

    public String getSelectEntitiesQuery() {
        return "SELECT (`name`, `type`) FROM {permissions_entity}";
    }

    public String getSelectOptionsQuery() {
        return "SELECT `permission`, `value`, `world` FROM `{permissions}` WHERE `name` = ? AND `type` = ? AND LENGTH(`value`) > 0";
    }

    public String getSelectOptionQuery() {
        return "SELECT `value` FROM `{permissions}` WHERE `name` = ? AND `type` = ? AND `permission` = ? AND `world` = ? AND LENGTH(`value`) > 0";
    }

    public String getInsertOptionQuery() {
        return "INSERT INTO `{permissions}` (`name`, `type`, `permission`, `world`, `value`) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE `value` = VALUES(`value`)";
    }

    public String getSelectPermissionQuery() {
        return "SELECT `permission`, `world` FROM `{permissions}` WHERE `name` = ? AND `type` = ? AND LENGTH(`value`) = 0 ORDER BY `id` DESC";
    }

    public String getSelectParentsQuery() {
        return "SELECT `id`, `parent`, `world` FROM `{}permissions_inheritance_old` WHERE `child` = ? AND `type` = ? ORDER BY `world`, `id` DESC";
    }

    public PreparedStatement prepareStatement(SqlDao dao, String query) throws SQLException {
        StringBuffer ret = new StringBuffer();
        Matcher m = TABLE_PATTERN.matcher(query);
        while (m.find()) {
            m.appendReplacement(ret, dao.getDataStore().getTableName(m.group(1), true));
        }
        m.appendTail(ret);

        return dao.prepareStatement(ret.toString());
    }

    public boolean hasTable(SqlDao dao, String table) throws SQLException {
        return dao.getConnection().getMetaData().getTables(null, null, dao.getDataStore().getTableName(table, true).toUpperCase(), null).next(); // Upper-case for H2
    }

    public void renameTable(SqlDao dao, String oldName, String newName) throws SQLException {
        final String expandedOld = dao.getDataStore().getTableName(oldName, true);
        final String expandedNew = dao.getDataStore().getTableName(newName, false);
        try (PreparedStatement stmt = prepareStatement(dao, "ALTER TABLE `" + expandedOld + "` RENAME `" + expandedNew + "`")) {
        /*try (PreparedStatement stmt = prepareStatement(dao, dao.getRenameTableQuery())) {
            stmt.setString(1, expandedOld);
            stmt.setString(2, expandedNew);*/
            stmt.executeUpdate();
        }
    }

    public String getOption(SqlDao dao, String name, LegacyMigration.Type type, String world, String option) throws SQLException {
        try (PreparedStatement stmt = dao.prepareStatement(getSelectOptionQuery())) {
            stmt.setString(1, name);
            stmt.setInt(2, type.ordinal());
            stmt.setString(3, option);
            if (world == null) {
                stmt.setNull(4, Types.VARCHAR);
            } else {
                stmt.setString(4, world);
            }

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString(1);
            } else {
                return null;
            }
        }
    }
}
