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

import ninja.leaping.configurate.loader.AtomicFiles;
import ninja.leaping.permissionsex.backend.sql.SqlDao;
import ninja.leaping.permissionsex.backend.sql.SqlSubjectRef;
import ninja.leaping.permissionsex.data.SubjectRef;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DAO covering legacy operations. Some things in here might be mildly weird because
 * they have to handle bridging between two models of working with the database.
 */
public class LegacyDao {
    private static final Pattern TABLE_PATTERN = Pattern.compile("\\{([^}]+)\\}");

    private final SqlDao dao;

    public LegacyDao(SqlDao dao) {
        this.dao = dao;
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

    public PreparedStatement prepareStatement(String query) throws SQLException {
        StringBuffer ret = new StringBuffer();
        Matcher m = TABLE_PATTERN.matcher(query);
        while (m.find()) {
            m.appendReplacement(ret, dao.getDataStore().getTableName(m.group(1), true));
        }
        m.appendTail(ret);

        return dao.prepareStatement(ret.toString());
    }

    public void renameTable(String oldName, String newName) throws SQLException {
        final String expandedOld = dao.getDataStore().getTableName(oldName, true);
        final String expandedNew = dao.getDataStore().getTableName(newName, false);
        try (PreparedStatement stmt = prepareStatement("ALTER TABLE `" + expandedOld + "` RENAME `" + expandedNew + "`")) {
        /*try (PreparedStatement stmt = prepareStatement(dao, dao.getRenameTableQuery())) {
            stmt.setString(1, expandedOld);
            stmt.setString(2, expandedNew);*/
            stmt.executeUpdate();
        }
    }

    public LegacySubjectRef getSubjectRef(String type, String identifier) throws SQLException {
        SqlSubjectRef ref = dao.getOrCreateSubjectRef(type, identifier);
        return new LegacySubjectRef(ref.getId(), ref.getType(), ref.getIdentifier());
    }

    public void setRankLadder(String key, List<LegacySubjectRef> ladder) throws SQLException {
        dao.executeInTransaction(() -> {
            try (PreparedStatement delete = prepareStatement("DELETE FROM {}rank_ladders WHERE `name`=?");
                 PreparedStatement insert = prepareStatement("INSERT INTO {}rank_ladders (`name`, `subject`) VALUES (?, ?)")) {
                delete.setString(1, key);
                delete.executeUpdate();

                if (ladder != null) {
                    insert.setString(1, key);
                    for (LegacySubjectRef ref : ladder) {
                        insert.setInt(2, ref.id);
                        insert.addBatch();
                    }
                    insert.executeBatch();
                }
            }
            return null;
        });
    }

    public void setSegments(Collection<LegacySegment> values) {
        AtomicFiles.createAtomicBufferedWriter()

    }

    public void deleteTable(String table) throws SQLException {
        dao.deleteTable(table);
    }

    public static class LegacySubjectRef {
        private final int id;
        private final String type, identifier;

        LegacySubjectRef(int id, String type, String identifier) {
            this.id = id;
            this.type = type;
            this.identifier = identifier;
        }
    }

    public static class LegacySegment {
        private final LegacySubjectRef ref;
        private final String world;
        private final Map<String, Integer> permissions;
        private final Map<String, String> options;
        private final List<LegacySubjectRef> parents;
        private Integer permissionDefault;

        public static LegacySegment empty(LegacySubjectRef ref, String world) {
            return new LegacySegment(ref, world, new HashMap<>(), new HashMap<>(), new ArrayList<>(), null);
        }

        public LegacySegment(LegacySubjectRef ref, String world, Map<String, Integer> permissions, Map<String, String> options, List<LegacySubjectRef> parents, Integer permissionDefault) {
            this.ref = ref;
            this.world = world;
            this.permissions = permissions;
            this.options = options;
            this.parents = parents;
            this.permissionDefault = permissionDefault;
        }

        public Map<String, Integer> getPermissions() {
            return permissions;
        }

        public Map<String, String> getOptions() {
            return options;
        }

        public List<LegacySubjectRef> getParents() {
            return parents;
        }

        public Integer getPermissionDefault() {
            return permissionDefault;
        }

        public LegacySubjectRef getRef() {
            return this.ref;
        }

        public String getWorld() {
            return world;
        }

        public void setPermissionDefault(int permissionDefault) {
            this.permissionDefault = permissionDefault;
        }
    }
}
