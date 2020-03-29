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
