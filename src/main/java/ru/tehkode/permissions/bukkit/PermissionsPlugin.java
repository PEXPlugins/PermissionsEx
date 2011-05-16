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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.tehkode.permissions.bukkit;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.*;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;
import ru.tehkode.permissions.*;
import ru.tehkode.permissions.backends.*;
import ru.tehkode.permissions.commands.CommandsManager;
import ru.tehkode.permissions.config.Configuration;

/**
 *
 * @author code
 */
public class PermissionsPlugin extends JavaPlugin {

    protected static final String configFile = "config.yml";
    protected static final Logger logger = Logger.getLogger("Minecraft");
    protected PermissionManager permissionsManager;
    protected CommandsManager commandsManager;
    protected Configuration config;

    public PermissionsPlugin() {
        super();

        PermissionBackend.registerBackendAlias("sql", SQLBackend.class);
        PermissionBackend.registerBackendAlias("file", FileBackend.class);

        logger.log(Level.INFO, "[PermissionsEx] PermissionEx plugin was Initialized.");
    }

    @Override
    public void onLoad() {
        this.config = this.loadConfig(configFile);
        this.commandsManager = new CommandsManager(this);
        this.permissionsManager = new PermissionManager(this.config);
    }

    @Override
    public void onEnable() {
        this.commandsManager.register(new ru.tehkode.permissions.bukkit.commands.PermissionsCommand());

        this.registerEvents();

        logger.log(Level.INFO, "[PermissionsEx] version [" + this.getDescription().getVersion() + "] (" + this.getDescription().getVersion() + ")  loaded");
    }

