/*
 * PermissionsEx - Permissions plugin for Bukkit
 * Copyright (C) 2011 t3hk0d3 http://www.tehkode.ru
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package ru.tehkode.permissions.bukkit.modifyworld;

import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.bukkit.PermissionsEx;
import ru.tehkode.permissions.config.Configuration;
import ru.tehkode.permissions.config.ConfigurationNode;

public class ModifyworldManager {

    protected PermissionManager permissionsManager;
    protected PermissionsEx pex;

    public ModifyworldManager(PermissionsEx plugin) {
        this.permissionsManager = PermissionsEx.getPermissionManager();
        this.pex = plugin;
    }

    public final void registerEvents() {
        // Migration code 1.0x to 1.10
        if (this.pex.getConfigurationNode().getProperty("permissions.modifyworld") instanceof Boolean) { //Migrate from 1.06 to 1.10
            this.pex.getConfigurationNode().setProperty("permissions.modifyworld.enable", this.pex.getConfigurationNode().getBoolean("permissions.modifyworld", false));
            this.pex.getConfigurationNode().setProperty("permissions.modifyworld.itemRestrictions", false);
            this.pex.getConfigurationNode().save();
        }
        
        ConfigurationNode config = this.pex.getConfigurationNode().getNode("permissions.modifyworld");
        if(config == null){
            config = Configuration.getEmptyNode();
        }

        if (!config.getBoolean("enable", false)) {
            Logger.getLogger("Minecraft").info("[PermissionsEx] Modifyworld are disabled. To enable set \"permissions.modifyworld\" to \"true\" in config.yml");
            return;
        }

        // Other EVENTS are modifyworld

        
        
        PluginManager pluginManager = this.pex.getServer().getPluginManager();
        
        // Well, code below looks stupid. Blame Bukkit devs.
        
        //Player events
        PlayerListener playerProtector = new PlayerListener();
        playerProtector.registerEvents(pluginManager, pex, config);
        
        //Block events
        BlockListener blockProtector = new BlockProtector();
        playerProtector.registerEvents(pluginManager, pex, config);
        
        //Entity events
        EntityListener entityProtector = new EntityListener();
        playerProtector.registerEvents(pluginManager, pex, config);
        
        //Vehicle events
        VehicleListener vehicleProtector = new VehicleListener();
        playerProtector.registerEvents(pluginManager, pex, config);

        Logger.getLogger("Minecraft").info("[PermissionsEx] Modifyworld are enabled.");

    }

    protected void informUser(Player player, String message) {
        if (this.pex.getConfigurationNode().getBoolean("permissions.informplayers.modifyworld", false)) {
            player.sendMessage(message);
        }
    }

    protected String getEntityName(Entity entity) {
        return entity.toString().substring(5).toLowerCase();
    }

    public class VehicleListener extends org.bukkit.event.vehicle.VehicleListener implements EventHandler {
        
        @Override
        public void registerEvents(PluginManager pluginManager, PermissionsEx pex, ConfigurationNode config) {
            pluginManager.registerEvent(Event.Type.VEHICLE_COLLISION_ENTITY, this, Priority.Low, pex);
            pluginManager.registerEvent(Event.Type.VEHICLE_ENTER, this, Priority.Low, pex);
            pluginManager.registerEvent(Event.Type.VEHICLE_DAMAGE, this, Priority.Low, pex);
        }

        @Override
        public void onVehicleDamage(VehicleDamageEvent event) {
            if (!(event.getAttacker() instanceof Player)) {
                return;
            }

            Player player = (Player) event.getAttacker();
            if (!permissionsManager.has(player, "modifyworld.vehicle.destroy")) {
                informUser(player, ChatColor.RED + "Sorry, you don't have enought permissions");
                event.setCancelled(true);
            }
        }

        @Override
        public void onVehicleEnter(VehicleEnterEvent event) {
            if (!(event.getEntered() instanceof Player)) {
                return;
            }

            Player player = (Player) event.getEntered();
            if (!permissionsManager.has(player, "modifyworld.vehicle.enter")) {
                informUser(player, ChatColor.RED + "Sorry, you don't have enought permissions");
                event.setCancelled(true);
            }
        }

        @Override
        public void onVehicleEntityCollision(VehicleEntityCollisionEvent event) {
            if (!(event.getEntity() instanceof Player)) {
                return;
            }

            Player player = (Player) event.getEntity();
            if (!permissionsManager.has(player, "modifyworld.vehicle.collide")) {
                event.setCancelled(true);
                event.setCollisionCancelled(true);
                event.setPickupCancelled(true);
            }
        }
    }

    public class BlockProtector extends BlockListener implements EventHandler {

        @Override
        public void registerEvents(PluginManager pluginManager, PermissionsEx pex, ConfigurationNode config) {
            pluginManager.registerEvent(Event.Type.BLOCK_PLACE, this, Priority.Low, pex);
            pluginManager.registerEvent(Event.Type.BLOCK_BREAK, this, Priority.Low, pex);
        }

        @Override
        public void onBlockBreak(BlockBreakEvent event) {
            if (!permissionsManager.has(event.getPlayer(), "modifyworld.blocks.destroy." + event.getBlock().getTypeId())) {
                informUser(event.getPlayer(), ChatColor.RED + "Sorry, you don't have enought permissions");
                event.setCancelled(true);
            }
        }

        @Override
        public void onBlockPlace(BlockPlaceEvent event) {
            if (!permissionsManager.has(event.getPlayer(), "modifyworld.blocks.place." + event.getBlock().getTypeId())) {
                informUser(event.getPlayer(), ChatColor.RED + "Sorry, you don't have enought permissions");
                event.setCancelled(true);
            }
        }
    }

    public class PlayerListener extends org.bukkit.event.player.PlayerListener implements EventHandler {

        @Override
        public void registerEvents(PluginManager pluginManager, PermissionsEx pex, ConfigurationNode config) {
            pluginManager.registerEvent(Event.Type.PLAYER_BED_ENTER, this, Priority.Low, pex);
            pluginManager.registerEvent(Event.Type.PLAYER_BUCKET_EMPTY, this, Priority.Low, pex);
            pluginManager.registerEvent(Event.Type.PLAYER_BUCKET_FILL, this, Priority.Low, pex);
            pluginManager.registerEvent(Event.Type.PLAYER_CHAT, this, Priority.Low, pex);
            pluginManager.registerEvent(Event.Type.PLAYER_DROP_ITEM, this, Priority.Low, pex);
            pluginManager.registerEvent(Event.Type.PLAYER_INTERACT, this, Priority.Low, pex);
            pluginManager.registerEvent(Event.Type.PLAYER_PICKUP_ITEM, this, Priority.Low, pex);

            try {
                // CB 806+ only
                pluginManager.registerEvent(Event.Type.PLAYER_INTERACT_ENTITY, this, Priority.Low, pex);
            } catch (NoSuchFieldError e) {
                Logger.getLogger("Minecraft").warning("[PermissionsEx] Disabling CB806+ events. Please, update your CraftBukkit.");
            }

            if (config.getBoolean("itemRestrictions", false)) {
                pluginManager.registerEvent(Event.Type.PLAYER_ITEM_HELD, this, Priority.Low, pex);
                pluginManager.registerEvent(Event.Type.PLAYER_INVENTORY, this, Priority.Low, pex);
            }
        }

        @Override
        public void onPlayerBedEnter(PlayerBedEnterEvent event) {
            if (!permissionsManager.has(event.getPlayer(), "modifyworld.usebeds")) {
                informUser(event.getPlayer(), ChatColor.RED + "Sorry, you don't have enought permissions");
                event.setCancelled(true);
            }
        }

        @Override
        public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
            if (!permissionsManager.has(event.getPlayer(), "modifyworld.bucket.empty")) {
                informUser(event.getPlayer(), ChatColor.RED + "Sorry, you don't have enought permissions");
                event.setCancelled(true);
            }
        }

        @Override
        public void onPlayerBucketFill(PlayerBucketFillEvent event) {
            if (!permissionsManager.has(event.getPlayer(), "modifyworld.bucket.fill")) {
                informUser(event.getPlayer(), ChatColor.RED + "Sorry, you don't have enought permissions");
                event.setCancelled(true);
            }
        }

        @Override
        public void onPlayerChat(PlayerChatEvent event) {
            if (!permissionsManager.has(event.getPlayer(), "modifyworld.chat")) {
                informUser(event.getPlayer(), ChatColor.RED + "Sorry, you don't have enought permissions");
                event.setCancelled(true);
            }
        }

        @Override
        public void onPlayerDropItem(PlayerDropItemEvent event) {
            if (!permissionsManager.has(event.getPlayer(), "modifyworld.items.drop." + event.getItemDrop().getItemStack().getTypeId())) {
                informUser(event.getPlayer(), ChatColor.RED + "Sorry, you don't have enought permissions");
                event.setCancelled(true);
            }
        }

        @Override
        public void onInventoryOpen(PlayerInventoryEvent event) {
            Inventory inventory = event.getPlayer().getInventory();
            for (ItemStack stack : inventory.getContents()) {
                if (!permissionsManager.has(event.getPlayer(), "modifyworld.items.canhave." + stack.getTypeId())) {
                    inventory.remove(stack);
                    informUser(event.getPlayer(), "You have prohibited item \"" + stack.getType().name() + "\" (" + stack.getAmount() + ").");
                }
            }
        }

        @Override
        public void onItemHeldChange(PlayerItemHeldEvent event) {
            Inventory inventory = event.getPlayer().getInventory();
            for (ItemStack stack : inventory.getContents()) {
                if (!permissionsManager.has(event.getPlayer(), "modifyworld.items.have." + stack.getTypeId())) {
                    inventory.remove(stack);
                    informUser(event.getPlayer(), "You have prohibited item \"" + stack.getType().name() + "\" (" + stack.getAmount() + ").");
                }
            }
        }

        @Override
        public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
            if (!permissionsManager.has(event.getPlayer(), "modifyworld.entity.interact." + getEntityName(event.getRightClicked()))) {
                event.setCancelled(true);
                informUser(event.getPlayer(), ChatColor.RED + "Sorry, you don't have enought permissions");
            }
        }

        @Override
        public void onPlayerPickupItem(PlayerPickupItemEvent event) {
            if (!permissionsManager.has(event.getPlayer(), "modifyworld.items.pickup." + event.getItem().getItemStack().getTypeId())) {
                //informUser(event.getPlayer(), ChatColor.RED + "Sorry, you don't have enought permissions");
                event.setCancelled(true);
            }
        }

        @Override
        public void onPlayerInteract(PlayerInteractEvent event) {
            Action action = event.getAction();
            if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) {
                return;
            }

            if (!permissionsManager.has(event.getPlayer(), "modifyworld.blocks.interact." + event.getClickedBlock().getTypeId())) {
                informUser(event.getPlayer(), ChatColor.RED + "Sorry, you don't have enought permissions");
                event.setCancelled(true);
            }
        }
    }

    public class EntityListener extends org.bukkit.event.entity.EntityListener implements EventHandler {

        @Override
        public void registerEvents(PluginManager pluginManager, PermissionsEx pex, ConfigurationNode config) {
            pluginManager.registerEvent(Event.Type.ENTITY_TARGET, this, Priority.Low, pex);
            pluginManager.registerEvent(Event.Type.ENTITY_INTERACT, this, Priority.Low, pex);
            pluginManager.registerEvent(Event.Type.ENTITY_DAMAGE, this, Priority.Low, pex);
        }

        @Override
        public void onEntityDamage(EntityDamageEvent event) {
            if (event instanceof EntityDamageByEntityEvent) { // player is damager
                EntityDamageByEntityEvent edbe = (EntityDamageByEntityEvent) event;
                if (!(edbe.getDamager() instanceof Player)) { // not caused by player
                    return;
                }

                Player player = (Player) edbe.getDamager();
                if (!permissionsManager.has(player, "modifyworld.entity.damage.deal." + getEntityName(event.getEntity()))) {
                    informUser(player, ChatColor.RED + "Sorry, you don't have enought permissions");
                    event.setCancelled(true);
                }
            } else if (event.getEntity() instanceof Player) { // player are been damaged by someone
                Player player = (Player) event.getEntity();
                if (!permissionsManager.has(player, "modifyworld.entity.damage.take." + getEntityName(event.getEntity()))) {
                    informUser(player, ChatColor.RED + "Sorry, you don't have enought permissions");
                    event.setCancelled(true);
                    event.setDamage(0);
                }
            }
        }

        @Override
        public void onEntityTarget(EntityTargetEvent event) {
            if (event.getTarget() instanceof Player) {
                Player player = (Player) event.getTarget();
                if (!permissionsManager.has(player, "modifyworld.entity.mobtarget." + getEntityName(event.getEntity()))) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
