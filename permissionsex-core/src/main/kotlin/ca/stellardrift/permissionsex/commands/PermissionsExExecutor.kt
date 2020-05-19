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
import ca.stellardrift.permissionsex.commands.Messages.COMMON_ARGS_SUBJECT
import ca.stellardrift.permissionsex.commands.Messages.COMMON_ARGS_TRANSIENT
import ca.stellardrift.permissionsex.commands.Messages.EXECUTOR_ERROR_GETTING_SUBJECT
import ca.stellardrift.permissionsex.commands.Messages.EXECUTOR_ERROR_SUBJECT_REQUIRED
import ca.stellardrift.permissionsex.commands.commander.Commander
import ca.stellardrift.permissionsex.commands.parse.CommandContext
import ca.stellardrift.permissionsex.commands.parse.CommandException
import ca.stellardrift.permissionsex.commands.parse.CommandExecutor
import ca.stellardrift.permissionsex.data.SubjectDataReference
import ca.stellardrift.permissionsex.subject.CalculatedSubject
import ca.stellardrift.permissionsex.util.SubjectIdentifier
import java.util.concurrent.ExecutionException

abstract class PermissionsExExecutor protected constructor(protected val pex: PermissionsEx<*>) :
    CommandExecutor {

    @Throws(CommandException::class)
    protected fun subjectOrSelf(src: Commander, args: CommandContext): CalculatedSubject {
        return try {
            val argIdentifier = args.getOne<SubjectIdentifier>(COMMON_ARGS_SUBJECT)
            if (argIdentifier != null) {
                pex.getSubjects(argIdentifier.key)[argIdentifier.value].get()
            } else {
                val ret = src.subjectIdentifier?: throw CommandException(EXECUTOR_ERROR_SUBJECT_REQUIRED())
                pex.getSubjects(ret.key)[ret.value].get()
            }
        } catch (e: InterruptedException) {
            throw CommandException(EXECUTOR_ERROR_GETTING_SUBJECT(), e)
        } catch (e: ExecutionException) {
            throw CommandException(EXECUTOR_ERROR_GETTING_SUBJECT(), e)
        }
    }

    @Throws(CommandException::class)
    protected fun getDataRef(src: Commander, args: CommandContext, permission: String): SubjectDataReference {
        val subject = subjectOrSelf(src, args)
        src.checkSubjectPermission(subject.identifier, permission)
        return if (args.hasAny(COMMON_ARGS_TRANSIENT)) subject.transientData() else subject.data()
    }
}
