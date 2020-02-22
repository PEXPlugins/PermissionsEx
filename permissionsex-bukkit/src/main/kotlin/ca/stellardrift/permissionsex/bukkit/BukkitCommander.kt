/*
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

package ca.stellardrift.permissionsex.bukkit

import ca.stellardrift.permissionsex.PermissionsEx
import ca.stellardrift.permissionsex.commands.commander.Commander
import ca.stellardrift.permissionsex.util.Translatable
import com.google.common.collect.Maps
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.Locale
import java.util.Optional

/**
 * An abstraction over the Sponge CommandSource that handles PEX-specific message formatting and localization
 */
class BukkitCommander internal constructor(
    pex: PermissionsExPlugin,
    private val commandSource: CommandSender
) : Commander<BaseComponent> {
    override val formatter: BukkitMessageFormatter = BukkitMessageFormatter(pex, this)
    override val name: String
        get() = commandSource.name

    override fun hasPermission(permission: String): Boolean = commandSource.hasPermission(permission)

    override val locale: Locale
        get() = if (commandSource is Player) BukkitMessageFormatter.toLocale(commandSource.locale) else Locale.getDefault()

    override val subjectIdentifier: Optional<Map.Entry<String, String>>
        get() = if (commandSource is Player) {
            Optional.of(
                Maps.immutableEntry(
                    PermissionsEx.SUBJECTS_USER,
                    commandSource.uniqueId.toString()
                )
            )
        } else Optional.empty()

    private fun sendMessageInternal(formatted: BaseComponent) {
        if (commandSource is Player) {
            commandSource.spigot().sendMessage(formatted)
        } else {
            commandSource.sendMessage(formatted.toLegacyText())
        }
    }

    override fun msg(text: BaseComponent) {
        text.color = ChatColor.DARK_AQUA
        sendMessageInternal(text)
    }

    override fun debug(text: BaseComponent) {
        text.color = ChatColor.GRAY
        sendMessageInternal(text)
    }

    override fun error(text: BaseComponent, err: Throwable?) {
        text.color = ChatColor.RED
        sendMessageInternal(text)
    }

    override fun msgPaginated(
        title: Translatable,
        header: Translatable?,
        text: Iterable<BaseComponent>
    ) {
        msg { send ->
            send(combined("# ", title, " #"))
            if (header != null) {
                send(-header)
            }
            text.forEach(send)
            send(-"#############################")
        }
    }


}
