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
import ca.stellardrift.permissionsex.commands.commander.Commander
import ca.stellardrift.permissionsex.util.Translations.t
import ca.stellardrift.permissionsex.util.Util
import ca.stellardrift.permissionsex.util.command.ChildCommands
import ca.stellardrift.permissionsex.util.command.CommandContext
import ca.stellardrift.permissionsex.util.command.CommandException
import ca.stellardrift.permissionsex.util.command.CommandExecutor
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
import java.util.regex.Pattern

fun createRootCommand(pex: PermissionsEx<*>): CommandSpec {
        val childrenList: Set<CommandSpec> = setOf(
            getDebugToggleCommand(pex),
            getRankingCommand(pex),
            getImportCommand(pex),
            getReloadCommand(pex),
            getVersionCommand(pex)
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
            .setDescription(t("Commands for PermissionsEx"))
            .setArguments(
                optional(
                    firstParsing(children, Util.contextTransientFlags(pex)
                            .buildWith(
                                seq(subject(t("subject"), pex), subjectChildren)
                            ), flags()
                            .flag("-transient")
                            .buildWith(
                                seq(subjectType(t("subject-type"), pex), literal(t("list"), "list"), optional(string(t("filter"))))
                            ))
                )
            )
            .setExecutor(object : CommandExecutor {
                @Throws(CommandException::class)
                override fun <TextType> execute(
                    src: Commander<TextType>,
                    args: CommandContext
                ) {
                    when {
                        args.hasAny("list") -> {
                            val subjectType = args.getOne<String>("subject-type")
                            args.checkPermission(src, "permissionsex.command.list.$subjectType")
                            val cache =
                                if (args.hasAny("transient")) pex.getSubjects(subjectType).transientData() else pex.getSubjects(
                                    subjectType
                                ).persistentData()
                            var iter  = cache.allIdentifiers.asSequence()
                            if (args.hasAny("filter")) {
                                val filter = args.getOne<String>("filter")
                                iter = iter.filter { it.startsWith(filter, ignoreCase = true) }
                            }
                            src.msgPaginated(
                                t("%s subjects", subjectType),
                                t("All subjects of type %s", subjectType),
                                iter.map { src.formatter.subject(subjectType to it) }.asIterable()
                            )
                        }
                        args.hasAny(subjectChildren.key.untranslated) -> {
                            ChildCommands.executor(subjectChildren).execute(src, args)
                        }
                        args.hasAny(children.key.untranslated) -> {
                            ChildCommands.executor(children).execute(src, args)
                        }
                        else -> {
                            src.msg { send ->
                                send(-"PermissionsEx " + (-"v${pex.version}").hl())
                                send(args.spec.getUsage(src))
                            }
                        }
                    }
                }
            })
            .build()
    }


    private fun getDebugToggleCommand(pex: PermissionsEx<*>): CommandSpec {
        return CommandSpec.builder()
            .setAliases("debug", "d")
            .setDescription(t("Toggle debug mode"))
            .setPermission("permissionsex.debug")
            .setArguments(optional(string(t("filter"))))
            .setExecutor(object : CommandExecutor {
                @Throws(CommandException::class)
                override fun <TextType> execute(
                    src: Commander<TextType>,
                    args: CommandContext
                ) {
                    val debugEnabled = !pex.hasDebugMode()
                    val filter = args.getOne<String>("filter")
                    src.msg {send ->
                        if (filter != null) {
                            pex.setDebugMode(debugEnabled, Pattern.compile(filter))
                            send(t(
                                    "Debug mode enabled: %s with filter %s", -debugEnabled, (-filter).hl()).tr()
                                )
                        } else {
                            pex.setDebugMode(debugEnabled)
                            send(t("Debug mode enabled: %s", -debugEnabled).tr())
                        }

                    }
                }
            })
            .build()
    }

    private fun getImportCommand(pex: PermissionsEx<*>): CommandSpec {
        return CommandSpec.builder()
            .setAliases("import")
            .setDescription(t("Import data into the current backend from another"))
            .setArguments(optional(string(t("backend"))))
            .setPermission("permissionsex.import")
            .setExecutor(object : PermissionsExExecutor(pex) {
                override fun <TextType> execute(src: Commander<TextType>, args: CommandContext) {
                    val backendRequested = args.getOne<String?>("backend")
                        if (backendRequested == null) { // We want to list available conversions
                            src.formatter.apply {
                                src.msgPaginated(t("Available Conversions"),
                                    t(
                                        "Any data from one of these sources can be imported with the command %s",
                                        (-"/pex import [id]").hl()
                                    ),
                                    pex.availableConversions
                                        .map { conv ->
                                                t(
                                                    "%s - /pex import %s",
                                                    conv.title,
                                                    src.formatter.callback(t(conv.store.name)) {
                                                        it.msg(
                                                            t(
                                                                "Beginning import from %s... (this may take a while)",
                                                                conv.title
                                                            )
                                                        )
                                                        pex.importDataFrom(conv).thenMessageSubject(it) { ->
                                                            t(
                                                                "Successfully imported data from %s into current data store",
                                                                conv.title
                                                            )
                                                        }

                                                    }).tr()
                                        })
                            }
                        } else {
                            for (result in pex.availableConversions) {
                                if (result.store.name.equals(backendRequested, ignoreCase = true)) {
                                    src.msg(t("Beginning import from %s... (this may take a while)", result.title))
                                    pex.importDataFrom(result).thenMessageSubject(src) { ->  t("Successfully imported data from %s into current data store", result.title) }
                                    return
                                }
                            }
                            if (pex.config.getDataStore(backendRequested) == null) {
                                throw CommandException(t("Unknown data store %s specified", backendRequested));
                            }
                            src.msg(t("Beginning import from data store %s... (this may take a while)", backendRequested));
                            pex.importDataFrom(backendRequested).thenMessageSubject(src) { -> t("Successfully imported data from data store %s into current backend", backendRequested)};
                        }
                }
            })
            .build();
    }
    private fun getReloadCommand(pex: PermissionsEx<*>): CommandSpec {
        return CommandSpec.builder()
            .setAliases("reload", "rel")
            .setDescription(t("Reload the PermissionsEx configuration"))
            .setPermission("permissionsex.reload")
            .setExecutor(object : CommandExecutor {
                @Throws(CommandException::class)
                override fun <TextType> execute(
                    src: Commander<TextType>,
                    args: CommandContext
                ) {
                    src.msg(t("Reloading PermissionsEx"))
                    pex.reload().thenRun { src.msg(t("The reload was successful")) }
                        .exceptionally { t ->
                            src.error(
                                t(
                                    "An error occurred while reloading PEX: %s\n " +
                                            "Please see the server console for details", t.localizedMessage
                                )
                            )
                            pex.logger.error(
                                t(
                                    "An error occurred while reloading PEX (triggered by %s's command): %s",
                                    src.name, t.localizedMessage
                                ), t
                            )
                            null
                        }
                }
            })
            .build()
    }

    private fun getCallbackTestCommand(): CommandSpec {
        return CommandSpec.builder()
            .setAliases("cbtest", "test")
            .setDescription(t("Test that callbacks are working"))
            .setExecutor(object : CommandExecutor {
                @Throws(CommandException::class)
                override fun <TextType> execute(
                    src: Commander<TextType>,
                    args: CommandContext
                ) {
                    src.msg {send ->
                        send(callback(
                            t("Click me!")
                        ) { sender ->
                            sender.msg(t("Callback executed successfully"))
                        })
                    }
                }
            })
            .build()
    }

    private fun getVersionCommand(pex: PermissionsEx<*>): CommandSpec {
        return CommandSpec.builder()
            .setAliases("version")
            .setDescription(t("Get information about the currently running PermissionsEx instance"))
            .setPermission("permissionsex.version")
            .setArguments(
                flags().flag(
                    "-verbose",
                    "v"
                ).buildWith(none())
            )
            .setExecutor(object : CommandExecutor {
                @Throws(CommandException::class)
                override fun <TextType> execute(
                    src: Commander<TextType>,
                    args: CommandContext
                ) {
                    val verbose = args.getOne<Boolean>("verbose")?: false
                    src.msg { send ->
                        send(t("PermissionsEx v%s", (-pex.version).hl()).tr())
                        send(t("Active data store: %s", pex.config.defaultDataStore.name).tr())
                        send(t("Available data store types: %s", DataStoreFactories.getKnownTypes()).tr())
                        send(-"")
                        if (verbose) {
                            send(t("Configuration directories").tr().header())
                            send(t("Config: %s", pex.getBaseDirectory(BaseDirectoryScope.CONFIG)).tr())
                            send(t("Jar: %s", pex.getBaseDirectory(BaseDirectoryScope.JAR)).tr())
                            send(t("Server: %s", pex.getBaseDirectory(BaseDirectoryScope.SERVER)).tr())
                            send(t("Worlds: %s", pex.getBaseDirectory(BaseDirectoryScope.WORLDS)).tr())
                        }
                    }
                }
            })
            .build()
    }