    @Override
    public void onDisable() {
        logger.log(Level.INFO, "[PermissionsEx-" + this.getDescription().getVersion() + "] disabled successfully.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        PluginDescriptionFile pdfFile = this.getDescription();
        if (args.length > 0) {
            return this.commandsManager.execute(sender, command, args);
        } else {
            if (sender instanceof Player) {
                sender.sendMessage(ChatColor.WHITE + "[PermissionsEx]: Running (" + pdfFile.getVersion() + ")");

                return !this.permissionsManager.has((Player) sender, "permissions.manage");
            } else {
                sender.sendMessage("[" + pdfFile.getName() + "] version [" + pdfFile.getVersion() + "] loaded");

                return false;
            }
        }
    }

    public static PermissionManager getPermissionManager() {
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("PermissionsEx");
        if (plugin == null || !(plugin instanceof PermissionsPlugin)) {
            throw new RuntimeException("Permissions manager are not accessable. PermissionsEx plugin disabled?");
        }

        return ((PermissionsPlugin) plugin).permissionsManager;
    }

    protected Configuration loadConfig(String name) {
        File configurationFile = new File(getDataFolder(), configFile);
        Configuration config;
        if (!configurationFile.exists()) {
            try {
                if (!getDataFolder().exists()) {
                    getDataFolder().mkdirs();
                }
                configurationFile.createNewFile(); // Try to create new one
                config = new Configuration(configurationFile);
                config.setProperty("permissions.basedir", getDataFolder().getPath());
                config.save();
            } catch (IOException e) {
                // And if failed (ex.: not enough rights) - catch exception
                throw new RuntimeException(e); // Rethrow exception
            }
        } else {
            config = new Configuration(configurationFile);
            config.load();
        }
        return config;
    }

    public void informUser(Player player, String message) {
        if(this.config.getBoolean("verbose", false)){
            player.sendMessage(message);
        }
    }

    protected void registerEvents() {
        BlockListener blockProtector = new BlockProtector();
        PlayerListener playerProtector = new PlayerListener();
        EntityListener entityProtector = new EntityListener();
        VehicleListener vehicleProtector = new VehicleListener();

        PluginManager pluginManager = this.getServer().getPluginManager();


        //Block events
        pluginManager.registerEvent(Event.Type.BLOCK_PLACE, blockProtector, Priority.Low, this);
        pluginManager.registerEvent(Event.Type.BLOCK_BREAK, blockProtector, Priority.Low, this);

        //Player events
        pluginManager.registerEvent(Event.Type.PLAYER_QUIT, playerProtector, Priority.Low, this);
        pluginManager.registerEvent(Event.Type.PLAYER_BED_ENTER, playerProtector, Priority.Low, this);
        pluginManager.registerEvent(Event.Type.PLAYER_BUCKET_EMPTY, playerProtector, Priority.Low, this);
        pluginManager.registerEvent(Event.Type.PLAYER_BUCKET_FILL, playerProtector, Priority.Low, this);
        pluginManager.registerEvent(Event.Type.PLAYER_CHAT, playerProtector, Priority.Low, this);
        pluginManager.registerEvent(Event.Type.PLAYER_DROP_ITEM, playerProtector, Priority.Low, this);
        pluginManager.registerEvent(Event.Type.PLAYER_INTERACT, playerProtector, Priority.Low, this);
        pluginManager.registerEvent(Event.Type.PLAYER_PICKUP_ITEM, playerProtector, Priority.Low, this);

        //Entity events
        pluginManager.registerEvent(Event.Type.ENTITY_TARGET, entityProtector, Priority.Low, this);
        pluginManager.registerEvent(Event.Type.ENTITY_INTERACT, entityProtector, Priority.Low, this);
        pluginManager.registerEvent(Event.Type.ENTITY_DAMAGE, entityProtector, Priority.Low, this);

        //Vehicle events
        pluginManager.registerEvent(Event.Type.VEHICLE_COLLISION_ENTITY, vehicleProtector, Priority.Low, this);
        pluginManager.registerEvent(Event.Type.VEHICLE_ENTER, vehicleProtector, Priority.Low, this);
        pluginManager.registerEvent(Event.Type.VEHICLE_DAMAGE, vehicleProtector, Priority.Low, this);

    }

    private class VehicleListener extends org.bukkit.event.vehicle.VehicleListener {

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

    private class EntityListener extends org.bukkit.event.entity.EntityListener {

        @Override
        public void onEntityDamage(EntityDamageEvent event) {
            if (event instanceof EntityDamageByEntityEvent) { // player is damager
                EntityDamageByEntityEvent edbe = (EntityDamageByEntityEvent) event;
                if (!(edbe.getDamager() instanceof Player)) { // not caused by player
                    return;
                }

                Player player = (Player) edbe.getDamager();
                if (!permissionsManager.has(player, "modifyworld.entity.damage.deal")) {
                    informUser(player, ChatColor.RED + "Sorry, you don't have enought permissions");
                    event.setCancelled(true);
                }
            } else if (event.getEntity() instanceof Player) { // player are been damaged by someone
                Player player = (Player) event.getEntity();
                if (!permissionsManager.has(player, "modifyworld.entity.damage.take")) {
                    informUser(player, ChatColor.RED + "Sorry, you don't have enought permissions");
                    event.setCancelled(true);
                }
            }
        }

        @Override
        public void onEntityTarget(EntityTargetEvent event) {
            if (event.getEntity() instanceof Player) {
                Player player = (Player) event.getEntity();
                if (!permissionsManager.has(player, "modifyworld.entity.mobtarget")) {
                    event.setCancelled(true);
                }
            }
        }
    }

    private class PlayerListener extends org.bukkit.event.player.PlayerListener {

        @Override
        public void onPlayerQuit(PlayerQuitEvent event) {
            super.onPlayerQuit(event);
            getPermissionManager().resetUser(event.getPlayer().getName());
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
            if (!permissionsManager.has(event.getPlayer(), "modifyworld.items.drop")) {
                informUser(event.getPlayer(), ChatColor.RED + "Sorry, you don't have enought permissions");
                event.setCancelled(true);
            }
        }

        @Override
        public void onPlayerPickupItem(PlayerPickupItemEvent event) {
            if (!permissionsManager.has(event.getPlayer(), "modifyworld.items.pickup." + event.getItem().getEntityId())) {
                informUser(event.getPlayer(), ChatColor.RED + "Sorry, you don't have enought permissions");
                event.setCancelled(true);
            }
        }

        @Override
        public void onPlayerInteract(PlayerInteractEvent event) {
            if (!permissionsManager.has(event.getPlayer(), "modifyworld.blocks.interact")) {
                informUser(event.getPlayer(), ChatColor.RED + "Sorry, you don't have enought permissions");
                event.setCancelled(true);
            }
        }
    }

    private class BlockProtector extends BlockListener {

        @Override
        public void onBlockBreak(BlockBreakEvent event) {
            super.onBlockBreak(event);
            if (!permissionsManager.has(event.getPlayer(), "modifyworld.blocks.destroy")) {
                informUser(event.getPlayer(), ChatColor.RED + "Sorry, you don't have enought permissions");
                event.setCancelled(true);
            }
        }

        @Override
        public void onBlockPlace(BlockPlaceEvent event) {
            super.onBlockPlace(event);
            if (!permissionsManager.has(event.getPlayer(), "modifyworld.blocks.place")) {
                informUser(event.getPlayer(), ChatColor.RED + "Sorry, you don't have enought permissions");
                event.setCancelled(true);
            }
        }
    }
}
