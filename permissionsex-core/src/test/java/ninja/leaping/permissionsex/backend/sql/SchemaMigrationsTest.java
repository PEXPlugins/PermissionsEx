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
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.permissionsex.PermissionsExTest;
import ninja.leaping.permissionsex.backend.DataStore;
import ninja.leaping.permissionsex.config.PermissionsExConfiguration;
import ninja.leaping.permissionsex.exception.PEBKACException;
import ninja.leaping.permissionsex.exception.PermissionsLoadingException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Test of migrations for SQL backend. Migrations prior to 2-to-3 are not tested because they are part of legacy code (that should not be seen beforehand)
 */
@SuppressWarnings("OptionalGetWithoutIsPresent")
@RunWith(Parameterized.class)
public class SchemaMigrationsTest extends PermissionsExTest {
    private static final AtomicInteger COUNTER = new AtomicInteger();
    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object> data() {
        String[] propertyTestDbs = System.getProperty("ninja.leaping.permissionsex.backend.sql.testDatabases", "").split(";", -1);
        if (propertyTestDbs.length == 1 && propertyTestDbs[0].equals("")) {
            propertyTestDbs = new String[0];
        }
        final Object[][] tests = new Object[propertyTestDbs.length][2];
        //tests[propertyTestDbs.length] = new Object[] {"h2", "jdbc:h2:{base}/test.db"};
        //tests[propertyTestDbs.length] = new Object[] {"mysql", "jdbc:mysql://localhost/pextest?user=root"};
        for (int i = 0; i < propertyTestDbs.length; ++i) {
            tests[i] = propertyTestDbs[i].split("!");
        }
        return Arrays.asList((Object[]) tests);
    }

    private final SqlDataStore sqlStore = new SqlDataStore();
    private String jdbcUrl;

    public SchemaMigrationsTest(String databaseName, String jdbcUrl) throws IOException {
        this.jdbcUrl = jdbcUrl;
    }

    @Before
    @Override
    public void setUp() throws IOException, PEBKACException, PermissionsLoadingException, ObjectMappingException {
        File testDir = tempFolder.newFolder();
        jdbcUrl = jdbcUrl.replaceAll("\\{base\\}", testDir.getCanonicalPath());
        sqlStore.setConnectionUrl(jdbcUrl);
        sqlStore.setPrefix("pextest" + COUNTER.getAndIncrement());
        sqlStore.setAutoInitialize(false);
        super.setUp();
    }

    @After
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
    protected PermissionsExConfiguration populate() {
        return new PermissionsExConfiguration() {
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
            public PermissionsExConfiguration reload() throws IOException {
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
                    SqlSubjectRef ref = dao.getSubjectRef(rs.getInt(1)).get();
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
