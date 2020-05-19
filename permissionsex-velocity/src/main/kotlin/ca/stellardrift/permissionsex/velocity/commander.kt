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

package ca.stellardrift.permissionsex.velocity

import ca.stellardrift.permissionsex.PermissionsEx
import ca.stellardrift.permissionsex.PermissionsEx.SUBJECTS_USER
import ca.stellardrift.permissionsex.commands.commander.Commander
import ca.stellardrift.permissionsex.commands.commander.MessageFormatter
import ca.stellardrift.permissionsex.commands.parse.CommandException
import ca.stellardrift.permissionsex.commands.parse.CommandSpec
import ca.stellardrift.permissionsex.proxycommon.IDENT_SERVER_CONSOLE
import ca.stellardrift.permissionsex.util.PEXComponentRenderer
import ca.stellardrift.permissionsex.util.SubjectIdentifier
import ca.stellardrift.permissionsex.util.coloredIfNecessary
import com.google.common.collect.Maps
import com.velocitypowered.api.command.Command
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import net.kyori.text.Component
import net.kyori.text.format.TextColor
import java.util.Locale

class VelocityCommand(private val pex: PermissionsExPlugin, val cmd: CommandSpec) : Command {

    override fun execute(source: CommandSource, args: Array<out String>) {
        val src = VelocityCommander(pex, source)
        cmd.process(src, args.joinToString(" "))
    }

    override fun suggest(source: CommandSource, currentArgs: Array<out String>): List<String> {
        val src = VelocityCommander(pex, source)
        return cmd.tabComplete(src, currentArgs.joinToString(" "))
    }

    override fun hasPermission(source: CommandSource, args: Array<out String>): Boolean {
        return try {
            cmd.checkPermission(VelocityCommander(pex, source))
            true
        } catch (e: CommandException) {
            false
        }
    }
}

class VelocityCommander(internal val pex: PermissionsExPlugin, private val src: CommandSource) :
    Commander {
    override val manager: PermissionsEx<*>
        get() = pex.manager
    override val formatter = VelocityMessageFormatter(this)
    override val name: String
        get() =
            (src as? Player)?.username ?: IDENT_SERVER_CONSOLE.value

    override val locale: Locale
        get() =
            (src as? Player)?.playerSettings?.locale ?: Locale.getDefault()

    override val subjectIdentifier: SubjectIdentifier?
        get() = when (src) {
                    is Player -> Maps.immutableEntry(SUBJECTS_USER, src.uniqueId.toString())
                    else -> IDENT_SERVER_CONSOLE
                }

    override fun hasPermission(permission: String): Boolean {
        return src.hasPermission(permission)
    }

    override fun msg(text: Component) {
        src.sendMessage(PEXComponentRenderer.render(text coloredIfNecessary TextColor.GOLD, locale))
    }

}

class VelocityMessageFormatter(val vCmd: VelocityCommander) :
    MessageFormatter(vCmd, vCmd.pex.manager, TextColor.YELLOW) {
    override fun transformCommand(cmd: String) = "/$cmd"
}


