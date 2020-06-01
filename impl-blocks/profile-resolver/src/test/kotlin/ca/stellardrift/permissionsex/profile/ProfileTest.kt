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

package ca.stellardrift.permissionsex.profile

import ca.stellardrift.permissionsex.util.MinecraftProfile
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class ProfileTest {
    @Test
    @Disabled("Makes network requests")
    fun integrationTest() {
        val results = ConcurrentHashMap<String, MinecraftProfile>()
        val numProfiles = lookupMinecraftProfilesByName(listOf("zml", "waylon531", "toolongsothiswontmatchanybody")) {
            results[it.name] = it
            CompletableFuture.completedFuture(null)
        }.join()

        assertEquals(2, numProfiles)

        assertEquals(MinecraftProfileImpl("zml", UUID.fromString("2f224fdf-ca9a-4043-8166-0d673ba4c0b8")), results["zml"])
        assertEquals(MinecraftProfileImpl("waylon531", UUID.fromString("5e63604d-4fda-40d8-a21c-ccfea1e51315")), results["waylon531"])
    }
}
