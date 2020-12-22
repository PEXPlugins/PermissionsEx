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

import ca.stellardrift.permissionsex.impl.PermissionsEx
import ca.stellardrift.permissionsex.impl.commands.commander.Commander
import ca.stellardrift.permissionsex.impl.commands.commander.MessageFormatter
import ca.stellardrift.permissionsex.subject.SubjectRef
import ca.stellardrift.permissionsex.util.styled
import java.util.Locale
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.service.pagination.PaginationService

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
    private val audience = pex.adventure.receiver(commandSource)

    override fun hasPermission(permission: String): Boolean {
        return commandSource.hasPermission(permission)
    }

    override val locale: Locale
        get() = commandSource.locale

    override val subjectIdentifier: SubjectRef<*>
        get() = PEXSubjectReference.of(
            commandSource.asSubjectReference(),
            pex
        )
    override val messageColor: TextColor = NamedTextColor.DARK_AQUA

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
                build.header(header.color(NamedTextColor.GRAY).toSponge())
            }
            build.contents(text.map { it.color(messageColor).toSponge() })
                .sendTo(commandSource)
        }
    }

    override fun audience(): Audience {
        return this.audience
    }
}

internal class SpongeMessageFormatter(private val cmd: SpongeCommander) : MessageFormatter(cmd, cmd.pex.manager) {

    override val <I> SubjectRef<I>.friendlyName: String?
        get() = (this.type().getAssociatedObject(this.identifier()) as? CommandSource)?.name
}
