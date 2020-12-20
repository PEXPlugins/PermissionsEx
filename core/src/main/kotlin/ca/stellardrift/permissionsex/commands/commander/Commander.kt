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
package ca.stellardrift.permissionsex.commands.commander

import ca.stellardrift.permissionsex.PermissionsEx
import ca.stellardrift.permissionsex.commands.Messages
import ca.stellardrift.permissionsex.commands.parse.CommandException
import ca.stellardrift.permissionsex.subject.SubjectRef
import ca.stellardrift.permissionsex.util.PEXComponentRenderer
import ca.stellardrift.permissionsex.util.SubjectIdentifier
import ca.stellardrift.permissionsex.util.component
import ca.stellardrift.permissionsex.util.invoke
import ca.stellardrift.permissionsex.util.join
import ca.stellardrift.permissionsex.util.unaryMinus
import ca.stellardrift.permissionsex.util.unaryPlus
import java.util.Locale
import net.kyori.adventure.audience.ForwardingAudience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor

/**
 * Interface implemented by objects that can execute commands and receive command output
 */
interface Commander : ForwardingAudience.Single {
    val manager: PermissionsEx<*>
    val name: String
    val locale: Locale
    val subjectIdentifier: SubjectRef<*>?
    val messageColor: TextColor

    fun hasPermission(permission: String): Boolean

    fun hasPermission(permission: Permission): Boolean {
        return hasPermission(permission.value)
    }

    val formatter: MessageFormatter

    @JvmDefault
    fun msg(cb: MessageFormatter.(send: (Component) -> Unit) -> Unit) {
        formatter.cb { msg(it) }
    }

    @JvmDefault
    fun debug(cb: MessageFormatter.(send: (Component) -> Unit) -> Unit) {
        formatter.cb { debug(it) }
    }

    @JvmDefault
    fun error(err: Throwable? = null, cb: MessageFormatter.(send: (Component) -> Unit) -> Unit) {
        formatter.cb { error(it, err) } // TODO: Does this make the most sense
    }

    /**
     * Send a message to this Commander. The message should be colored the appropriate output colour if it does not yet have a colour
     */
    fun msg(text: Component) {
        sendMessage(PEXComponentRenderer.render(text.colorIfAbsent(messageColor), locale))
    }

    /**
     * Send debug text
     */
    @JvmDefault
    fun debug(text: Component) {
        msg(text.colorIfAbsent(NamedTextColor.GRAY))
    }

    @JvmDefault
    fun error(text: Component, err: Throwable? = null) {
        val hoverText = when {
            err == null -> null
            !hasPermission("permissionsex.show-stacktrace-on-hover") -> null
            else -> (-"The error that occurred was:") {
                for (line in err.stackTrace) {
                    append(Component.newline())
                    append(+line.toString().replace("\t", "    "))
                }
            }
        }

        msg(if (hoverText == null) {
            text.colorIfAbsent(NamedTextColor.RED)
        } else {
            component {
                append(text)
                color(NamedTextColor.RED)
                hoverEvent(HoverEvent.showText(hoverText))
            }
        })
    }

    @JvmDefault
    fun msgPaginated(
        title: Component,
        header: Component? = null,
        text: Iterable<Component>
    ) {
        msg { send ->
            val marker = +"#"
            send(listOf(marker, title, marker).join(Component.space()))
            if (header != null) {
                send(header)
            }
            text.forEach(send)
            send(+"#############################")
        }
    }

    @JvmDefault
    @Throws(CommandException::class)
    fun checkSubjectPermission(
        subject: SubjectIdentifier,
        basePermission: String
    ) {
        if (!hasPermission("$basePermission.${subject.key}.${subject.value}") &&
                (subject != subjectIdentifier || !hasPermission("$basePermission.own"))
        ) {
            throw CommandException(Messages.EXECUTOR_ERROR_NO_PERMISSION())
        }
    }
}
