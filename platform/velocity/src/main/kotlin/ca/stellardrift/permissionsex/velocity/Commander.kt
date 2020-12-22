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

import ca.stellardrift.permissionsex.impl.PermissionsEx
import ca.stellardrift.permissionsex.impl.commands.commander.Commander
import ca.stellardrift.permissionsex.impl.commands.commander.MessageFormatter
import ca.stellardrift.permissionsex.impl.commands.parse.CommandException
import ca.stellardrift.permissionsex.impl.commands.parse.CommandSpec
import ca.stellardrift.permissionsex.proxycommon.ProxyCommon.IDENT_SERVER_CONSOLE
import ca.stellardrift.permissionsex.subject.SubjectRef
import com.velocitypowered.api.command.Command
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import java.util.Locale
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor

internal class VelocityCommand(private val pex: PermissionsExPlugin, val cmd: CommandSpec) : Command {

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

internal class VelocityCommander(internal val pex: PermissionsExPlugin, private val src: CommandSource) :
    Commander {
    override val manager: PermissionsEx<*>
        get() = pex.manager
    override val formatter = VelocityMessageFormatter(this)

    override val name: String
        get() =
            (src as? Player)?.username ?: IDENT_SERVER_CONSOLE.identifier()

    override val locale: Locale
        get() =
            (src as? Player)?.playerSettings?.locale ?: Locale.getDefault()

    override val subjectIdentifier: SubjectRef<*>?
        get() = when (src) {
                    is Player -> SubjectRef.subject(pex.users.type(), src.uniqueId)
                    else -> IDENT_SERVER_CONSOLE
                }

    override val messageColor: TextColor get() = NamedTextColor.GOLD

    override fun hasPermission(permission: String): Boolean {
        return src.hasPermission(permission)
    }

    override fun audience(): Audience {
        return this.src
    }
}

internal class VelocityMessageFormatter(vCmd: VelocityCommander) :
    MessageFormatter(vCmd, vCmd.pex.manager, NamedTextColor.YELLOW) {
    override fun transformCommand(cmd: String) = "/$cmd"
}
