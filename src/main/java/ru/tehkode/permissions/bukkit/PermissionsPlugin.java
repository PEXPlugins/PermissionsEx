/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ru.tehkode.permissions.bukkit;

import com.nijiko.Messaging;
import com.nijikokun.bukkit.Permissions.Permissions;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.commands.CommandsManager;
import ru.tehkode.permissions.config.Configuration;

/**
 *
 * @author code
 */
public class PermissionsPlugin extends JavaPlugin {
    protected static final String configFile = "config.yml";
    public static final Logger logger = Logger.getLogger("Minecraft");
    public static String name = "PermissionsEx";
    public static String version = "100";
    public static String codename = "Martlet";

    public PermissionManager permissionsManager;
    public CommandsManager commandsManager;

    protected BlockListener blockProtector = new BlockProtector();
    

    public PermissionsPlugin(){
        logger.log(Level.INFO, "[PermissionsEx] (" + codename + ") was Initialized.");
    }

    protected Configuration loadConfig(String name) {
        File configurationFile = new File(getDataFolder(), Permissions.configFile);
        Configuration config = null;
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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        Player player = null;
        PluginDescriptionFile pdfFile = this.getDescription();
        if (sender instanceof Player) {
            player = (Player) sender;
            Messaging.save(player);
        }
        if (args.length > 0) {
            return this.commandsManager.execute(sender, command, args);
        } else {
            if (player != null) {
                Messaging.send("&7f[PermissionsEx]: Running &f[" + pdfFile.getVersion() + "] (" + Permissions.codename + ")");
            } else {
                sender.sendMessage("[" + pdfFile.getName() + "] version [" + pdfFile.getVersion() + "] (" + Permissions.codename + ")  loaded");
            }
        }
        return false;
    }

    @Override
    public void onDisable() {
        this.permissionsManager = null;
        Permissions.Security = null;
        Permissions.logger.log(Level.INFO, "[PermissionsEx] (" + Permissions.codename + ") disabled successfully.");
    }

    @Override
    public void onEnable() {
        this.permissionsManager = new PermissionManager(this.loadConfig(Permissions.name));
        Permissions.Security = this.permissionsManager.getPermissionHandler();

        this.commandsManager.register(new ru.tehkode.permissions.bukkit.commands.Permissions());

        Permissions.logger.log(Level.INFO, "[PermissionsEx] version [" + this.getDescription().getVersion() + "] (" + Permissions.codename + ")  loaded");
        this.getServer().getPluginManager().registerEvent(Event.Type.BLOCK_PLACE, this.blockProtector, Priority.High, this);
        this.getServer().getPluginManager().registerEvent(Event.Type.BLOCK_BREAK, this.blockProtector, Priority.High, this);
    }

    @Override
    public void onLoad() {
        this.commandsManager = new CommandsManager(this);
    }

    private class BlockProtector extends BlockListener {

        @Override
        public void onBlockBreak(BlockBreakEvent event) {
            super.onBlockBreak(event);
            Player player = event.getPlayer();
            if (!Permissions.Security.has(player, "modifyworld.destroy")) {
                event.setCancelled(true);
            }
        }

        @Override
        public void onBlockPlace(BlockPlaceEvent event) {
            super.onBlockPlace(event);
            Player player = event.getPlayer();
            if (!Permissions.Security.has(player, "modifyworld.place")) {
                event.setCancelled(true);
            }
        }
    }

}
