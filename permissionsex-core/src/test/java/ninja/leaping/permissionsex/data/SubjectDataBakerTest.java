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
package ninja.leaping.permissionsex.data;

import com.google.common.collect.ImmutableList;
import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.PermissionsExTest;
import ninja.leaping.permissionsex.backend.DataStore;
import ninja.leaping.permissionsex.backend.memory.MemoryDataStore;
import ninja.leaping.permissionsex.config.PermissionsExConfiguration;
import ninja.leaping.permissionsex.exception.PEBKACException;
import ninja.leaping.permissionsex.subject.CalculatedSubject;
import ninja.leaping.permissionsex.exception.PermissionsLoadingException;
import ninja.leaping.permissionsex.subject.SubjectType;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static ninja.leaping.permissionsex.PermissionsEx.GLOBAL_CONTEXT;
import static org.junit.Assert.assertEquals;

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
        CalculatedSubject parentS = groupCache.get("parent").thenCompose(parent -> parent.data().update(old -> old.setPermission(GLOBAL_CONTEXT, "#test.permission.parent", 1)).thenApply(data -> parent)).get();
        CalculatedSubject childS = groupCache.get("child").thenCompose(child -> child.data().update(old -> old.addParent(GLOBAL_CONTEXT, groupCache.getTypeInfo().getTypeName(), parentS.getIdentifier().getValue())
                .setPermission(GLOBAL_CONTEXT, "#test.permission.child", 1)
        ).thenApply(data -> child)).get();
        CalculatedSubject subjectS = groupCache.get("subject").thenCompose(subject -> subject.data().update(old -> old.addParent(GLOBAL_CONTEXT, childS.getIdentifier().getKey(), childS.getIdentifier().getValue())).thenApply(data -> subject)).get();

        assertEquals(1, parentS.getPermissions(GLOBAL_CONTEXT).get("test.permission.parent"));
        assertEquals(1, childS.getPermissions(GLOBAL_CONTEXT).get("test.permission.parent"));
        assertEquals(1, childS.getPermissions(GLOBAL_CONTEXT).get("test.permission.child"));
        assertEquals(0, subjectS.getPermissions(GLOBAL_CONTEXT).get("test.permission.parent"));
        assertEquals(1, subjectS.getPermissions(GLOBAL_CONTEXT).get("test.permission.child"));
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
                return new MemoryDataStore();
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
}
