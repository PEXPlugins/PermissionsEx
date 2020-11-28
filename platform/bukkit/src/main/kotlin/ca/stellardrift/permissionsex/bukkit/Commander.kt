/*
 * PermissionsEx - a permissions plugin for your server ecosystem
 * Copyright © 2021 zml [at] stellardrift [dot] ca and PermissionsEx contributors
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

import ca.stellardrift.permissionsex.minecraft.command.Commander
import ca.stellardrift.permissionsex.minecraft.command.MessageFormatter
import ca.stellardrift.permissionsex.subject.SubjectRef
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

internal class BukkitMessageFormatter(plugin: PermissionsExPlugin) : MessageFormatter(plugin.mcManager) {

    override fun <I : Any?> friendlyName(reference: SubjectRef<I>): String? {
        return (reference.type().getAssociatedObject(reference.identifier()) as? CommandSender)?.name
    }
}

/**
 * An abstraction over the Sponge CommandSource that handles PEX-specific message formatting and localization
 */
internal class BukkitCommander internal constructor(
    private val pex: PermissionsExPlugin,
    internal val commandSource: CommandSender
) : Commander {
    private val formatter: BukkitMessageFormatter = BukkitMessageFormatter(pex)

    override fun hasPermission(permission: String): Boolean = commandSource.hasPermission(permission)

    private val audience = this.pex.adventure.sender(this.commandSource)

    override fun audience(): Audience {
        return this.audience
    }

    override fun name(): Component {
        return text(commandSource.name)
    }

    override fun subjectIdentifier(): SubjectRef<*>? {
        return if (commandSource is Player) SubjectRef.subject(pex.userSubjects.type(), commandSource.uniqueId) else null
    }

    override fun formatter(): MessageFormatter {
        return this.formatter
    }
}
