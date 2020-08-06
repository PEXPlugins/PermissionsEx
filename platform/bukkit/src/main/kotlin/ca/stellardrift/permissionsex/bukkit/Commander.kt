/*
 * PermissionsEx - a permissions plugin for your server ecosystem
 * Copyright © 2020 zml [at] stellardrift [dot] ca and PermissionsEx contributors
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

package ca.stellardrift.permissionsex.bukkit

import ca.stellardrift.permissionsex.PermissionsEx
import ca.stellardrift.permissionsex.bukkit.Compatibility.getLocale
import ca.stellardrift.permissionsex.commands.commander.Commander
import ca.stellardrift.permissionsex.commands.commander.MessageFormatter
import ca.stellardrift.permissionsex.commands.parse.CommandSpec
import ca.stellardrift.permissionsex.util.PEXComponentRenderer
import ca.stellardrift.permissionsex.util.SubjectIdentifier
import ca.stellardrift.permissionsex.util.coloredIfNecessary
import ca.stellardrift.permissionsex.util.subjectIdentifier
import java.util.Locale
import net.kyori.text.Component
import net.kyori.text.adapter.bukkit.TextAdapter
import net.kyori.text.format.TextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

fun Iterable<CommandSender>.sendMessage(text: Component) = TextAdapter.sendMessage(this, text)
fun Iterable<CommandSender>.sendActionBar(text: Component) = TextAdapter.sendActionBar(this, text)

fun CommandSender.sendMessage(text: Component) = TextAdapter.sendMessage(this, text)
fun CommandSender.sendActionBar(text: Component) = TextAdapter.sendActionBar(this, text)

/**
 * Take a locale string provided from a minecraft client and attempt to parse it as a locale.
 * These are not strictly compliant with the iso standard, so we try to make things a bit more normalized.
 *
 * @param mcLocaleString The locale string, in the format provided by the Minecraft client
 * @return A Locale object matching the provided locale string
 */
fun String.toLocale(): Locale {
    val parts = this.split("_", limit = 3)
    return when (parts.size) {
        0 -> Locale.getDefault()
        1 -> Locale(parts[0])
        2 -> Locale(parts[0], parts[1])
        3 -> Locale(parts[0], parts[1], parts[2])
        else -> throw IllegalArgumentException("Provided locale '$this' was not in a valid format!")
    }
}

class BukkitMessageFormatter(private val cmd: BukkitCommander) : MessageFormatter(cmd, cmd.pex.manager) {

    override val Map.Entry<String, String>.friendlyName: String?
        get() = (cmd.pex.manager.getSubjects(key).typeInfo.getAssociatedObject(value) as? CommandSender)?.name
}

/**
 * An abstraction over the Sponge CommandSource that handles PEX-specific message formatting and localization
 */
class BukkitCommander internal constructor(
    internal val pex: PermissionsExPlugin,
    private val commandSource: CommandSender
) : Commander {
    override val manager: PermissionsEx<*>
        get() = pex.manager
    override val formatter: BukkitMessageFormatter = BukkitMessageFormatter(this)
    override val name: String
        get() = commandSource.name

    override fun hasPermission(permission: String): Boolean = commandSource.hasPermission(permission)

    override val locale: Locale
        get() = if (commandSource is Player) getLocale(commandSource) else Locale.getDefault()

    override val subjectIdentifier: SubjectIdentifier?
        get() = if (commandSource is Player) {
                subjectIdentifier(
                    PermissionsEx.SUBJECTS_USER,
                    commandSource.uniqueId.toString()
                )
        } else null

    override fun msg(text: Component) {
        commandSource.sendMessage(PEXComponentRenderer.render(text coloredIfNecessary TextColor.DARK_AQUA, locale))
    }
}

/**
 * Wrapper class between PEX commands and the Bukkit command class
 */
class PEXBukkitCommand(private val command: CommandSpec, private val plugin: PermissionsExPlugin) : CommandExecutor, TabExecutor {
    override fun onCommand(sender: CommandSender, command: Command, alias: String, args: Array<String>): Boolean {
        this.command.process(BukkitCommander(plugin, sender), args.joinToString(" "))
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        return this.command.tabComplete(BukkitCommander(plugin, sender), args.joinToString(" "))
    }
}
