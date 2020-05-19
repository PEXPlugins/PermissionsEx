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

import ca.stellardrift.permissionsex.PermissionsEx
import ca.stellardrift.permissionsex.commands.commander.Commander
import ca.stellardrift.permissionsex.commands.commander.MessageFormatter
import ca.stellardrift.permissionsex.util.SubjectIdentifier
import com.google.common.base.Strings
import net.kyori.text.Component
import net.kyori.text.serializer.plain.PlainComponentSerializer
import java.util.Locale

class TestCommander(override val manager: PermissionsEx<*>) : Commander {
    override val name: String
        get() = "Test"

    override val locale: Locale
        get() = Locale.ROOT

    override val subjectIdentifier: SubjectIdentifier?
        get() = null

    override fun hasPermission(permission: String): Boolean {
        return true
    }

    override val formatter: MessageFormatter = TestMessageFormatter(this)

    override fun msg(text: Component) {
        println("msg: ${PlainComponentSerializer.INSTANCE.serialize(text)}")
    }

    override fun debug(text: Component) {
        println("debug: ${PlainComponentSerializer.INSTANCE.serialize(text)}")
    }

    override fun error(text: Component, err: Throwable?) {
        System.err.println("error: ${PlainComponentSerializer.INSTANCE.serialize(text)}")
        err?.printStackTrace()
    }

    override fun msgPaginated(
        title: Component,
        header: Component?,
        text: Iterable<Component>
    ) {
        val titleStr = PlainComponentSerializer.INSTANCE.serialize(title)
        println(titleStr)
        if (header != null) {
            println(PlainComponentSerializer.INSTANCE.serialize(header))
        }
        println(Strings.repeat("=", titleStr.length))
        for (line in text) {
            println(PlainComponentSerializer.INSTANCE.serialize(line))
        }
    }
}

internal class TestMessageFormatter(commander: TestCommander) :
    MessageFormatter(commander, commander.manager)
