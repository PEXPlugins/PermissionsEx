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
package ca.stellardrift.permissionsex.bungee

import ca.stellardrift.permissionsex.minecraft.MinecraftPermissionsEx
import ca.stellardrift.permissionsex.minecraft.command.Commander
import ca.stellardrift.permissionsex.minecraft.command.MessageFormatter
import ca.stellardrift.permissionsex.proxycommon.ProxyCommon
import ca.stellardrift.permissionsex.proxycommon.ProxyCommon.IDENT_SERVER_CONSOLE
import ca.stellardrift.permissionsex.subject.SubjectRef
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.connection.ProxiedPlayer

internal class BungeeCommander(
    private val pex: PermissionsExPlugin,
    internal val src: CommandSender
) : Commander {
    private val audience = pex.adventure.sender(src)

    override fun audience(): Audience = this.audience
    override fun name(): Component = text(src.name)
    override fun subjectIdentifier(): SubjectRef<*> = when (this.src) {
        is ProxiedPlayer -> SubjectRef.subject(pex.users.type(), src.uniqueId)
        else -> IDENT_SERVER_CONSOLE
    }
    override fun formatter(): MessageFormatter = this.pex.mcManager.messageFormatter()
    override fun hasPermission(permission: String): Boolean = this.src.hasPermission(permission)
}

internal class BungeePluginMessageFormatter(manager: MinecraftPermissionsEx<*>) : MessageFormatter(
    manager,
    NamedTextColor.GOLD,
    NamedTextColor.YELLOW
) {

    override fun <I> friendlyName(reference: SubjectRef<I>): String? {
        return (reference.type().getAssociatedObject(reference.identifier()) as? CommandSender)?.name
    }

    /**
     * On Bungee and other proxies, we have an extra `/` in front of commands -- this makes clickables work properly
     */
    override fun transformCommand(cmd: String): String {
        return "${ProxyCommon.PROXY_COMMAND_PREFIX}$cmd"
    }
}
