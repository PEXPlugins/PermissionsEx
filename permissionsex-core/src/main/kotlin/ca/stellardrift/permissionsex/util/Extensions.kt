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

@file:JvmName("Utilities")
package ca.stellardrift.permissionsex.util
import ca.stellardrift.permissionsex.commands.Messages
import ca.stellardrift.permissionsex.commands.commander.Commander
import ca.stellardrift.permissionsex.commands.commander.MessageFormatter
import ca.stellardrift.permissionsex.subject.CalculatedSubject
import com.google.common.reflect.TypeToken
import net.kyori.text.Component
import net.kyori.text.serializer.plain.PlainComponentSerializer
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.kotlin.get
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializerCollection
import ninja.leaping.configurate.reactive.Publisher
import ninja.leaping.configurate.reactive.Subscriber
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

inline fun <reified T: Any> Optional<*>.castMap(operation: T.() -> Unit) {
    (this.orElse(null) as? T)?.apply(operation)
}

inline fun <reified T: Any, R> Optional<*>.castMap(operation: T.() -> R?): R? {
    return (this.orElse(null) as? T)?.run(operation)
}

inline fun <reified T: Any> Optional<*>.cast(): Optional<T> {
    return this.map {
        if (it is T) {
            it
        } else {
            null
        }
    }
}

inline fun <reified T> CalculatedSubject.option(key: String): T? {
    val ret = getOption(key).orElse(null)
    val type = TypeToken.of(T::class.java)
    return TypeSerializerCollection.defaults().get<T>()?.deserialize(type, ConfigurationNode.root().setValue(ret))
}

fun CompletableFuture<*>.thenMessageSubject(
        src: Commander,
        message: Component
): CompletableFuture<Void> {
    return thenMessageSubject(src) { send -> send(message) }
}

fun CompletableFuture<*>.thenMessageSubject(
        src: Commander,
        message: MessageFormatter.(send: (Component) -> Unit) -> Unit
): CompletableFuture<Void> {
    return thenRun { src.msg(message) }
            .exceptionally { orig: Throwable ->
                var err = orig
                val cause = err.cause
                if (err is CompletionException && cause != null) {
                    err = cause
                }
                if (err is RuntimeCommandException) {
                    src.error(err.translatedMessage)
                } else {
                    src.error(err) { send ->
                        send(Messages.EXECUTOR_ERROR_ASYNC_TASK(err.javaClass.simpleName, err.message
                                ?: "null"))
                    }
                    src.formatter.pex.logger.error(Messages.EXECUTOR_ERROR_ASYNC_TASK_CONSOLE(src.name), err)
                }
                null
            }
}

fun <V> Publisher<V>.toCompletableFuture(): CompletableFuture<V> {
   val ret = CompletableFuture<V>()
    subscribe(object : Subscriber<V> {
        override fun submit(item: V) {
            ret.complete(item)
        }

        override fun onError(e: Throwable) {
            ret.completeExceptionally(e)
        }
    })
    return ret
}

internal class RuntimeCommandException(val translatedMessage: Component) :
        RuntimeException(PlainComponentSerializer.INSTANCE.serialize(translatedMessage)) {

    companion object {
        private const val serialVersionUID = -7243817601651202895L
    }
}
