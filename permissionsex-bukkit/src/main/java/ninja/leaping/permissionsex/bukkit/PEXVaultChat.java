/**
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
package ninja.leaping.permissionsex.bukkit;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import net.milkbowl.vault.chat.Chat;
import ninja.leaping.permissionsex.PermissionsEx;
import org.bukkit.OfflinePlayer;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("deprecation")
class PEXVaultChat extends Chat {
    private final PermissionsExPlugin plugin;
    private final PEXVault perms;
    PEXVaultChat(PEXVault perms) {
        super(perms);
        this.perms = perms;
        this.plugin = perms.plugin;
    }

    @Override
    public String getName() {
        return this.plugin.getName();
    }

    @Override
    public boolean isEnabled() {
        return this.plugin.isEnabled();
    }

    private Set<Map.Entry<String, String>> contextsFrom(@Nullable String world) {
        return world == null ? PermissionsEx.GLOBAL_CONTEXT : ImmutableSet.of(Maps.immutableEntry("world", world));
    }

    @Override
    public String getGroupInfoString(final String world, String name, final String key, String defaultValue) {
        return perms.getGroup(name).getOption(contextsFrom(world), key).orElse(defaultValue);
    }

    @Override
    public void setGroupInfoString(final String world, String name, final String key, final String value) {
        perms.getSubject(name).data().updateSegment(contextsFrom(world), input -> input.withOption(key, value));
    }


    @Override
    public String getPlayerInfoString(String world, OfflinePlayer player, String node, String defaultValue) {
        return perms.getSubject(player).getOption(contextsFrom(world), node).orElse(defaultValue);
    }

    @Override
    public void setPlayerInfoString(final String world, OfflinePlayer player, final String node, final String value) {
        perms.getSubject(player).data().updateSegment(contextsFrom(world), input -> input.withOption(node, value));
    }

    // -- Passthrough methods

    @Override
    public String getGroupPrefix(String world, String name) {
        return getGroupInfoString(world, name, "prefix", "");
    }

    @Override
    public void setGroupPrefix(String world, String name, String prefix) {
        setGroupInfoString(world, name, "prefix", prefix);
    }

    @Override
    public String getGroupSuffix(String world, String name) {
        return getGroupInfoString(world, name, "suffix", "");
    }

    @Override
    public void setGroupSuffix(String world, String name, String suffix) {
        setGroupInfoString(world, name, "suffix", suffix);
    }

    @Override
    public int getGroupInfoInteger(String world, String name, String node, int defaultValue) {
        try {
            return Integer.parseInt(getGroupInfoString(world, name, node, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public void setGroupInfoInteger(String world, String name, String node, int value) {
        setGroupInfoString(world, name, node, String.valueOf(value));
    }


    @Override
    public double getGroupInfoDouble(String world, String name, String node, double defaultValue) {
        try {
            return Double.parseDouble(getGroupInfoString(world, name, node, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public void setGroupInfoDouble(String world, String name, String node, double value) {
        setGroupInfoString(world, name, node, String.valueOf(value));
    }


    @Override
    public boolean getGroupInfoBoolean(String world, String name, String node, boolean defaultValue) {
        return Boolean.parseBoolean(getGroupInfoString(world, name, node, String.valueOf(defaultValue)));
    }

    @Override
    public void setGroupInfoBoolean(String world, String name, String node, boolean value) {
        setGroupInfoString(world, name, node, String.valueOf(value));
    }


    @Override
    public String getPlayerPrefix(String world, OfflinePlayer player) {
        return getPlayerInfoString(world, player, "prefix", null);
    }

    @Override
    public void setPlayerPrefix(String world, OfflinePlayer player, String prefix) {
        setPlayerInfoString(world, player, "prefix", prefix);
    }

    @Override
    public String getPlayerSuffix(String world, OfflinePlayer player) {
        return getPlayerInfoString(world, player, "suffix", null);
    }

    @Override
    public void setPlayerSuffix(String world, OfflinePlayer player, String suffix) {
        setPlayerInfoString(world, player, "suffix", suffix);
    }

    @Override
    public int getPlayerInfoInteger(String world, OfflinePlayer player, String node, int defaultValue) {
        try {
            return Integer.parseInt(getPlayerInfoString(world, player, node, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public void setPlayerInfoInteger(String world, OfflinePlayer player, String node, int value) {
        setPlayerInfoString(world, player, node, String.valueOf(value));
    }

    @Override
    public double getPlayerInfoDouble(String world, OfflinePlayer player, String node, double defaultValue) {
        try {
            return Double.parseDouble(getPlayerInfoString(world, player, node, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public void setPlayerInfoDouble(String world, OfflinePlayer player, String node, double value) {
        setPlayerInfoString(world, player, node, String.valueOf(value));
    }

    @Override
    public boolean getPlayerInfoBoolean(String world, OfflinePlayer player, String node, boolean defaultValue) {
        return Boolean.parseBoolean(getPlayerInfoString(world, player, node, String.valueOf(defaultValue)));
    }

    @Override
    public void setPlayerInfoBoolean(String world, OfflinePlayer player, String node, boolean value) {
        setPlayerInfoString(world, player, node, String.valueOf(value));
    }

    // -- Deprecated stuff
    @SuppressWarnings("deprecation")
    private OfflinePlayer pFromName(String name) {
        return plugin.getServer().getOfflinePlayer(name);
    }

    @Override
    public int getPlayerInfoInteger(String world, String name, String node, int defaultValue) {
        return getPlayerInfoInteger(world, pFromName(name), node, defaultValue);
    }

    @Override
    public void setPlayerInfoInteger(String world, String name, String node, int value) {
        setPlayerInfoInteger(world, pFromName(name), node, value);
    }

    @Override
    @Deprecated
    public String getPlayerInfoString(String world, String name, String node, String defaultValue) {
        return getPlayerInfoString(world, pFromName(name), node, defaultValue);
    }

    @Override
    public void setPlayerInfoString(String world, String name, String node, String value) {
        setPlayerInfoString(world, pFromName(name), node, value);
    }

    @Override
    public boolean getPlayerInfoBoolean(String world, String name, String node, boolean defaultValue) {
        return getPlayerInfoBoolean(world, pFromName(name), node, defaultValue);
    }

    @Override
    public void setPlayerInfoBoolean(String world, String name, String node, boolean value) {
        setPlayerInfoBoolean(world, pFromName(name), node, value);
    }

    @Override
    public double getPlayerInfoDouble(String world, String name, String node, double defaultValue) {
        return getPlayerInfoDouble(world, pFromName(name), node, defaultValue);
    }

    @Override
    public void setPlayerInfoDouble(String world, String name, String node, double value) {
        setPlayerInfoDouble(world, pFromName(name), node, value);
    }

    @Override
    public String getPlayerPrefix(String world, String name) {
        return getPlayerPrefix(world, pFromName(name));
    }

    @Override
    public void setPlayerPrefix(String world, String name, String prefix) {
        setPlayerPrefix(world, pFromName(name), prefix);
    }

    @Override
    public String getPlayerSuffix(String world, String name) {
        return getPlayerSuffix(world, pFromName(name));
    }

    @Override
    public void setPlayerSuffix(String world, String name, String suffix) {
        setPlayerSuffix(world, pFromName(name), suffix);
    }
}
