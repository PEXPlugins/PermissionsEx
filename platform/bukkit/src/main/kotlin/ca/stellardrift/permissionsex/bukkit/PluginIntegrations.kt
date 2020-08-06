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

@file:JvmName("PEXPluginIntegrations")

package ca.stellardrift.permissionsex.bukkit

import ca.stellardrift.permissionsex.context.SimpleContextDefinition
import ca.stellardrift.permissionsex.subject.CalculatedSubject
import ca.stellardrift.permissionsex.util.castMap
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.bukkit.WorldGuardPlugin
import com.sk89q.worldguard.protection.regions.ProtectedRegion
import net.milkbowl.vault.chat.Chat
import net.milkbowl.vault.permission.Permission
import org.bukkit.entity.Player
import org.bukkit.plugin.ServicePriority

// TODO: Make region parents work properly
class WorldGuardRegionContext(private val plugin: WorldGuardPlugin) : SimpleContextDefinition("region") {
    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: (value: String) -> Unit) {
        subject.associatedObject.castMap<Player> {
            val wgPlayer = plugin.wrapPlayer(this)
            val regionProvider = WorldGuard.getInstance().platform.regionContainer[wgPlayer.world]
            if (regionProvider != null) {
                val regions = regionProvider.getApplicableRegions(BukkitAdapter.asBlockVector(this.location))
                val seen = mutableSetOf<ProtectedRegion>()
                for (region in regions) {
                    var current: ProtectedRegion? = region
                    while (current != null && current !in seen) {
                        seen += current
                        consumer(region.id)
                        current = current.parent
                    }
                }
            }
        }
    }

    override fun suggestValues(subject: CalculatedSubject): Set<String> {
        return subject.associatedObject.castMap<Player, Set<String>> {
            val wgPlayer = plugin.wrapPlayer(this)
            (WorldGuard.getInstance().platform.regionContainer[wgPlayer.world]?.regions?.keys)
        } ?: setOf()
    }
}

fun detectWorldGuard(plugin: PermissionsExPlugin) {
    val wgPlugin = plugin.server.pluginManager.getPlugin("WorldGuard")
    if (wgPlugin != null) {
        try {
            Class.forName("com.sk89q.worldguard.WorldGuard")
        } catch (ex: ClassNotFoundException) {
            return
        }

        plugin.manager.registerContextDefinition(WorldGuardRegionContext(wgPlugin as WorldGuardPlugin))
        plugin.manager.logger.info(Messages.INTEGRATIONS_WORLDGUARD_SUCCESS())
    }
}

fun detectVault(plugin: PermissionsExPlugin) {
    if (plugin.server.pluginManager.isPluginEnabled("Vault")) {
        val vault = PEXVault(plugin)
        plugin.server.servicesManager.apply {
            register(Permission::class.java, vault, plugin, ServicePriority.High)
            register(Chat::class.java, PEXVaultChat(vault), plugin, ServicePriority.High)
        }
        plugin.manager.logger.info(Messages.INTEGRATIONS_VAULT_SUCCESS())
    }
}
