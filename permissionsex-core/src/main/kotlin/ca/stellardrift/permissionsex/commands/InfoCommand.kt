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
import ca.stellardrift.permissionsex.commands.Messages.INFO_ACTIVE_CONTEXTS
import ca.stellardrift.permissionsex.commands.Messages.INFO_ACTIVE_USED_CONTEXTS
import ca.stellardrift.permissionsex.commands.Messages.INFO_ASSOCIATED_OBJECT
import ca.stellardrift.permissionsex.commands.Messages.INFO_DESCRIPTION
import ca.stellardrift.permissionsex.commands.Messages.INFO_HEADER
import ca.stellardrift.permissionsex.commands.Messages.INFO_HEADER_OPTIONS
import ca.stellardrift.permissionsex.commands.Messages.INFO_HEADER_OPTIONS_TRANSIENT
import ca.stellardrift.permissionsex.commands.Messages.INFO_HEADER_PARENTS_TRANSIENT
import ca.stellardrift.permissionsex.commands.Messages.INFO_HEADER_PERMISSIONS
import ca.stellardrift.permissionsex.commands.Messages.INFO_PERMISSIONS_DEFAULT
import ca.stellardrift.permissionsex.commands.commander.Commander
import ca.stellardrift.permissionsex.commands.commander.MessageFormatter
import ca.stellardrift.permissionsex.data.ImmutableSubjectData
import ca.stellardrift.permissionsex.util.command.CommandContext
import ca.stellardrift.permissionsex.util.command.CommandException
import ca.stellardrift.permissionsex.util.command.CommandSpec

    internal fun getInfoCommand(pex: PermissionsEx<*>): CommandSpec {
        return CommandSpec.builder()
            .setAliases("info", "i", "who")
            .setDescription(INFO_DESCRIPTION.get())
            .setExecutor(SubjectInfoPrintingExecutor(pex))
            .build()
    }

    // TODO: Pagination builder
    internal class SubjectInfoPrintingExecutor constructor(pex: PermissionsEx<*>) : PermissionsExExecutor(pex) {
        @Throws(CommandException::class)
        override fun <TextType: Any> execute(
            src: Commander<TextType>,
            args: CommandContext
        ) {
            val subject = subjectOrSelf(src, args)
            src.checkSubjectPermission(subject.identifier, "permissionsex.info")
            val transientData = subject.transientData().get()
            val data = subject.data().get()
            src.msg { send ->
                send(INFO_HEADER(subject(subject)).header())
                if (pex.hasDebugMode()) {
                    val associatedObject = subject.associatedObject
                    associatedObject.ifPresent { o ->
                        send(INFO_ASSOCIATED_OBJECT().hl() + -o.toString())
                    }
                }
                send(INFO_ACTIVE_CONTEXTS().hl() + -subject.activeContexts.toString())
                send(INFO_ACTIVE_USED_CONTEXTS().hl() + -subject.usedContextValues.join().toString())

                if (data.allPermissions.isNotEmpty() || data.allDefaultValues.isNotEmpty()) {
                    send(INFO_HEADER_PERMISSIONS().hl())
                    printPermissions(src, data)
                }
                if (transientData.allPermissions.isNotEmpty() || transientData.allDefaultValues.isNotEmpty()) {
                    send(Messages.INFO_HEADER_PERMISSIONS_TRANSIENT().hl())
                    printPermissions(src, transientData)
                }
                if (data.allOptions.isNotEmpty()) {
                    send(INFO_HEADER_OPTIONS().hl())
                    printOptions(src, data)
                }
                if (transientData.allOptions.isNotEmpty()) {
                    send(INFO_HEADER_OPTIONS_TRANSIENT().hl())
                    printOptions(src, transientData)
                }
                if (data.allParents.isNotEmpty()) {
                    send(Messages.INFO_HEADER_PARENTS().hl())
                    printParents(src, data)
                }
                if (transientData.allParents.isNotEmpty()) {
                    send(INFO_HEADER_PARENTS_TRANSIENT().hl())
                    printParents(src, transientData)
                }
            }
        }

        private fun <Text: Any> MessageFormatter<Text>.printPermissions(
            src: Commander<Text>,
            data: ImmutableSubjectData
        ) {
            val targetContexts = data.allPermissions.keys + data.allDefaultValues.keys
            for (entry in targetContexts) {
                src.msg(listOf(-INDENT, formatContexts(entry), -":").concat())
                src.msg(-DOUBLE_INDENT + INFO_PERMISSIONS_DEFAULT(-data.getDefaultValue(entry).toString()).hl())
                data.getPermissions(entry).forEach { (k, v) ->
                    src.msg(-DOUBLE_INDENT + permission(k, v))
                }
            }
        }

        private fun <Text: Any> MessageFormatter<Text>.printOptions(
            src: Commander<Text>,
            data: ImmutableSubjectData
        ) {

            for ((key, value) in data.allOptions) {
                src.msg(listOf(-INDENT, formatContexts(key), -":").concat())
                value.forEach { (k, v) ->
                    src.msg(-DOUBLE_INDENT + option(k, v))
                }
            }
        }

        private fun <TextType: Any> MessageFormatter<TextType>.printParents(
            src: Commander<TextType>,
            data: ImmutableSubjectData
        ) {
            for ((key, value) in data.allParents) {
                src.msg(listOf(-INDENT, formatContexts(key), -":").concat())
                for (parent in value) {
                    src.msg(-DOUBLE_INDENT + subject(parent))
                }
            }
        }

        companion object {
            private const val INDENT = "  "
            private const val DOUBLE_INDENT =
                INDENT + INDENT
        }
    }
