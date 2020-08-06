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
import java.util.Optional
import java.util.UUID
import org.bukkit.entity.Player

/**
 * Metadata for user types
 */
class UserSubjectTypeDescription(typeName: String, private val plugin: PermissionsExPlugin) : SubjectTypeDefinition<Player>(typeName) {
    override fun isNameValid(name: String): Boolean {
        return try {
            UUID.fromString(name)
            true
        } catch (ex: IllegalArgumentException) {
            false
        }
    }

    override fun getAliasForName(input: String): Optional<String> {
        try {
            UUID.fromString(input)
        } catch (ex: IllegalArgumentException) {
            val player = plugin.server.getPlayer(input)
            if (player != null) {
                return Optional.of(player.uniqueId.toString())
            } else {
                val offline = plugin.server.getOfflinePlayer(input)
                if (offline != null && offline.uniqueId != null) {
                    return Optional.of(offline.uniqueId.toString())
                }
            }
        }
        return Optional.empty()
    }

    override fun getAssociatedObject(identifier: String): Optional<Player> {
        return try {
            Optional.ofNullable(plugin.server.getPlayer(UUID.fromString(identifier)))
        } catch (ex: IllegalArgumentException) { // not a valid user ID
            Optional.empty()
        }
    }
}
