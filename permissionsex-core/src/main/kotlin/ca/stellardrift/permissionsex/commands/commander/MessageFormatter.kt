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

import ca.stellardrift.permissionsex.data.SubjectDataReference
import ca.stellardrift.permissionsex.rank.RankLadder
import ca.stellardrift.permissionsex.subject.CalculatedSubject
import ca.stellardrift.permissionsex.util.Translatable
import ca.stellardrift.permissionsex.util.TranslatableProvider
import com.google.common.collect.Maps

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

/**
 * Interface specifying code to get specific elements of commands
 */
interface MessageFormatter<TextType: Any> {
    /**
     * Print the subject in a user-friendly manner. May link to the subject info printout
     *
     * @param subject The subject to show
     * @return the formatted value
     */
    fun subject(subject: Map.Entry<String, String>): TextType

    fun subject(subject: Pair<String, String>): TextType {
        return subject(Maps.immutableEntry(subject.first, subject.second))
    }

    operator fun Map.Entry<String, String>.unaryMinus(): TextType {
        return subject(this)
    }

    fun subject(subject: CalculatedSubject): TextType {
        return subject(subject.identifier)
    }

    operator fun CalculatedSubject.unaryMinus(): TextType {
        return subject(this);
    }

    fun subject(ref: SubjectDataReference): TextType {
        return subject(ref.identifier)
    }

    operator fun SubjectDataReference.unaryMinus(): TextType {
        return subject(this)
    }

    /**
     * Print the rank ladder in a user-friendly manner. May link to the subject info printout
     *
     * @param ladder The ladder to show
     * @return the formatted value
     */
    fun ladder(ladder: RankLadder): TextType

    operator fun RankLadder.unaryMinus(): TextType {
        return ladder(this)
    }

    /**
     * Print the given boolean in a user-friendly manner.
     * Generally this means green if true, or red if false
     * @param value The value to print
     * @return the formatted value
     */
    fun booleanVal(value: Boolean): TextType


    operator fun Boolean.unaryMinus(): TextType {
        return booleanVal(this)
    }

    /**
     * Create a clickable button that will execute a command or
     * @param type The style of button to present
     * @param label The label for the button
     * @param tooltip A tooltip to optionally show when hovering over a button
     * @param command The command to execute
     * @param execute Whether the command provided will be executed or only added to the user's input
     * @return the formatted text
     */
    fun button(
        type: ButtonType,
        label: Translatable,
        tooltip: Translatable?,
        command: String,
        execute: Boolean
    ): TextType

    fun permission(permission: String, value: Int): TextType
    fun option(permission: String, value: String): TextType
    /**
     * Execute a callback when the given text is clicked
     *
     * @param title The text to show. The text will be shown underlined in the highlight colour.
     * @param callback The function to call
     * @return The updated text
     */
    fun callback(
        title: Translatable,
        callback: (Commander<TextType>) -> Unit
    ): TextType

    /**
     * Format the given line of text to be used in a header
     *
     * @return The input text with header formatting wrapping.
     */
    fun TextType.header(): TextType

    /**
     * Highlight the passed text
     *
     * @return The highlighted text
     */
    fun TextType.hl(): TextType

    /**
     * Combines an array containing elements of type [TextType] and [java.lang.String] into a single message
     *
     * @param elements The elements to combine
     * @return A combined, formatted element
     */
    fun combined(vararg elements: Any): TextType

    /**
     * Return the internal representation of the given translatable text.
     *
     * @param tr The translatable text
     * @return the formatted value
     */
    fun Translatable.tr(): TextType

    operator fun Translatable.unaryMinus(): TextType {
        return this.tr()
    }

    @JvmDefault
    operator fun TranslatableProvider.invoke(vararg args: TextType): TextType {
        return this.get(*args).tr()
    }

    /**
     * Convert a plain string to formatted text
     */
    operator fun String.unaryMinus(): TextType

    operator fun TextType.plus(other: TextType): TextType

    fun Collection<TextType>.concat(separator: TextType = -""): TextType
}
