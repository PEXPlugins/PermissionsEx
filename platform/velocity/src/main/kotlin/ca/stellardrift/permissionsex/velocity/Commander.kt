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

import ca.stellardrift.permissionsex.minecraft.command.Commander
import ca.stellardrift.permissionsex.minecraft.command.MessageFormatter
import ca.stellardrift.permissionsex.proxycommon.ProxyCommon.IDENT_SERVER_CONSOLE
import ca.stellardrift.permissionsex.subject.SubjectRef
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor

internal class VelocityCommander(private val pex: PermissionsExPlugin, private val src: CommandSource) : Commander {
    private val formatter = VelocityMessageFormatter(pex)

    override fun hasPermission(permission: String): Boolean {
        return src.hasPermission(permission)
    }

    override fun audience(): Audience {
        return this.src
    }

    override fun name(): Component {
        return text((src as? Player)?.username ?: IDENT_SERVER_CONSOLE.identifier())
    }

    override fun subjectIdentifier(): SubjectRef<*>? {
        return when (src) {
            is Player -> SubjectRef.subject(pex.users.type(), src.uniqueId)
            else -> IDENT_SERVER_CONSOLE
        }
    }

    override fun formatter(): MessageFormatter {
        return this.formatter
    }
}

internal class VelocityMessageFormatter(plugin: PermissionsExPlugin) :
    MessageFormatter(plugin.mcManager, NamedTextColor.GOLD, NamedTextColor.YELLOW) {
    override fun transformCommand(cmd: String) = "/$cmd"
}
