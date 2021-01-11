/*
 * PermissionsEx - a permissions plugin for your server ecosystem
 * Copyright © 2021 zml [at] stellardrift [dot] ca and PermissionsEx contributors
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
package ca.stellardrift.permissionsex.bukkit;

import ca.stellardrift.permissionsex.context.SimpleContextDefinition;
import ca.stellardrift.permissionsex.impl.util.PCollections;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

final class PluginIntegrations {
    static void detectWorldGuard(final PermissionsExPlugin plugin) {
        final @Nullable Plugin wgPlugin = plugin.getServer().getPluginManager().getPlugin("WorldGuard");
        if (wgPlugin != null) {
            try {
                Class.forName("com.sk89q.worldguard.WorldGuard");
            } catch (final ClassNotFoundException ex) {
                return;
            }

            plugin.engine().registerContextDefinition(new WorldGuardRegionContext((WorldGuardPlugin) wgPlugin));
            plugin.logger().info(Messages.INTEGRATIONS_WORLDGUARD_SUCCESS.tr());
        }
    }

    static void detectVault(final PermissionsExPlugin plugin) {
        if (plugin.getServer().getPluginManager().isPluginEnabled("Vault")) {
            final PEXVault vault = new PEXVault(plugin);
            final ServicesManager services = plugin.getServer().getServicesManager();
            services.register(Permission.class, vault, plugin, ServicePriority.High);
            services.register(Chat.class, new PEXVaultChat(vault), plugin, ServicePriority.High);
            plugin.logger().info(Messages.INTEGRATIONS_VAULT_SUCCESS.tr());
        }
    }

    // TODO: Make region parents work properly
    static final class WorldGuardRegionContext extends SimpleContextDefinition {
        private final WorldGuardPlugin plugin;

        WorldGuardRegionContext(final WorldGuardPlugin plugin) {
            super("region");
            this.plugin = plugin;
        }

        @Override
        public void accumulateCurrentValues(final CalculatedSubject subject, final Consumer<String> consumer) {
            final @Nullable Object associated = subject.associatedObject();
            if (associated instanceof Player) {
                final LocalPlayer wgPlayer = this.plugin.wrapPlayer((Player) associated);
                final @Nullable RegionManager regionProvider = WorldGuard.getInstance().getPlatform().getRegionContainer().get(wgPlayer.getWorld());
                if (regionProvider != null) {
                    final ApplicableRegionSet regions = regionProvider.getApplicableRegions(wgPlayer.getLocation().toVector().toBlockPoint());
                    final Set<ProtectedRegion> seen = new HashSet<>();
                    for (final ProtectedRegion region : regions) {
                        @Nullable ProtectedRegion current = region;
                        while (current != null && !seen.contains(current)){
                            seen.add(current);
                            consumer.accept(region.getId());
                            current = current.getParent();
                        }
                    }
                }
            }
        }

        @Override
        public Set<String> suggestValues(final CalculatedSubject subject) {
            final @Nullable Object associated = subject.associatedObject();
            if (associated instanceof Player) {
                final LocalPlayer wgPlayer = this.plugin.wrapPlayer((Player) associated);
                final @Nullable RegionManager container = WorldGuard.getInstance().getPlatform().getRegionContainer().get(wgPlayer.getWorld());
                if (container != null) {
                    return container.getRegions().keySet();
                }
            }

            return PCollections.set();
        }
    }

}
