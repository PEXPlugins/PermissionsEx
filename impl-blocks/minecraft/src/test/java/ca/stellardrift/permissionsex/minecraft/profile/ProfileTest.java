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
package ca.stellardrift.permissionsex.minecraft.profile;

import ca.stellardrift.permissionsex.minecraft.profile.MinecraftProfile;
import ca.stellardrift.permissionsex.minecraft.profile.ProfileApiResolver;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProfileTest {
    @Test
    @Disabled("Makes network requests")
    void integrationTest() {
        final ProfileApiResolver resolver = ProfileApiResolver.resolver(ForkJoinPool.commonPool());
        final Map<String, MinecraftProfile> results = resolver.resolveByName(Arrays.asList("zml", "waylon531", "toolongsothiswontmatchanybody"))
                .collectMap(MinecraftProfile::name)
                .block();

        assertEquals(2, results.size());

        assertEquals(MinecraftProfile.of(UUID.fromString("2f224fdf-ca9a-4043-8166-0d673ba4c0b8"), "zml"), results.get("zml"));
        assertEquals(MinecraftProfile.of(UUID.fromString("5e63604d-4fda-40d8-a21c-ccfea1e51315"), "waylon531"), results.get("waylon531"));
    }
}
