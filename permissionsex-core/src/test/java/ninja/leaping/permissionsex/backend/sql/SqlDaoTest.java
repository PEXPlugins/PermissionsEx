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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.permissionsex.PermissionsExTest;
import ninja.leaping.permissionsex.backend.DataStore;
import ninja.leaping.permissionsex.config.PermissionsExConfiguration;
import ninja.leaping.permissionsex.exception.PEBKACException;
import ninja.leaping.permissionsex.exception.PermissionsLoadingException;
import ninja.leaping.permissionsex.rank.RankLadder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@RunWith(Parameterized.class)
public class SqlDaoTest extends PermissionsExTest {
    private static final AtomicInteger COUNTER = new AtomicInteger();
    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object> data() {
        String[] propertyTestDbs = System.getProperty("ninja.leaping.permissionsex.backend.sql.testDatabases", "").split(";", -1);
        if (propertyTestDbs.length == 1 && propertyTestDbs[0].equals("")) {
            propertyTestDbs = new String[0];
        }
        final Object[][] tests = new Object[propertyTestDbs.length + 1][2];
        tests[propertyTestDbs.length] = new Object[] {"h2", "jdbc:h2:{base}/test.db"};
        for (int i = 0; i < propertyTestDbs.length; ++i) {
            tests[i] = propertyTestDbs[i].split("!");
        }
        return Arrays.asList((Object[]) tests);
    }

    private final SqlDataStore sqlStore = new SqlDataStore();
    private  String jdbcUrl;

    public SqlDaoTest(String databaseName, String jdbcUrl) throws IOException {
        this.jdbcUrl = jdbcUrl;
    }

