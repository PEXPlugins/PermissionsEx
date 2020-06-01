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
import ca.stellardrift.permissionsex.commands.commander.Commander
import ca.stellardrift.permissionsex.commands.commander.MessageFormatter
import ca.stellardrift.permissionsex.proxycommon.IDENT_SERVER_CONSOLE
import ca.stellardrift.permissionsex.util.PEXComponentRenderer
import ca.stellardrift.permissionsex.util.SubjectIdentifier
import ca.stellardrift.permissionsex.util.castMap
import ca.stellardrift.permissionsex.util.coloredIfNecessary
import ca.stellardrift.permissionsex.util.subjectIdentifier
import java.util.Locale
import net.kyori.text.Component
import net.kyori.text.adapter.bungeecord.TextAdapter
import net.kyori.text.format.TextColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.connection.ProxiedPlayer

fun Iterable<CommandSender>.sendMessage(text: Component) = TextAdapter.sendComponent(this, text)
fun CommandSender.sendMessage(text: Component) = TextAdapter.sendComponent(this, text)

class BungeeCommander(internal val pex: PermissionsExPlugin, private val src: CommandSender) :
    Commander {
    override val manager: PermissionsEx<*>
        get() = pex.manager
    override val formatter = BungeePluginMessageFormatter(this)
    override val name: String get() = src.name

    override val locale: Locale get() =
        (src as? ProxiedPlayer)?.locale ?: Locale.getDefault()

    override val subjectIdentifier: SubjectIdentifier?
        get() = when (src) {
            is ProxiedPlayer -> subjectIdentifier(PermissionsEx.SUBJECTS_USER, src.uniqueId.toString())
            else -> IDENT_SERVER_CONSOLE
        }

    override fun hasPermission(permission: String): Boolean {
        return src.hasPermission(permission)
    }

    override fun msg(text: Component) {
        src.sendMessage(PEXComponentRenderer.render(text coloredIfNecessary TextColor.GOLD, locale))
    }
}

class BungeePluginMessageFormatter(val sender: BungeeCommander) : MessageFormatter(sender, sender.pex.manager, hlColor = TextColor.YELLOW) {

    override val SubjectIdentifier.friendlyName: String? get() {
        return sender.pex.manager.getSubjects(key).typeInfo.getAssociatedObject(value).castMap<CommandSender, String> { name }
    }

    /**
     * On Bungee and other proxies, we have an extra `/` in front of commands -- this makes clickables work properly
     */
    override fun transformCommand(cmd: String): String {
        return "/$cmd"
    }
}
