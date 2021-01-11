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

import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.OfflinePlayer;
import org.checkerframework.checker.nullness.qual.Nullable;

import static java.util.Objects.requireNonNull;

final class PEXVaultChat extends Chat {
    private static final String OPTION_PREFIX = "prefix";
    private static final String OPTION_SUFFIX = "suffix";

    private final PEXVault perms;

    PEXVaultChat(final PEXVault permissions) {
        super(permissions);
        this.perms = permissions;
    }

    private PermissionsExPlugin plugin() {
        return this.perms.pex;
    }

    @Override
    public String getName() {
        return this.plugin().getName();
    }

    @Override
    public boolean isEnabled() {
        return this.plugin().isEnabled();
    }

    @Override
    public @Nullable String getGroupInfoString(final @Nullable String world, final String name, final String key, final @Nullable String defaultValue) {
        requireNonNull(name, "name");
        requireNonNull(key, "key");
        final CalculatedSubject subj = this.perms.getGroup(name);
        return subj.option(this.perms.contextsFrom(subj, world), key).orElse(defaultValue);
    }

    @Override
    public void setGroupInfoString(final @Nullable String world, final String name, final String key, final @Nullable String value) {
        requireNonNull(name, "name");
        requireNonNull(key, "key");
        this.perms.getGroup(name).data()
            .update(this.perms.contextsFrom(world), it -> value == null ? it.withoutOption(key) : it.withOption(key, value));
    }

    @Override
    public @Nullable String getPlayerInfoString(final @Nullable String world, final OfflinePlayer player, final String node, final @Nullable String defaultValue) {
        requireNonNull(player, "player");
        requireNonNull(node, "node");
        final CalculatedSubject subj = this.perms.getUser(player);
        return subj.option(this.perms.contextsFrom(subj, world), node).orElse(defaultValue);
    }

    @Override
    public void setPlayerInfoString(final @Nullable String world, final OfflinePlayer player, final String node, final @Nullable String value) {
        requireNonNull(player, "player");
        requireNonNull(node, "node");
        this.perms.getUser(player).data()
            .update(this.perms.contextsFrom(world), it -> value == null ? it.withoutOption(node) : it.withOption(node, value));
    }

    // -- Passthrough methods
    @Override
    public @Nullable String getGroupPrefix(final @Nullable String world, final String name) {
        return getGroupInfoString(world, name, OPTION_PREFIX, null);
    }

    @Override
    public void setGroupPrefix(final @Nullable String world, final String name, final String prefix) {
        setGroupInfoString(world, name, OPTION_PREFIX, prefix);
    }

    @Override
    public @Nullable String getGroupSuffix(final @Nullable String world, final String name) {
        return getGroupInfoString(world, name, OPTION_SUFFIX, null);
    }

    @Override
    public void setGroupSuffix(final @Nullable String world, final String name, final String suffix) {
        setGroupInfoString(world, name, OPTION_SUFFIX, suffix);
    }

    @Override
    public int getGroupInfoInteger(final @Nullable String world, final String name, final String node, final int defaultValue) {
        try {
            final @Nullable String plain = getGroupInfoString(world, name, node, null);
            return plain == null ? defaultValue : Integer.parseInt(plain);
        } catch (final NumberFormatException ex) {
            return defaultValue;
        }
    }

    @Override
    public void setGroupInfoInteger(final @Nullable String world, final String name, final String node, final int value) {
        setGroupInfoString(world, name, node, Integer.toString(value));
    }

    @Override
    public double getGroupInfoDouble(final @Nullable String world, final String name, final String node, final double defaultValue) {
        try {
            final @Nullable String plain = getGroupInfoString(world, name, node, null);
            return plain == null ? defaultValue : Double.parseDouble(plain);
        } catch (final NumberFormatException ex) {
            return defaultValue;
        }
    }

    @Override
    public void setGroupInfoDouble(final @Nullable String world, final String name, final String node, final double value) {
        setGroupInfoString(world, name, node, Double.toString(value));
    }

    @Override
    public boolean getGroupInfoBoolean(final @Nullable String world, final String name, final String node, final boolean defaultValue) {
        final @Nullable String plain = getGroupInfoString(world, name, node, null);
        return plain == null ? defaultValue : Boolean.parseBoolean(plain);
    }

    @Override
    public void setGroupInfoBoolean(final @Nullable String world, final String name, final String node, final boolean value) {
        setGroupInfoString(world, name, node, Boolean.toString(value));
    }

    @Override
    public @Nullable String getPlayerPrefix(final @Nullable String world, final OfflinePlayer player) {
        return getPlayerInfoString(world, player, OPTION_PREFIX, null);
    }

    @Override
    public void setPlayerPrefix(final @Nullable String world, final OfflinePlayer player, final String prefix) {
        setPlayerInfoString(world, player, OPTION_PREFIX, prefix);
    }

