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
package ca.stellardrift.permissionsex.impl.data;

import ca.stellardrift.permissionsex.PermissionsEngine;
import ca.stellardrift.permissionsex.context.ContextDefinition;
import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.context.SimpleContextDefinition;
import ca.stellardrift.permissionsex.impl.PermissionsEx;
import ca.stellardrift.permissionsex.impl.context.ServerTagContextDefinition;
import ca.stellardrift.permissionsex.impl.context.TimeContextDefinition;
import ca.stellardrift.permissionsex.impl.util.PCollections;
import ca.stellardrift.permissionsex.test.PermissionsExTest;
import ca.stellardrift.permissionsex.datastore.DataStore;
import ca.stellardrift.permissionsex.impl.backend.memory.MemoryDataStore;
import ca.stellardrift.permissionsex.impl.config.EmptyPlatformConfiguration;
import ca.stellardrift.permissionsex.impl.config.PermissionsExConfiguration;
import ca.stellardrift.permissionsex.exception.PEBKACException;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import ca.stellardrift.permissionsex.impl.subject.SubjectTypeCollectionImpl;
import ca.stellardrift.permissionsex.util.NodeTree;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

public class SubjectDataBakerTest extends PermissionsExTest {

    static Set<ContextValue<?>> cSet(final ContextValue<?>... values) {
        return PCollections.set(values);
    }

    /**
     * Arrangement:
     * parent
     *    child
     *       subject
     *ignored inheritance permission in parent does not have effect unless checked in parent or child
     * ignored inheritance permission in child has effect in both
     */
    @Test
    public void testIgnoredInheritancePermissions() throws ExecutionException, InterruptedException {
        SubjectTypeCollectionImpl<String> groupCache = getManager().subjects(SUBJECTS_GROUP);
        CalculatedSubject parentS = groupCache.get("parent").thenCompose(parent -> parent.data().update(PermissionsEx.GLOBAL_CONTEXT, old -> old.withPermission("#test.permission.parent", 1)).thenApply(data -> parent)).get();
        CalculatedSubject childS = groupCache.get("child").thenCompose(child -> child.data().update(PermissionsEngine.GLOBAL_CONTEXT, old -> old.plusParent(parentS.identifier())
                .withPermission("#test.permission.child", 1)
        ).thenApply(data -> child)).get();
        CalculatedSubject subjectS = groupCache.get("subject").thenCompose(subject -> subject.data().update(PermissionsEx.GLOBAL_CONTEXT, old -> old.plusParent(childS.identifier())).thenApply(data -> subject)).get();

        assertEquals(1, parentS.permissions(PermissionsEx.GLOBAL_CONTEXT).get("test.permission.parent"));
        assertEquals(1, childS.permissions(PermissionsEx.GLOBAL_CONTEXT).get("test.permission.parent"));
        assertEquals(1, childS.permissions(PermissionsEx.GLOBAL_CONTEXT).get("test.permission.child"));
        assertEquals(0, subjectS.permissions(PermissionsEx.GLOBAL_CONTEXT).get("test.permission.parent"));
        assertEquals(1, subjectS.permissions(PermissionsEx.GLOBAL_CONTEXT).get("test.permission.child"));
    }

    @Test
    public void testFallbackSubject() {
        getManager().subjects(PermissionsEngine.SUBJECTS_FALLBACK).transientData().update(PermissionsEngine.SUBJECTS_USER, old -> old.withSegment(PermissionsEx.GLOBAL_CONTEXT, s -> s.withPermission("messages.welcome", 1))).join();

        CalculatedSubject subject = getManager().subjects(SUBJECTS_USER).get(UUID.randomUUID()).join();

        assertTrue(subject.hasPermission("messages.welcome")); // we are inheriting from fallback

        subject.transientData().update(PermissionsEngine.GLOBAL_CONTEXT, data -> data.plusParent(SUBJECTS_GROUP, "member")).join();

        assertFalse(subject.hasPermission("messages.welcome")); // now that we have data stored for the subject, we no longer inherit from fallback.
    }

    private static ZonedDateTime nowUtc() {
        return ZonedDateTime.now(ZoneOffset.UTC);
    }

