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

import ca.stellardrift.permissionsex.BaseDirectoryScope
import ca.stellardrift.permissionsex.PermissionsEx
import ca.stellardrift.permissionsex.backend.DataStoreFactories
import ca.stellardrift.permissionsex.commands.Messages.CALLBACKTEST_CBTEXT
import ca.stellardrift.permissionsex.commands.Messages.CALLBACKTEST_DESCRIPTION
import ca.stellardrift.permissionsex.commands.Messages.CALLBACKTEST_SUCCESS
import ca.stellardrift.permissionsex.commands.Messages.COMMON_ARGS_SUBJECT
import ca.stellardrift.permissionsex.commands.Messages.COMMON_ARGS_SUBJECT_TYPE
import ca.stellardrift.permissionsex.commands.Messages.COMMON_ARGS_TRANSIENT
import ca.stellardrift.permissionsex.commands.Messages.DEBUG_DESCRIPTION
import ca.stellardrift.permissionsex.commands.Messages.DEBUG_SUCCESS
import ca.stellardrift.permissionsex.commands.Messages.DEBUG_SUCCESS_FILTER
import ca.stellardrift.permissionsex.commands.Messages.IMPORT_ACTION_BEGINNING
import ca.stellardrift.permissionsex.commands.Messages.IMPORT_ACTION_SUCCESS
import ca.stellardrift.permissionsex.commands.Messages.IMPORT_ARG_DATA_STORE
import ca.stellardrift.permissionsex.commands.Messages.IMPORT_DESCRIPTION
import ca.stellardrift.permissionsex.commands.Messages.IMPORT_ERROR_UNKNOWN_STORE
import ca.stellardrift.permissionsex.commands.Messages.IMPORT_LISTING_HEADER
import ca.stellardrift.permissionsex.commands.Messages.IMPORT_LISTING_SUBTITLE
import ca.stellardrift.permissionsex.commands.Messages.PEX_ARGS_FILTER
import ca.stellardrift.permissionsex.commands.Messages.PEX_ARGS_LIST
import ca.stellardrift.permissionsex.commands.Messages.PEX_DESCRIPTION
import ca.stellardrift.permissionsex.commands.Messages.PEX_LIST_HEADER
import ca.stellardrift.permissionsex.commands.Messages.PEX_LIST_SUBTITLE
import ca.stellardrift.permissionsex.commands.Messages.RELOAD_ACTION_BEGIN
import ca.stellardrift.permissionsex.commands.Messages.RELOAD_ACTION_SUCCESS
import ca.stellardrift.permissionsex.commands.Messages.RELOAD_DESCRIPTION
import ca.stellardrift.permissionsex.commands.Messages.VERSION_BASEDIRS_CONFIG
import ca.stellardrift.permissionsex.commands.Messages.VERSION_BASEDIRS_HEADER
import ca.stellardrift.permissionsex.commands.Messages.VERSION_BASEDIRS_JAR
import ca.stellardrift.permissionsex.commands.Messages.VERSION_BASEDIRS_SERVER
import ca.stellardrift.permissionsex.commands.Messages.VERSION_BASEDIRS_WORLDS
import ca.stellardrift.permissionsex.commands.Messages.VERSION_DESCRIPTION
import ca.stellardrift.permissionsex.commands.Messages.VERSION_RESPONSE_ACTIVE_DATA_STORE
import ca.stellardrift.permissionsex.commands.Messages.VERSION_RESPONSE_AVAILABLE_DATA_STORES
import ca.stellardrift.permissionsex.commands.commander.Commander
import ca.stellardrift.permissionsex.commands.commander.pexPerm
import ca.stellardrift.permissionsex.commands.parse.ChildCommandBuilder
import ca.stellardrift.permissionsex.commands.parse.CommandContext
import ca.stellardrift.permissionsex.commands.parse.CommandException
import ca.stellardrift.permissionsex.commands.parse.CommandSpec
import ca.stellardrift.permissionsex.commands.parse.StructuralArguments.firstParsing
import ca.stellardrift.permissionsex.commands.parse.StructuralArguments.flags
import ca.stellardrift.permissionsex.commands.parse.StructuralArguments.optional
import ca.stellardrift.permissionsex.commands.parse.StructuralArguments.seq
import ca.stellardrift.permissionsex.commands.parse.command
import ca.stellardrift.permissionsex.commands.parse.contextTransientFlags
import ca.stellardrift.permissionsex.commands.parse.literal
import ca.stellardrift.permissionsex.commands.parse.none
import ca.stellardrift.permissionsex.commands.parse.string
import ca.stellardrift.permissionsex.commands.parse.subject
import ca.stellardrift.permissionsex.commands.parse.subjectType
import ca.stellardrift.permissionsex.util.component
import ca.stellardrift.permissionsex.util.plus
import ca.stellardrift.permissionsex.util.thenMessageSubject
import ca.stellardrift.permissionsex.util.unaryMinus
import ca.stellardrift.permissionsex.util.unaryPlus
import net.kyori.text.TextComponent.make
import java.util.regex.Pattern

