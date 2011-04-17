package com.nijikokun.bukkit.Permissions;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import com.nijiko.Messaging;
import com.nijiko.Misc;
import com.nijiko.permissions.PermissionHandler;
import java.io.File;
import java.io.IOException;
import org.bukkit.event.block.BlockListener;
import ru.tehkode.permission.PermissionManager;
import ru.tehkode.permission.config.Configuration;

/**
 * Permissions 2.x
 * Copyright (C) 2011  Matt 'The Yeti' Burnett <admin@theyeticave.net>
 * Original Credit & Copyright (C) 2010 Nijikokun <nijikokun@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Permissions Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Permissions Public License for more details.
 *
 * You should have received a copy of the GNU Permissions Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
public class Permissions extends JavaPlugin {

    protected final static String configFile = "config.yml";

    public static final Logger logger = Logger.getLogger("Minecraft");
    public static Plugin instance;
    public static Server Server = null;
    public static String name = "PermissionsEx";
    public static String version = "2.7";
    public static String codename = "Slime";
    
    
    protected BlockListener blockProtector = new BlockProtector();
    /**
     * Controller for permissions and security.
     */
    public static PermissionHandler Security;
    /**
     * Miscellaneous object for various functions that don't belong anywhere else
     */
    public static Misc Misc = new Misc();

    protected PermissionManager manager;

    public Permissions() {
        super();

        instance = this;

        // Enabled
        logger.log(Level.INFO, "[PermissionsEx] ("+codename+") was Initialized.");
    }

    @Override
    public void onLoad() {
    }

    @Override
    public void onEnable() {
        this.manager = new PermissionManager(this.loadConfig(name));
        
        Permissions.Security = this.manager.getPermissionHandler();

        logger.log(Level.INFO, "[PermissionsEx] version ["+this.getDescription().getVersion()+"] ("+codename+")  loaded");

        this.getServer().getPluginManager().registerEvent(Event.Type.BLOCK_PLACE, this.blockProtector, Priority.High, this);
        this.getServer().getPluginManager().registerEvent(Event.Type.BLOCK_BREAK, this.blockProtector, Priority.High, this);
    }

    @Override
    public void onDisable() {
        this.manager = null;
        Permissions.Security = null;
        logger.log(Level.INFO, "[PermissionsEx] ("+codename+") disabled successfully.");
    }

    public PermissionHandler getHandler() {
        return this.manager.getPermissionHandler();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        Player player = null;
        String commandName = command.getName().toLowerCase();
        PluginDescriptionFile pdfFile = this.getDescription();

        if (sender instanceof Player) {
            player = (Player) sender;

            Messaging.save(player);
        }

        if (commandName.compareToIgnoreCase("permissions") == 0) {
            if (player != null) {
                Messaging.send("&7-------[ &fPermissionsEx&7 ]-------");
                Messaging.send("&7Currently running version: &f[" + pdfFile.getVersion() + "] (" + codename + ")");
                Messaging.send("&7-------[ &fPermissionsEx&7 ]-------");
            } else {
                sender.sendMessage("[" + pdfFile.getName() + "] version [" + pdfFile.getVersion() + "] (" + codename + ")  loaded");
            }
        }

        logger.info("Got message " + command + " commndLabel " + args);

        return false;
    }

    protected Configuration loadConfig(String name){
        File configFile = new File(getDataFolder(), Permissions.configFile);

        Configuration config = null;

        if(!configFile.exists()){
            try {
                if(!getDataFolder().exists()){
                    getDataFolder().mkdirs();
                }
                configFile.createNewFile(); // Try to create new one

                config = new Configuration(configFile);
                config.setProperty("permissions.basedir", getDataFolder().getPath());
                config.save();
            } catch(IOException e){ // And if failed (ex.: not enough rights) - catch exception
                throw new RuntimeException(e); // Rethrow exception
            }
        } else {
            config = new Configuration(configFile);
            config.load();
        }

         
        return config;
    }

    private class BlockProtector extends BlockListener {

        @Override
        public void onBlockBreak(BlockBreakEvent event) {
            super.onBlockBreak(event);

            Player player = event.getPlayer();

            logger.log(Level.INFO, "[PermissionsEx] Player "+player+" want to break block ("+event.getBlock().getLocation()+" )");
            if(!Permissions.Security.has(player, "modifyworld.destroy")){
                logger.log(Level.INFO, "[PermissionsEx] Player "+player+" want to break block ("+event.getBlock().getLocation()+" ) but not allowed to do so");
                event.setCancelled(true);
            }
        }

        @Override
        public void onBlockPlace(BlockPlaceEvent event) {
            super.onBlockPlace(event);

            Player player = event.getPlayer();

            logger.log(Level.INFO, "[PermissionsEx] Player "+player+" want to break block ("+event.getBlock().getLocation()+" )");
            if(!Permissions.Security.has(player, "modifyworld.place")){
                logger.log(Level.INFO, "[PermissionsEx] Player "+player+" want to break block ("+event.getBlock().getLocation()+" ) but not allowed to do so");
                event.setCancelled(true);
            }
        }
        
    }
}
