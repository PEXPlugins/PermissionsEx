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

package ca.stellardrift.permissionsex.bungee

import ca.stellardrift.permissionsex.PermissionsEx
import ca.stellardrift.permissionsex.bungeetext.BungeeMessageFormatter
import ca.stellardrift.permissionsex.proxycommon.IDENT_SERVER_CONSOLE
import ca.stellardrift.permissionsex.util.Translatable
import ca.stellardrift.permissionsex.util.castMap
import ca.stellardrift.permissionsex.util.command.Commander
import ca.stellardrift.permissionsex.util.command.MessageFormatter
import com.google.common.collect.Maps.immutableEntry
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.connection.ProxiedPlayer
import java.util.Locale
import java.util.Optional


class BungeeCommander(pex: PermissionsExPlugin, private val src: CommandSender) : Commander<BaseComponent> {
    private val formatter = BungeePluginMessageFormatter(pex, this)
    override fun getName(): String {
        return src.name
    }

    override fun getLocale(): Locale {
        return (src as? ProxiedPlayer)?.locale ?: Locale.getDefault()
    }

    override fun getSubjectIdentifier(): Optional<Map.Entry<String, String>> {
        return Optional.ofNullable(when (src) {
            is ProxiedPlayer -> immutableEntry(PermissionsEx.SUBJECTS_USER, src.uniqueId.toString())
            else -> IDENT_SERVER_CONSOLE
        })
    }

    override fun hasPermission(permission: String?): Boolean {
        return src.hasPermission(permission)
    }

    override fun fmt(): MessageFormatter<BaseComponent> {
        return formatter
    }

    override fun msg(text: BaseComponent) {
        text.color = ChatColor.GOLD
        src.sendMessage(text)
    }

    override fun debug(text: BaseComponent) {
        text.color = ChatColor.GRAY
        src.sendMessage(text)
    }

    override fun error(text: BaseComponent) {
        text.color = ChatColor.RED
        src.sendMessage(text)
    }

    override fun msgPaginated(title: Translatable, header: Translatable?, text: Iterable<BaseComponent>) {
        msg(fmt().combined("# ", fmt().tr(title), " #"))
        if (header != null) {
            msg(fmt().tr(header))
        }
        text.forEach(this::msg)

        msg(fmt().combined("#################################"))
    }

}

class BungeePluginMessageFormatter(pex: PermissionsExPlugin, sender: BungeeCommander) : BungeeMessageFormatter(sender, pex.manager, hlColour = ChatColor.YELLOW, callbacks = pex.callbackController) {

    override fun getFriendlyName(subj: Map.Entry<String, String>): String? {
        return pex.getSubjects(subj.key).typeInfo.getAssociatedObject(subj.value).castMap<CommandSender, String> { name }
    }

}
