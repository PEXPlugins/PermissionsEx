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

package ca.stellardrift.permissionsex.backend;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class ConversionUtilsTest {
    @Test
    public void testConvertPermission() {
        final Map<String, String> expectedConversions = ImmutableMap.of(
                "permissions.*", "permissions",
                "worldedit.navigation.(jumpto|thru).*", "worldedit.navigation.{jumpto,thru}",
                "worldedit.navigation.(jumpto|thru).(tool|command)", "worldedit.navigation.{jumpto,thru}.{tool,command}",
                "worldedit.navigation.(jumpto.*", "worldedit.navigation.(jumpto"
        );
        for (Map.Entry<String, String> ent : expectedConversions.entrySet()) {
            assertEquals(ent.getValue(), ConversionUtils.convertLegacyPermission(ent.getKey()));
        }
    }
}
