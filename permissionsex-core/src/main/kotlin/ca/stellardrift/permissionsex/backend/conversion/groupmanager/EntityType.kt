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

import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.kotlin.get

enum class EntityType {
    USER {
        override val inheritanceKey: String
            get() = "group"

        override fun getGlobalNode(dataStore: GroupManagerDataStore): ConfigurationNode? = null

        override fun getWorldNode(dataStore: GroupManagerDataStore, world: String): ConfigurationNode? {
            val worldPair = dataStore.getUserGroupsConfigForWorld(world) ?: return null
            return worldPair.user
        }

        override fun getNodeForSubject(root: ConfigurationNode, name: String): ConfigurationNode = root[name]
    },
    GROUP {
        override fun getGlobalNode(dataStore: GroupManagerDataStore): ConfigurationNode = dataStore.globalGroups

        override fun getWorldNode(dataStore: GroupManagerDataStore, world: String): ConfigurationNode? {
            val worldPair = dataStore.getUserGroupsConfigForWorld(world) ?: return null
            return worldPair.group
        }

        override fun getNodeForSubject(root: ConfigurationNode, name: String): ConfigurationNode {
            val ret = root[name]
            if (ret.isVirtual) {
                val global = root["g:$name"]
                if (!global.isVirtual) {
                    return global
                }
            }
            return ret
        }
    },
    OTHER {
        override fun getGlobalNode(dataStore: GroupManagerDataStore): ConfigurationNode? = null
        override fun getWorldNode(dataStore: GroupManagerDataStore, world: String): ConfigurationNode? = null
        override fun getNodeForSubject(root: ConfigurationNode, name: String): ConfigurationNode? = null
    };

    open val inheritanceKey: String
        get() = "inheritance"

    abstract fun getGlobalNode(dataStore: GroupManagerDataStore): ConfigurationNode?
    abstract fun getWorldNode(dataStore: GroupManagerDataStore, world: String): ConfigurationNode?
    abstract fun getNodeForSubject(root: ConfigurationNode, name: String): ConfigurationNode?

    companion object {

        fun forTypeString(type: String): EntityType {
            return when (type) {
                "user" -> USER
                "group" -> GROUP
                else -> OTHER
            }
        }
    }
}
