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
package ca.stellardrift.permissionsex.fabric.impl

import ca.stellardrift.permissionsex.util.CachingValue
import java.util.function.Supplier
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity

fun <Value> tickCachedValue(server: MinecraftServer, maxDelta: Long, update: Supplier<Value>): CachingValue<Value> {
    return CachingValue({ server.ticks.toLong() }, maxDelta, update)
}

fun <R> ServerCommandSource.ifPlayer(operation: (ServerPlayerEntity) -> R): R? {
    val ent = this.entity
    if (ent is ServerPlayerEntity) {
        return operation(ent)
    }
    return null
}
