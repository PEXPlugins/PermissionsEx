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

package ca.stellardrift.permissionsex.sponge

import ca.stellardrift.permissionsex.PermissionsEx
import ca.stellardrift.permissionsex.commands.commander.Commander
import ca.stellardrift.permissionsex.commands.commander.MessageFormatter
import ca.stellardrift.permissionsex.util.PEXComponentRenderer
import ca.stellardrift.permissionsex.util.SubjectIdentifier
import ca.stellardrift.permissionsex.util.castMap
import ca.stellardrift.permissionsex.util.coloredIfNecessary
import ca.stellardrift.permissionsex.util.styled
import ca.stellardrift.permissionsex.util.subjectIdentifier
import net.kyori.text.Component
import net.kyori.text.adapter.spongeapi.TextAdapter
import net.kyori.text.format.TextColor
import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.service.pagination.PaginationService
import org.spongepowered.api.text.channel.ChatTypeMessageReceiver
import org.spongepowered.api.text.channel.MessageReceiver
import org.spongepowered.api.text.chat.ChatType
import org.spongepowered.api.text.chat.ChatTypes
import java.util.Locale

fun MessageReceiver.sendMessage(message: Component) = TextAdapter.sendComponent(this, message)
fun Iterable<MessageReceiver>.sendMessage(message: Component) = TextAdapter.sendComponent(this, message)
fun ChatTypeMessageReceiver.sendMessage(message: Component, type: ChatType = ChatTypes.SYSTEM) = TextAdapter.sendComponent(this, message, type)
fun Iterable<ChatTypeMessageReceiver>.sendMessage(message: Component, type: ChatType = ChatTypes.SYSTEM) = TextAdapter.sendComponent(this, message, type)


/**
 * An abstraction over the Sponge CommandSource that handles PEX-specific message formatting and localization
 */
internal class SpongeCommander(
    val pex: PermissionsExPlugin,
    private val commandSource: CommandSource
) : Commander {
    override val manager: PermissionsEx<*>
        get() = pex.manager
    override val formatter: SpongeMessageFormatter = SpongeMessageFormatter(this)
    override val name: String
        get() = commandSource.name

    override fun hasPermission(permission: String): Boolean {
        return commandSource.hasPermission(permission)
    }

    override val locale: Locale
        get() = commandSource.locale

    override val subjectIdentifier: SubjectIdentifier?
        get() = subjectIdentifier(
                commandSource.containingCollection.identifier,
                commandSource.identifier
            )

    override fun msg(text: Component) {
        commandSource.sendMessage(PEXComponentRenderer.render(text coloredIfNecessary TextColor.DARK_AQUA, locale))
    }

    override fun msgPaginated(
        title: Component,
        header: Component?,
        text: Iterable<Component>
    ) {
        val build =
            pex.game.serviceManager.provide(
                PaginationService::class.java
            ).get().builder()
        formatter.apply {
            build.title(title.styled { header().hl() }.toSponge())
            if (header != null) {
                build.header(header.color(TextColor.GRAY).toSponge())
            }
            build.contents(text.map { it.color(TextColor.DARK_AQUA).toSponge() })
                .sendTo(commandSource)

        }
    }
}

internal class SpongeMessageFormatter(private val cmd: SpongeCommander) : MessageFormatter(cmd, cmd.pex.manager) {

    override val SubjectIdentifier.friendlyName: String?
        get() = cmd.pex.manager.getSubjects(key).typeInfo.getAssociatedObject(value).castMap<CommandSource, String> { name }
}
