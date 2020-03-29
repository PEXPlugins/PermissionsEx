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
import ca.stellardrift.permissionsex.commands.commander.MessageFormatter
import ca.stellardrift.permissionsex.commands.commander.Commander
import ca.stellardrift.permissionsex.proxycommon.IDENT_SERVER_CONSOLE
import ca.stellardrift.permissionsex.util.PEXComponentRenderer
import ca.stellardrift.permissionsex.util.castMap
import ca.stellardrift.permissionsex.util.coloredIfNecessary
import ca.stellardrift.permissionsex.util.join
import ca.stellardrift.permissionsex.util.unaryPlus
import com.google.common.collect.Maps.immutableEntry
import net.kyori.text.BuildableComponent
import net.kyori.text.Component
import net.kyori.text.ComponentBuilder
import net.kyori.text.TextComponent
import net.kyori.text.adapter.bungeecord.TextAdapter
import net.kyori.text.event.ClickEvent
import net.kyori.text.format.TextColor
import net.kyori.text.format.TextDecoration
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.connection.ProxiedPlayer
import java.util.Locale
import java.util.Optional

fun Iterable<CommandSender>.sendMessage(text: Component) = TextAdapter.sendComponent(this, text)
fun CommandSender.sendMessage(text: Component) = TextAdapter.sendComponent(this, text)

class BungeeCommander(pex: PermissionsExPlugin, private val src: CommandSender) :
    Commander {
    override val formatter = BungeePluginMessageFormatter(pex, this)
    override val name: String get() = src.name

    override val locale: Locale get() =
        (src as? ProxiedPlayer)?.locale ?: Locale.getDefault()

    override val subjectIdentifier: Optional<Map.Entry<String, String>>
        get() = Optional.ofNullable(when (src) {
            is ProxiedPlayer -> immutableEntry(PermissionsEx.SUBJECTS_USER, src.uniqueId.toString())
            else -> IDENT_SERVER_CONSOLE
        })

    override fun hasPermission(permission: String): Boolean {
        return src.hasPermission(permission)
    }

    private fun msgInternal(text: Component) {
        src.sendMessage(PEXComponentRenderer.render(text, locale))
    }

    override fun msg(text: Component) {
        msgInternal(text coloredIfNecessary TextColor.GOLD)
    }

    override fun debug(text: Component) {
        msgInternal(text coloredIfNecessary TextColor.GRAY)
    }

    override fun error(text: Component, err: Throwable?) {
        msgInternal(text coloredIfNecessary TextColor.RED)
    }

    override fun msgPaginated(title: Component, header: Component?, text: Iterable<Component>) {
        msg { send ->
            val marker = +"#"
            send(listOf(marker, title, marker).join(TextComponent.space()))
            if (header != null) {
                send(header)
            }
            text.forEach(send)
            send(+"#############################")
        }
    }

}

class BungeePluginMessageFormatter(val pex: PermissionsExPlugin, sender: BungeeCommander) : MessageFormatter(sender, pex.manager, hlColor = TextColor.YELLOW) {

    override val Map.Entry<String, String>.friendlyName: String? get() {
        return pex.manager.getSubjects(key).typeInfo.getAssociatedObject(value).castMap<CommandSender, String> { name }
    }

    /**
     * On Bungee and other proxies, we have an extra `/` in front of commands -- this makes clickables work properly
     */
    override fun transformCommand(cmd: String): String {
        return "/$cmd"
    }

    override fun <C : BuildableComponent<C, B>, B : ComponentBuilder<C, B>> B.callback(func: (Commander) -> Unit): B {
        val command = pex.callbackController.registerCallback(cmd) { func(it) }

        decoration(TextDecoration.UNDERLINED, true)
        color(hlColor)
        clickEvent(ClickEvent.runCommand(transformCommand(command)))
        return this
    }

}
