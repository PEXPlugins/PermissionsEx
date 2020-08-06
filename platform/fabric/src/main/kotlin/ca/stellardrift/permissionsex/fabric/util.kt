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

package ca.stellardrift.permissionsex.fabric

import ca.stellardrift.permissionsex.util.CachingValue
import ca.stellardrift.text.fabric.ComponentCommandOutput
import ca.stellardrift.text.fabric.ComponentPlayer
import java.util.Locale
import net.kyori.text.Component
import net.minecraft.network.MessageType
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.CommandOutput
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity

/**
 * Take a locale string provided from a minecraft client and attempt to parse it as a locale.
 * These are not strictly compliant with the iso standard, so we try to make things a bit more normalized.
 *
 * @return A Locale object matching the provided locale string
 */
fun String.asMCLocale(): Locale {
    val parts = split("_", limit = 3).toTypedArray()
    return when (parts.size) {
        0 -> Locale.getDefault()
        1 -> Locale(parts[0])
        2 -> Locale(parts[0], parts[1])
        3 -> Locale(parts[0], parts[1], parts[2])
        else -> throw IllegalArgumentException("Provided locale '$this' was not in a valid format!")
    }
}

fun <Value> tickCachedValue(server: MinecraftServer, maxDelta: Long, update: () -> Value): CachingValue<Value> {
    return CachingValue({ server.ticks.toLong() }, maxDelta, update)
}

fun <R> ServerCommandSource.ifPlayer(operation: (ServerPlayerEntity) -> R): R? {
    val ent = this.entity
    if (ent is ServerPlayerEntity) {
        return operation(ent)
    }
    return null
}

@JvmName("sendPlayerMessage")
@JvmOverloads
fun ServerPlayerEntity.sendMessage(text: Component, type: MessageType = MessageType.SYSTEM) {
    ComponentPlayer.of(this).sendMessage(text, type)
}

fun CommandOutput.sendMessage(text: Component) {
    ComponentCommandOutput.of(this).sendMessage(text)
}
