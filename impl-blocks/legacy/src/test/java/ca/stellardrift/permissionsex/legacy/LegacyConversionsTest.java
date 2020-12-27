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
package ca.stellardrift.permissionsex.legacy;

import org.junit.jupiter.api.Test;
import org.pcollections.HashTreePMap;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyConversionsTest {
    @Test
    public void testConvertPermission() {
        final Map<String, String> expectedConversions = HashTreePMap.<String, String>empty()
                .plus("permissions.*", "permissions")
                .plus("worldedit.navigation.(jumpto|thru).*", "worldedit.navigation.{jumpto,thru}")
                .plus("worldedit.navigation.(jumpto|thru).(tool|command)", "worldedit.navigation.{jumpto,thru}.{tool,command}")
                .plus("worldedit.navigation.(jumpto.*", "worldedit.navigation.(jumpto");
        for (Map.Entry<String, String> ent : expectedConversions.entrySet()) {
            assertEquals(ent.getValue(), LegacyConversions.convertLegacyPermission(ent.getKey()));
        }
    }
}
