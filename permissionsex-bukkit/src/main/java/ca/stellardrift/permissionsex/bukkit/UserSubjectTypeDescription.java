/*
 * PermissionsEx - a permissions plugin for your server ecosystem
 * Copyright © 2020 zml [at] stellardrift [dot] ca and PermissionsEx contributors
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

import ca.stellardrift.permissionsex.subject.SubjectTypeDefinition;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

/**
 * Metadata for user types
 */
public class UserSubjectTypeDescription extends SubjectTypeDefinition<Player> {
    private final PermissionsExPlugin plugin;

    public UserSubjectTypeDescription(String typeName, PermissionsExPlugin plugin) {
        super(typeName);
        this.plugin = plugin;
    }

    @Override
    public boolean isNameValid(String name) {
        try {
            UUID.fromString(name);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    @Override
    public Optional<String> getAliasForName(String input) {
        try {
            UUID.fromString(input);
        } catch (IllegalArgumentException ex) {
            Player player = plugin.getServer().getPlayer(input);
            if (player != null) {
                return Optional.of(player.getUniqueId().toString());
            } else {
                @SuppressWarnings("deprecation")
                OfflinePlayer offline = plugin.getServer().getOfflinePlayer(input);
                if (offline != null && offline.getUniqueId() != null) {
                    return Optional.of(offline.getUniqueId().toString());
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Player> getAssociatedObject(String identifier) {
        try {
            return Optional.ofNullable(plugin.getServer().getPlayer(UUID.fromString(identifier)));
        } catch (IllegalArgumentException ex) { // not a valid user ID
            return Optional.empty();
        }
    }
}
