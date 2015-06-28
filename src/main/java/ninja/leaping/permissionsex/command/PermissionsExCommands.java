/**
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
package ninja.leaping.permissionsex.command;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.data.SubjectCache;
import ninja.leaping.permissionsex.util.StartsWithPredicate;
import ninja.leaping.permissionsex.util.Util;
import ninja.leaping.permissionsex.util.command.ChildCommands;
import ninja.leaping.permissionsex.util.command.CommandContext;
import ninja.leaping.permissionsex.util.command.CommandException;
import ninja.leaping.permissionsex.util.command.CommandExecutor;
import ninja.leaping.permissionsex.util.command.CommandSpec;
import ninja.leaping.permissionsex.util.command.Commander;
import ninja.leaping.permissionsex.util.command.args.CommandElement;

import javax.annotation.Nullable;
import java.util.Set;

import static ninja.leaping.permissionsex.util.Translations._;
import static ninja.leaping.permissionsex.util.command.args.GameArguments.*;
import static ninja.leaping.permissionsex.util.command.args.GenericArguments.*;

/**
 * Commands used by PermissionsEx
 */
public class PermissionsExCommands {

    public static CommandSpec createRootCommand(final PermissionsEx pex) {
        final Set<CommandSpec> childrenList = ImmutableSet.<CommandSpec>builder()
                .addAll(pex.getImplementationCommands())
                .add(getDebugToggleCommand(pex))
                .add(RankingCommands.getRankingCommand(pex))
                .add(getImportCommand(pex))
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
                .setDescription(_("Commands for PermissionsEx"))
                .setArguments(optional(
                                firstParsing(
                                        children,
                                        Util.contextTransientFlags()
                                                .buildWith(seq(subject(_("subject"), pex), subjectChildren)),
                                        flags()
                                                .flag("-transient")
                                                .buildWith(seq(subjectType(_("subject-type"), pex), literal(_("list"), "list"), optional(string(_("filter")))))
                                )
                        )
                )
                .setExecutor(new CommandExecutor() {
                    @Override
                    public <TextType> void execute(final Commander<TextType> src, CommandContext args) throws CommandException {
                        if (args.hasAny("list")) {
                            final String subjectType = args.getOne("subject-type");
                            args.checkPermission(src, "permissionsex.command.list." + subjectType);
                            SubjectCache cache = args.hasAny("transient") ? pex.getTransientSubjects(subjectType) : pex.getSubjects(subjectType);
                            Iterable<String> iter = cache.getAllIdentifiers();
                            if (args.hasAny("filter")) {
                                iter = Iterables.filter(iter, new StartsWithPredicate(args.<String>getOne("filter")));
                            }

                            src.msgPaginated(_("%s subjects", subjectType), _("All subjects of type %s", subjectType), Iterables.transform(iter, new Function<String, TextType>() {
                                @Nullable
                                @Override
                                public TextType apply(String input) {
                                    return src.fmt().subject(Maps.immutableEntry(subjectType, input));
                                }
                            }));
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

    private static CommandSpec getDebugToggleCommand(final PermissionsEx pex) {
        return CommandSpec.builder()
                .setAliases("debug", "d")
                .setDescription(_("Toggle debug mode"))
                .setPermission("permissionsex.debug")
                .setExecutor(new CommandExecutor() {
                    @Override
                    public <TextType> void execute(Commander<TextType> src, CommandContext args) throws CommandException {
                       boolean debugEnabled = !pex.hasDebugMode();
                        pex.setDebugMode(debugEnabled);
                        src.msg(_("Debug mode enabled: %s", src.fmt().booleanVal(debugEnabled)));
                    }
                })
                .build();
    }

    private static CommandSpec getImportCommand(final PermissionsEx pex) {
        return CommandSpec.builder()
                .setAliases("import")
                .setDescription(_("Import data into the current backend from another"))
                .setArguments(string(_("backend")))
                .setPermission("permissionsex.import")
                .setExecutor(new PermissionsExExecutor(pex) {
                    @Override
                    public <TextType> void execute(Commander<TextType> src, CommandContext args) throws CommandException {
                        messageSubjectOnFuture(pex.importDataFrom(args.<String>getOne("backend")), src, _("Successfully imported data from backend %s into current backend", args.getOne("backend")));
                    }
                })
                .build();
    }

}
