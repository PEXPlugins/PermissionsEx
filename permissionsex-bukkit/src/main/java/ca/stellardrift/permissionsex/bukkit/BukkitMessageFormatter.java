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
package ca.stellardrift.permissionsex.bukkit;

import ca.stellardrift.permissionsex.bungeetext.BungeeMessageFormatter;
import ca.stellardrift.permissionsex.util.Util;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;

/**
 * Factory to create formatted elements of messages
 */
public class BukkitMessageFormatter extends BungeeMessageFormatter {
    private final CommandSender sender;

    BukkitMessageFormatter(PermissionsExPlugin pex, CommandSender sender) {
        super(pex.getManager(), ChatColor.AQUA);
        this.sender = sender;
    }

    /**
     * Take a locale string provided from a minecraft client and attempt to parse it as a locale.
     * These are not strictly compliant with the iso standard, so we try to make things a bit more normalized.
     *
     * @param mcLocaleString The locale string, in the format provided by the Minecraft client
     * @return A Locale object matching the provided locale string
     */
    public static Locale toLocale(String mcLocaleString) {
        String[] parts = mcLocaleString.split("_", 3);
        switch (parts.length) {
            case 0:
                return Locale.getDefault();
            case 1:
                return new Locale(parts[0]);
            case 2:
                return new Locale(parts[0], parts[1]);
            case 3:
                return new Locale(parts[0], parts[1], parts[2]);
            default:
                throw new IllegalArgumentException("Provided locale '" + mcLocaleString + "' was not in a valid format!");
        }
    }

    @NotNull
    @Override
    public Locale getLocale() {
        return sender instanceof Player ? toLocale(((Player) sender).getLocale()) : Locale.getDefault();
    }

    @Nullable
    @Override
    public String getFriendlyName(@NotNull Map.Entry<String, String> subj) {
        return Util.castOptional(getPex().getSubjects(subj.getKey()).getTypeInfo().getAssociatedObject(subj.getValue()), CommandSender.class).map(CommandSender::getName).orElse(null);
    }
}
