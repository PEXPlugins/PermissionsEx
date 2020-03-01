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

package ca.stellardrift.permissionsex.commands

import ca.stellardrift.permissionsex.PermissionsEx
import ca.stellardrift.permissionsex.commands.Messages.COMMON_ARGS_CONTEXT_GLOBAL
import ca.stellardrift.permissionsex.commands.Messages.COMMON_ARGS_SUBJECT
import ca.stellardrift.permissionsex.commands.Messages.COMMON_ARGS_TRANSIENT
import ca.stellardrift.permissionsex.commands.Messages.EXECUTOR_ERROR_ASYNC_TASK
import ca.stellardrift.permissionsex.commands.Messages.EXECUTOR_ERROR_ASYNC_TASK_CONSOLE
import ca.stellardrift.permissionsex.commands.Messages.EXECUTOR_ERROR_GETTING_SUBJECT
import ca.stellardrift.permissionsex.commands.Messages.EXECUTOR_ERROR_NO_PERMISSION
import ca.stellardrift.permissionsex.commands.Messages.EXECUTOR_ERROR_SUBJECT_REQUIRED
import ca.stellardrift.permissionsex.commands.commander.Commander
import ca.stellardrift.permissionsex.commands.commander.MessageFormatter
import ca.stellardrift.permissionsex.context.ContextValue
import ca.stellardrift.permissionsex.data.SubjectDataReference
import ca.stellardrift.permissionsex.subject.CalculatedSubject
import ca.stellardrift.permissionsex.util.Translatable
import ca.stellardrift.permissionsex.util.command.CommandContext
import ca.stellardrift.permissionsex.util.command.CommandException
import ca.stellardrift.permissionsex.util.command.CommandExecutor
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutionException

abstract class PermissionsExExecutor protected constructor(protected val pex: PermissionsEx<*>) :
    CommandExecutor {
    protected fun <TextType : Any> MessageFormatter<TextType>.formatContexts(
        contexts: Set<ContextValue<*>?>
    ): TextType {
        return if (contexts.isEmpty()) {
            COMMON_ARGS_CONTEXT_GLOBAL()
        } else {
            -contexts.toString()
        }
    }

    @Throws(CommandException::class)
    protected fun subjectOrSelf(
        src: Commander<*>,
        args: CommandContext
    ): CalculatedSubject {
        return try {
            if (args.hasAny(COMMON_ARGS_SUBJECT)) {
                val ret =
                    args.getOne<Map.Entry<String, String>>(COMMON_ARGS_SUBJECT)
                pex.getSubjects(ret.key)[ret.value].get()
            } else {
                val ret =
                    src.subjectIdentifier
                if (!ret.isPresent) {
                    throw CommandException(EXECUTOR_ERROR_SUBJECT_REQUIRED.get())
                } else {
                    pex.getSubjects(ret.get().key)[ret.get().value].get()
                }
            }
        } catch (e: InterruptedException) {
            throw CommandException(
                EXECUTOR_ERROR_GETTING_SUBJECT.get(),
                e
            )
        } catch (e: ExecutionException) {
            throw CommandException(
                EXECUTOR_ERROR_GETTING_SUBJECT.get(),
                e
            )
        }
    }

    @Throws(CommandException::class)
    protected fun <TextType : Any> getDataRef(
        src: Commander<TextType>,
        args: CommandContext,
        permission: String
    ): SubjectDataReference {
        val subject = subjectOrSelf(src, args)
        src.checkSubjectPermission(subject.identifier, permission)
        return if (args.hasAny(COMMON_ARGS_TRANSIENT)) subject.transientData() else subject.data()
    }

    @Throws(CommandException::class)
    protected fun Commander<*>.checkSubjectPermission(
        subject: Map.Entry<String, String>,
        basePermission: String
    ) {
        if (!hasPermission("$basePermission.${subject.key}.${subject.value}")
            && (subject != subjectIdentifier.orElse(null) || !hasPermission("$basePermission.own"))
        ) {
            throw CommandException(EXECUTOR_ERROR_NO_PERMISSION.get())
        }
    }


    protected fun <TextType : Any> CompletableFuture<*>.thenMessageSubject(
        src: Commander<TextType>,
        message: MessageFormatter<TextType>.() -> Translatable
    ): CompletableFuture<Void> {
        return thenMessageSubject(src) { send -> send(message().tr()) }
    }

    protected fun <TextType : Any> CompletableFuture<*>.thenMessageSubject(
        src: Commander<TextType>,
        message: MessageFormatter<TextType>.(send: (TextType) -> Unit) -> Unit
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
                        send(EXECUTOR_ERROR_ASYNC_TASK[err.javaClass.simpleName, err.message ?: "null"].tr())
                    }
                    pex.logger.error(EXECUTOR_ERROR_ASYNC_TASK_CONSOLE[src.name], err)
                }
                null
            }
    }

    internal class RuntimeCommandException(val translatedMessage: Translatable) :
        RuntimeException(translatedMessage.untranslated) {

        companion object {
            private const val serialVersionUID = -7243817601651202895L
        }

    }

}
