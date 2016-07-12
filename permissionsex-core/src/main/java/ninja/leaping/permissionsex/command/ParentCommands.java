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

import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.data.SubjectRef;
import ninja.leaping.permissionsex.util.command.CommandContext;
import ninja.leaping.permissionsex.util.command.CommandException;
import ninja.leaping.permissionsex.util.command.CommandSpec;
import ninja.leaping.permissionsex.util.command.Commander;

import static ninja.leaping.permissionsex.util.Translations.t;
import static ninja.leaping.permissionsex.util.command.args.GameArguments.subject;

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
                        SubjectRef parent = args.getOne("parent");
                        updateDataSegment(src, args, "permissionsex.parent.add", seg -> seg.withAddedParent(parent),
                                (subj, contexts) -> t("Added parent %s for %s in %s context", src.fmt().subject(parent), src.fmt().hl(src.fmt().subject(subj)), formatContexts(src, contexts)));
                    }
                })
                .build();
    }

    private static CommandSpec getRemoveParentCommand(final PermissionsEx pex) {
        return CommandSpec.builder()
                .setAliases("remove", "rem", "delete", "del", "r", "d", "-")
                .setArguments(subject(t("parent"), pex, PermissionsEx.SUBJECTS_GROUP))
                .setExecutor(new PermissionsExExecutor(pex) {
                    @Override
                    public <TextType> void execute(Commander<TextType> src, CommandContext args) throws CommandException {
                        SubjectRef parent = args.getOne("parent");
                        updateDataSegment(src, args, "permissionsex.parent.remove", seg -> seg.withRemovedParent(parent),
                                (subj, contexts) -> t("Removed parent %s for %s in %s context", src.fmt().subject(parent), src.fmt().hl(src.fmt().subject(subj)), formatContexts(src, contexts)));
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
                        SubjectRef parent = args.getOne("parent");
                        updateDataSegment(src, args, "permissionsex.parent.set", seg -> seg.withoutParents().withAddedParent(parent),
                                (subj, contexts) -> t("Set parent for %s to %s in %s context", src.fmt().hl(src.fmt().subject(subj)), src.fmt().subject(parent), formatContexts(src, contexts)));
                    }
                })
                .build();

    }
}
