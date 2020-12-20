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
package ca.stellardrift.permissionsex.sponge.command

import ca.stellardrift.permissionsex.PermissionsEx
import ca.stellardrift.permissionsex.commands.commander.Commander
import ca.stellardrift.permissionsex.commands.commander.MessageFormatter
import ca.stellardrift.permissionsex.sponge.PermissionsExPlugin
import ca.stellardrift.permissionsex.sponge.asPex
import ca.stellardrift.permissionsex.subject.SubjectRef
import ca.stellardrift.permissionsex.util.styled
import java.util.Locale
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.spongepowered.api.command.CommandCause
import org.spongepowered.api.entity.living.player.server.ServerPlayer
import org.spongepowered.api.util.locale.LocaleSource

/**
 * An abstraction over the Sponge CommandSource that handles PEX-specific message formatting and localization
 */
internal class SpongeCommander(
    val pex: PermissionsExPlugin,
    private val cause: CommandCause
) : Commander {
    override val manager: PermissionsEx<*>
        get() = pex.manager
    override val formatter: SpongeMessageFormatter = SpongeMessageFormatter(this)
    override val name: String = cause.subject.friendlyIdentifier.orElse(cause.subject.identifier)
    override val locale: Locale = cause.first(LocaleSource::class.java).map { it.locale }.orElse(Locale.getDefault())

    override fun hasPermission(permission: String): Boolean {
        return cause.hasPermission(permission)
    }

    override val subjectIdentifier: SubjectRef<*>?
        get() = pex.service?.let { cause.asSubjectReference().asPex(it) }

    override val messageColor: TextColor = NamedTextColor.DARK_AQUA

    override fun msgPaginated(
        title: Component,
        header: Component?,
        text: Iterable<Component>
    ) {
        val build = pex.game.serviceProvider.paginationService().builder()
        formatter.apply {
            build.title(title.styled { header().hl() })
            if (header != null) {
                build.header(header.color(NamedTextColor.GRAY))
            }
            build.contents(text.map { it.colorIfAbsent(messageColor) })
                .sendTo(cause.audience)
        }
    }

    override fun audience(): Audience {
        return this.cause.audience
    }
}

internal class SpongeMessageFormatter(private val cmd: SpongeCommander) : MessageFormatter(cmd, cmd.pex.manager) {

    override val <I> SubjectRef<I>.friendlyName: String?
        get() = (type().getAssociatedObject(identifier()) as? ServerPlayer)?.name // TODO: Named interface?
}
