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

import net.kyori.text.Component
import java.util.Locale
import java.util.Optional

/**
 * Interface implemented by objects that can execute commands and receive command output
 */
interface Commander {
    val name: String
    val locale: Locale
    val subjectIdentifier: Optional<Map.Entry<String, String>>

    fun hasPermission(permission: String): Boolean

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

    fun msg(text: Component)
    fun debug(text: Component)

    fun error(text: Component, err: Throwable? = null)

    fun msgPaginated(
        title: Component,
        header: Component? = null,
        text: Iterable<Component>
    )
}
