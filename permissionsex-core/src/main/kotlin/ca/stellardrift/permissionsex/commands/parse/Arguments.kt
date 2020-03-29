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

import ca.stellardrift.permissionsex.util.command.args.CommandArgs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import net.kyori.text.Component
import java.util.UUID


fun <S: Any> string(description: Component): ArgumentParser<S, String> = StringParser(description)

internal class StringParser<S: Any>(description: Component): ArgumentParser<S, String>(description) {
    override suspend fun parse(src: S, args: CommandArgs): String {
        return args.next()
    }
}

fun <S: Any> uuid(description: Component): ArgumentParser<S, UUID> = UUIDParser(description)

internal class UUIDParser<S: Any>(description: Component): ArgumentParser<S, UUID>(description) {
    override suspend fun parse(src: S, args: CommandArgs): UUID {
        TODO("Not yet implemented")
    }

}

/*fun <V: Any> argument(action: ArgumentParserBuilder<V>.() -> Unit): ArgumentParser<V> {
    val builder = ArgumentParserBuilder<V>()
    builder.action()
    return builder.build()
}

class ArgumentParserBuilder<S: Any, V: Any> {
    var parseFunction: (CommandArgs) -> V = TODO()
    val tabCompletionFunc: (CommandArgs) -> List<String> = { listOf() }

    fun build(): ArgumentParser<S, V> {
        return ArgumentParser(parseFunction, tabCompletionFunc)
    }
}*/

abstract class ArgumentParser<S: Any, V: Any>(val description: Component) {
    abstract suspend fun parse(src: S, args: CommandArgs): V
    fun tabComplete(src: S, args: CommandArgs): Flow<String> = emptyFlow()

}
