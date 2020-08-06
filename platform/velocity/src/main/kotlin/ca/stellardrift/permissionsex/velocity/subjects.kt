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

package ca.stellardrift.permissionsex.velocity

import ca.stellardrift.permissionsex.PermissionsEx.SUBJECTS_USER
import ca.stellardrift.permissionsex.proxycommon.IDENT_SERVER_CONSOLE
import ca.stellardrift.permissionsex.subject.CalculatedSubject
import ca.stellardrift.permissionsex.subject.SubjectTypeDefinition
import com.google.common.collect.Maps
import com.velocitypowered.api.permission.PermissionFunction
import com.velocitypowered.api.permission.PermissionSubject
import com.velocitypowered.api.permission.Tristate
import com.velocitypowered.api.proxy.Player
import java.util.UUID

class UserSubjectTypeDefinition(private val plugin: PermissionsExPlugin) : SubjectTypeDefinition<Player>(SUBJECTS_USER) {
    override fun isNameValid(name: String): Boolean {
        return try {
            UUID.fromString(name)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun getAliasForName(name: String): String? {
        return try {
            UUID.fromString(name)
            null
        } catch (e: Exception) {
            plugin.server.getPlayer(name).orElse(null)?.run { uniqueId.toString() }
        }
    }

    override fun getAssociatedObject(identifier: String): Player? {
        return try {
            val id = UUID.fromString(identifier)
            plugin.server.getPlayer(id).orElse(null)
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}

class PEXPermissionFunction(val plugin: PermissionsExPlugin, private val source: PermissionSubject) : PermissionFunction {
    val subject: CalculatedSubject by lazy {
        val ident = when (source) {
            is Player -> Maps.immutableEntry(SUBJECTS_USER, source.gameProfile.id.toString())
            else -> IDENT_SERVER_CONSOLE
        }
        plugin.manager.getSubjects(ident.key)[ident.value].join()
    }

    override fun getPermissionValue(permission: String): Tristate {
        return subject.getPermission(permission).asTristate()
    }
}
