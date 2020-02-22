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

package ca.stellardrift.permissionsex.util.command

import ca.stellardrift.permissionsex.commands.commander.ButtonType
import ca.stellardrift.permissionsex.commands.commander.Commander
import ca.stellardrift.permissionsex.commands.commander.MessageFormatter
import ca.stellardrift.permissionsex.rank.RankLadder
import ca.stellardrift.permissionsex.util.Translatable
import java.util.Locale

/**
 * Created by zml on 04.04.15.
 */
internal class TestMessageFormatter :
    MessageFormatter<String> {
    override fun subject(subject: Map.Entry<String, String>): String {
        return subject.key + ":" + subject.value
    }

    override fun ladder(ladder: RankLadder): String {
        return ladder.name
    }

    override fun booleanVal(value: Boolean): String {
        return value.toString()
    }

    override fun button(
        type: ButtonType,
        label: Translatable,
        tooltip: Translatable?,
        command: String,
        execute: Boolean
    ): String {
        return label.tr()
    }

    override fun permission(permission: String, value: Int): String {
        return "$permission=$value"
    }

    override fun option(permission: String, value: String): String {
        return "$permission=$value"
    }

    override fun String.header(): String {
        return this + "\n" + "=".repeat(this.length)
    }

    override fun String.hl(): String {
        return "*$this*"
    }

    override fun combined(vararg elements: Any): String {
        return elements.joinToString(separator = "")
    }

    override fun Translatable.tr(): String {
        return translateFormatted(Locale.ROOT)
    }

    override fun callback(
        title: Translatable,
        callback: (Commander<String>) -> Unit
    ): String {
        return title.tr()
    }

    companion object {
        val INSTANCE = TestMessageFormatter()
    }

    override fun String.unaryMinus(): String {
        return this
    }

    override fun String.plus(other: String): String {
        return this + other
    }

    override fun Collection<String>.concat(separator: String): String {
        return this.joinToString(separator = separator)
    }
}