    @Override
    public @Nullable String getPlayerSuffix(final @Nullable String world, final OfflinePlayer player) {
        return getPlayerInfoString(world, player, OPTION_SUFFIX, null);
    }

    @Override
    public void setPlayerSuffix(final @Nullable String world, final OfflinePlayer player, final String suffix) {
        setPlayerInfoString(world, player, OPTION_SUFFIX, suffix);
    }

    @Override
    public int getPlayerInfoInteger(final @Nullable String world, final OfflinePlayer player, final String node, final int defaultValue) {
        try {
            final @Nullable String plain = getPlayerInfoString(world, player, node, null);
            return plain == null ? defaultValue : Integer.parseInt(plain);
        } catch (final NumberFormatException ex) {
            return defaultValue;
        }
    }

    @Override
    public void setPlayerInfoInteger(final @Nullable String world, final OfflinePlayer player, final String node, final int value) {
        setPlayerInfoString(world, player, node, Integer.toString(value));
    }

    @Override
    public double getPlayerInfoDouble(final @Nullable String world, final OfflinePlayer player, final String node, final double defaultValue) {
        try {
            final @Nullable String plain = getPlayerInfoString(world, player, node, null);
            return plain == null ? defaultValue : Double.parseDouble(plain);
        } catch (final NumberFormatException ex) {
            return defaultValue;
        }
    }

    @Override
    public void setPlayerInfoDouble(final @Nullable String world, final OfflinePlayer player, final String node, final double value) {
        setPlayerInfoString(world, player, node, Double.toString(value));
    }

    @Override
    public boolean getPlayerInfoBoolean(final @Nullable String world, final OfflinePlayer player, final String node, final boolean defaultValue) {
        final @Nullable String plain = getPlayerInfoString(world, player, node, null);
        return plain == null ? defaultValue : Boolean.parseBoolean(plain);
    }

    @Override
    public void setPlayerInfoBoolean(final @Nullable String world, final OfflinePlayer player, final String node, final boolean value) {
        setPlayerInfoString(world, player, node, Boolean.toString(value));
    }

    // -- Deprecated stuff
    @SuppressWarnings("deprecation")
    private OfflinePlayer pFromName(final String name) {
        return this.plugin().getServer().getOfflinePlayer(name);
    }

    @Override
    @Deprecated
    public int getPlayerInfoInteger(final @Nullable String world, final String name, final String node, final int defaultValue) {
        return getPlayerInfoInteger(world, pFromName(name), node, defaultValue);
    }

    @Override
    @Deprecated
    public void setPlayerInfoInteger(final @Nullable String world, final String name, final String node, final int value) {
        setPlayerInfoInteger(world, pFromName(name), node, value);
    }

    @Override
    @Deprecated
    public @Nullable String getPlayerInfoString(final @Nullable String world, final String name, final String node, final @Nullable String defaultValue) {
        return getPlayerInfoString(world, pFromName(name), node, defaultValue);
    }

    @Override
    @Deprecated
    public void setPlayerInfoString(final @Nullable String world, final String name, final String node, final String value) {
        setPlayerInfoString(world, pFromName(name), node, value);
    }

    @Override
    @Deprecated
    public boolean getPlayerInfoBoolean(final @Nullable String world, final String name, final String node, final boolean defaultValue) {
        return getPlayerInfoBoolean(world, pFromName(name), node, defaultValue);
    }

    @Override
    @Deprecated
    public void setPlayerInfoBoolean(final @Nullable String world, final String name, final String node, final boolean value) {
        setPlayerInfoBoolean(world, pFromName(name), node, value);
    }

    @Override
    @Deprecated
    public double getPlayerInfoDouble(final @Nullable String world, final String name, final String node, final double defaultValue) {
        return getPlayerInfoDouble(world, pFromName(name), node, defaultValue);
    }

    @Override
    @Deprecated
    public void setPlayerInfoDouble(final @Nullable String world, final String name, final String node, final double value) {
        setPlayerInfoDouble(world, pFromName(name), node, value);
    }

    @Override
    @Deprecated
    public @Nullable String getPlayerPrefix(final @Nullable String world, final String name) {
        return getPlayerPrefix(world, pFromName(name));
    }

    @Override
    @Deprecated
    public void setPlayerPrefix(final @Nullable String world, final String name, final String prefix) {
        setPlayerPrefix(world, pFromName(name), prefix);
    }

    @Override
    @Deprecated
    public @Nullable String getPlayerSuffix(final @Nullable String world, final String name) {
        return getPlayerSuffix(world, pFromName(name));
    }

    @Override
    @Deprecated
    public void setPlayerSuffix(final @Nullable String world, final String name, final String suffix) {
        setPlayerSuffix(world, pFromName(name), suffix);
    }

}
