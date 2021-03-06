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

import ca.stellardrift.permissionsex.minecraft.MinecraftPermissionsEx;
import ca.stellardrift.permissionsex.minecraft.command.MessageFormatter;
import ca.stellardrift.permissionsex.subject.SubjectRef;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.Nullable;

final class BukkitMessageFormatter extends MessageFormatter {

    public BukkitMessageFormatter(final MinecraftPermissionsEx<?> manager) {
        super(manager);
    }

    @Override
    protected @Nullable <I> String friendlyName(final SubjectRef<I> reference) {
        final @Nullable Object associated = reference.type().getAssociatedObject(reference.identifier());
        return associated instanceof CommandSender ? ((CommandSender) associated).getName() : super.friendlyName(reference);
    }

}
