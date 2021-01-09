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

import ca.stellardrift.permissionsex.minecraft.MinecraftPermissionsEx
import ca.stellardrift.permissionsex.minecraft.command.Commander
import ca.stellardrift.permissionsex.minecraft.command.MessageFormatter
import ca.stellardrift.permissionsex.sponge.PermissionsExPlugin
import ca.stellardrift.permissionsex.sponge.asPex
import ca.stellardrift.permissionsex.subject.SubjectRef
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.format.NamedTextColor
import org.spongepowered.api.command.CommandCause
import org.spongepowered.api.util.Nameable

/**
 * An abstraction over the Sponge CommandSource that handles PEX-specific message formatting and localization
 */
internal class SpongeCommander(
    private val pex: PermissionsExPlugin,
    private val cause: CommandCause
) : Commander {

    override fun hasPermission(permission: String): Boolean {
        return cause.hasPermission(permission)
    }

    override fun sendPaginated(
        title: ComponentLike,
        header: ComponentLike?,
        text: Iterable<ComponentLike>
    ) {
        val build = pex.game.serviceProvider.paginationService().builder()
        formatter().apply {
            build.title(title.asComponent().style { header(hl(it)) })
            if (header != null) {
                build.header(header.asComponent().color(NamedTextColor.GRAY))
            }
            build.contents(text.map { it.asComponent().colorIfAbsent(formatter().responseColor()) })
                .sendTo(cause.audience)
        }
    }

    override fun audience(): Audience {
        return this.cause.audience
    }

    override fun name(): Component {
        return text(cause.subject.friendlyIdentifier.orElse(cause.subject.identifier))
    }

    override fun subjectIdentifier(): SubjectRef<*>? {
        return pex.service?.let { cause.asSubjectReference().asPex(it) }
    }

    override fun formatter(): MessageFormatter {
        return this.pex.mcManager.messageFormatter()
    }
}

internal class SpongeMessageFormatter(manager: MinecraftPermissionsEx<*>) : MessageFormatter(manager) {

    override fun <I> friendlyName(reference: SubjectRef<I>): String? {
        return (reference.type().getAssociatedObject(reference.identifier()) as? Nameable)?.name
    }
}
