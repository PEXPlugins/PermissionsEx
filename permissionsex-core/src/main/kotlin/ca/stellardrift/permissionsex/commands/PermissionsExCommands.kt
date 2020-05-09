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
import ca.stellardrift.permissionsex.commands.Messages.RELOAD_ACTION_ERROR
import ca.stellardrift.permissionsex.commands.Messages.RELOAD_ACTION_ERROR_CONSOLE
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
import ca.stellardrift.permissionsex.util.Util
import ca.stellardrift.permissionsex.util.command.ChildCommands
import ca.stellardrift.permissionsex.util.command.CommandContext
import ca.stellardrift.permissionsex.util.command.CommandException
import ca.stellardrift.permissionsex.util.command.CommandSpec
import ca.stellardrift.permissionsex.util.command.args.GameArguments.subject
import ca.stellardrift.permissionsex.util.command.args.GameArguments.subjectType
import ca.stellardrift.permissionsex.util.command.args.GenericArguments.firstParsing
import ca.stellardrift.permissionsex.util.command.args.GenericArguments.flags
import ca.stellardrift.permissionsex.util.command.args.GenericArguments.literal
import ca.stellardrift.permissionsex.util.command.args.GenericArguments.none
import ca.stellardrift.permissionsex.util.command.args.GenericArguments.optional
import ca.stellardrift.permissionsex.util.command.args.GenericArguments.seq
import ca.stellardrift.permissionsex.util.command.args.GenericArguments.string
import ca.stellardrift.permissionsex.util.component
import ca.stellardrift.permissionsex.util.plus
import ca.stellardrift.permissionsex.util.thenMessageSubject
import ca.stellardrift.permissionsex.util.toComponent
import ca.stellardrift.permissionsex.util.unaryMinus
import ca.stellardrift.permissionsex.util.unaryPlus
import net.kyori.text.TextComponent.make
import java.util.regex.Pattern

