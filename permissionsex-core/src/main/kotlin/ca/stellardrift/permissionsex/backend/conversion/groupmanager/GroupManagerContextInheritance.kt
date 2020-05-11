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

package ca.stellardrift.permissionsex.backend.conversion.groupmanager

import ca.stellardrift.permissionsex.backend.memory.MemoryContextInheritance
import ca.stellardrift.permissionsex.context.ContextValue
import ca.stellardrift.permissionsex.data.ContextInheritance
import ninja.leaping.configurate.ConfigurationNode
import java.util.ArrayList

class GroupManagerContextInheritance(mirrorsNode: ConfigurationNode) : ContextInheritance {
    private val worlds: MutableMap<String, MutableList<ContextValue<*>>> = hashMapOf()

    init {
        for ((key, value) in mirrorsNode.childrenMap) {
            val worldContext = ContextValue<String>("world", key.toString())
            for (child in value.childrenMap.keys) {
                val world = worlds.computeIfAbsent(child.toString()) { ArrayList() }
                world.add(worldContext)
            }
        }
    }

    override fun getParents(context: ContextValue<*>): List<ContextValue<*>> {
        return if (context.key == "world") {
            worlds[context.rawValue] ?: return emptyList()
        } else {
            emptyList()
        }
    }

    override fun setParents(context: ContextValue<*>, parents: List<ContextValue<*>>): ContextInheritance = this

    override fun getAllParents(): Map<ContextValue<*>, List<ContextValue<*>>> {
        return worlds.mapKeys { MemoryContextInheritance.ctxFromString(it.key) }
    }
}
