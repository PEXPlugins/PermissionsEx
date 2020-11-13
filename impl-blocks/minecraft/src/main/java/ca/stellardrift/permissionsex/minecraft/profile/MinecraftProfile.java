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

import com.google.gson.annotations.SerializedName;
import net.kyori.adventure.identity.Identity;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

import java.util.UUID;

/**
 * A profile containing user data for a Minecraft user.
 *
 * @since 2.0.0
 */
@Gson.TypeAdapters
@Value.Immutable(copy = false)
public interface MinecraftProfile extends Identity {

    /**
     * Create a new builder for a profile object.
     * @return new profile builder
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Create a new profile with the provided parameters.
     *
     * @param uuid unique ID
     * @param name player name
     * @return a new profile
     * @since 2.0.0
     */
    static MinecraftProfile of(final UUID uuid, final String name) {
        return new MinecraftProfileImpl(uuid, name);
    }

    /**
     * The unique identifier for a user.
     *
     * @return user id
     * @since 2.0.0
     */
    @Override
    @Value.Parameter
    @SerializedName("id")
    UUID uuid();

    /**
     * The changeable name for a user.
     *
     * @return user name
     * @since 2.0.0
     */
    @Value.Parameter
    String name();

    class Builder extends MinecraftProfileImpl.Builder {}
}
