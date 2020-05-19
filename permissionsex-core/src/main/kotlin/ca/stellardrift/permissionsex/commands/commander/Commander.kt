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
import ca.stellardrift.permissionsex.util.SubjectIdentifier
import ca.stellardrift.permissionsex.util.coloredIfNecessary
import ca.stellardrift.permissionsex.util.component
import ca.stellardrift.permissionsex.util.invoke
import ca.stellardrift.permissionsex.util.join
import ca.stellardrift.permissionsex.util.unaryMinus
import ca.stellardrift.permissionsex.util.unaryPlus
import net.kyori.text.Component
import net.kyori.text.TextComponent
import net.kyori.text.event.HoverEvent
import net.kyori.text.format.TextColor
import java.util.Locale

/**
 * Interface implemented by objects that can execute commands and receive command output
 */
interface Commander {
    val manager: PermissionsEx<*>
    val name: String
    val locale: Locale
    val subjectIdentifier: SubjectIdentifier?

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
        formatter.cb { error(it, err)} // TODO: Does this make the most sense
    }

    /**
     * Send a message to this Commander. The message should be colored the appropriate output colour if it does not yet have a colour
     */
    fun msg(text: Component)

    /**
     * Send debug text
     */
    @JvmDefault
    fun debug(text: Component) {
        msg(text coloredIfNecessary TextColor.GRAY)
    }

    @JvmDefault
    fun error(text: Component, err: Throwable? = null) {
        val hoverText = when {
            err == null -> null
            !hasPermission("permissionsex.show-stacktrace-on-hover") -> null
            else -> (-"The error that occurred was:") {
                for (line in err.stackTrace) {
                    append(TextComponent.newline())
                    append(line.toString().replace("\t", "    "))
                }
            }
        }

        msg(if (hoverText == null) {
            text coloredIfNecessary TextColor.RED
        } else {
            component {
                append(text)
                color(TextColor.RED)
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
            send(listOf(marker, title, marker).join(TextComponent.space()))
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
        if (!hasPermission("$basePermission.${subject.key}.${subject.value}")
                && (subject != subjectIdentifier || !hasPermission("$basePermission.own"))
        ) {
            throw CommandException(Messages.EXECUTOR_ERROR_NO_PERMISSION())
        }
    }
}
