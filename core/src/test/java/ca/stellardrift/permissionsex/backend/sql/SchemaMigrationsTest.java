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

package ca.stellardrift.permissionsex.backend.sql;

import ca.stellardrift.permissionsex.PermissionsExTest;
import ca.stellardrift.permissionsex.backend.DataStore;
import ca.stellardrift.permissionsex.config.EmptyPlatformConfiguration;
import ca.stellardrift.permissionsex.config.PermissionsExConfiguration;
import ca.stellardrift.permissionsex.exception.PEBKACException;
import ca.stellardrift.permissionsex.exception.PermissionsLoadingException;
import com.google.common.collect.ImmutableList;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Test of migrations for SQL backend. Migrations prior to 2-to-3 are not tested because they are part of legacy code (that should not be seen beforehand)
 */
@SuppressWarnings("OptionalGetWithoutIsPresent")
public class SchemaMigrationsTest extends PermissionsExTest {
    private static final AtomicInteger COUNTER = new AtomicInteger();
    private final String jdbcUrl;

    /*public class H2SchemaMigrationsTest extends SchemaMigrationsTest {
        public H2SchemaMigrationsTest() {
            super("jdbc:h2:{base}/test.db");
        }
    }

    /*public class MySqlSchemaMigrationsTest extends SchemaMigrationsTest {
        public MySqlSchemaMigrationsTest() {
            super("jdbc:mysql://localhost/pextest?user=root");
        }
    }*/

    public SchemaMigrationsTest() {
        this.jdbcUrl = "jdbc:h2:{base}/test.db";
    }

    /*public SchemaMigrationsTest(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }*/

    private final SqlDataStore sqlStore = new SqlDataStore("schema-migrations");

    @BeforeEach
    public void setUp(TestInfo info, @TempDir Path tempFolder) throws IOException, PEBKACException, PermissionsLoadingException, ObjectMappingException {
        Path testDir = tempFolder.resolve("sql");
        String jdbcUrl = this.jdbcUrl.replaceAll("\\{base\\}", testDir.toAbsolutePath().toString().replace('\\', '/'));
        sqlStore.setConnectionUrl(jdbcUrl);
        sqlStore.setPrefix("pextest" + COUNTER.getAndIncrement());
        sqlStore.setAutoInitialize(false);
        super.setUp(info, tempFolder);
    }

    @AfterEach
    @Override
    public void tearDown() {
        // Delete all created tables;
        try (Connection conn = sqlStore.getDataSource().getConnection()) {
            ResultSet tables = conn.getMetaData().getTables(null, null, "PEXTEST%", null);
            Statement stmt = conn.createStatement();
            while (tables.next()) {
                stmt.addBatch("DROP TABLE " + tables.getString("TABLE_NAME"));
            }
            stmt.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        super.tearDown();
    }

    @Override
    protected PermissionsExConfiguration<?> populate() {
        return new PermissionsExConfiguration<EmptyPlatformConfiguration>() {
            @Override
            public DataStore getDataStore(String name) {
                return null;
            }

            @Override
            public DataStore getDefaultDataStore() {
                return sqlStore;
            }

            @Override
            public boolean isDebugEnabled() {
                return false;
            }

            @Override
            public List<String> getServerTags() {
                return ImmutableList.of();
            }

            @Override
            public void validate() throws PEBKACException {
            }

            @Override
            public EmptyPlatformConfiguration getPlatformConfig() {
                return new EmptyPlatformConfiguration();
            }

            @Override
            public PermissionsExConfiguration<EmptyPlatformConfiguration> reload() throws IOException {
                return this;
            }
        };
    }

    @Test
    public void testInitialRun() throws SQLException {
        try (SqlDao dao = sqlStore.getDao()) {
            assertEquals(SqlConstants.VERSION_NOT_INITIALIZED, dao.getSchemaVersion());
        }
    }

    @Test
    public void testInitializationSetsLatest() throws SQLException {
        sqlStore.initializeTables();
        try (SqlDao dao = sqlStore.getDao()) {
            assertEquals(SchemaMigrations.VERSION_LATEST, dao.getSchemaVersion());
        }
    }

    @Test
    public void testTwoToThree() throws SQLException, IOException {
        if (!this.jdbcUrl.startsWith("jdbc:mysql")) {
            System.out.println("Legacy migrations only tested on MySQL");
            return;
        }
        try (SqlDao dao = sqlStore.getDao()) {
            try (InputStream str = getClass().getResourceAsStream("2to3.old.sql"); BufferedReader reader = new BufferedReader(new InputStreamReader(str, StandardCharsets.UTF_8))) {
                dao.executeStream(reader);
            }
            SchemaMigrations.twoToThree().migrate(dao);
            try (PreparedStatement stmt = dao.prepareStatement("SELECT id FROM {}subjects")) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    SubjectRef ref = dao.getSubjectRef(rs.getInt(1)).get();
                    final String out = String.format("Subject %s:%s", ref.getType(), ref.getIdentifier());
                    System.out.println(out);
                    final char[] outLine = new char[out.length()];
                    Arrays.fill(outLine, '=');;
                    System.out.println(outLine);

                    for (Segment seg : dao.getSegments(ref)) {
                        System.out.println(seg);
                    }
                }
            }
        }

    }
}
