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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.backend.ConversionUtils;
import ninja.leaping.permissionsex.backend.sql.dao.LegacyDao;
import ninja.leaping.permissionsex.backend.sql.dao.LegacyMigration;
import ninja.leaping.permissionsex.backend.sql.dao.SchemaMigration;
import ninja.leaping.permissionsex.util.GuavaCollectors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Schema migrations for the SQL database
 */
public class SchemaMigrations {
    public static final int VERSION_LATEST = 4;

    public static List<SchemaMigration> getMigrations() {
        List<SchemaMigration> migrations = new ArrayList<>();
        migrations.add(0, SchemaMigrations.initialToZero());
        migrations.add(1, SchemaMigrations.zeroToOne());
        migrations.add(2, SchemaMigrations.oneToTwo());
        migrations.add(3, SchemaMigrations.twoToThree());
        migrations.add(VERSION_LATEST, SchemaMigrations.threeToFour());
        return migrations;
    }

    public static SchemaMigration threeToFour() {
        return dao -> {
            // split out segments by weight
            // take out any non-inheritable permissions from those

        };
    }

    // Pre-2.x only needs to support MySQL because tbh nobody uses SQLite
    public static SchemaMigration twoToThree() {
        // The big one
        return dao_doNotUse -> {
            LegacyDao legacy = new LegacyDao(dao_doNotUse);
            // Go from 1.x to a fixed early 2.x schema so that further schema upgrades can all start from the same point
            // This means that nothing but LegacyDao should be referenced
            // We can also assume MySQL is being used since SQLite was used by like nobody
            legacy.renameTable("permissions", "permissions_old");
            legacy.renameTable("permissions_entity", "permissions_entity_old");
            legacy.renameTable("permissions_inheritance", "permissions_inheritance_old");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(SchemaMigration.class.getResourceAsStream("twotothree-migration.sql")))){
                dao_doNotUse.executeStream(reader);
            } catch (IOException e) {
                throw new SQLException(e);
            }

            // Transfer world inheritance
            try (PreparedStatement stmt = legacy.prepareStatement("SELECT id, child, parent FROM {}permissions_inheritance_old WHERE type=2 ORDER BY child, parent, id ASC")) {
                ResultSet rs = stmt.executeQuery();
                try (PreparedStatement insert = legacy.prepareStatement("INSERT INTO {}context_inheritance (child_key, child_value, parent_key, parent_value) VALUES (?, ?, ?, ?)")) {
                    insert.setString(1, "world");
                    insert.setString(3, "world");
                    while (rs.next()) {
                        insert.setString(2, rs.getString(2));
                        insert.setString(4, rs.getString(3));
                        insert.addBatch();
                    }
                    insert.executeBatch();
                }
            }

            Map<String, List<LegacyDao.LegacySubjectRef>> defaultSubjects = new HashMap<>();
            Map<String, List<Map.Entry<LegacyDao.LegacySubjectRef, Integer>>> tempRankLadders = new HashMap<>();

