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
import ca.stellardrift.permissionsex.commands.Messages.COMMON_ARGS_TRANSIENT
import ca.stellardrift.permissionsex.commands.Messages.DELETE_ERROR_DOES_NOT_EXIST
import ca.stellardrift.permissionsex.commands.Messages.DELETE_SUCCESS
import ca.stellardrift.permissionsex.commands.commander.Commander
import ca.stellardrift.permissionsex.commands.parse.CommandContext
import ca.stellardrift.permissionsex.commands.parse.CommandException
import ca.stellardrift.permissionsex.commands.parse.CommandSpec
import ca.stellardrift.permissionsex.commands.parse.command
import ca.stellardrift.permissionsex.util.thenMessageSubject

/**
 * Command that deletes all data for a subject
 */
internal fun getDeleteCommand(pex: PermissionsEx<*>): CommandSpec {
    return command("delete", "del", "remove", "rem") {
        executor(object : PermissionsExExecutor(pex) {
            override fun execute(
                src: Commander,
                args: CommandContext
            ) {
                val subject = subjectOrSelf(src, args)
                src.checkSubjectPermission(subject.identifier, "permissionsex.delete")
                val cache =
                    if (args.hasAny(COMMON_ARGS_TRANSIENT)) subject.transientData().cache else subject.data().cache
                cache.isRegistered(subject.identifier.value)
                    .thenCompose { registered ->
                        if (!registered) {
                            throw CommandException(DELETE_ERROR_DOES_NOT_EXIST(src.formatter.subject(subject)))
                        }
                        cache.remove(subject.identifier.value)
                    }.thenMessageSubject(src) { send -> send(DELETE_SUCCESS(+subject)) }
            }
        })
    }
}