    /**
     * Test that contexts are resolved correctly.
     *
     * Subjects:
     * user a:
     *    in contexts world=nether -- some.perm=1 + some.meme=-1
     *    in contexts world=nether, before-time=now+2 days -- some.meme=1, some.cat=1
     *    in contexts world=nether, server-tag=bad -- some.day=1
     *    in contexts server-tag=good -- some.year=1
     * Queries:
     *  given active contexts world=nether, before-time=now, server-tag=good:
     *  - some.perm=1
     *  - some.meme=1
     *  - some.cat=1
     *  - some.day=0
     *  - some.year=1
     *  - some.world=1
     *
     *  given active contexts world=nether:
     *  - some.perm=1
     *  - some.meme=-1
     *  - some.cat=0
     *  - some.day=0
     *  - some.year=0
     *  - some.world=1
     *
     *  given no active contexts:
     *  - some.perm=0
     *  - some.cat=0
     *  - some.meme=0
     *  - some.day=0
     *  - some.year=0
     *  - some.world=1
     *
     */
    @Test
    public void testContextCalculation() throws ExecutionException, InterruptedException {
        final ContextDefinition<String> worldCtx = WORLD_CONTEXT,
                    serverTypeCtx = ServerTagContextDefinition.INSTANCE;
        final ContextDefinition<ZonedDateTime> beforeTimeCtx = TimeContextDefinition.BEFORE_TIME;

        CalculatedSubject subject = getManager().subjects(SUBJECTS_GROUP).get("a").get();
        subject.data().update(data -> data.withSegment(cSet(worldCtx.createValue("nether")), c -> c.withPermissions(ImmutableMap.of("some.perm", 1, "some.meme", -1)))
                .withSegment(cSet(worldCtx.createValue("nether"), beforeTimeCtx.createValue(nowUtc().plus(2, ChronoUnit.DAYS))), s -> s.withPermissions(ImmutableMap.of("some.meme", 1, "some.cat", 1)))
                .withSegment(cSet(worldCtx.createValue("nether"), serverTypeCtx.createValue("bad")), s -> s.withPermission("some.day", 1))
                .withSegment(cSet(serverTypeCtx.createValue("good")), s -> s.withPermission("some.year", 1))
                .withSegment(PermissionsEngine.GLOBAL_CONTEXT, s -> s.withPermission("some.world", 1))).join();

        Set<ContextValue<?>> activeSetA = cSet(worldCtx.createValue("nether"), beforeTimeCtx.createValue(nowUtc()), serverTypeCtx.createValue("good"));
        Set<ContextValue<?>> activeSetB = cSet(worldCtx.createValue("nether"));
        Set<ContextValue<?>> activeSetC = PermissionsEx.GLOBAL_CONTEXT;
        NodeTree permsA = subject.permissions(activeSetA);
        NodeTree permsB = subject.permissions(activeSetB);
        NodeTree permsC = subject.permissions(activeSetC);

        // Set A
        assertEquals(1, permsA.get("some.perm"));
        assertEquals(1, permsA.get("some.cat"));
        assertEquals(1, permsA.get("some.meme"));
        assertEquals(0, permsA.get("some.day"));
        assertEquals(1, permsA.get("some.year"));
        assertEquals(1, permsA.get("some.world"));

        // Set B
        assertEquals(1, permsB.get("some.perm"));
        assertEquals(-1, permsB.get("some.meme"));
        assertEquals(0, permsB.get("some.cat"));
        assertEquals(0, permsB.get("some.day"));
        assertEquals(0, permsB.get("some.year"));
        assertEquals(1, permsB.get("some.world"));

        // Set C
        assertEquals(0, permsC.get("some.perm"));
        assertEquals(0, permsC.get("some.meme"));
        assertEquals(0, permsC.get("some.cat"));
        assertEquals(0, permsC.get("some.day"));
        assertEquals(0, permsC.get("some.year"));
        assertEquals(1, permsC.get("some.world"));

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
                return MemoryDataStore.create("data-baker");
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

    private static final SimpleContextDefinition WORLD_CONTEXT = SimpleContextDefinition.context("world", (s, a) -> {});
}
