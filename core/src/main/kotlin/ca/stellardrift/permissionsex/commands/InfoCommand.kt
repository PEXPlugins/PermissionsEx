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
import ca.stellardrift.permissionsex.commands.Messages.INFO_HEADER_PERMISSIONS_TRANSIENT
import ca.stellardrift.permissionsex.commands.Messages.INFO_PERMISSIONS_DEFAULT
import ca.stellardrift.permissionsex.commands.commander.Commander
import ca.stellardrift.permissionsex.commands.commander.MessageFormatter
import ca.stellardrift.permissionsex.commands.parse.CommandContext
import ca.stellardrift.permissionsex.commands.parse.CommandException
import ca.stellardrift.permissionsex.commands.parse.command
import ca.stellardrift.permissionsex.data.ImmutableSubjectData
import ca.stellardrift.permissionsex.util.TranslatableProvider
import ca.stellardrift.permissionsex.util.join
import ca.stellardrift.permissionsex.util.plus
import ca.stellardrift.permissionsex.util.toComponent
import ca.stellardrift.permissionsex.util.unaryPlus
import net.kyori.text.BuildableComponent
import net.kyori.text.Component
import net.kyori.text.ComponentBuilder

internal fun getInfoCommand(pex: PermissionsEx<*>) =
    command("info", "i", "who") {
        description = INFO_DESCRIPTION()
        executor(SubjectInfoPrintingExecutor(pex))
    }

fun <C : BuildableComponent<C, B>, B : ComponentBuilder<C, B>> MessageFormatter.hlKeyVal(
    key: B,
    value: Component
): Component {
    return key.hl().build() + value
}

// TODO: Pagination builder
internal class SubjectInfoPrintingExecutor constructor(pex: PermissionsEx<*>) : PermissionsExExecutor(pex) {
    @Throws(CommandException::class)
    override fun execute(src: Commander, args: CommandContext) {
        val subject = subjectOrSelf(src, args)
        src.checkSubjectPermission(subject.identifier, "permissionsex.info")
        val transientData = subject.transientData().get()
        val data = subject.data().get()
        src.msg { send ->
            fun head(msg: TranslatableProvider) {
                send(msg.get().hl().build())
            }
            send(INFO_HEADER[subject(subject)].header().build())
            if (pex.hasDebugMode()) {
                val associatedObject = subject.associatedObject
                if (associatedObject != null) {
                    send(INFO_ASSOCIATED_OBJECT.get().hl().append(associatedObject.toString()).build())
                }
            }
            send(hlKeyVal(INFO_ACTIVE_CONTEXTS.get(), +subject.activeContexts.toString()))
            send(hlKeyVal(INFO_ACTIVE_USED_CONTEXTS.get(), +subject.usedContextValues.join().toString()))

            if (data.allPermissions.isNotEmpty() || data.allDefaultValues.isNotEmpty()) {
                head(INFO_HEADER_PERMISSIONS)
                printPermissions(src, data)
            }
            if (transientData.allPermissions.isNotEmpty() || transientData.allDefaultValues.isNotEmpty()) {
                head(INFO_HEADER_PERMISSIONS_TRANSIENT)
                printPermissions(src, transientData)
            }
            if (data.allOptions.isNotEmpty()) {
                head(INFO_HEADER_OPTIONS)
                printOptions(src, data)
            }
            if (transientData.allOptions.isNotEmpty()) {
                head(INFO_HEADER_OPTIONS_TRANSIENT)
                printOptions(src, transientData)
            }
            if (data.allParents.isNotEmpty()) {
                head(Messages.INFO_HEADER_PARENTS)
                printParents(src, data)
            }
            if (transientData.allParents.isNotEmpty()) {
                head(INFO_HEADER_PARENTS_TRANSIENT)
                printParents(src, transientData)
            }
        }
    }

    private fun MessageFormatter.printPermissions(
        src: Commander,
        data: ImmutableSubjectData
    ) {
        val targetContexts = data.allPermissions.keys + data.allDefaultValues.keys
        for (entry in targetContexts) {
            src.msg(listOf(INDENT, entry.toComponent(), COLON).join(separator = null))
            src.msg(DOUBLE_INDENT + INFO_PERMISSIONS_DEFAULT[data.getDefaultValue(entry).toComponent()].hl().build())
            data.getPermissions(entry).forEach { (k, v) ->
                src.msg(DOUBLE_INDENT + permission(k, v))
            }
        }
    }

    private fun MessageFormatter.printOptions(
        src: Commander,
        data: ImmutableSubjectData
    ) {

        for ((key, value) in data.allOptions) {
            src.msg(listOf(INDENT, key.toComponent(), COLON).join(separator = null))
            value.forEach { (k, v) ->
                src.msg(DOUBLE_INDENT + option(k, v))
            }
        }
    }

    private fun MessageFormatter.printParents(
        src: Commander,
        data: ImmutableSubjectData
    ) {
        for ((key, value) in data.allParents) {
            src.msg(listOf(INDENT, key.toComponent(), COLON).join(separator = null))
            for (parent in value) {
                src.msg(DOUBLE_INDENT + subject(parent))
            }
        }
    }

    companion object {
        private val NONE = +""
        private val COLON = +":"
        private val INDENT = +"  "
        private val DOUBLE_INDENT = +"    "
    }
}