fun createRootCommand(pex: PermissionsEx<*>): CommandSpec {
    val (childrenKey, childrenArg, childrenExec) = ChildCommandBuilder().run {
        child(getDebugToggleCommand(pex))
        child(getRankingCommand(pex))
        child(getImportCommand(pex))
        child(getReloadCommand(pex))
        child(getVersionCommand(pex))
        child(pex.callbackController.createCommand())

        pex.implementationCommands.forEach { child(it) }
        build()
    }

    val (subjectChildrenKey, subjectChildrenArg, subjectChildrenExec) = ChildCommandBuilder().run {
        child(getOptionCommand(pex))
        child(getPermissionCommand(pex))
        child(getPermissionDefaultCommand(pex))
        child(getInfoCommand(pex))
        child(getParentCommand(pex))
        child(getDeleteCommand(pex))

        build()
    }

    return command("pex", "permissionsex", "permissions") {
        description = PEX_DESCRIPTION()
        args = optional(
            firstParsing(
                childrenArg,
                contextTransientFlags(pex)
                    .buildWith(
                        seq(
                            subject(pex) key COMMON_ARGS_SUBJECT(),
                            subjectChildrenArg
                        )
                    ),
                flags()
                    .flag("-transient")
                    .buildWith(
                        seq(
                            subjectType(pex) key COMMON_ARGS_SUBJECT_TYPE(),
                            literal("list") key PEX_ARGS_LIST(),
                            optional(
                                string() key PEX_ARGS_FILTER()
                            )
                        )
                    )
            )
        )
        executor { src, args ->
            when {
                args.hasAny("list") -> {
                    val subjectType = args.getOne<String>(COMMON_ARGS_SUBJECT_TYPE)!!
                    args.checkPermission(src, "permissionsex.list.$subjectType")
                    val cache =
                        if (args.hasAny(COMMON_ARGS_TRANSIENT)) pex.getSubjects(subjectType)
                            .transientData() else pex.getSubjects(
                            subjectType
                        ).persistentData()
                    var iter = cache.allIdentifiers.asSequence()
                    if (args.hasAny(PEX_ARGS_FILTER)) {
                        val filter = args.getOne<String>(PEX_ARGS_FILTER)!!
                        iter = iter.filter { it.startsWith(filter, ignoreCase = true) }
                    }
                    src.msgPaginated(
                        PEX_LIST_HEADER(subjectType),
                        PEX_LIST_SUBTITLE(subjectType),
                        iter.map { src.formatter.subject(subjectType to it) }.asIterable()
                    )
                }
                subjectChildrenKey in args -> {
                    subjectChildrenExec(src, args)
                }
                childrenKey in args -> {
                    childrenExec(src, args)
                }
                else -> {
                    src.msg { send ->
                        send(+"PermissionsEx " + (-"v${pex.version}").hl().build())
                        send(args.spec.getUsage(src))
                    }
                }
            }
        }
    }
}


private fun getDebugToggleCommand(pex: PermissionsEx<*>) =
    command("debug", "d") {
        description = DEBUG_DESCRIPTION()
        permission = pexPerm("debug")
        args = optional(string() key PEX_ARGS_FILTER())
        executor { src, args ->
            val debugEnabled = !pex.hasDebugMode()
            val filter = args.getOne<String>(PEX_ARGS_FILTER)
            src.msg { send ->
                if (filter != null) {
                    pex.setDebugMode(debugEnabled, Pattern.compile(filter))
                    send(
                        DEBUG_SUCCESS_FILTER(debugEnabled, (-filter).hl())
                    )
                } else {
                    pex.setDebugMode(debugEnabled)
                    send(DEBUG_SUCCESS(debugEnabled))
                }
            }
        }
    }

