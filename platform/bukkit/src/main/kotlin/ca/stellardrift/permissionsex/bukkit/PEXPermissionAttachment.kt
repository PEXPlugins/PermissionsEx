/*
 * PermissionsEx - a permissions plugin for your server ecosystem
 * Copyright © 2020 zml [at] stellardrift [dot] ca and PermissionsEx contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package ca.stellardrift.permissionsex.bukkit

import ca.stellardrift.permissionsex.impl.PermissionsEx
import ca.stellardrift.permissionsex.subject.SubjectRef
import ca.stellardrift.permissionsex.subject.SubjectType
import java.util.UUID
import org.bukkit.entity.Player
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionAttachment
import org.bukkit.plugin.Plugin

internal val ATTACHMENT_TYPE = SubjectType.builder("attachment", UUID::class.java)
    .serializedBy(UUID::toString)
    .deserializedBy(UUID::fromString)
    .build()

/**
 * Permissions attachment that integrates with the PEX backend
 */
internal class PEXPermissionAttachment(plugin: Plugin, parent: Player, private val perm: PEXPermissible) : PermissionAttachment(plugin, parent), SubjectRef<UUID> {
    private val identifier = UUID.randomUUID()
    private val subjectData: SubjectRef.ToData<UUID> =
        perm.manager.subjects(ATTACHMENT_TYPE).transientData().referenceTo(identifier).join()

    init {
        subjectData.update { it.setOption(PermissionsEx.GLOBAL_CONTEXT, "plugin", this.plugin.name) }
    }

    override fun type(): SubjectType<UUID> {
        return ATTACHMENT_TYPE
    }

    override fun identifier(): UUID {
        return this.identifier
    }

    override fun getPermissions(): Map<String, Boolean> {
        return subjectData.get().getPermissions(PermissionsEx.GLOBAL_CONTEXT)
            .mapValues { (_, v) -> v > 0 }
    }

    override fun setPermission(name: String, value: Boolean) {
        subjectData.update {
            it.setPermission(PermissionsEx.GLOBAL_CONTEXT, name, if (value) 1 else -1)
        }
    }

    override fun setPermission(perm: Permission, value: Boolean) = setPermission(perm.name, value)

    override fun unsetPermission(name: String) {
        subjectData.update {
            it.setPermission(PermissionsEx.GLOBAL_CONTEXT, name, 0)
        }
    }

    override fun unsetPermission(perm: Permission) = unsetPermission(perm.name)

    override fun remove(): Boolean {
        subjectData.update { null }
        return perm.removeAttachmentInternal(this)
    }
}
