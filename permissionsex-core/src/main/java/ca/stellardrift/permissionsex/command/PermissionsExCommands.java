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

package ca.stellardrift.permissionsex.command;

import ca.stellardrift.permissionsex.BaseDirectoryScope;
import ca.stellardrift.permissionsex.PermissionsEx;
import ca.stellardrift.permissionsex.backend.DataStoreFactories;
import ca.stellardrift.permissionsex.backend.conversion.ConversionResult;
import ca.stellardrift.permissionsex.data.SubjectCache;
import ca.stellardrift.permissionsex.util.GuavaStartsWithPredicate;
import ca.stellardrift.permissionsex.util.Util;
import ca.stellardrift.permissionsex.util.command.*;
import ca.stellardrift.permissionsex.util.command.args.CommandElement;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ca.stellardrift.permissionsex.util.Translations.t;
import static ca.stellardrift.permissionsex.util.command.args.GameArguments.subject;
import static ca.stellardrift.permissionsex.util.command.args.GameArguments.subjectType;
import static ca.stellardrift.permissionsex.util.command.args.GenericArguments.*;

/**
 * Commands used by PermissionsEx
 */
public class PermissionsExCommands {

    public static CommandSpec createRootCommand(final PermissionsEx<?> pex) {
        final Set<CommandSpec> childrenList = ImmutableSet.<CommandSpec>builder()
                .addAll(pex.getImplementationCommands())
                .add(getDebugToggleCommand(pex))
                .add(RankingCommands.getRankingCommand(pex))
                .add(getImportCommand(pex))
                .add(getReloadCommand(pex))
                .add(getVersionCommand(pex))
                //.add(getCallbackTestCommand())
                .build();

        final CommandElement children = ChildCommands.args(childrenList.toArray(new CommandSpec[childrenList.size()]));
        final CommandElement subjectChildren = ChildCommands.args(OptionCommands.getOptionCommand(pex),
                                                                  PermissionsCommands.getPermissionCommand(pex),
                                                                  PermissionsCommands.getPermissionDefaultCommand(pex),
                                                                  InfoCommand.getInfoCommand(pex),
                                                                  ParentCommands.getParentCommand(pex),
                                                                  DeleteCommand.getDeleteCommand(pex));

        return CommandSpec.builder()
                .setAliases("pex", "permissionsex", "permissions")
                .setDescription(t("Commands for PermissionsEx"))
                .setArguments(optional(
                                firstParsing(
                                        children,
                                        Util.contextTransientFlags(pex)
                                                .buildWith(seq(subject(t("subject"), pex), subjectChildren)),
                                        flags()
                                                .flag("-transient")
                                                .buildWith(seq(subjectType(t("subject-type"), pex), literal(t("list"), "list"), optional(string(t("filter")))))
                                )
                        )
                )
                .setExecutor(new CommandExecutor() {
                    @Override
                    public <TextType> void execute(final Commander<TextType> src, CommandContext args) throws CommandException {
                        if (args.hasAny("list")) {
                            final String subjectType = args.getOne("subject-type");
                            args.checkPermission(src, "permissionsex.command.list." + subjectType);
                            SubjectCache cache = args.hasAny("transient") ? pex.getSubjects(subjectType).transientData() : pex.getSubjects(subjectType).persistentData();
                            Iterable<String> iter = cache.getAllIdentifiers();
                            if (args.hasAny("filter")) {
                                iter = Iterables.filter(iter, new GuavaStartsWithPredicate(args.<String>getOne("filter")));
                            }

                            src.msgPaginated(t("%s subjects", subjectType), t("All subjects of type %s", subjectType), Iterables.transform(iter, input -> src.fmt().subject(Maps.immutableEntry(subjectType, input))));
                        } else if (args.hasAny(subjectChildren.getKey().getUntranslated())) {
                            ChildCommands.executor(subjectChildren).execute(src, args);
                        } else if (args.hasAny(children.getKey().getUntranslated())) {
                            ChildCommands.executor(children).execute(src, args);
                        } else {
                            src.msg(src.fmt().combined("PermissionsEx ", src.fmt().hl(src.fmt().combined("v", pex.getVersion()))));
                            src.msg(args.getSpec().getUsage(src));
                        }
                    }
                })
                .build();
    }

    private static CommandSpec getDebugToggleCommand(final PermissionsEx<?> pex) {
        return CommandSpec.builder()
                .setAliases("debug", "d")
                .setDescription(t("Toggle debug mode"))
                .setPermission("permissionsex.debug")
                .setArguments(optional(string(t("filter"))))
                .setExecutor(new CommandExecutor() {
                    @Override
                    public <TextType> void execute(Commander<TextType> src, CommandContext args) throws CommandException {
                        boolean debugEnabled = !pex.hasDebugMode();
                        String filter = args.getOne("filter");
                        if (filter != null) {
                            pex.setDebugMode(debugEnabled, Pattern.compile(filter));
                            src.msg(t("Debug mode enabled: %s with filter %s", src.fmt().booleanVal(debugEnabled), src.fmt().hl(src.fmt().combined(filter))));
                        } else {
                            pex.setDebugMode(debugEnabled);
                            src.msg(t("Debug mode enabled: %s", src.fmt().booleanVal(debugEnabled)));
                        }
                    }
                })
                .build();
    }

