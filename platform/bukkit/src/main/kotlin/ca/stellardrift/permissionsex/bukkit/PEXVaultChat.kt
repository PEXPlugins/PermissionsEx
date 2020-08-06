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

import net.milkbowl.vault.chat.Chat
import org.bukkit.OfflinePlayer

private const val OPTION_PREFIX = "prefix"
private const val OPTION_SUFFIX = "suffix"

internal class PEXVaultChat(private val perms: PEXVault) : Chat(perms) {
    private val plugin: PermissionsExPlugin = perms.pex

    override fun getName(): String = this.plugin.name
    override fun isEnabled(): Boolean = this.plugin.isEnabled

    override fun getGroupInfoString(world: String, name: String, key: String, defaultValue: String?): String? {
        val subj = perms.getGroup(name)
        return subj.getOption(perms.contextsFrom(subj, world), key).orElse(defaultValue)
    }

    override fun setGroupInfoString(world: String, name: String, key: String, value: String?) {
        perms.getSubject(name).data()
            .update { it.setOption(perms.contextsFrom(world), key, value) }
    }

    override fun getPlayerInfoString(world: String, player: OfflinePlayer, node: String, defaultValue: String?): String? {
        val subj = perms.getSubject(player)
        return subj.getOption(perms.contextsFrom(subj, world), node).orElse(defaultValue)
    }

    override fun setPlayerInfoString(world: String, player: OfflinePlayer, node: String, value: String?) {
        perms.getSubject(player).data()
            .update { it.setOption(perms.contextsFrom(world), node, value) }
    }

    // -- Passthrough methods
    override fun getGroupPrefix(world: String, name: String): String? {
        return getGroupInfoString(world, name, OPTION_PREFIX, null)
    }

    override fun setGroupPrefix(world: String, name: String, prefix: String) {
        setGroupInfoString(world, name, OPTION_PREFIX, prefix)
    }

    override fun getGroupSuffix(world: String, name: String): String? {
        return getGroupInfoString(world, name, OPTION_SUFFIX, null)
    }

    override fun setGroupSuffix(world: String, name: String, suffix: String) {
        setGroupInfoString(world, name, OPTION_SUFFIX, suffix)
    }

    override fun getGroupInfoInteger(world: String, name: String, node: String, defaultValue: Int): Int {
        return try {
            getGroupInfoString(world, name, node, null)?.toInt() ?: defaultValue
        } catch (e: NumberFormatException) {
            defaultValue
        }
    }

    override fun setGroupInfoInteger(world: String, name: String, node: String, value: Int) {
        setGroupInfoString(world, name, node, value.toString())
    }

    override fun getGroupInfoDouble(world: String, name: String, node: String, defaultValue: Double): Double {
        return try {
            getGroupInfoString(world, name, node, null)?.toDouble() ?: defaultValue
        } catch (e: NumberFormatException) {
            defaultValue
        }
    }

    override fun setGroupInfoDouble(world: String, name: String, node: String, value: Double) {
        setGroupInfoString(world, name, node, value.toString())
    }

    override fun getGroupInfoBoolean(world: String, name: String, node: String, defaultValue: Boolean): Boolean {
        return getGroupInfoString(world, name, node, null)?.toBoolean() ?: defaultValue
    }

    override fun setGroupInfoBoolean(world: String, name: String, node: String, value: Boolean) {
        setGroupInfoString(world, name, node, value.toString())
    }

    override fun getPlayerPrefix(world: String, player: OfflinePlayer): String? {
        return getPlayerInfoString(world, player, OPTION_PREFIX, null)
    }

    override fun setPlayerPrefix(world: String, player: OfflinePlayer, prefix: String) {
        setPlayerInfoString(world, player, OPTION_PREFIX, prefix)
    }

    override fun getPlayerSuffix(world: String, player: OfflinePlayer): String? {
        return getPlayerInfoString(world, player, OPTION_SUFFIX, null)
    }

    override fun setPlayerSuffix(world: String, player: OfflinePlayer, suffix: String) {
        setPlayerInfoString(world, player, OPTION_SUFFIX, suffix)
    }

    override fun getPlayerInfoInteger(world: String, player: OfflinePlayer, node: String, defaultValue: Int): Int {
        return try {
            getPlayerInfoString(world, player, node, null)?.toInt() ?: defaultValue
        } catch (e: NumberFormatException) {
            defaultValue
        }
    }

    override fun setPlayerInfoInteger(world: String, player: OfflinePlayer, node: String, value: Int) {
        setPlayerInfoString(world, player, node, value.toString())
    }

    override fun getPlayerInfoDouble(world: String, player: OfflinePlayer, node: String, defaultValue: Double): Double {
        return try {
            getPlayerInfoString(world, player, node, null)?.toDouble() ?: defaultValue
        } catch (e: NumberFormatException) {
            defaultValue
        }
    }

    override fun setPlayerInfoDouble(world: String, player: OfflinePlayer, node: String, value: Double) {
        setPlayerInfoString(world, player, node, value.toString())
    }

    override fun getPlayerInfoBoolean(world: String, player: OfflinePlayer, node: String, defaultValue: Boolean): Boolean {
        return getPlayerInfoString(world, player, node, null)?.toBoolean() ?: defaultValue
    }

    override fun setPlayerInfoBoolean(world: String, player: OfflinePlayer, node: String, value: Boolean) {
        setPlayerInfoString(world, player, node, value.toString())
    }

    // -- Deprecated stuff
    private fun pFromName(name: String): OfflinePlayer {
        return plugin.server.getOfflinePlayer(name)
    }

    override fun getPlayerInfoInteger(world: String, name: String, node: String, defaultValue: Int): Int {
        return getPlayerInfoInteger(world, pFromName(name), node, defaultValue)
    }

    override fun setPlayerInfoInteger(world: String, name: String, node: String, value: Int) {
        setPlayerInfoInteger(world, pFromName(name), node, value)
    }

    @Deprecated("")
    override fun getPlayerInfoString(world: String, name: String, node: String, defaultValue: String?): String? {
        return getPlayerInfoString(world, pFromName(name), node, defaultValue)
    }

    override fun setPlayerInfoString(world: String, name: String, node: String, value: String) {
        setPlayerInfoString(world, pFromName(name), node, value)
    }

    override fun getPlayerInfoBoolean(world: String, name: String, node: String, defaultValue: Boolean): Boolean {
        return getPlayerInfoBoolean(world, pFromName(name), node, defaultValue)
    }

    override fun setPlayerInfoBoolean(world: String, name: String, node: String, value: Boolean) {
        setPlayerInfoBoolean(world, pFromName(name), node, value)
    }

    override fun getPlayerInfoDouble(world: String, name: String, node: String, defaultValue: Double): Double {
        return getPlayerInfoDouble(world, pFromName(name), node, defaultValue)
    }

    override fun setPlayerInfoDouble(world: String, name: String, node: String, value: Double) {
        setPlayerInfoDouble(world, pFromName(name), node, value)
    }

    override fun getPlayerPrefix(world: String, name: String): String? {
        return getPlayerPrefix(world, pFromName(name))
    }

    override fun setPlayerPrefix(world: String, name: String, prefix: String) {
        setPlayerPrefix(world, pFromName(name), prefix)
    }

    override fun getPlayerSuffix(world: String, name: String): String? {
        return getPlayerSuffix(world, pFromName(name))
    }

    override fun setPlayerSuffix(world: String, name: String, suffix: String) {
        setPlayerSuffix(world, pFromName(name), suffix)
    }
}
