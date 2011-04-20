/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.tehkode.permissions.bukkit;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.commands.CommandsManager;
import ru.tehkode.permissions.config.Configuration;

/**
 *
 * @author code
 */
public class PermissionsPlugin extends JavaPlugin {

    protected static final String configFile = "config.yml";
    protected static final Logger logger = Logger.getLogger("Minecraft");
    public PermissionManager permissionsManager;
    public CommandsManager commandsManager;
    protected BlockListener blockProtector = new BlockProtector();

    public PermissionsPlugin() {
        super();
        
        logger.log(Level.INFO, "[PermissionsEx] PermissionEx plugin was Initialized.");
    }

    @Override
    public void onLoad() {
        this.commandsManager = new CommandsManager(this);
        this.permissionsManager = new PermissionManager(this.loadConfig(configFile));
    }

    @Override
    public void onEnable() {
        this.commandsManager.register(new ru.tehkode.permissions.bukkit.commands.PermissionsCommand());

        this.getServer().getPluginManager().registerEvent(Event.Type.BLOCK_PLACE, this.blockProtector, Priority.Low, this);
        this.getServer().getPluginManager().registerEvent(Event.Type.BLOCK_BREAK, this.blockProtector, Priority.Low, this);

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
                sender.sendMessage(ChatColor.WHITE + "[PermissionsEx]: Running &f[" + pdfFile.getVersion() + "]");
            } else {
                sender.sendMessage("[" + pdfFile.getName() + "] version [" + pdfFile.getVersion() + "] loaded");
            }
        }

        return true;
    }

    public static PermissionManager getPermissionManager() {
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("PermissionsEx");
        if (plugin == null || !(plugin instanceof PermissionsPlugin)) {
            throw new RuntimeException("Permissions manager are not accessable. Permissions plugin disabled?");
        }

        return ((PermissionsPlugin) plugin).permissionsManager;
    }

    protected Configuration loadConfig(String name) {
        File configurationFile = new File(getDataFolder(), configFile);
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

    private class BlockProtector extends BlockListener {

        @Override
        public void onBlockBreak(BlockBreakEvent event) {
            super.onBlockBreak(event);
            Player player = event.getPlayer();

            PermissionUser user = permissionsManager.getUser(player.getName());

            if (!permissionsManager.has(player, "modifyworld.destroy")) {
                event.setCancelled(true);
            }
        }

        @Override
        public void onBlockPlace(BlockPlaceEvent event) {
            super.onBlockPlace(event);
            Player player = event.getPlayer();
            if (!permissionsManager.has(player, "modifyworld.place")) {
                event.setCancelled(true);
            }
        }
    }
}
