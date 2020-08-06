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

import ca.stellardrift.permissionsex.PermissionsEx
import ca.stellardrift.permissionsex.context.ContextValue
import ca.stellardrift.permissionsex.subject.CalculatedSubject
import ca.stellardrift.permissionsex.util.subjectIdentifier
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import net.milkbowl.vault.permission.Permission
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player

internal class PEXVault(val pex: PermissionsExPlugin) : Permission() {
    init {
        this.plugin = pex
    }

    override fun getName(): String = this.pex.name
    override fun isEnabled(): Boolean = this.pex.isEnabled
    override fun hasSuperPermsCompat(): Boolean = true
    override fun hasGroupSupport(): Boolean = true

    override fun getGroups(): Array<String> = this.pex.groupSubjects.allIdentifiers.toTypedArray()

    private fun <T> CompletableFuture<T>.getUnchecked(): T {
        return try {
            get()
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        } catch (e: ExecutionException) {
            throw RuntimeException(e)
        }
    }

    fun getGroup(name: String): CalculatedSubject {
        return this.pex.groupSubjects[name].getUnchecked()
    }

    fun getSubject(player: OfflinePlayer): CalculatedSubject {
        return this.pex.userSubjects[player.uniqueId.toString()].getUnchecked()
    }

    fun getSubject(player: String): CalculatedSubject {
        return this.pex.userSubjects[player].getUnchecked()
    }

    fun contextsFrom(world: String?): Set<ContextValue<*>> {
        return if (world == null) {
            PermissionsEx.GLOBAL_CONTEXT
        } else {
            setOf(WorldContextDefinition.createValue(world))
        }
    }

    /**
     * Get an active world from an offline player if possible
     *
     * @param player The offline player object
     * @return Maybe a world?
     */
    fun getActiveWorld(player: OfflinePlayer): String? {
        val ply = player.player
        return ply?.world?.name
    }

    /**
     * This will replace the world in the given subject's active contexts.
     * Unfortunately, PEX's context system does not map perfectly onto Vault's idea of per-world permissions,
     * so this may not perfectly match cases where the world being queried does not match an online player's current world.
     *
     * @param subject The subject to provide active contexts for
     * @param worldOverride The world to override with, if any
     * @return The appropriate context values
     */
    fun contextsFrom(subject: CalculatedSubject, worldOverride: String?): Set<ContextValue<*>> {
        val origContexts = subject.activeContexts
        if (worldOverride == null) {
            return origContexts
        }
        if ((subject.associatedObject as? Player)?.world?.name.equals(worldOverride, ignoreCase = true) == true) {
            return origContexts
        }
        origContexts.removeIf { it.definition === WorldContextDefinition }
        origContexts.add(WorldContextDefinition.createValue(worldOverride))
        return origContexts
    }

    override fun groupHas(world: String, name: String, permission: String): Boolean {
        val subj = getGroup(name)
        return subj.getPermission(contextsFrom(subj, world), permission) > 0
    }

    override fun groupAdd(world: String, name: String, permission: String): Boolean {
        return !getGroup(name).data()
            .update { it.setPermission(contextsFrom(world), permission, 1) }.isCancelled
    }

    override fun groupRemove(world: String, name: String, permission: String): Boolean {
        return !getGroup(name).data()
            .update { it.setPermission(contextsFrom(world), permission, 0) }.isCancelled
    }

    override fun playerHas(world: String, player: OfflinePlayer, permission: String): Boolean {
        val subj = getSubject(player)
        val perm = subj.getPermission(contextsFrom(subj, world), permission)
        return when {
            perm > 0 -> true
            perm < 0 -> false
            else -> this.pex.manager.config.platformConfig.fallbackOp && player.isOp
        }
    }

    override fun playerAdd(world: String, player: OfflinePlayer, permission: String): Boolean {
        return !getSubject(player).data()
            .update { it.setPermission(contextsFrom(world), permission, 1) }.isCancelled
    }

