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

import ca.stellardrift.permissionsex.PermissionsEx
import ca.stellardrift.permissionsex.context.ContextDefinition
import ca.stellardrift.permissionsex.context.ContextValue
import com.google.common.collect.ImmutableSet
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.spongeapi.SpongeComponentSerializer
import org.spongepowered.api.service.context.Context
import org.spongepowered.api.text.Text

typealias ContextSet = Set<ContextValue<*>>

fun Component.toSponge(): Text {
    return SpongeComponentSerializer.get().serialize(this)
}

fun Text.toAdventure(): Component {
    return SpongeComponentSerializer.get().deserialize(this)
}

fun ContextSet.toSponge(): MutableSet<Context> {
    return mapTo(mutableSetOf()) { Context(it.key(), it.rawValue()) }
}

private fun <T> Context.toPex(def: ContextDefinition<T>): ContextValue<T>? {
    val value = def.deserialize(this.value)
    return if (value == null) null else def.createValue(value)
}

fun Set<Context>.toPex(manager: PermissionsEx<*>): ContextSet {
    val builder = ImmutableSet.builder<ContextValue<*>>()
    for (ctx in this) {
        val def = manager.getContextDefinition(ctx.key, true)
            ?: throw IllegalStateException("A fallback context value was expected!")
        val ctxVal = ctx.toPex(def)
        if (ctxVal != null) {
            builder.add(ctxVal)
        }
    }
    return builder.build()
}
