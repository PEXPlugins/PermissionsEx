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

import com.google.common.collect.ImmutableSet;
import ca.stellardrift.permissionsex.PermissionsEx;
import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.data.SubjectDataReference;
import ca.stellardrift.permissionsex.util.command.CommandContext;
import ca.stellardrift.permissionsex.util.command.CommandException;
import ca.stellardrift.permissionsex.util.command.CommandSpec;
import ca.stellardrift.permissionsex.util.command.Commander;

import java.util.Map;
import java.util.Set;

import static ca.stellardrift.permissionsex.util.Translations.t;
import static ca.stellardrift.permissionsex.util.command.args.GameArguments.subject;

public class ParentCommands {
    private ParentCommands() {}
    public static CommandSpec getParentCommand(PermissionsEx pex) {
        return CommandSpec.builder()
                .setAliases("parents", "parent", "par", "p")
                .setChildren(getAddParentCommand(pex), getRemoveParentCommand(pex), getSetParentsCommand(pex))
                .build();
    }

    private static CommandSpec getAddParentCommand(final PermissionsEx pex) {
        return CommandSpec.builder()
                .setAliases("add", "a", "+")
                .setArguments(subject(t("parent"), pex, PermissionsEx.SUBJECTS_GROUP))
                .setExecutor(new PermissionsExExecutor(pex) {
                    @Override
                    public <TextType> void execute(Commander<TextType> src, CommandContext args) throws CommandException {
                        SubjectDataReference ref = getDataRef(src, args, "permissionsex.parent.add");
                        Set<ContextValue<?>> contexts = ImmutableSet.copyOf(args.getAll("context"));
                        Map.Entry<String, String> parent = args.getOne("parent");
                        messageSubjectOnFuture(
                                ref.update(old -> old.addParent(contexts, parent.getKey(), parent.getValue())), src,
                                t("Added parent %s for %s in %s context", src.fmt().subject(parent), src.fmt().hl(src.fmt().subject(ref)), formatContexts(src, contexts)));
                    }
                })
                .build();
    }

    private static CommandSpec getRemoveParentCommand(final PermissionsEx pex) {
        return CommandSpec.builder()
                .setAliases("remove", "rem", "delete", "del", "-")
                .setArguments(subject(t("parent"), pex, PermissionsEx.SUBJECTS_GROUP))
                .setExecutor(new PermissionsExExecutor(pex) {
                    @Override
                    public <TextType> void execute(Commander<TextType> src, CommandContext args) throws CommandException {
                        SubjectDataReference ref = getDataRef(src, args, "permissionsex.parent.remove");
                        Set<ContextValue<?>> contexts = ImmutableSet.copyOf(args.getAll("context"));
                        Map.Entry<String, String> parent = args.getOne("parent");
                        messageSubjectOnFuture(
                                ref.update(old -> old.removeParent(contexts, parent.getKey(), parent.getValue())), src,
                                t("Removed parent %s for %s in %s context", src.fmt().subject(parent), src.fmt().hl(src.fmt().subject(ref)), formatContexts(src, contexts)));
                    }
                })
                .build();
    }

    private static CommandSpec getSetParentsCommand(final PermissionsEx pex) {
        return CommandSpec.builder()
                .setAliases("set", "replace", "=")
                .setArguments(subject(t("parent"), pex, PermissionsEx.SUBJECTS_GROUP))
                .setExecutor(new PermissionsExExecutor(pex) {
                    @Override
                    public <TextType> void execute(Commander<TextType> src, CommandContext args) throws CommandException {
                        SubjectDataReference ref = getDataRef(src, args, "permissionsex.parent.set");
                        Set<ContextValue<?>> contexts = ImmutableSet.copyOf(args.getAll("context"));
                        Map.Entry<String, String> parent = args.getOne("parent");
                        messageSubjectOnFuture(
                                ref.update(old -> old.clearParents(contexts).addParent(contexts, parent.getKey(), parent.getValue())), src,
                                t("Set parent for %s to %s in %s context", src.fmt().hl(src.fmt().subject(ref)), src.fmt().subject(parent), formatContexts(src, contexts)));
                    }
                })
                .build();

    }
}