    @Before
    @Override
    public void setUp() throws IOException, PEBKACException, PermissionsLoadingException, ObjectMappingException {
        File testDir = tempFolder.newFolder();
        jdbcUrl = jdbcUrl.replaceAll("\\{base\\}", testDir.getCanonicalPath());
        sqlStore.setConnectionUrl(jdbcUrl);
        sqlStore.setPrefix("pextest" + COUNTER.getAndIncrement());
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
    public void testGlobalParameters() throws SQLException {
        try (SqlDao dao = sqlStore.getDao()) {
            assertFalse(dao.getGlobalParameter("nonexistant").isPresent());

            dao.setGlobalParameter("potato", "russet");
            assertEquals("russet", dao.getGlobalParameter("potato").get());

            dao.setGlobalParameter("potato", "sweet");
            assertEquals("sweet", dao.getGlobalParameter("potato").get());

            dao.setGlobalParameter("potato", null);
            assertFalse(dao.getGlobalParameter("potato").isPresent());
        }
    }

    @Test
    public void testGetSubjectRef() throws SQLException {
        try (SqlDao dao = sqlStore.getDao()) {
            assertFalse(dao.getSubjectRef("group", "admin").isPresent());
            assertFalse(dao.getSubjectRef(1).isPresent());
        }
    }

    @Test
    public void testGetOrCreateSubjectRef() throws SQLException {
        try (SqlDao dao = sqlStore.getDao()) {
            assertFalse(dao.getSubjectRef("group", "admin").isPresent());
            SqlSubjectRef created = dao.getOrCreateSubjectRef("group", "admin");
            SqlSubjectRef fetched = dao.getSubjectRef("group", "admin").get();
            assertEquals(created.getId(), fetched.getId());

            SqlSubjectRef couldBeCreated = dao.getOrCreateSubjectRef("group", "admin");
            assertEquals(created.getId(), couldBeCreated.getId());

            SqlSubjectRef gottenById = dao.getSubjectRef(created.getId()).get();
            assertEquals(created.getId(), gottenById.getId());
            assertEquals(created, gottenById);
        }
    }

    @Test
    public void testRemoveSubject() throws SQLException {
        try (SqlDao dao = sqlStore.getDao()) {
            SqlSubjectRef first = dao.getOrCreateSubjectRef("group", "one");
            SqlSubjectRef second = dao.getOrCreateSubjectRef("group", "two");

            assertTrue(dao.removeSubject("group", "one"));
            assertFalse(dao.getSubjectRef(first.getType(), first.getIdentifier()).isPresent());

            assertTrue(dao.removeSubject(second));
            assertFalse(dao.getSubjectRef(second.getId()).isPresent());
        }
    }

    @Test
    public void getRegisteredTypes() throws SQLException {
        try (SqlDao dao = sqlStore.getDao()) {
            dao.getOrCreateSubjectRef("group", "one");
            dao.getOrCreateSubjectRef("group", "two");
            dao.getOrCreateSubjectRef("user", UUID.randomUUID().toString());
            dao.getOrCreateSubjectRef("default", "user");
            dao.getOrCreateSubjectRef("system", "console");
            assertEquals(ImmutableSet.of("group", "user", "default", "system"), dao.getRegisteredTypes());
        }
    }

    @Test
    public void getAllIdentifiers() throws SQLException {
        try (SqlDao dao = sqlStore.getDao()) {
            dao.getOrCreateSubjectRef("group", "one");
            dao.getOrCreateSubjectRef("group", "two");
            dao.getOrCreateSubjectRef("default", "user");
            dao.getOrCreateSubjectRef("default", "default");
            dao.getOrCreateSubjectRef("system", "console");

            assertEquals(ImmutableSet.of("one", "two"), dao.getAllIdentifiers("group"));
            assertEquals(ImmutableSet.of("user", "default"), dao.getAllIdentifiers("default"));
        }
    }

    @Test
    public void testAddRemoveSegment() throws SQLException {
        try (SqlDao dao = sqlStore.getDao()) {
            SqlSubjectRef subject = dao.getOrCreateSubjectRef("group", "one");
            assertTrue(dao.getSegments(subject).isEmpty());

            Segment seg = dao.addSegment(subject);
            List<Segment> segments = dao.getSegments(subject);
            assertFalse(segments.isEmpty());
            assertEquals(seg.getId(), segments.get(0).getId());

            assertTrue(dao.removeSegment(seg));
            assertTrue(dao.getSegments(subject).isEmpty());
        }
    }

    @Test
    public void testContexts() throws SQLException {
        try (SqlDao dao = sqlStore.getDao()) {
            final SqlSubjectRef subject = dao.getOrCreateSubjectRef("test", "contexts");

            Segment testSeg = dao.addSegment(subject);
            assertTrue(testSeg.getContexts().isEmpty());

            final Set<Entry<String, String>> contexts = ImmutableSet.of(Maps.immutableEntry("world", "DIM-1"),
                    Maps.immutableEntry("server-tag", "minigames"));
            dao.setContexts(testSeg, contexts);
            testSeg = dao.getSegments(subject).get(0);
            assertEquals(contexts, testSeg.getContexts());

            dao.setContexts(testSeg, ImmutableSet.of());
            testSeg = dao.getSegments(subject).get(0);
            assertTrue(testSeg.getContexts().isEmpty());

        }
    }

    @Test
    public void testOptions() throws SQLException {
        try (SqlDao dao = sqlStore.getDao()) {
            SqlSubjectRef subject = dao.getOrCreateSubjectRef("group", "one");
            Segment seg = dao.addSegment(subject);
            assertFalse(seg.getOptions().containsKey("test"));

            // Set individually
            dao.setOption(seg, "test", "potato");
            dao.setOption(seg, "test2", "orange");
            dao.setOption(seg, "test", "pear");
            seg = dao.getSegments(subject).get(0); // Probably not the most efficient, but having extra code paths just for testing is a bad idea (maybe a get options/etc?)
            assertEquals("pear", seg.getOptions().get("test"));
            assertEquals("orange", seg.getOptions().get("test2"));

            // Bulk set
            dao.setOptions(seg, ImmutableMap.of("direction", "left", "speed", "vroom"));
            seg = dao.getSegments(subject).get(0);
            assertFalse(seg.getOptions().containsKey("test"));
            assertFalse(seg.getOptions().containsKey("test2"));
            assertEquals("left", seg.getOptions().get("direction"));
            assertEquals("vroom", seg.getOptions().get("speed"));

            // Clear a single option
            dao.clearOption(seg, "direction");
            seg = dao.getSegments(subject).get(0);
            assertFalse(seg.getOptions().containsKey("direction"));
            assertEquals("vroom", seg.getOptions().get("speed"));

            // Working through the Segment object
            seg = seg.withOption("test", "potato")
                    .withOption("direction", "north");
            seg.doUpdates(dao);
            seg = dao.getSegments(subject).get(0);
            assertEquals("potato", seg.getOptions().get("test"));
            assertEquals("vroom", seg.getOptions().get("speed"));
            assertEquals("north", seg.getOptions().get("direction"));

            seg = seg.withoutOptions();
            seg.doUpdates(dao);
            seg = dao.getSegments(subject).get(0);
            assertTrue(seg.getOptions().isEmpty());

            seg = seg.withOptions(ImmutableMap.of("absorbency", "high", "color", "yellow"));
            seg.doUpdates(dao);
            seg = dao.getSegments(subject).get(0);
            assertEquals("high", seg.getOptions().get("absorbency"));
            assertEquals("yellow", seg.getOptions().get("color"));

            seg = seg.withOptions(ImmutableMap.of("absorbency", "high", "color", "yellow"))
                    .withoutOptions()
                    .withOption("test", "orange");
            seg.doUpdates(dao);
            seg = dao.getSegments(subject).get(0);

            assertEquals(1, seg.getOptions().size());
            assertEquals("orange", seg.getOptions().get("test"));
        }
    }

    @Test
    public void testPermissions() throws SQLException {
        try (SqlDao dao = sqlStore.getDao()) {
            SqlSubjectRef subject = dao.getOrCreateSubjectRef("group", "one");
            Segment seg = dao.addSegment(subject);
            assertFalse(seg.getPermissions().containsKey("test.first"));

            // Set individually
            dao.setPermission(seg, "test.first", 1);
            dao.setPermission(seg, "test.second", 5);
            dao.setPermission(seg, "test.first", -1);
            seg = dao.getSegments(subject).get(0); // Probably not the most efficient, but having extra code paths just for testing is a bad idea (maybe a get options/etc?)
            assertEquals(-1, seg.getPermissions().get("test.first").intValue());
            assertEquals(5, seg.getPermissions().get("test.second").intValue());

            // Bulk set
            dao.setPermissions(seg, ImmutableMap.of("test.steering", 1, "test.acceleration", -1));
            seg = dao.getSegments(subject).get(0);
            assertFalse(seg.getPermissions().containsKey("test.first"));
            assertFalse(seg.getPermissions().containsKey("test.second"));
            assertEquals(1, seg.getPermissions().get("test.steering").intValue());
            assertEquals(-1, seg.getPermissions().get("test.acceleration").intValue());

            // Clear a single option
            dao.clearPermission(seg, "test.steering");
            seg = dao.getSegments(subject).get(0);
            assertFalse(seg.getPermissions().containsKey("test.steering"));
            assertEquals(-1, seg.getPermissions().get("test.acceleration").intValue());

            // Working through the Segment object
            seg = seg.withPermission("test.first", -2)
                    .withPermission("test.steering", 1);
            seg.doUpdates(dao);
            seg = dao.getSegments(subject).get(0);
            assertEquals(-2, seg.getPermissions().get("test.first").intValue());
            assertEquals(-1, seg.getPermissions().get("test.acceleration").intValue());
            assertEquals(1, seg.getPermissions().get("test.steering").intValue());

            seg = seg.withoutPermissions();
            seg.doUpdates(dao);
            seg = dao.getSegments(subject).get(0);
            assertTrue(seg.getPermissions().isEmpty());

            seg = seg.withPermissions(ImmutableMap.of("test.absorb", 42, "test.color.change", -4));
            seg.doUpdates(dao);
            seg = dao.getSegments(subject).get(0);
            assertEquals(42, seg.getPermissions().get("test.absorb").intValue());
            assertEquals(-4, seg.getPermissions().get("test.color.change").intValue());

            seg = seg.withPermissions(ImmutableMap.of("test.absorb", 42, "test.color.change", -4))
                    .withoutPermissions()
                    .withPermission("test.first", 2);
            seg.doUpdates(dao);
            seg = dao.getSegments(subject).get(0);

            assertEquals(1, seg.getPermissions().size());
            assertEquals(2, seg.getPermissions().get("test.first").intValue());
        }
    }

    @Test
    public void testParents() throws SQLException {
        try (SqlDao dao = sqlStore.getDao()) {
            final SqlSubjectRef member = dao.getOrCreateSubjectRef("group", "member"),
                    vip = dao.getOrCreateSubjectRef("group", "vip"),
                    potato = dao.getOrCreateSubjectRef("group", "potato"),
                    guest = dao.getOrCreateSubjectRef("group", "guest"),
                    novice = dao.getOrCreateSubjectRef("group", "novice");

            Segment vipSeg = dao.addSegment(vip);
            assertTrue(vipSeg.getParents().isEmpty());
            dao.addParent(vipSeg, member);
            vipSeg = dao.getSegments(vip).get(0);
            assertTrue(vipSeg.getParents().contains(member));

            dao.removeParent(vipSeg, member);
            vipSeg = dao.getSegments(vip).get(0);
            assertEquals(0, vipSeg.getParents().size());

            dao.setParents(vipSeg, ImmutableList.of(guest, novice));
            vipSeg = dao.getSegments(vip).get(0);
            assertEquals(2, vipSeg.getParents().size());
            assertTrue(vipSeg.getParents().contains(guest));
            assertTrue(vipSeg.getParents().contains(novice));

            vipSeg = vipSeg.withoutParents();
            vipSeg.doUpdates(dao);
            vipSeg = dao.getSegments(vip).get(0);
            assertEquals(0, vipSeg.getParents().size());


            vipSeg = vipSeg.withAddedParent(potato);
            vipSeg.doUpdates(dao);
            vipSeg = dao.getSegments(vip).get(0);
            assertEquals(1, vipSeg.getParents().size());
            assertTrue(vipSeg.getParents().contains(potato));

            vipSeg = vipSeg.withAddedParent(member);
            vipSeg.doUpdates(dao);
            vipSeg = dao.getSegments(vip).get(0);
            assertEquals(2, vipSeg.getParents().size());
            assertTrue(vipSeg.getParents().contains(potato));
            assertTrue(vipSeg.getParents().contains(member));

            final SqlSubjectRef unallocatedGroup = SqlSubjectRef.unresolved("group", "unresolved");
            assertTrue(unallocatedGroup.isUnallocated());

            // Test unallocated
            dao.addParent(vipSeg, unallocatedGroup);
            assertFalse(unallocatedGroup.isUnallocated());
            vipSeg = dao.getSegments(vip).get(0);
            assertTrue(vipSeg.getParents().contains(unallocatedGroup));
        }
    }

    @Test
    public void testSetDefaultValue() throws SQLException {
        try (SqlDao dao = sqlStore.getDao()) {
            final SqlSubjectRef subject = dao.getOrCreateSubjectRef("test", "defvalue");
            Segment newSeg = Segment.unallocated().withDefaultValue(5);
            dao.allocateSegment(subject, newSeg);

            Segment testSeg = dao.getSegments(subject).get(0);
            assertEquals(5, testSeg.getPermissionDefault().intValue());

            testSeg = testSeg.withDefaultValue(-4);
            testSeg.doUpdates(dao);
            testSeg = dao.getSegments(subject).get(0);
            assertEquals(-4, testSeg.getPermissionDefault().intValue());

            testSeg = testSeg.withDefaultValue(null);
            testSeg.doUpdates(dao);
            testSeg = dao.getSegments(subject).get(0);
            assertEquals(null, testSeg.getPermissionDefault());
        }
    }

    @Test
    public void testContextInheritance() throws SQLException {
        final Entry<String, String> worldNether = Maps.immutableEntry("world", "DIM-1"),
                serverMinigames = Maps.immutableEntry("server-tag", "minigames");
        final List<Entry<String, String>> worldNetherParents = ImmutableList.of(Maps.immutableEntry("world", "world")),
                serverTagMinigamesParents = ImmutableList.of(Maps.immutableEntry("server-tag", "adventure"), Maps.immutableEntry("world", "minigames"));

        try (SqlDao dao = sqlStore.getDao()) {
            // resolve, set, set to null, add new
            SqlContextInheritance inherit = dao.getContextInheritance();
            assertTrue(inherit.getAllParents().isEmpty());

            dao.setContextInheritance(worldNether, worldNetherParents);

            inherit = dao.getContextInheritance();
            assertEquals(worldNetherParents, inherit.getParents(worldNether));

            inherit = inherit.setParents(serverMinigames, serverTagMinigamesParents);
            inherit.doUpdate(dao);
            inherit = dao.getContextInheritance();

            assertEquals(serverTagMinigamesParents, inherit.getParents(serverMinigames));
            assertEquals(worldNetherParents, inherit.getParents(worldNether));

        }
    }

    @Test
    public void testRankLadder() throws SQLException {
        try (SqlDao dao = sqlStore.getDao()) {
            final SqlSubjectRef member = dao.getOrCreateSubjectRef("group", "member"),
                    vip = dao.getOrCreateSubjectRef("group", "vip"),
                    potato = dao.getOrCreateSubjectRef("group", "potato"),
                    guest = dao.getOrCreateSubjectRef("group", "guest"),
                    novice = dao.getOrCreateSubjectRef("group", "novice");

            assertFalse(dao.hasEntriesForRankLadder("uncreated"));
            RankLadder uncreated = dao.getRankLadder("uncreated");
            assertTrue(uncreated.getRanks().isEmpty());

            dao.setRankLadder("test1", dao.getRankLadder("test1").addRank(guest).addRank(novice).addRank(member).addRank(vip));
            RankLadder testLadder = dao.getRankLadder("test1");
            assertEquals(ImmutableList.of(guest, novice, member, vip), testLadder.getRanks());

            dao.setRankLadder("test1", dao.getRankLadder("another").addRank(potato));
            testLadder = dao.getRankLadder("test1");
            assertEquals(ImmutableList.of(potato), testLadder.getRanks());

            assertEquals(ImmutableSet.of("test1"), dao.getAllRankLadderNames());

            dao.setRankLadder("test1", null);
            testLadder = dao.getRankLadder("test1");
            assertEquals(ImmutableSet.of(), dao.getAllRankLadderNames());
            assertTrue(testLadder.getRanks().isEmpty());
        }

    }

    @Test
    public void testInitializeTables() throws SQLException {
        try (SqlDao dao = sqlStore.getDao()) {
            dao.initializeTables(); // Because tables are already initialized, this should do nothing
        }
    }
}
