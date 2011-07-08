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
package ru.tehkode.permissions.bukkit;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.plugin.Plugin;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.config.Configuration;

public class ChatManager extends PlayerListener {

    protected String format = "<%prefix%player%suffix> %message";
    protected String globalFormat = "<%prefix%player%suffix> &e%message";
    protected boolean isEnabled = false;
    protected boolean forceRangedChat = false;
    protected double chatRange = 100;

    public ChatManager(Plugin plugin, Configuration config) {
        this.isEnabled = config.getBoolean("permissions.chat.enable", this.isEnabled);
        this.format = config.getString("permissions.chat.format", this.format);
        this.globalFormat = config.getString("permissions.chat.global-format", this.globalFormat);
        this.forceRangedChat = config.getBoolean("permissions.chat.force-ranged", this.forceRangedChat);
        this.chatRange = config.getDouble("permissions.chat.chat-range", this.chatRange);

        config.save();
    }

    public void registerEvents(Plugin plugin) {
        if (this.isEnabled) {
            plugin.getServer().getPluginManager().registerEvent(Type.PLAYER_CHAT, this, Priority.Normal, plugin);
        }
    }

    @Override
    public void onPlayerChat(PlayerChatEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();

        PermissionUser user = PermissionsEx.getPermissionManager().getUser(player);
        if (user == null) {
            return;
        }

        String message = user.getOption("message-format", player.getWorld().getName(), this.format);
        boolean localChat = user.getOptionBoolean("force-ranged-chat", player.getWorld().getName(), this.forceRangedChat);

        String chatMessage = event.getMessage();
        if (chatMessage.startsWith("!") && user.has("permissions.chat.global", player.getWorld().getName())) {
            localChat = false;
            chatMessage = chatMessage.substring(1);

            message = user.getOption("global-message-format", player.getWorld().getName(), this.globalFormat);
        }

        message = this.colorize(message);

        if (user.has("permissions.chat.color", player.getWorld().getName())) {
            chatMessage = this.colorize(chatMessage);
        }

        message = message.replace("%prefix", this.colorize(user.getPrefix()))
                         .replace("%suffix", this.colorize(user.getSuffix()))
                         .replace("%world", player.getWorld().getName())
                         .replace("%message", chatMessage)
                         .replace("%player", player.getName());
        
        message = this.replaceTime(message);
        
        
        event.setFormat("%2$s");
        event.setMessage(message);

        if (localChat) {
            event.getRecipients().clear();
            event.getRecipients().addAll(this.getLocalRecipients(player, message, user.getOptionDouble("chat-range", player.getWorld().getName(), chatRange)));
        }
    }

    protected List<Player> getLocalRecipients(Player sender, String message, double range) {
        Location playerLocation = sender.getLocation();
        List<Player> recipients = new LinkedList<Player>();
        double squaredDistance = Math.pow(range, 2);
        for (Player recipient : Bukkit.getServer().getOnlinePlayers()) {
            // Recipient are not from same world
            if (!recipient.getWorld().equals(sender.getWorld())) {
                continue;
            }

            if (playerLocation.distanceSquared(recipient.getLocation()) > squaredDistance) {
                continue;
            }

            recipients.add(recipient);
        }
        return recipients;
    }

    protected String replaceTime(String message) {
        Calendar calendar = Calendar.getInstance();

        if (message.contains("%h")) {
            message = message.replace("%h", String.format("%02d", calendar.get(Calendar.HOUR)));
        }

        if (message.contains("%H")) {
            message = message.replace("%H", String.format("%02d", calendar.get(Calendar.HOUR_OF_DAY)));
        }

        if (message.contains("%g")) {
            message = message.replace("%g", Integer.toString(calendar.get(Calendar.HOUR)));
        }

        if (message.contains("%G")) {
            message = message.replace("%G", Integer.toString(calendar.get(Calendar.HOUR_OF_DAY)));
        }

        if (message.contains("%i")) {
            message = message.replace("%i", String.format("%02d", calendar.get(Calendar.MINUTE)));
        }

        if (message.contains("%s")) {
            message = message.replace("%s", String.format("%02d", calendar.get(Calendar.SECOND)));
        }

        if (message.contains("%a")) {
            message = message.replace("%a", (calendar.get(Calendar.AM_PM) == 0) ? "am" : "pm");
        }

        if (message.contains("%A")) {
            message = message.replace("%A", (calendar.get(Calendar.AM_PM) == 0) ? "AM" : "PM");
        }

        return message;
    }

    protected String colorize(String string) {
        return string.replaceAll("&([a-z0-9])", "\u00A7$1");
    }
}
