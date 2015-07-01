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
import ninja.leaping.permissionsex.data.ImmutableOptionSubjectData;
import ninja.leaping.permissionsex.util.command.CommandContext;
import ninja.leaping.permissionsex.util.command.CommandException;
import ninja.leaping.permissionsex.util.command.CommandSpec;
import ninja.leaping.permissionsex.util.command.Commander;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ninja.leaping.permissionsex.util.Translations._;

public class InfoCommand {
    public static CommandSpec getInfoCommand(PermissionsEx pex) {
        return CommandSpec.builder()
                .setAliases("info", "i", "who")
                .setDescription(_("Provide information about the user "))
                .setExecutor(new SubjectInfoPrintingExecutor(pex))
                .build();
    }

    // TODO: Pagination builder

    private static class SubjectInfoPrintingExecutor extends PermissionsExExecutor {
        private static final String INDENT = "  ";
        private static final String DOUBLE_INDENT = INDENT + INDENT;
        private final PermissionsEx pex;

        private SubjectInfoPrintingExecutor(PermissionsEx pex) {
            super(pex);
            this.pex = pex;
        }

        @Override
        public <TextType> void execute(Commander<TextType> src, CommandContext args) throws CommandException {
            Map.Entry<String, String> subject = subjectOrSelf(src, args);
            checkSubjectPermission(src, subject, "permissionsex.info");
            final ImmutableOptionSubjectData transientData = getSubjectData(pex.getTransientSubjects(subject.getKey()), subject.getValue());
            final ImmutableOptionSubjectData data = getSubjectData(pex.getSubjects(subject.getKey()), subject.getValue());

            src.msg(src.fmt().header(src.fmt().tr(_("Information for %s", src.fmt().subject(subject)))));
            if (!data.getAllPermissions().isEmpty()) {
                src.msg(src.fmt().hl(src.fmt().tr(_("Permissions:"))));
                printPermissions(src, data);
            }
            if (!transientData.getAllPermissions().isEmpty()) {
                src.msg(src.fmt().hl(src.fmt().tr(_("Transient permissions:"))));
                printPermissions(src, transientData);
            }

            if (!data.getAllOptions().isEmpty()) {
                src.msg(src.fmt().hl(src.fmt().tr(_("Options:"))));
                printOptions(src, data);
            }
            if (!transientData.getAllOptions().isEmpty()) {
                src.msg(src.fmt().hl(src.fmt().tr(_("Transient options:"))));
                printOptions(src, transientData);
            }

            if (!data.getAllParents().isEmpty()) {
                src.msg(src.fmt().hl(src.fmt().tr(_("Parents:"))));
                printParents(src, data);
            }
            if (!transientData.getAllParents().isEmpty()) {
                src.msg(src.fmt().hl(src.fmt().tr(_("Transient parents:"))));
                printParents(src, transientData);
            }

        }

        private <TextType> void printPermissions(Commander<TextType> src, ImmutableOptionSubjectData data) {
            Set<Set<Map.Entry<String, String>>> targetContexts = new HashSet<>();
            targetContexts.addAll(data.getAllPermissions().keySet());
            targetContexts.addAll(data.getAllDefaultValues().keySet());

            for (Set<Map.Entry<String, String>> entry : targetContexts) {
                src.msg(src.fmt().combined(INDENT, formatContexts(src, entry), ":"));
                src.msg(src.fmt().combined(DOUBLE_INDENT, src.fmt().hl(src.fmt().tr(_("Default permission: %s", data.getDefaultValue(entry))))));
                for (Map.Entry<String, Integer> ent : data.getPermissions(entry).entrySet()) {
                    src.msg(src.fmt().combined(DOUBLE_INDENT, src.fmt().permission(ent.getKey(), ent.getValue())));
                }
            }
        }

        private <TextType> void printOptions(Commander<TextType> src, ImmutableOptionSubjectData data) {
            for (Map.Entry<Set<Map.Entry<String, String>>, Map<String, String>> ent : data.getAllOptions().entrySet()) {
                src.msg(src.fmt().combined(INDENT, formatContexts(src, ent.getKey()), ":"));
                for (Map.Entry<String, String> option : ent.getValue().entrySet()) {
                    src.msg(src.fmt().combined(DOUBLE_INDENT, src.fmt().option(option.getKey(), option.getValue())));
                }
            }
        }

        private <TextType> void printParents(Commander<TextType> src, ImmutableOptionSubjectData data) {
            for (Map.Entry<Set<Map.Entry<String, String>>, List<Map.Entry<String, String>>> ent : data.getAllParents().entrySet()) {

                src.msg(src.fmt().combined(INDENT, formatContexts(src, ent.getKey()), ":"));
                for (Map.Entry<String, String> parent : ent.getValue()) {
                    src.msg(src.fmt().combined(DOUBLE_INDENT, src.fmt().subject(parent)));
                }
            }
        }
    }
}
