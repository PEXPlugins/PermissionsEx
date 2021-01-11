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

import ca.stellardrift.permissionsex.minecraft.command.Commander;
import ca.stellardrift.permissionsex.minecraft.command.MessageFormatter;
import ca.stellardrift.permissionsex.subject.SubjectRef;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import static net.kyori.adventure.text.Component.text;

/**
 * An abstraction over the Sponge CommandSource that handles
 * PEX-specific message formatting and localization.
 */
final class BukkitCommander implements Commander {
    private final PermissionsExPlugin plugin;
    private final CommandSender source;
    private final Audience audience;

    BukkitCommander(final PermissionsExPlugin plugin, final CommandSender source) {
        this.plugin = plugin;
        this.source = source;
        this.audience = plugin.adventure().sender(source);
    }

    @Override
    public Component name() {
        return text(this.source.getName());
    }

    @Override
    public @Nullable SubjectRef<?> subjectIdentifier() {
        if (this.source instanceof Player) {
            return SubjectRef.subject(this.plugin.users().type(), ((Player) this.source).getUniqueId());
        } else {
            return null;
        }
    }

    @Override
    public MessageFormatter formatter() {
        return this.plugin.manager().messageFormatter();
    }

    @Override
    public boolean hasPermission(final String permission) {
        return this.source.hasPermission(permission);
    }

    @Override
    public @NonNull Audience audience() {
        return this.audience;
    }

    CommandSender source() {
        return this.source;
    }

}