            /*
             * Transfer premissions & options data
             * Our strategy here is to start with permissions, then construct segments by world.
             * At the end of the whole process, all these segments will be converted into modern segments
             */
            try (PreparedStatement select = legacy.prepareStatement("SELECT type, name FROM {}permissions_entity_old")) {
                ResultSet rs = select.executeQuery();
                while (rs.next()) {
                    LegacyDao.LegacySubjectRef ref = legacy.getSubjectRef(LegacyMigration.Type.values()[rs.getInt(1)].name().toLowerCase(), rs.getString(2));
                    LegacyDao.LegacySegment currentSeg = null;
                    Map<String, LegacyDao.LegacySegment> worldSegments = new HashMap<>();
                    try (PreparedStatement selectPermissionsOptions = legacy.prepareStatement("SELECT id, permission, world, value FROM {}permissions_old WHERE type=? AND name=? ORDER BY world, id DESC")) {
                        selectPermissionsOptions.setInt(1, rs.getInt(1));
                        selectPermissionsOptions.setString(2, rs.getString(2));

                        ResultSet perms = selectPermissionsOptions.executeQuery();
                        String rank = null, rankLadder = null;
                        while (perms.next()) {
                            String worldChecked = perms.getString(3);
                            if (worldChecked != null && worldChecked.isEmpty()) {
                                worldChecked = null;
                            }
                            if (currentSeg == null || !Objects.equals(worldChecked, currentSeg.getWorld())) {
                                currentSeg = LegacyDao.LegacySegment.empty(ref, worldChecked);
                                worldSegments.put(currentSeg.getWorld(), currentSeg);
                            }
                            String key = perms.getString(2);
                            final String value = perms.getString(4);
                            if (value == null || value.isEmpty()) {
                                // permission
                                int val = key.startsWith("-") ? -1 : 1;
                                if (val == -1) {
                                    key = key.substring(1);
                                }
                                if (key.equals("*")) {
                                    currentSeg.setPermissionDefault(val);
                                    continue;
                                }
                                key = ConversionUtils.convertLegacyPermission(key);
                                currentSeg.getPermissions().put(key, val);
                            } else { // option -- let's see if it's something that's become another flag rather than an option
                                if (currentSeg.getWorld() == null) { // check for rank ladder
                                    boolean rankEq = key.equals("rank"), rankLadderEq = !rankEq && key.equals("rank-ladder");
                                    if (rankEq || rankLadderEq) {
                                        if (rankEq) {
                                            rank = value;
                                        } else { // then it's the rank ladder
                                            rankLadder = value;
                                        }
                                        if (rank != null && rankLadder != null) {
                                            List<Map.Entry<LegacyDao.LegacySubjectRef, Integer>> ladder = tempRankLadders.computeIfAbsent(rankLadder, ign -> new ArrayList<>());
                                            try {
                                                ladder.add(Maps.immutableEntry(ref, Integer.parseInt(rank)));
                                            } catch (IllegalArgumentException ex) {}
                                            rankLadder = null;
                                            rank = null;
                                        }
                                        continue;
                                    }
                                }

                                if (key.equals("default") && value.equalsIgnoreCase("true")) { // check for default status
                                    defaultSubjects.computeIfAbsent(currentSeg.getWorld(), ign -> new ArrayList<>()).add(ref);
                                } else {
                                    currentSeg.getOptions().put(key, value);
                                }
                            }
                        }

                        if (rank != null) {
                            List<Map.Entry<LegacyDao.LegacySubjectRef, Integer>> ladder = tempRankLadders.computeIfAbsent("default", ign -> new ArrayList<>());
                            try {
                                ladder.add(Maps.immutableEntry(ref, Integer.parseInt(rank)));
                            } catch (IllegalArgumentException ex) {}

                        }
                    }

                    try (PreparedStatement selectInheritance = legacy.prepareStatement(legacy.getSelectParentsQuery())) {
                        selectInheritance.setString(1, rs.getString(2));
                        selectInheritance.setInt(2, rs.getInt(1));

                        ResultSet inheritance = selectInheritance.executeQuery();
                        while (inheritance.next()) {
                            if (currentSeg == null || !Objects.equals(inheritance.getString(3), currentSeg.getWorld())) {
                                String checkWorld = inheritance.getString(3);
                                currentSeg = worldSegments.get(checkWorld);
                                if (currentSeg == null) {
                                    currentSeg = LegacyDao.LegacySegment.empty(ref, checkWorld);
                                    worldSegments.put(currentSeg.getWorld(), currentSeg);
                                }
                            }
                            currentSeg.getParents().add(legacy.getSubjectRef(PermissionsEx.SUBJECTS_GROUP, inheritance.getString(2)));
                        }
                    }
                    legacy.setSegments(worldSegments.values());
                }

            }


            for (Map.Entry<String, List<Map.Entry<LegacyDao.LegacySubjectRef, Integer>>> ent : tempRankLadders.entrySet()) {
                List<LegacyDao.LegacySubjectRef> ladder = ent.getValue().stream()
                        .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                        .map(Map.Entry::getKey)
                        .collect(GuavaCollectors.toImmutableList());
                legacy.setRankLadder(ent.getKey(), ladder);
            }

