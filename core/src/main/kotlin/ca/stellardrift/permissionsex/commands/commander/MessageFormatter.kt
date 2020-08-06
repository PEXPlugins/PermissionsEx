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
import ca.stellardrift.permissionsex.data.SubjectDataReference
import ca.stellardrift.permissionsex.subject.CalculatedSubject
import ca.stellardrift.permissionsex.subject.SubjectType
import ca.stellardrift.permissionsex.util.SubjectIdentifier
import ca.stellardrift.permissionsex.util.colored
import ca.stellardrift.permissionsex.util.component
import ca.stellardrift.permissionsex.util.decorated
import ca.stellardrift.permissionsex.util.plusAssign
import ca.stellardrift.permissionsex.util.unaryPlus
import com.google.common.collect.Maps
import java.util.concurrent.ExecutionException
import net.kyori.text.BuildableComponent
import net.kyori.text.Component
import net.kyori.text.ComponentBuilder
import net.kyori.text.TextComponent
import net.kyori.text.TextComponent.space
import net.kyori.text.event.ClickEvent
import net.kyori.text.event.HoverEvent
import net.kyori.text.format.Style
import net.kyori.text.format.TextColor
import net.kyori.text.format.TextDecoration

enum class ButtonType {
    /**
     * A button for a positive action, like adding or setting to true
     */
    POSITIVE,
    /**
     * A button for a negative or cautionary action, like removing or setting to false
     */
    NEGATIVE,
    /**
     * A button for a neutral action, like moving or renaming
     */
    NEUTRAL
}

val EQUALS_SIGN = "=" colored TextColor.GRAY
val SLASH = +"/"
abstract class MessageFormatter(
    internal val cmd: Commander,
    internal val pex: PermissionsEx<*>,
    val hlColor: TextColor = TextColor.AQUA
) {

    /**
     * Given a command in standard format, correct it to refer to specifically the proxy format
     */
    protected open fun transformCommand(cmd: String) = cmd

    protected open val SubjectIdentifier.friendlyName: String? get() = null

    fun subject(subject: Pair<String, String>): Component {
        return subject(Maps.immutableEntry(subject.first, subject.second))
    }

    operator fun SubjectIdentifier.unaryPlus() = subject(this)

    fun subject(subject: CalculatedSubject) = subject(subject.identifier)

    operator fun CalculatedSubject.unaryPlus() = subject(this)

    fun subject(ref: SubjectDataReference) = subject(ref.identifier)

    operator fun SubjectDataReference.unaryPlus() = subject(this)

    /**
     * Print the subject in a user-friendly manner. May link to the subject info printout
     *
     * @param subject The subject to show
     * @return the formatted value
     */
    fun subject(subject: SubjectIdentifier): Component {
        val subjType: SubjectType = pex.getSubjects(subject.key)
        var name = subject.friendlyName
        if (name == null) {
            try {
                name = subjType.persistentData().getData(subject.value, null).get()
                    .getOptions(PermissionsEx.GLOBAL_CONTEXT)["name"]
            } catch (e: ExecutionException) {
                throw RuntimeException(e)
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }
        }

        val nameText = if (name != null) {
            component {
                append(subject.value colored TextColor.GRAY)
                append(SLASH)
                append(name)
            }
        } else {
            +subject.value
        }

        return component {
            this += subject.key decorated TextDecoration.BOLD
            this += space()
            this += nameText
            hoverEvent(HoverEvent.showText(Messages.FORMATTER_BUTTON_INFO_PROMPT()))
            clickEvent(ClickEvent.runCommand(transformCommand("/pex ${subject.key} ${subject.value} info")))
        }
    }

    /**
     * Create a clickable button that will execute a command or suggest a command to be executed
     *
     * @param type The style of button to present
     * @param tooltip A tooltip to optionally show when hovering over a button
     * @param command The command to execute
     * @param execute Whether the command provided will be executed or only added to the user's input
     * @return the formatted text
     */
    fun <C : BuildableComponent<C, B>, B : ComponentBuilder<C, B>> B.button(
        type: ButtonType,
        tooltip: Component?,
        command: String,
        execute: Boolean
    ): B {
        color(
            when (type) {
                ButtonType.POSITIVE -> TextColor.GREEN
                ButtonType.NEGATIVE -> TextColor.RED
                ButtonType.NEUTRAL -> hlColor
            }
        )

        if (tooltip != null) {
            hoverEvent(HoverEvent.showText(tooltip))
        }

        clickEvent(
            if (execute) {
                ClickEvent.runCommand(transformCommand(command))
            } else {
                ClickEvent.suggestCommand(transformCommand(command))
            }
        )
        return this
    }

    /**
     * Adds a click event to the provided component builder
     *
     * @param func The function to call
     * @return The updated text
     */
    fun <C : BuildableComponent<C, B>, B : ComponentBuilder<C, B>> B.callback(func: (Commander) -> Unit): B {
        val command = pex.callbackController.registerCallback(cmd, func)
        decoration(TextDecoration.UNDERLINED, true)
        color(hlColor)
        clickEvent(ClickEvent.runCommand(transformCommand(command)))
        return this
    }

    fun permission(permission: String, value: Int): Component {
        return TextComponent.make {
            it += permission colored when {
                value > 0 -> TextColor.GREEN
                value < 0 -> TextColor.RED
                else -> TextColor.GRAY
            }
            it += EQUALS_SIGN
            it += TextComponent.of(value)
        }
    }

    fun option(permission: String, value: String): Component {
        return TextComponent.make(permission) {
            it += EQUALS_SIGN
            it += +value
        }
    }

    fun <C : BuildableComponent<C, B>, B : ComponentBuilder<C, B>> B.header(): B {
        return decoration(TextDecoration.BOLD, true)
    }

    fun <C : BuildableComponent<C, B>, B : ComponentBuilder<C, B>> B.hl(): B {
        return color(hlColor)
    }

    fun Style.Builder.header(): Style.Builder {
        return decoration(TextDecoration.BOLD, true)
    }

    fun Style.Builder.hl(): Style.Builder {
        return color(hlColor)
    }
}
