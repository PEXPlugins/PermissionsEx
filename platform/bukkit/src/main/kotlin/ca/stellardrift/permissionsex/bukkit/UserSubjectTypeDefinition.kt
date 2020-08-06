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

import ca.stellardrift.permissionsex.subject.SubjectTypeDefinition
import java.util.UUID
import org.bukkit.entity.Player

/**
 * Metadata for user types
 */
class UserSubjectTypeDefinition(typeName: String, private val plugin: PermissionsExPlugin) : SubjectTypeDefinition<Player>(typeName) {
    override fun isNameValid(name: String): Boolean {
        return try {
            UUID.fromString(name)
            true
        } catch (ex: IllegalArgumentException) {
            false
        }
    }

    override fun getAliasForName(name: String): String? {
        try {
            UUID.fromString(name)
        } catch (ex: IllegalArgumentException) {
            val player = plugin.server.getPlayer(name)
            if (player != null) {
                return player.uniqueId.toString()
            } else {
                val offline = plugin.server.getOfflinePlayer(name)
                if (offline != null && offline.uniqueId != null) {
                    return offline.uniqueId.toString()
                }
            }
        }
        return null
    }

    override fun getAssociatedObject(identifier: String): Player? {
        return try {
            plugin.server.getPlayer(UUID.fromString(identifier))
        } catch (ex: IllegalArgumentException) { // not a valid user ID
            null
        }
    }
}