    override fun playerAddTransient(player: OfflinePlayer, permission: String): Boolean {
        return playerAddTransient(getActiveWorld(player)!!, player, permission)
    }

    override fun playerAddTransient(player: Player, permission: String): Boolean {
        return playerAddTransient(player.world.name, player, permission)
    }

    override fun playerAddTransient(worldName: String, player: OfflinePlayer, permission: String): Boolean {
        return !getSubject(player).transientData()
            .update { it.setPermission(contextsFrom(worldName), permission, 1) }.isCancelled
    }

    override fun playerRemoveTransient(worldName: String, player: OfflinePlayer, permission: String): Boolean {
        return !getSubject(player).transientData()
            .update { it.setPermission(contextsFrom(worldName), permission, 0) }.isCancelled
    }

    override fun playerRemove(world: String, player: OfflinePlayer, permission: String): Boolean {
        return !getSubject(player).data()
            .update { it.setPermission(contextsFrom(world), permission, 0) }.isCancelled
    }

    override fun playerRemoveTransient(player: Player, permission: String): Boolean {
        return playerRemoveTransient(player.world.name, player, permission)
    }

    override fun playerRemoveTransient(player: OfflinePlayer, permission: String): Boolean {
        return playerRemoveTransient(getActiveWorld(player)!!, player, permission)
    }

    override fun playerInGroup(world: String, player: OfflinePlayer, group: String): Boolean {
        val subj = getSubject(player)
        return subjectIdentifier(PermissionsEx.SUBJECTS_GROUP, group) in subj.getParents(contextsFrom(subj, world))
    }

    override fun playerAddGroup(world: String, player: OfflinePlayer, group: String): Boolean {
        return !getSubject(player).data()
            .update { it.addParent(contextsFrom(world), PermissionsEx.SUBJECTS_GROUP, group) }.isCancelled
    }

    override fun playerRemoveGroup(world: String, player: OfflinePlayer, group: String): Boolean {
        return !getSubject(player).data()
            .update { it.removeParent(contextsFrom(world), PermissionsEx.SUBJECTS_GROUP, group) }.isCancelled
    }

    override fun getPlayerGroups(world: String, player: OfflinePlayer): Array<String> {
        val subj = getSubject(player)
        return subj.getParents(contextsFrom(subj, world))
            .mapNotNull { (key, value) ->
            if (key == PermissionsEx.SUBJECTS_GROUP) value else null
        }.toTypedArray()
    }

    override fun getPrimaryGroup(world: String, player: OfflinePlayer): String {
        val groups = getPlayerGroups(world, player)
        return (if (!groups.isEmpty()) groups[0] else null)!!
    }

    // -- Deprecated methods
    private fun pFromName(name: String): OfflinePlayer {
        return this.plugin.server.getOfflinePlayer(name)
    }

    override fun playerHas(world: String, name: String, permission: String): Boolean {
        return playerHas(world, pFromName(name), permission)
    }

    override fun playerAdd(world: String, name: String, permission: String): Boolean {
        return playerAdd(world, pFromName(name), permission)
    }

    override fun playerRemove(world: String, name: String, permission: String): Boolean {
        return playerRemove(world, pFromName(name), permission)
    }

    override fun playerInGroup(world: String, player: String, group: String): Boolean {
        return playerInGroup(world, pFromName(player), group)
    }

    override fun playerAddGroup(world: String, player: String, group: String): Boolean {
        return playerAddGroup(world, pFromName(player), group)
    }

    override fun playerRemoveGroup(world: String, player: String, group: String): Boolean {
        return playerRemoveGroup(world, pFromName(player), group)
    }

    override fun getPlayerGroups(world: String, player: String): Array<String> {
        return getPlayerGroups(world, pFromName(player))
    }

    override fun getPrimaryGroup(world: String, player: String): String {
        return getPrimaryGroup(world, pFromName(player))
    }
}