            if (!defaultSubjects.isEmpty()) {
                LegacyDao.LegacySubjectRef defaultSubj = legacy.getSubjectRef(PermissionsEx.SUBJECTS_DEFAULTS, PermissionsEx.SUBJECTS_USER);
                legacy.setSegments(defaultSubjects.entrySet().stream()
                        .map(ent -> {
                            LegacyDao.LegacySegment seg = LegacyDao.LegacySegment.empty(defaultSubj, ent.getKey());
                            seg.getParents().addAll(ent.getValue());
                            return seg;
                        })
                        .collect(Collectors.toList()));
            }

            legacy.deleteTable("permissions_old");
            legacy.deleteTable("permissions_entity_old");
            legacy.deleteTable("permissions_inheritance_old");
        };
    }

    public static SchemaMigration oneToTwo() {
        return dao -> {
            // Change encoding for all columns to utf8mb4
            // Change collation for all columns to utf8mb4_general_ci
            new LegacyDao(dao).prepareStatement("ALTER TABLE `{permissions}` DROP KEY `unique`, MODIFY COLUMN `permission` TEXT NOT NULL").execute();
        };
    }

    public static SchemaMigration zeroToOne() {
        return dao -> {
            PreparedStatement updateStmt = dao.prepareStatement(new LegacyDao(dao).getInsertOptionQuery());
            ResultSet res = new LegacyDao(dao).prepareStatement("SELECT `name`, `type` FROM `{permissions_entity}` WHERE `default`='1'").executeQuery();
            while (res.next()) {
                updateStmt.setString(1, res.getString(1));
                updateStmt.setInt(2, res.getInt(2));
                updateStmt.setString(3, "default");
                updateStmt.setString(4, "");
                updateStmt.setString(5, "true");
                updateStmt.addBatch();
            }
            updateStmt.executeBatch();

            // Update tables
            dao.prepareStatement("ALTER TABLE `{permissions_entity}` DROP COLUMN `default`").execute();
        };
    }

    public static SchemaMigration initialToZero() {
        return (LegacyMigration) dao -> {
            // TODO: Table modifications not supported in SQLite
            // Prefix/sufix -> options
            LegacyDao legacy = new LegacyDao(dao);
            PreparedStatement updateStmt = legacy.prepareStatement(legacy.getInsertOptionQuery());
            ResultSet res = dao.prepareStatement("SELECT `name`, `type`, `prefix`, `suffix` FROM `{permissions_entity}` WHERE LENGTH(`prefix`)>0 OR LENGTH(`suffix`)>0").executeQuery();
            while (res.next()) {
                String prefix = res.getString("prefix");
                if (!prefix.isEmpty() && !prefix.equals("null")) {
                    updateStmt.setString(1, res.getString(1));
                    updateStmt.setInt(2, res.getInt(2));
                    updateStmt.setString(3, "prefix");
                    updateStmt.setString(4, "");
                    updateStmt.setString(5, prefix);
                    updateStmt.addBatch();
                }
                String suffix = res.getString("suffix");
                if (!suffix.isEmpty() && !suffix.equals("null")) {
                    updateStmt.setString(1, res.getString(1));
                    updateStmt.setInt(2, res.getInt(2));
                    updateStmt.setString(3, "suffix");
                    updateStmt.setString(4, "");
                    updateStmt.setString(5, suffix);
                    updateStmt.addBatch();
                }
            }
            updateStmt.executeBatch();

            // Data type corrections

            // Update tables
            dao.prepareStatement("ALTER TABLE `{permissions_entity}` DROP KEY `name`").execute();
            dao.prepareStatement("ALTER TABLE `{permissions_entity}` DROP COLUMN `prefix`, DROP COLUMN `suffix`").execute();
            dao.prepareStatement("ALTER TABLE `{permissions_entity}` ADD CONSTRAINT UNIQUE KEY `name` (`name`, `type`)").execute();

            dao.prepareStatement("ALTER TABLE `{permissions}` DROP KEY `unique`").execute();
            dao.prepareStatement("ALTER TABLE `{permissions}` ADD CONSTRAINT UNIQUE `unique` (`name`,`permission`,`world`,`type`)").execute();
        };

    }
}
