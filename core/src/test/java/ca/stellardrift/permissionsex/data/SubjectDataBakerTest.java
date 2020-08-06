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

package ca.stellardrift.permissionsex.data;

import ca.stellardrift.permissionsex.PermissionsEx;
import ca.stellardrift.permissionsex.PermissionsExTest;
import ca.stellardrift.permissionsex.backend.DataStore;
import ca.stellardrift.permissionsex.backend.memory.MemoryDataStore;
import ca.stellardrift.permissionsex.config.EmptyPlatformConfiguration;
import ca.stellardrift.permissionsex.config.PermissionsExConfiguration;
import ca.stellardrift.permissionsex.context.*;
import ca.stellardrift.permissionsex.exception.PEBKACException;
import ca.stellardrift.permissionsex.exception.PermissionsLoadingException;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import ca.stellardrift.permissionsex.subject.SubjectType;
import ca.stellardrift.permissionsex.util.NodeTree;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static ca.stellardrift.permissionsex.context.Context_definitionKt.cSet;
import static org.junit.jupiter.api.Assertions.*;

public class SubjectDataBakerTest extends PermissionsExTest {

    /**
     * Arrangement:
     * parent
     *    child
     *       subject
     *ignored inheritance permission in parent does not have effect unless checked in parent or child
     * ignored inheritance permission in child has effect in both
     */
    @Test
    public void testIgnoredInheritancePermissions() throws ExecutionException, PermissionsLoadingException, InterruptedException {
        SubjectType groupCache = getManager().getSubjects(PermissionsEx.SUBJECTS_GROUP);
        CalculatedSubject parentS = groupCache.get("parent").thenCompose(parent -> parent.data().update(old -> old.setPermission(PermissionsEx.GLOBAL_CONTEXT, "#test.permission.parent", 1)).thenApply(data -> parent)).get();
        CalculatedSubject childS = groupCache.get("child").thenCompose(child -> child.data().update(old -> old.addParent(PermissionsEx.GLOBAL_CONTEXT, groupCache.getTypeInfo().getTypeName(), parentS.getIdentifier().getValue())
                .setPermission(PermissionsEx.GLOBAL_CONTEXT, "#test.permission.child", 1)
        ).thenApply(data -> child)).get();
        CalculatedSubject subjectS = groupCache.get("subject").thenCompose(subject -> subject.data().update(old -> old.addParent(PermissionsEx.GLOBAL_CONTEXT, childS.getIdentifier().getKey(), childS.getIdentifier().getValue())).thenApply(data -> subject)).get();

        assertEquals(1, parentS.getPermissions(PermissionsEx.GLOBAL_CONTEXT).get("test.permission.parent"));
        assertEquals(1, childS.getPermissions(PermissionsEx.GLOBAL_CONTEXT).get("test.permission.parent"));
        assertEquals(1, childS.getPermissions(PermissionsEx.GLOBAL_CONTEXT).get("test.permission.child"));
        assertEquals(0, subjectS.getPermissions(PermissionsEx.GLOBAL_CONTEXT).get("test.permission.parent"));
        assertEquals(1, subjectS.getPermissions(PermissionsEx.GLOBAL_CONTEXT).get("test.permission.child"));
    }

    @Test
    public void testFallbackSubject() {
        getManager().getSubjects(PermissionsEx.SUBJECTS_FALLBACK).transientData().update(PermissionsEx.SUBJECTS_USER, old -> old.setPermission(PermissionsEx.GLOBAL_CONTEXT, "messages.welcome", 1)).join();

        CalculatedSubject subject = getManager().getSubjects(PermissionsEx.SUBJECTS_USER).get("test").join();

        assertTrue(subject.hasPermission("messages.welcome")); // we are inheriting from fallback

        subject.transientData().update(data -> data.addParent(PermissionsEx.GLOBAL_CONTEXT, PermissionsEx.SUBJECTS_GROUP, "member")).join();

        assertFalse(subject.hasPermission("messages.welcome")); // now that we have data stored for the subject, we no longer inherit from fallback.
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
        ContextDefinition<String> worldCtx = WorldContextDefinition.INSTANCE,
                    serverTypeCtx = ServerTagContextDefinition.INSTANCE;
        ContextDefinition<ZonedDateTime> beforeTimeCtx = BeforeTimeContextDefinition.INSTANCE;

        CalculatedSubject subject = getManager().getSubjects(PermissionsEx.SUBJECTS_GROUP).get("a").get();
        subject.data().update(data -> {
            return data.setPermissions(cSet(worldCtx.createValue("nether")), ImmutableMap.of("some.perm", 1, "some.meme", -1))
                    .setPermissions(cSet(worldCtx.createValue("nether"), beforeTimeCtx.createValue(ZonedDateTime.now().plus(2, ChronoUnit.DAYS))), ImmutableMap.of("some.meme", 1, "some.cat", 1))
                    .setPermission(cSet(worldCtx.createValue("nether"), serverTypeCtx.createValue("bad")), "some.day", 1)
                    .setPermission(cSet(serverTypeCtx.createValue("good")), "some.year", 1)
                    .setPermission(PermissionsEx.GLOBAL_CONTEXT, "some.world", 1);

        }).join();

        Set<ContextValue<?>> activeSetA = cSet(worldCtx.createValue("nether"), beforeTimeCtx.createValue(ZonedDateTime.now()), serverTypeCtx.createValue("good"));
        Set<ContextValue<?>> activeSetB = cSet(worldCtx.createValue("nether"));
        Set<ContextValue<?>> activeSetC = PermissionsEx.GLOBAL_CONTEXT;
        NodeTree permsA = subject.getPermissions(activeSetA);
        NodeTree permsB = subject.getPermissions(activeSetB);
        NodeTree permsC = subject.getPermissions(activeSetC);

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
                return new MemoryDataStore("data-baker");
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

    private static class WorldContextDefinition extends SimpleContextDefinition {
        public static final WorldContextDefinition INSTANCE = new WorldContextDefinition();

        private WorldContextDefinition() {
            super("world");
        }
    }
}