fun createRootCommand(pex: PermissionsEx<*>): CommandSpec {
        val childrenList: Set<CommandSpec> = setOf(
            getDebugToggleCommand(pex),
            getRankingCommand(pex),
            getImportCommand(pex),
            getReloadCommand(pex),
            getVersionCommand(pex),
            pex.callbackController.createCommand()
        ) + pex.implementationCommands

        val children =
            ChildCommands.args(*childrenList.toTypedArray())
        val subjectChildren = ChildCommands.args(
            getOptionCommand(pex),
            getPermissionCommand(pex),
            getPermissionDefaultCommand(pex),
            getInfoCommand(pex),
            getParentCommand(pex),
            getDeleteCommand(pex)
        )

        return CommandSpec.builder()
            .setAliases("pex", "permissionsex", "permissions")
            .setDescription(PEX_DESCRIPTION())
            .setArguments(
                optional(
                    firstParsing(children, Util.contextTransientFlags(pex)
                            .buildWith(
                                seq(subject(COMMON_ARGS_SUBJECT(), pex), subjectChildren)
                            ), flags()
                            .flag("-transient")
                            .buildWith(
                                seq(subjectType(COMMON_ARGS_SUBJECT_TYPE(), pex), literal(PEX_ARGS_LIST(), "list"), optional(string(
                                    PEX_ARGS_FILTER())))
                            ))
                )
            )
            .setExecutor { src, args ->
                when {
                    args.hasAny("list") -> {
                        val subjectType = args.getOne<String>(COMMON_ARGS_SUBJECT_TYPE)!!
                        args.checkPermission(src, "permissionsex.command.list.$subjectType")
                        val cache =
                                if (args.hasAny(COMMON_ARGS_TRANSIENT)) pex.getSubjects(subjectType).transientData() else pex.getSubjects(
                                        subjectType
                                ).persistentData()
                        var iter  = cache.allIdentifiers.asSequence()
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
                    subjectChildren.key in args -> {
                        ChildCommands.executor(subjectChildren).execute(src, args)
                    }
                    children.key in args -> {
                        ChildCommands.executor(children).execute(src, args)
                    }
                    else -> {
                        src.msg { send ->
                            send(+"PermissionsEx " + (-"v${pex.version}").hl().build())
                            send(args.spec.getUsage(src))
                        }
                    }
                }
            }
                .build()
    }


    private fun getDebugToggleCommand(pex: PermissionsEx<*>): CommandSpec {
        return CommandSpec.builder()
            .setAliases("debug", "d")
            .setDescription(DEBUG_DESCRIPTION())
            .setPermission("permissionsex.debug")
            .setArguments(optional(string(PEX_ARGS_FILTER())))
            .setExecutor { src, args ->
                val debugEnabled = !pex.hasDebugMode()
                val filter = args.getOne<String>(PEX_ARGS_FILTER)
                src.msg {send ->
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
                .build()
    }

    private fun getImportCommand(pex: PermissionsEx<*>): CommandSpec {
        return CommandSpec.builder()
            .setAliases("import")
            .setDescription(IMPORT_DESCRIPTION())
            .setArguments(optional(string(IMPORT_ARG_DATA_STORE())))
            .setPermission("permissionsex.import")
            .setExecutor(object : PermissionsExExecutor(pex) {
                override fun execute(src: Commander, args: CommandContext) {
                    val backendRequested = args.getOne<String?>(IMPORT_ARG_DATA_STORE)
                        if (backendRequested == null) { // We want to list available conversions
                            src.formatter.apply {
                                src.msgPaginated(
                                    IMPORT_LISTING_HEADER(),
                                    IMPORT_LISTING_SUBTITLE((-"/pex import [id]").hl().build()),
                                    pex.availableConversions
                                        .map { conv -> component {
                                            append(conv.title)
                                            append(+" - /pex import ")
                                            append((-conv.store.name).callback {
                                                it.msg { send ->
                                                    send(IMPORT_ACTION_BEGINNING(conv.title))
                                                }
                                                pex.importDataFrom(conv).thenMessageSubject(it) { send ->
                                                    send(IMPORT_ACTION_SUCCESS(conv.title))
                                                }

                                            })
                                        }})
                            }
                        } else {
                            for (result in pex.availableConversions) {
                                if (result.store.name.equals(backendRequested, ignoreCase = true)) {
                                    src.msg { send ->
                                        send(IMPORT_ACTION_BEGINNING(result.title))
                                    }
                                    pex.importDataFrom(result).thenMessageSubject(src) { send ->  send(IMPORT_ACTION_SUCCESS(result.title)) }
                                    return
                                }
                            }
                            if (pex.config.getDataStore(backendRequested) == null) {
                                throw CommandException(IMPORT_ERROR_UNKNOWN_STORE(backendRequested))
                            }
                            src.msg { send ->
                                send(IMPORT_ACTION_BEGINNING(backendRequested))
                            }
                            pex.importDataFrom(backendRequested).thenMessageSubject(src) { send ->  send(IMPORT_ACTION_SUCCESS(backendRequested)) }
                        }
                }
            })
            .build();
    }
    private fun getReloadCommand(pex: PermissionsEx<*>): CommandSpec {
        return CommandSpec.builder()
            .setAliases("reload", "rel")
            .setDescription(RELOAD_DESCRIPTION())
            .setPermission("permissionsex.reload")
            .setExecutor { src, args ->
                src.msg(RELOAD_ACTION_BEGIN())
                pex.reload().thenRun { src.msg(RELOAD_ACTION_SUCCESS()) }
                        .exceptionally { t ->
                            src.error(
                                    RELOAD_ACTION_ERROR(t.localizedMessage))
                            pex.logger.error(RELOAD_ACTION_ERROR_CONSOLE(src.name, t.localizedMessage), t)
                            null
                        }
            }
                .build()
    }

    private fun getCallbackTestCommand(): CommandSpec {
        return CommandSpec.builder()
            .setAliases("cbtest", "test")
            .setDescription(CALLBACKTEST_DESCRIPTION())
            .setExecutor { src, args ->
                src.msg {send ->
                    send(CALLBACKTEST_CBTEXT.get().callback { sender ->
                        sender.msg(CALLBACKTEST_SUCCESS())
                    }.build())
                }
            }
                .build()
    }

    private fun getVersionCommand(pex: PermissionsEx<*>): CommandSpec {
        return CommandSpec.builder()
            .setAliases("version")
            .setDescription(VERSION_DESCRIPTION())
            .setPermission("permissionsex.version")
            .setArguments(
                flags().flag(
                    "-verbose",
                    "v"
                ).buildWith(none())
            )
            .setExecutor { src, args ->
                val verbose = args.getOne<Boolean>("verbose")?: false
                src.msg { send ->
                    send(make("PermissionsEx v") {
                        it.append((-pex.version).hl())
                    })
                    send(VERSION_RESPONSE_ACTIVE_DATA_STORE(-pex.config.defaultDataStore.name))
                    send(VERSION_RESPONSE_AVAILABLE_DATA_STORES(-DataStoreFactories.getKnownTypes().toString()))
                    send(+"")
                    if (verbose) {
                        send(VERSION_BASEDIRS_HEADER.get().header().build())
                        send(VERSION_BASEDIRS_CONFIG(pex.getBaseDirectory(BaseDirectoryScope.CONFIG).toComponent()))
                        send(VERSION_BASEDIRS_JAR(pex.getBaseDirectory(BaseDirectoryScope.JAR).toComponent()))
                        send(VERSION_BASEDIRS_SERVER(pex.getBaseDirectory(BaseDirectoryScope.SERVER).toComponent()))
                        send(VERSION_BASEDIRS_WORLDS(pex.getBaseDirectory(BaseDirectoryScope.WORLDS).toComponent()))
                    }
                }
            }
                .build()
    }
