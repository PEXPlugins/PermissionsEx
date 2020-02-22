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

import ca.stellardrift.permissionsex.commands.commander.Commander
import ca.stellardrift.permissionsex.commands.commander.MessageFormatter
import ca.stellardrift.permissionsex.util.Translatable
import com.google.common.base.Strings
import java.util.Locale
import java.util.Optional

/**
 * Created by zml on 04.04.15.
 */
class TestCommander :
    Commander<String> {
    override val name: String
        get() = "Test"

    override val locale: Locale
        get() = Locale.ROOT

    override val subjectIdentifier: Optional<Map.Entry<String, String>>
        get() = Optional.empty()

    override fun hasPermission(permission: String): Boolean {
        return true
    }

    override val formatter: MessageFormatter<String>
        get() = TestMessageFormatter.INSTANCE

    override fun msg(text: String) {
        println("msg: $text")
    }

    override fun debug(text: String) {
        println("debug: $text")
    }

    override fun error(text: String, err: Throwable?) {
        System.err.println("error: $text")
    }

    override fun msgPaginated(
        title: Translatable,
        header: Translatable?,
        text: Iterable<String>
    ) {
        val titleStr = title.translateFormatted(Locale.ROOT)
        println(titleStr)
        println(header!!.translateFormatted(Locale.ROOT))
        println(Strings.repeat("=", titleStr.length))
        for (line in text) {
            println(line)
        }
    }
}
