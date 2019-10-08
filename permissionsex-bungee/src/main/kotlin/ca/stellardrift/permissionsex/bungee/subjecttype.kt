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

package ca.stellardrift.permissionsex.bungee

import ca.stellardrift.permissionsex.PermissionsEx.SUBJECTS_USER
import ca.stellardrift.permissionsex.subject.SubjectTypeDefinition
import net.md_5.bungee.api.connection.ProxiedPlayer
import java.util.Optional
import java.util.UUID

class UserSubjectTypeDefinition(private val plugin: PermissionsExPlugin) : SubjectTypeDefinition<ProxiedPlayer>(SUBJECTS_USER) {
    override fun isNameValid(name: String): Boolean {
        return try {
            UUID.fromString(name)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun getAliasForName(name: String): Optional<String> {
        return try {
            UUID.fromString(name)
            Optional.empty()
        } catch (e: Exception) {
            Optional.ofNullable(plugin.proxy.getPlayer(name)?.uniqueId?.toString())
        }
    }

    override fun getAssociatedObject(identifier: String): Optional<ProxiedPlayer> {
        return try {
            val id = UUID.fromString(identifier)
            Optional.ofNullable(plugin.proxy.getPlayer(id))
        } catch (e: IllegalArgumentException) {
            Optional.empty()
        }
    }

}
