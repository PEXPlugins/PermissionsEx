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
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import ninja.leaping.permissionsex.data.SubjectRef;
import ninja.leaping.permissionsex.util.Translatable;
import ninja.leaping.permissionsex.util.command.Commander;
import ninja.leaping.permissionsex.util.command.MessageFormatter;
import org.apache.commons.lang.LocaleUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * An abstraction over the Sponge CommandSource that handles PEX-specific message formatting and localization
 */
public class BukkitCommander implements Commander<BaseComponent> {
    private final CommandSender commandSource;
    private final BukkitMessageFormatter formatter;

    public BukkitCommander(PermissionsExPlugin pex, CommandSender commandSource) {
        this.commandSource = commandSource;
        this.formatter = new BukkitMessageFormatter(pex, commandSource);
    }

    @Override
    public String getName() {
        return this.commandSource.getName();
    }

    @Override
    public boolean hasPermission(String permission) {
        return commandSource.hasPermission(permission);
    }

    @Override
    public Locale getLocale() {
        return commandSource instanceof Player ? LocaleUtils.toLocale(((Player) commandSource).spigot().getLocale()) : Locale.getDefault();
    }

    @Override
    public Optional<SubjectRef> getSubjectIdentifier() {
        return Optional.empty();
        //return Optional.of(Maps.immutableEntry(commandSource.getContainingCollection().getIdentifier(), commandSource.getIdentifier()));
    }

    @Override
    public Set<Map.Entry<String, String>> getActiveContexts() {
        if (commandSource instanceof Player) {
            return ImmutableSet.of(Maps.immutableEntry("world", ((Player) commandSource).getWorld().getName()));
        } else {
            return ImmutableSet.of();
        }
    }

    @Override
    public MessageFormatter<BaseComponent> fmt() {
        return formatter;
    }

    private void sendMessageInternal(BaseComponent formatted) {
        if (commandSource instanceof Player) {
            ((Player) commandSource).spigot().sendMessage(formatted);
        } else {
            commandSource.sendMessage(formatted.toLegacyText());
        }
    }

    @Override
    public void msg(BaseComponent text) {
        text.setColor(ChatColor.DARK_AQUA);
        sendMessageInternal(text);
    }

    @Override
    public void debug(BaseComponent text) {
        text.setColor(ChatColor.GRAY);
        sendMessageInternal(text);
    }

    @Override
    public void error(BaseComponent text) {
        text.setColor(ChatColor.RED);
        sendMessageInternal(text);
    }

    @Override
    public void msgPaginated(Translatable title, @Nullable Translatable header, final Iterable<BaseComponent> text) {
        msg(fmt().combined("# ", fmt().tr(title), " #"));
        msg(fmt().tr(header));
        for (BaseComponent component : text) {
            msg(component);
        }
        msg(fmt().combined("#################################"));
    }
}
