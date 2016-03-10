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

import ninja.leaping.permissionsex.subject.SubjectTypeDefinition;
import ninja.leaping.permissionsex.util.command.Commander;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

/**
 * Metadata for user types
 */
public class UserSubjectTypeDescription extends SubjectTypeDefinition {
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
                OfflinePlayer offline = plugin.getServer().getOfflinePlayer(input);
                if (offline != null && offline.getUniqueId() != null) {
                    return Optional.of(offline.getUniqueId().toString());
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Commander<?>> getCommanderFor(String identifier) {
        return Optional.ofNullable(plugin.getServer().getPlayer(UUID.fromString(identifier))).map(ply -> new BukkitCommander(plugin, ply));
    }
}
