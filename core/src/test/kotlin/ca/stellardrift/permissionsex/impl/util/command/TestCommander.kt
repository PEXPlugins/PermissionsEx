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
package ca.stellardrift.permissionsex.impl.util.command

import ca.stellardrift.permissionsex.impl.PermissionsEx
import ca.stellardrift.permissionsex.impl.commands.commander.Commander
import ca.stellardrift.permissionsex.impl.commands.commander.MessageFormatter
import ca.stellardrift.permissionsex.subject.SubjectRef
import com.google.common.base.Strings
import java.util.Locale
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer

class TestCommander(override val manager: PermissionsEx<*>) : Commander {
    override val name: String
        get() = "Test"

    override val locale: Locale
        get() = Locale.ROOT

    override val subjectIdentifier: SubjectRef<*>?
        get() = null
    override val messageColor: TextColor
        get() = NamedTextColor.DARK_GREEN

    override fun hasPermission(permission: String): Boolean {
        return true
    }

    override val formatter: MessageFormatter = TestMessageFormatter(this)

    override fun msg(text: Component) {
        println("msg: ${PlainComponentSerializer.plain().serialize(text)}")
    }

    override fun debug(text: Component) {
        println("debug: ${PlainComponentSerializer.plain().serialize(text)}")
    }

    override fun error(text: Component, err: Throwable?) {
        System.err.println("error: ${PlainComponentSerializer.plain().serialize(text)}")
        err?.printStackTrace()
    }

    override fun msgPaginated(
        title: Component,
        header: Component?,
        text: Iterable<Component>
    ) {
        val titleStr = PlainComponentSerializer.plain().serialize(title)
        println(titleStr)
        if (header != null) {
            println(PlainComponentSerializer.plain().serialize(header))
        }
        println(Strings.repeat("=", titleStr.length))
        for (line in text) {
            println(PlainComponentSerializer.plain().serialize(line))
        }
    }

    override fun audience(): Audience {
        return Audience.empty() // TODO: is this needed?
    }
}

internal class TestMessageFormatter(commander: TestCommander) :
    MessageFormatter(commander, commander.manager)
