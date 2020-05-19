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

package ca.stellardrift.permissionsex.commands.parse

import ca.stellardrift.permissionsex.exception.PermissionsException
import com.google.common.base.Strings
import net.kyori.text.Component
import java.util.Locale
import kotlin.math.min

/**
 * Exception relating to the execution of a command
 */
open class CommandException : PermissionsException {
    constructor(message: Component) : super(message)
    constructor(message: Component, cause: Throwable) : super(message, cause)

    companion object {
        private const val serialVersionUID = -5529841181684157987L
    }
}

/**
 * Exception thrown when arguments are parsed
 */
class ArgumentParseException : CommandException {
    private val sourceString: String
    val position: Int

    constructor(message: Component, source: String, position: Int) : super(message) {
        sourceString = source
        this.position = position
    }

    constructor(message: Component, cause: Throwable, source: String, position: Int) : super(message, cause) {
        sourceString = source
        this.position = position
    }

    override fun getLocalizedMessage(locale: Locale): String {
        return if (sourceString.isEmpty()) {
            super.getLocalizedMessage(locale)
        } else {
            """
     ${super.getLocalizedMessage(locale)}
     $annotatedPosition
     """.trimIndent()
        }
    }

    val annotatedPosition: String
        get() {
            var source = sourceString
            var position = position
            if (source.length > 80) {
                if (position >= 37) {
                    val startPos = position - 37
                    val endPos = min(source.length, position + 37)
                    source = if (endPos < source.length) {
                        "..." + source.substring(startPos, endPos) + "..."
                    } else {
                        "..." + source.substring(startPos, endPos)
                    }
                    position -= 40
                } else {
                    source = source.substring(0, 77) + "..."
                }
            }
            return """
        $source
        ${Strings.repeat(" ", position)}^
        """.trimIndent()
        }

    companion object {
        private const val serialVersionUID = -8753442830556455069L
    }
}
