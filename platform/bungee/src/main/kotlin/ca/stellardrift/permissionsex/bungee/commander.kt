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
import ca.stellardrift.permissionsex.util.SubjectIdentifier
import ca.stellardrift.permissionsex.util.subjectIdentifier
import java.util.Locale
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.connection.ProxiedPlayer

class BungeeCommander(internal val pex: PermissionsExPlugin, private val src: CommandSender) :
    Commander {
    override val messageColor: TextColor = NamedTextColor.GOLD
    private val audience = pex.adventure.audience(src)
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

    override fun audience(): Audience {
        return this.audience
    }

    override fun hasPermission(permission: String): Boolean {
        return src.hasPermission(permission)
    }
}

class BungeePluginMessageFormatter(val sender: BungeeCommander) : MessageFormatter(sender, sender.pex.manager, hlColor = NamedTextColor.YELLOW) {

    override val SubjectIdentifier.friendlyName: String? get() {
        return (sender.pex.manager.getSubjects(key).typeInfo.getAssociatedObject(value) as? CommandSender)?.name
    }

    /**
     * On Bungee and other proxies, we have an extra `/` in front of commands -- this makes clickables work properly
     */
    override fun transformCommand(cmd: String): String {
        return "/$cmd"
    }
}
