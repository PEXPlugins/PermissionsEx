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

import ca.stellardrift.permissionsex.datastore.ProtoDataStore;
import ca.stellardrift.permissionsex.test.EmptyTestConfiguration;
import ca.stellardrift.permissionsex.test.PermissionsExTest;
import ca.stellardrift.permissionsex.impl.config.PermissionsExConfiguration;
import ca.stellardrift.permissionsex.exception.PEBKACException;
import ca.stellardrift.permissionsex.exception.PermissionsLoadingException;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
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

    private ProtoDataStore<?> sqlStore;

    @BeforeEach
    @Override
    public void setUp(TestInfo info, @TempDir Path tempFolder) throws IOException, PEBKACException, PermissionsLoadingException {
        Path testDir = tempFolder.resolve("sql");
        String jdbcUrl = this.jdbcUrl.replaceAll("\\{base\\}", testDir.toAbsolutePath().toString().replace('\\', '/'));
        sqlStore = SqlDataStore.create(
            "schema-migrations",
            jdbcUrl,
            "pextest" + COUNTER.getAndIncrement(),
            false
        );
        super.setUp(info, tempFolder);
    }

    @Override
    protected SqlDataStore dataStore() {
        return (SqlDataStore) super.dataStore();
    }

    @AfterEach
    @Override
    public void tearDown() {
        // Delete all created tables;
        try (Connection conn = dataStore().getDataSource().getConnection()) {
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
        return new EmptyTestConfiguration(this.sqlStore);
    }

    @Test
    public void testInitialRun() throws SQLException {
        try (SqlDao dao = dataStore().getDao()) {
            assertEquals(SqlConstants.VERSION_NOT_INITIALIZED, dao.getSchemaVersion());
        }
    }

    @Test
    public void testInitializationSetsLatest() throws SQLException {
        dataStore().initializeTables();
        try (SqlDao dao = dataStore().getDao()) {
            assertEquals(SchemaMigrations.VERSION_LATEST, dao.getSchemaVersion());
        }
    }

    @Test
    public void testTwoToThree() throws SQLException, IOException {
        if (!this.jdbcUrl.startsWith("jdbc:mysql")) {
            System.out.println("Legacy migrations only tested on MySQL");
            return;
        }
        try (final SqlDao dao = dataStore().getDao()) {
            try (final InputStream str = getClass().getResourceAsStream("2to3.old.sql"); BufferedReader reader = new BufferedReader(new InputStreamReader(str, StandardCharsets.UTF_8))) {
                dao.executeStream(reader);
            }
            SchemaMigrations.twoToThree().migrate(dao);
            try (final PreparedStatement stmt = dao.prepareStatement("SELECT id FROM {}subjects")) {
                final ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    final SqlSubjectRef<?> ref = dao.getSubjectRef(rs.getInt(1)).get();
                    final String out = String.format("Subject %s:%s", ref.rawType(), ref.rawIdentifier());
                    System.out.println(out);
                    final char[] outLine = new char[out.length()];
                    Arrays.fill(outLine, '=');
                    System.out.println(outLine);

                    for (SqlSegment seg : dao.getSegments(ref)) {
                        System.out.println(seg);
                    }
                }
            }
        }

    }
}