    private static CommandSpec getImportCommand(final PermissionsEx<?> pex) {
        return CommandSpec.builder()
                .setAliases("import")
                .setDescription(t("Import data into the current backend from another"))
                .setArguments(optional(string(t("backend"))))
                .setPermission("permissionsex.import")
                .setExecutor(new PermissionsExExecutor(pex) {
                    @Override
                    public <TextType> void execute(Commander<TextType> src, CommandContext args) throws CommandException {
                        final String backendRequested = args.getOne("backend");
                        if (backendRequested == null) { // We want to list available conversions
                            src.msgPaginated(t("Available Conversions"), t("Any data from one of these sources can be imported with the command %s", src.fmt().hl(src.fmt().combined("/pex import [id]"))),
                                    pex.getAvailableConversions().stream()
                                            .map(conv -> src.fmt().tr(t("%s - /pex import %s", conv.getTitle(), src.fmt().callback(t(conv.getStore().getName()), send -> {
                                                src.msg(t("Beginning import from %s... (this may take a while)", conv.getTitle()));
                                                messageSubjectOnFuture(pex.importDataFrom(conv), src, t("Successfully imported data from %s into current data store", conv.getTitle()));

                                            }))))
                                            .collect(Collectors.toList()));
                        } else {
                            for (ConversionResult result : pex.getAvailableConversions()) {
                                if (result.getStore().getName().equalsIgnoreCase(backendRequested)) {
                                    src.msg(t("Beginning import from %s... (this may take a while)", result.getTitle()));
                                    messageSubjectOnFuture(pex.importDataFrom(result), src, t("Successfully imported data from %s into current data store", result.getTitle()));
                                    return;
                                }
                            }
                            if (pex.getConfig().getDataStore(backendRequested) == null) {
                                throw new CommandException(t("Unknown data store %s specified", backendRequested));
                            }
                            src.msg(t("Beginning import from data store %s... (this may take a while)", backendRequested));
                            messageSubjectOnFuture(pex.importDataFrom(backendRequested), src, t("Successfully imported data from data store %s into current backend", backendRequested));
                        }
                    }
                })
                .build();
    }

    private static CommandSpec getReloadCommand(final PermissionsEx<?> pex) {
        return CommandSpec.builder()
                .setAliases("reload", "rel")
                .setDescription(t("Reload the PermissionsEx configuration"))
                .setPermission("permissionsex.reload")
                .setExecutor(new CommandExecutor() {
                    @Override
                    public <TextType> void execute(final Commander<TextType> src, CommandContext args) throws CommandException {
                        src.msg(t("Reloading PermissionsEx"));
                        pex.reload().thenRun(() -> {
                            src.msg(t("The reload was successful"));
                        }).exceptionally(t -> {
                            src.error(t("An error occurred while reloading PEX: %s\n " +
                                    "Please see the server console for details", t.getLocalizedMessage()));
                            pex.getLogger().error(t("An error occurred while reloading PEX (triggered by %s's command): %s",
                                    src.getName(), t.getLocalizedMessage()), t);
                            return null;
                        });
                    }
                })
                .build();
    }

    private static CommandSpec getCallbackTestCommand() {
       return CommandSpec.builder()
               .setAliases("cbtest", "test")
               .setDescription(t("Test that callbacks are working"))
               .setExecutor(new CommandExecutor() {
                   @Override
                   public <TextType> void execute(Commander<TextType> src, CommandContext args) throws CommandException {
                       src.msg(src.fmt().callback(t("Click me!"), send -> {
                           send.msg(t("Callback executed successfully"));
                       }));
                   }
               })
               .build();
    }

    private static CommandSpec getVersionCommand(final PermissionsEx<?> pex) {
        return CommandSpec.builder()
                .setAliases("version")
                .setDescription(t("Get information about the currently running PermissionsEx instance"))
                .setPermission("permissionsex.version")
                .setArguments(flags().flag("-verbose", "v").buildWith(none()))
                .setExecutor(new CommandExecutor() {
                    @Override
                    public <TextType> void execute(Commander<TextType> src, CommandContext args) throws CommandException {
                        Boolean verboseBoxed = args.<Boolean>getOne("verbose");
                        boolean verbose = verboseBoxed == null ? false: verboseBoxed;

                        src.msg(t("PermissionsEx v%s", src.fmt().hl(src.fmt().combined(pex.getVersion()))));
                        src.msg(t("Active data store: %s", pex.getConfig().getDefaultDataStore().getName()));
                        src.msg(t("Available data store types: %s", DataStoreFactories.getKnownTypes()));
                        src.msg(t(""));
                        if (verbose) {
                            src.msg(src.fmt().header(src.fmt().tr(t("Configuration directories"))));
                            src.msg(t("Config: %s", pex.getBaseDirectory(BaseDirectoryScope.CONFIG)));
                            src.msg(t("Jar: %s", pex.getBaseDirectory(BaseDirectoryScope.JAR)));
                            src.msg(t("Server: %s", pex.getBaseDirectory(BaseDirectoryScope.SERVER)));
                            src.msg(t("Worlds: %s", pex.getBaseDirectory(BaseDirectoryScope.WORLDS)));
                        }
                    }
                })
                .build();
    }

}