private fun getImportCommand(pex: PermissionsEx<*>) =
    command("import") {
        description = IMPORT_DESCRIPTION()
        args = optional(string() key IMPORT_ARG_DATA_STORE())
        permission = pexPerm("import")
        executor(object : PermissionsExExecutor(pex) {
            override fun execute(src: Commander, args: CommandContext) {
                val backendRequested = args.getOne<String?>(IMPORT_ARG_DATA_STORE)
                if (backendRequested == null) { // We want to list available conversions
                    src.formatter.apply {
                        src.msgPaginated(
                            IMPORT_LISTING_HEADER(),
                            IMPORT_LISTING_SUBTITLE((-"/pex import [id]").hl().build()),
                            pex.availableConversions
                                .map { conv ->
                                    component {
                                        append(conv.title)
                                        append(+" - /pex import ")
                                        append((-conv.store.name).callback {
                                            it.msg(IMPORT_ACTION_BEGINNING(conv.title))
                                            pex.importDataFrom(conv)
                                                .thenMessageSubject(it, IMPORT_ACTION_SUCCESS(conv.title))
                                        })
                                    }
                                })
                    }
                } else {
                    for (result in pex.availableConversions) {
                        if (result.store.name.equals(backendRequested, ignoreCase = true)) {
                            src.msg { send ->
                                send(IMPORT_ACTION_BEGINNING(result.title))
                            }
                            pex.importDataFrom(result).thenMessageSubject(src, IMPORT_ACTION_SUCCESS(result.title))
                            return
                        }
                    }
                    if (pex.config.getDataStore(backendRequested) == null) {
                        throw CommandException(IMPORT_ERROR_UNKNOWN_STORE(backendRequested))
                    }
                    src.msg { send ->
                        send(IMPORT_ACTION_BEGINNING(backendRequested))
                    }
                    pex.importDataFrom(backendRequested)
                        .thenMessageSubject(src, IMPORT_ACTION_SUCCESS(backendRequested))
                }
            }
        })
    }

private fun getReloadCommand(pex: PermissionsEx<*>): CommandSpec {
    return command("reload", "rel") {
        description = RELOAD_DESCRIPTION()
        permission = pexPerm("reload")
        executor { src, _ ->
            src.msg(RELOAD_ACTION_BEGIN())
            pex.reload().thenMessageSubject(src, RELOAD_ACTION_SUCCESS())
        }
    }
}

private fun getCallbackTestCommand(): CommandSpec {
    return command("cbtest", "test") {
        description = CALLBACKTEST_DESCRIPTION()
        executor { src, _ ->
            src.msg { send ->
                send(CALLBACKTEST_CBTEXT.get().callback { sender ->
                    sender.msg(CALLBACKTEST_SUCCESS())
                }.build())
            }
        }
    }
}

private fun getVersionCommand(pex: PermissionsEx<*>): CommandSpec {
    return command("version") {
        description = VERSION_DESCRIPTION()
        permission = pexPerm("version")
        args(flags().flag("-verbose", "v").buildWith(none()))
        executor { src, args ->
            val verbose = args.getOne<Boolean>("verbose") ?: false
            src.msg { send ->
                send(make("PermissionsEx v") {
                    it.append((-pex.version).hl())
                })
                send(VERSION_RESPONSE_ACTIVE_DATA_STORE(-pex.config.defaultDataStore.name))
                send(VERSION_RESPONSE_AVAILABLE_DATA_STORES(-DataStoreFactories.getKnownTypes().toString()))
                send(+"")
                if (verbose) {
                    send(VERSION_BASEDIRS_HEADER.get().header().build())
                    send(VERSION_BASEDIRS_CONFIG(pex.getBaseDirectory(BaseDirectoryScope.CONFIG)))
                    send(VERSION_BASEDIRS_JAR(pex.getBaseDirectory(BaseDirectoryScope.JAR)))
                    send(VERSION_BASEDIRS_SERVER(pex.getBaseDirectory(BaseDirectoryScope.SERVER)))
                    send(VERSION_BASEDIRS_WORLDS(pex.getBaseDirectory(BaseDirectoryScope.WORLDS)))
                }
            }
        }
    }
}
