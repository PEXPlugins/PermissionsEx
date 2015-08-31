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

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.PermissionsExTest;
import ninja.leaping.permissionsex.subject.CalculatedSubject;
import ninja.leaping.permissionsex.exception.PermissionsLoadingException;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static ninja.leaping.permissionsex.PermissionsEx.GLOBAL_CONTEXT;
import static org.junit.Assert.assertEquals;

public class SubjectDataBakerTest extends PermissionsExTest {
    @Override
    protected void populate(ConfigurationNode node) {
        node.getNode("backends", "test", "type").setValue("memory");
        node.getNode("default-backend").setValue("test");
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
    public void testIgnoredInheritancePermissions() throws ExecutionException, PermissionsLoadingException {
        SubjectCache groupCache = getManager().getSubjects(PermissionsEx.SUBJECTS_GROUP);
        ImmutableSubjectData parentData = groupCache.getData("parent", null);
        parentData = parentData.setPermission(GLOBAL_CONTEXT, "#test.permission.parent", 1);
        groupCache.set("parent", parentData);
        ImmutableSubjectData childData = groupCache.getData("child", null);
        childData = childData.addParent(GLOBAL_CONTEXT, groupCache.getType(), "parent")
                             .setPermission(GLOBAL_CONTEXT, "#test.permission.child", 1);
        groupCache.set("child", childData);
        ImmutableSubjectData subjectData = groupCache.getData("subject", null);
        subjectData = subjectData.addParent(GLOBAL_CONTEXT, groupCache.getType(), "child");
        groupCache.set("subject", subjectData);

        CalculatedSubject calculatedParent = getManager().getCalculatedSubject(groupCache.getType(), "parent"),
                            calculatedChild = getManager().getCalculatedSubject(groupCache.getType(), "child"),
                            calculatedSubject = getManager().getCalculatedSubject(groupCache.getType(), "subject");

        assertEquals(1, calculatedParent.getPermissions(GLOBAL_CONTEXT).get("test.permission.parent"));
        assertEquals(1, calculatedChild.getPermissions(GLOBAL_CONTEXT).get("test.permission.parent"));
        assertEquals(1, calculatedChild.getPermissions(GLOBAL_CONTEXT).get("test.permission.child"));
        assertEquals(0, calculatedSubject.getPermissions(GLOBAL_CONTEXT).get("test.permission.parent"));
        assertEquals(1, calculatedSubject.getPermissions(GLOBAL_CONTEXT).get("test.permission.child"));
    }
}
