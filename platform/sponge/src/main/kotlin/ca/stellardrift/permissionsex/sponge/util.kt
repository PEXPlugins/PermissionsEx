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

package ca.stellardrift.permissionsex.sponge

import ca.stellardrift.permissionsex.util.MinecraftProfile
import java.util.UUID
import net.kyori.text.Component
import net.kyori.text.serializer.gson.GsonComponentSerializer
import org.spongepowered.api.profile.GameProfile
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.serializer.TextSerializers

class SpongeMinecraftProfile(private val profile: GameProfile) : MinecraftProfile {
    override val name: String
        get() = profile.name.get()
    override val uuid: UUID
        get() = profile.uniqueId
}

fun Component.toSponge(): Text {
    return TextSerializers.JSON.deserialize(GsonComponentSerializer.INSTANCE.serialize(this))
}

fun Text.toComponent(): Component {
    return GsonComponentSerializer.INSTANCE.deserialize(TextSerializers.JSON.serialize(this))
}
