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

package ca.stellardrift.permissionsex.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import reactor.core.publisher.Flux
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class ProfileTest {
    @Test
    //@Disabled("Makes network requests")
    fun integrationTest() = runBlocking {
        val profiles = resolveMinecraftProfile(
            Flux.just(
                "zml",
                "waylon531",
                "toolongsothiswontmatchanybody"
            )
        )
            .collectMap(MinecraftProfile::name).awaitSingle()

        assertEquals(2, profiles.size)

        assertEquals(
            MinecraftProfileImpl(
                "zml",
                UUID.fromString("2f224fdf-ca9a-4043-8166-0d673ba4c0b8")
            ), profiles["zml"])
        assertEquals(
            MinecraftProfileImpl(
                "waylon531",
                UUID.fromString("5e63604d-4fda-40d8-a21c-ccfea1e51315")
            ), profiles["waylon531"])
    }
}
