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

import ca.stellardrift.permissionsex.commands.commander.Commander
import ca.stellardrift.permissionsex.util.Translatable
import com.google.common.collect.Maps
import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.service.pagination.PaginationService
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.format.TextColors
import java.util.Locale
import java.util.Optional

/**
 * An abstraction over the Sponge CommandSource that handles PEX-specific message formatting and localization
 */
internal class SpongeCommander(
    private val pex: PermissionsExPlugin,
    private val commandSource: CommandSource
) : Commander<Text.Builder> {
    override val formatter: SpongeMessageFormatter = SpongeMessageFormatter(pex, this)
    override val name: String
        get() = commandSource.name

    override fun hasPermission(permission: String): Boolean {
        return commandSource.hasPermission(permission)
    }

    override val locale: Locale
        get() = commandSource.locale

    override val subjectIdentifier: Optional<Map.Entry<String, String>>
        get() = Optional.of(
            Maps.immutableEntry(
                commandSource.containingCollection.identifier,
                commandSource.identifier
            )
        )

    override fun msg(text: Text.Builder) {
        commandSource.sendMessage(text.color(TextColors.DARK_AQUA).build())
    }

    override fun debug(text: Text.Builder) {
        commandSource.sendMessage(text.color(TextColors.GRAY).build())
    }

    override fun error(text: Text.Builder, err: Throwable?) {
        commandSource.sendMessage(text.color(TextColors.RED).build())
    }

    override fun msgPaginated(
        title: Translatable,
        header: Translatable?,
        text: Iterable<Text.Builder>
    ) {
        val build =
            pex.game.serviceManager.provide(
                PaginationService::class.java
            ).get().builder()
        formatter.apply {
            build.title(title.tr().header().hl().build())
            if (header != null) {
                build.header(header.tr().color(TextColors.GRAY).build())
            }
            build.contents(text.map { it.color(TextColors.DARK_AQUA).build() })
                .sendTo(commandSource)

        }
    }

}
