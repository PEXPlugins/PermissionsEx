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

import com.google.common.collect.ImmutableSet;
import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.util.command.CommandContext;
import ninja.leaping.permissionsex.util.command.CommandException;
import ninja.leaping.permissionsex.util.command.CommandExecutor;
import ninja.leaping.permissionsex.util.command.CommandSpec;
import ninja.leaping.permissionsex.util.command.Commander;
import ninja.leaping.permissionsex.util.command.args.CommandElement;

import java.util.Set;

import ninja.leaping.permissionsex.util.command.ChildCommands;

import static ninja.leaping.permissionsex.util.Translations._;
import static ninja.leaping.permissionsex.util.command.args.GenericArguments.*;
import static ninja.leaping.permissionsex.util.command.args.GameArguments.*;

/**
 * Commands used by PermissionsEx
 */
public class PermissionsExCommands {

    public static CommandSpec createRootCommand(final PermissionsEx pex) {
        final Set<CommandSpec> childrenList = ImmutableSet.<CommandSpec>builder()
                .addAll(pex.getImplementationCommands())
                .add(getDebugToggleCommand(pex))
                .build();

        final CommandElement children = ChildCommands.args(childrenList.toArray(new CommandSpec[childrenList.size()]));
        final CommandElement subjectChildren = ChildCommands.args(OptionCommands.getOptionCommand(pex),
                                                                  PermissionsCommands.getPermissionCommand(pex),
                                                                  PermissionsCommands.getPermissionDefaultCommand(pex),
                                                                  InfoCommand.getInfoCommand(pex),
                                                                  ParentCommands.getParentCommand(pex));

        return CommandSpec.builder()
                .setAliases("pex", "permissionsex", "permissions")
                .setDescription(_("Commands for PermissionsEx"))
                .setArguments(flags()
                        .flag("-transient")
                        .valueFlag(context(_("context")), "-context", "-contexts", "c")
                        .buildWith(optional(
                        firstParsing(
                                children,
                                seq(subject(_("subject"), pex), subjectChildren)))
                ))
                .setExecutor(new CommandExecutor() {
                    @Override
                    public <TextType> void execute(Commander<TextType> src, CommandContext args) throws CommandException {
                        if (args.hasAny(subjectChildren.getKey().getUntranslated())) {
                            ChildCommands.executor(subjectChildren).execute(src, args);
                            return;
                        } else if (args.hasAny(children.getKey().getUntranslated())) {
                            ChildCommands.executor(children).execute(src, args);
                            return;
                        }

                        src.msg(src.fmt().combined("PermissionsEx ", src.fmt().hl(src.fmt().combined("v", pex.getVersion()))));
                        src.msg(args.getSpec().getUsage(src));
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

}
