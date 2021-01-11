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
import ca.stellardrift.permissionsex.impl.util.PCollections;
import ca.stellardrift.permissionsex.subject.SubjectRef;
import ca.stellardrift.permissionsex.test.EmptyTestConfiguration;
import ca.stellardrift.permissionsex.test.PermissionsExTest;
import ca.stellardrift.permissionsex.impl.config.PermissionsExConfiguration;
import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.exception.PEBKACException;
import ca.stellardrift.permissionsex.exception.PermissionsLoadingException;
import ca.stellardrift.permissionsex.rank.RankLadder;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.pcollections.HashTreePSet;
import org.pcollections.PSet;
import org.pcollections.PVector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class SqlDaoTest extends PermissionsExTest {
    private static final AtomicInteger COUNTER = new AtomicInteger();

    /*public static Stream<Arguments> data() {
        String[] propertyTestDbs = System.getProperty("ninja.leaping.permissionsex.backend.sql.testDatabases", "").split(";", -1);
        if (propertyTestDbs.length == 1 && propertyTestDbs[0].equals("")) {
            propertyTestDbs = new String[0];
        }
        return Streams.concat(Arrays.stream(propertyTestDbs), Stream.of("jdbc:h2:file:{base}/test.db"))
                .map(Arguments::of);
    }*/

    public SqlDaoTest() {
        this.jdbcUrl = "jdbc:h2:file:{base}/test.db";
    }

    private ProtoDataStore<?> sqlStore;
    private final String jdbcUrl;

    @Override
    protected SqlDataStore dataStore() {
        return (SqlDataStore) super.dataStore();
    }

    @BeforeEach
    @Override
    public void setUp(TestInfo info, @TempDir Path tempDir) throws IOException, PEBKACException, PermissionsLoadingException {
        Path testDir = tempDir.resolve(info.getDisplayName() + "-dao");
        final String jdbcUrl = this.jdbcUrl.replaceAll("\\{base}", testDir.toAbsolutePath().toString().replace('\\', '/'));
        sqlStore = SqlDataStore.create(
            "sql-dao",
            jdbcUrl,
            "pextest" + COUNTER.getAndIncrement(),
            true
        );
        super.setUp(info, tempDir);
    }

    @AfterEach
    @Override
    public void tearDown() {
        // Delete all created tables;
        try (Connection conn = dataStore().getDataSource().getConnection()) {
            ResultSet tables = conn.getMetaData().getTables(null, null, dataStore().prefix() + "%", null);
            Statement stmt = conn.createStatement();
            while (tables.next()) {
                stmt.addBatch("DROP TABLE " + tables.getString("TABLE_NAME"));
            }
            stmt.executeBatch();
        } catch (final SQLException ex) {
            throw new RuntimeException(ex);
        }
        super.tearDown();
    }

    @Override
    protected PermissionsExConfiguration<?> populate() {
        return new EmptyTestConfiguration(sqlStore);
    }

    @Test
    public void testGlobalParameters() throws SQLException {
        try (SqlDao dao = dataStore().getDao()) {
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
        try (SqlDao dao = dataStore().getDao()) {
            assertFalse(dao.getSubjectRef("group", "admin").isPresent());
            assertFalse(dao.getSubjectRef(1).isPresent());
        }
    }

    @Test
    public void testGetOrCreateSubjectRef() throws SQLException {
        try (SqlDao dao = dataStore().getDao()) {
            assertFalse(dao.getSubjectRef("group", "admin").isPresent());
            SqlSubjectRef<?> created = dao.getOrCreateSubjectRef("group", "admin");
            SqlSubjectRef<?> fetched = dao.getSubjectRef("group", "admin").get();
            assertEquals(created.id(), fetched.id());

            SqlSubjectRef<?> couldBeCreated = dao.getOrCreateSubjectRef("group", "admin");
            assertEquals(created.id(), couldBeCreated.id());

            SqlSubjectRef<?> gottenById = dao.getSubjectRef(created.id()).get();
            assertEquals(created.id(), gottenById.id());
            assertEquals(created, gottenById);
        }
    }

    @Test
    public void testRemoveSubject() throws SQLException {
        try (SqlDao dao = dataStore().getDao()) {
            SqlSubjectRef<?> first = dao.getOrCreateSubjectRef("group", "one");
            SqlSubjectRef<?> second = dao.getOrCreateSubjectRef("group", "two");

            assertTrue(dao.removeSubject("group", "one"));
            assertFalse(dao.getSubjectRef(first.rawType(), first.rawIdentifier()).isPresent());

            assertTrue(dao.removeSubject(second));
            assertFalse(dao.getSubjectRef(second.id()).isPresent());
        }
    }

    @Test
    public void getRegisteredTypes() throws SQLException {
        try (SqlDao dao = dataStore().getDao()) {
            dao.getOrCreateSubjectRef("group", "one");
            dao.getOrCreateSubjectRef("group", "two");
            dao.getOrCreateSubjectRef("user", UUID.randomUUID().toString());
            dao.getOrCreateSubjectRef("default", "user");
            dao.getOrCreateSubjectRef("system", "console");
            assertEquals(PCollections.set("group", "user", "default", "system"), dao.getRegisteredTypes());
        }
    }

    @Test
    public void getAllIdentifiers() throws SQLException {
        try (SqlDao dao = dataStore().getDao()) {
            dao.getOrCreateSubjectRef("group", "one");
            dao.getOrCreateSubjectRef("group", "two");
            dao.getOrCreateSubjectRef("default", "user");
            dao.getOrCreateSubjectRef("default", "default");
            dao.getOrCreateSubjectRef("system", "console");

            assertEquals(PCollections.set("one", "two"), dao.getAllIdentifiers("group"));
            assertEquals(PCollections.set("user", "default"), dao.getAllIdentifiers("default"));
        }
    }

    @Test
    public void testAddRemoveSegment() throws SQLException {
        try (SqlDao dao = dataStore().getDao()) {
            SqlSubjectRef<?> subject = dao.getOrCreateSubjectRef("group", "one");
            assertTrue(dao.getSegments(subject).isEmpty());

            SqlSegment seg = dao.addSegment(subject);
            List<SqlSegment> segments = dao.getSegments(subject);
            assertFalse(segments.isEmpty());
            assertEquals(seg.id(), segments.get(0).id());

            assertTrue(dao.removeSegment(seg));
            assertTrue(dao.getSegments(subject).isEmpty());
        }
    }

    @Test
    public void testContexts() throws SQLException {
        try (SqlDao dao = dataStore().getDao()) {
            final SqlSubjectRef<?> subject = dao.getOrCreateSubjectRef("test", "contexts");

            SqlSegment testSeg = dao.addSegment(subject);
            assertTrue(testSeg.contexts().isEmpty());
            assertTrue(dao.getUsedContextKeys().isEmpty());

            final PSet<ContextValue<?>> contexts = PCollections.set(new ContextValue<String>("world", "DIM-1"),
                    new ContextValue<String>("server-tag", "minigames"));
            dao.setContexts(testSeg, contexts);
            testSeg = dao.getSegments(subject).get(0);
            assertEquals(contexts, testSeg.contexts());
            assertEquals(PCollections.set("world", "server-tag"), dao.getUsedContextKeys());

            dao.setContexts(testSeg, PCollections.set());
            testSeg = dao.getSegments(subject).get(0);
            assertTrue(testSeg.contexts().isEmpty());
            assertTrue(dao.getUsedContextKeys().isEmpty());

        }
    }

    @Test
    public void testOptions() throws SQLException {
        try (SqlDao dao = dataStore().getDao()) {
            SqlSubjectRef<?> subject = dao.getOrCreateSubjectRef("group", "one");
            SqlSegment seg = dao.addSegment(subject);
            assertFalse(seg.options().containsKey("test"));

            // Set individually
            dao.setOption(seg, "test", "potato");
            dao.setOption(seg, "test2", "orange");
            dao.setOption(seg, "test", "pear");
            seg = dao.getSegments(subject).get(0); // Probably not the most efficient, but having extra code paths just for testing is a bad idea (maybe a get options/etc?)
            assertEquals("pear", seg.options().get("test"));
            assertEquals("orange", seg.options().get("test2"));

            // Bulk set
            dao.setOptions(seg, ImmutableMap.of("direction", "left", "speed", "vroom"));
            seg = dao.getSegments(subject).get(0);
            assertFalse(seg.options().containsKey("test"));
            assertFalse(seg.options().containsKey("test2"));
            assertEquals("left", seg.options().get("direction"));
            assertEquals("vroom", seg.options().get("speed"));

            // Clear a single option
            dao.clearOption(seg, "direction");
            seg = dao.getSegments(subject).get(0);
            assertFalse(seg.options().containsKey("direction"));
            assertEquals("vroom", seg.options().get("speed"));

            // Working through the Segment object
            seg = seg.withOption("test", "potato")
                    .withOption("direction", "north");
            seg.doUpdates(dao);
            seg = dao.getSegments(subject).get(0);
            assertEquals("potato", seg.options().get("test"));
            assertEquals("vroom", seg.options().get("speed"));
            assertEquals("north", seg.options().get("direction"));

            seg = seg.withoutOptions();
            seg.doUpdates(dao);
            seg = dao.getSegments(subject).get(0);
            assertTrue(seg.options().isEmpty());

            seg = seg.withOptions(ImmutableMap.of("absorbency", "high", "color", "yellow"));
            seg.doUpdates(dao);
            seg = dao.getSegments(subject).get(0);
            assertEquals("high", seg.options().get("absorbency"));
            assertEquals("yellow", seg.options().get("color"));

            seg = seg.withOptions(ImmutableMap.of("absorbency", "high", "color", "yellow"))
                    .withoutOptions()
                    .withOption("test", "orange");
            seg.doUpdates(dao);
            seg = dao.getSegments(subject).get(0);

            assertEquals(1, seg.options().size());
            assertEquals("orange", seg.options().get("test"));
        }
    }

    @Test
    public void testPermissions() throws SQLException {
        try (SqlDao dao = dataStore().getDao()) {
            SqlSubjectRef<?> subject = dao.getOrCreateSubjectRef("group", "one");
            SqlSegment seg = dao.addSegment(subject);
            assertFalse(seg.permissions().containsKey("test.first"));

            // Set individually
            dao.setPermission(seg, "test.first", 1);
            dao.setPermission(seg, "test.second", 5);
            dao.setPermission(seg, "test.first", -1);
            seg = dao.getSegments(subject).get(0); // Probably not the most efficient, but having extra code paths just for testing is a bad idea (maybe a get options/etc?)
            assertEquals(-1, seg.permissions().get("test.first").intValue());
            assertEquals(5, seg.permissions().get("test.second").intValue());

            // Bulk set
            dao.setPermissions(seg, ImmutableMap.of("test.steering", 1, "test.acceleration", -1));
            seg = dao.getSegments(subject).get(0);
            assertFalse(seg.permissions().containsKey("test.first"));
            assertFalse(seg.permissions().containsKey("test.second"));
            assertEquals(1, seg.permissions().get("test.steering").intValue());
            assertEquals(-1, seg.permissions().get("test.acceleration").intValue());

            // Clear a single option
            dao.clearPermission(seg, "test.steering");
            seg = dao.getSegments(subject).get(0);
            assertFalse(seg.permissions().containsKey("test.steering"));
            assertEquals(-1, seg.permissions().get("test.acceleration").intValue());

            // Working through the Segment object
            seg = seg.withPermission("test.first", -2)
                    .withPermission("test.steering", 1);
            seg.doUpdates(dao);
            seg = dao.getSegments(subject).get(0);
            assertEquals(-2, seg.permissions().get("test.first").intValue());
            assertEquals(-1, seg.permissions().get("test.acceleration").intValue());
            assertEquals(1, seg.permissions().get("test.steering").intValue());

            seg = seg.withoutPermissions();
            seg.doUpdates(dao);
            seg = dao.getSegments(subject).get(0);
            assertTrue(seg.permissions().isEmpty());

            seg = seg.withPermissions(ImmutableMap.of("test.absorb", 42, "test.color.change", -4));
            seg.doUpdates(dao);
            seg = dao.getSegments(subject).get(0);
            assertEquals(42, seg.permissions().get("test.absorb").intValue());
            assertEquals(-4, seg.permissions().get("test.color.change").intValue());

            seg = seg.withPermissions(ImmutableMap.of("test.absorb", 42, "test.color.change", -4))
                    .withoutPermissions()
                    .withPermission("test.first", 2);
            seg.doUpdates(dao);
            seg = dao.getSegments(subject).get(0);

            assertEquals(1, seg.permissions().size());
            assertEquals(2, seg.permissions().get("test.first").intValue());
        }
    }

    @Test
    public void testParents() throws SQLException {
        try (SqlDao dao = dataStore().getDao()) {
            final SqlSubjectRef<?> member = dao.getOrCreateSubjectRef("group", "member"),
                    vip = dao.getOrCreateSubjectRef("group", "vip"),
                    potato = dao.getOrCreateSubjectRef("group", "potato"),
                    guest = dao.getOrCreateSubjectRef("group", "guest"),
                    novice = dao.getOrCreateSubjectRef("group", "novice");

            SqlSegment vipSeg = dao.addSegment(vip);
            assertTrue(vipSeg.parents().isEmpty());
            dao.addParent(vipSeg, member);
            vipSeg = dao.getSegments(vip).get(0);
            assertTrue(vipSeg.parents().contains(member));

            dao.removeParent(vipSeg, member);
            vipSeg = dao.getSegments(vip).get(0);
            assertEquals(0, vipSeg.parents().size());

            dao.setParents(vipSeg, PCollections.vector(guest, novice));
            vipSeg = dao.getSegments(vip).get(0);
            assertEquals(2, vipSeg.parents().size());
            assertTrue(vipSeg.parents().contains(guest));
            assertTrue(vipSeg.parents().contains(novice));

            vipSeg = vipSeg.withoutParents();
            vipSeg.doUpdates(dao);
            vipSeg = dao.getSegments(vip).get(0);
            assertEquals(0, vipSeg.parents().size());


            vipSeg = vipSeg.plusParent(potato);
            vipSeg.doUpdates(dao);
            vipSeg = dao.getSegments(vip).get(0);
            assertEquals(1, vipSeg.parents().size());
            assertTrue(vipSeg.parents().contains(potato));

            vipSeg = vipSeg.plusParent(member);
            vipSeg.doUpdates(dao);
            vipSeg = dao.getSegments(vip).get(0);
            assertEquals(2, vipSeg.parents().size());
            assertTrue(vipSeg.parents().contains(potato));
            assertTrue(vipSeg.parents().contains(member));

            final SqlSubjectRef<?> unallocatedGroup = SqlSubjectRef.unresolved(this.manager(), "group", "unresolved");
            assertTrue(unallocatedGroup.isUnallocated());

            // Test unallocated
            dao.addParent(vipSeg, unallocatedGroup);
            assertFalse(unallocatedGroup.isUnallocated());
            vipSeg = dao.getSegments(vip).get(0);
            assertTrue(vipSeg.parents().contains(unallocatedGroup));
        }
    }

    @Test
    public void testSetDefaultValue() throws SQLException {
        try (SqlDao dao = dataStore().getDao()) {
            final SqlSubjectRef<?> subject = dao.getOrCreateSubjectRef("test", "defvalue");
            SqlSegment newSeg = SqlSegment.unallocated().withFallbackPermission(5);
            dao.allocateSegment(subject, newSeg);

            SqlSegment testSeg = dao.getSegments(subject).get(0);
            assertEquals(5, testSeg.fallbackPermission());

            testSeg = testSeg.withFallbackPermission(-4);
            testSeg.doUpdates(dao);
            testSeg = dao.getSegments(subject).get(0);
            assertEquals(-4, testSeg.fallbackPermission());

            testSeg = testSeg.withFallbackPermission(0);
            testSeg.doUpdates(dao);
            testSeg = dao.getSegments(subject).get(0);
            assertEquals(0, testSeg.fallbackPermission());
        }
    }

    @Test
    public void testContextInheritance() throws SQLException {
        final ContextValue<String> worldNether = new ContextValue<>("world", "DIM-1"),
                serverMinigames = new ContextValue<>("server-tag", "minigames");
        final PVector<ContextValue<?>> worldNetherParents = PCollections.vector(new ContextValue<String>("world", "world")),
                serverTagMinigamesParents = PCollections.vector(new ContextValue<>("server-tag", "adventure"), new ContextValue<>("world", "minigames"));

        try (SqlDao dao = dataStore().getDao()) {
            // resolve, set, set to null, add new
            SqlContextInheritance inherit = dao.getContextInheritance();
            assertTrue(inherit.allParents().isEmpty());

            dao.setContextInheritance(worldNether, worldNetherParents);

            inherit = dao.getContextInheritance();
            assertEquals(worldNetherParents, inherit.parents(worldNether));

            inherit = inherit.parents(serverMinigames, serverTagMinigamesParents);
            inherit.doUpdate(dao);
            inherit = dao.getContextInheritance();

            assertEquals(serverTagMinigamesParents, inherit.parents(serverMinigames));
            assertEquals(worldNetherParents, inherit.parents(worldNether));

        }
    }

    @Test
    public void testRankLadder() throws SQLException {
        try (SqlDao dao = dataStore().getDao()) {
            final SqlSubjectRef<?> member = dao.getOrCreateSubjectRef("group", "member"),
                    vip = dao.getOrCreateSubjectRef("group", "vip"),
                    potato = dao.getOrCreateSubjectRef("group", "potato"),
                    guest = dao.getOrCreateSubjectRef("group", "guest"),
                    novice = dao.getOrCreateSubjectRef("group", "novice");

            assertFalse(dao.hasEntriesForRankLadder("uncreated"));
            RankLadder uncreated = dao.getRankLadder("uncreated");
            assertTrue(uncreated.ranks().isEmpty());

            dao.setRankLadder("test1", dao.getRankLadder("test1").with(guest).with(novice).with(member).with(vip));
            RankLadder testLadder = dao.getRankLadder("test1");
            Assertions.assertEquals(PCollections.<SubjectRef<?>>vector(guest, novice, member, vip), testLadder.ranks());

            dao.setRankLadder("test1", dao.getRankLadder("another").with(potato));
            testLadder = dao.getRankLadder("test1");
            Assertions.assertEquals(PCollections.vector(potato), testLadder.ranks());

            assertEquals(HashTreePSet.singleton("test1"), dao.getAllRankLadderNames());

            dao.setRankLadder("test1", null);
            testLadder = dao.getRankLadder("test1");
            assertEquals(PCollections.set(), dao.getAllRankLadderNames());
            assertTrue(testLadder.ranks().isEmpty());
        }

    }

    @Test
    public void testInitializeTables() throws SQLException {
        try (SqlDao dao = dataStore().getDao()) {
            dao.initializeTables(); // Because tables are already initialized, this should do nothing
        }
    }
}
