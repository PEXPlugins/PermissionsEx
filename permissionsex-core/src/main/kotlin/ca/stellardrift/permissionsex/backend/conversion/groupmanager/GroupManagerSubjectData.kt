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

import ca.stellardrift.permissionsex.PermissionsEx
import ca.stellardrift.permissionsex.backend.ConversionUtils
import ca.stellardrift.permissionsex.backend.conversion.ReadOnlySubjectData
import ca.stellardrift.permissionsex.context.ContextValue
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Maps
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.kotlin.get
import ninja.leaping.configurate.objectmapping.ObjectMappingException

class GroupManagerSubjectData(
    private val identifier: String,
    private val dataStore: GroupManagerDataStore,
    private val type: EntityType
) : ReadOnlySubjectData() {

    private fun getNodeForContexts(contexts: Set<ContextValue<*>>): ConfigurationNode? {
        if (!isValidContexts(contexts)) {
            return null
        }

        val rootNode = if (contexts.isEmpty()) {
            this.type.getGlobalNode(this.dataStore)
        } else {
            this.type.getWorldNode(this.dataStore, contexts.iterator().next().rawValue)
        }

        if (rootNode != null) {
            val ret = this.type.getNodeForSubject(rootNode, this.identifier)
            if (ret != null && !ret.isVirtual) {
                return ret
            }
        }

        return null
    }

    override fun getAllOptions(): Map<Set<ContextValue<*>>, Map<String, String>> {
        return activeContexts.associateWith { getOptions(it) }
            .filterValues { it.isNotEmpty() }
    }

    override fun getOptions(contexts: Set<ContextValue<*>>): Map<String, String> {
        val specificNode = getNodeForContexts(contexts) ?: return emptyMap()
        return try {
            specificNode["info"].get<Map<String, String>>(emptyMap())
        } catch (e: ObjectMappingException) {
            emptyMap()
        }

    }

    override fun getAllPermissions(): Map<Set<ContextValue<*>>, Map<String, Int>> {
        return activeContexts.associateWith { getPermissions(it) }
            .filterValues { it.isNotEmpty() }
    }

    override fun getPermissions(contexts: Set<ContextValue<*>>): Map<String, Int> {
        val specificNode = getNodeForContexts(contexts) ?: return emptyMap()
        val ret = hashMapOf<String, Int>()
        for (node in specificNode["permissions"].childrenList) {
            var perm: String = node.string ?: continue
            if (perm == "*") {
                continue
            }

            var value = 1
            if (perm.startsWith("-")) {
                value = -1
                perm = perm.substring(1)
            }
            perm = ConversionUtils.convertLegacyPermission(perm)
            ret[perm] = value
        }
        return ret
    }

    override fun getAllParents(): Map<Set<ContextValue<*>>, List<Map.Entry<String, String>>> {
        return activeContexts.associateWith { getParents(it) }
            .filterValues { it.isNotEmpty() }
    }

    override fun getParents(contexts: Set<ContextValue<*>>): List<Map.Entry<String, String>> {
        val specificNode = getNodeForContexts(contexts) ?: return emptyList()

        return try {
            specificNode[this.type.inheritanceKey].get<List<String>>(emptyList())
                .map {
                    var name = it
                    if (name.startsWith("g:")) {
                        name = name.substring(2)
                    }
                    Maps.immutableEntry(PermissionsEx.SUBJECTS_GROUP, name)
                }
        } catch (e: ObjectMappingException) {
            emptyList()
        }

    }

    override fun getDefaultValue(contexts: Set<ContextValue<*>>): Int {
        val specificNode = getNodeForContexts(contexts) ?: return 0
        val values = specificNode["permissions"].getList { it }
        if ("*" in values) {
            return 1
        } else if ("-*" in values) {
            return -1
        }

        return 0
    }

    override fun getActiveContexts(): Set<Set<ContextValue<*>>> {
        val activeContextsBuilder = ImmutableSet.builder<Set<ContextValue<*>>>()
        if (getNodeForContexts(PermissionsEx.GLOBAL_CONTEXT) != null) {
            activeContextsBuilder.add(PermissionsEx.GLOBAL_CONTEXT)
        }

        for (world in this.dataStore.knownWorlds) {
            val worldContext = setOf(ContextValue<Any>("world", world))
            if (getNodeForContexts(worldContext) != null) {
                activeContextsBuilder.add(worldContext)
            }
        }

        return activeContextsBuilder.build()
    }

    override fun getAllDefaultValues(): Map<Set<ContextValue<*>>, Int> {
        return activeContexts.associateWith { getDefaultValue(it) }
            .filterValues { it != 0 }
    }
}

private fun isValidContexts(contexts: Set<ContextValue<*>>): Boolean {
    if (contexts.size == 1 && contexts.iterator().next().key == "world") {
        return true
    } else if (contexts.isEmpty()) {
        return true
    }
    return false
}
