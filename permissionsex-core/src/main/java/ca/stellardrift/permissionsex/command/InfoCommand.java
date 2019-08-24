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
package ca.stellardrift.permissionsex.command;

import ca.stellardrift.permissionsex.PermissionsEx;
import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.data.ImmutableSubjectData;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import ca.stellardrift.permissionsex.util.command.CommandContext;
import ca.stellardrift.permissionsex.util.command.CommandException;
import ca.stellardrift.permissionsex.util.command.CommandSpec;
import ca.stellardrift.permissionsex.util.command.Commander;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static ca.stellardrift.permissionsex.util.Translations.t;

public class InfoCommand {
    public static CommandSpec getInfoCommand(PermissionsEx pex) {
        return CommandSpec.builder()
                .setAliases("info", "i", "who")
                .setDescription(t("Provide information about a subject"))
                .setExecutor(new SubjectInfoPrintingExecutor(pex))
                .build();
    }

    // TODO: Pagination builder

    private static class SubjectInfoPrintingExecutor extends PermissionsExExecutor {
        private static final String INDENT = "  ";
        private static final String DOUBLE_INDENT = INDENT + INDENT;

        private SubjectInfoPrintingExecutor(PermissionsEx pex) {
            super(pex);
        }

        @Override
        public <TextType> void execute(Commander<TextType> src, CommandContext args) throws CommandException {
            CalculatedSubject subject = subjectOrSelf(src, args);
            checkSubjectPermission(src, subject.getIdentifier(), "permissionsex.info");
            final ImmutableSubjectData transientData = subject.transientData().get();
            final ImmutableSubjectData data = subject.data().get();

            src.msg(src.fmt().header(src.fmt().tr(t("Information for %s", src.fmt().subject(subject)))));
            if (pex.hasDebugMode()) {
                Optional<?> associatedObject = subject.getAssociatedObject();
                associatedObject.ifPresent(o -> src.msg(src.fmt().combined(src.fmt().hl(src.fmt().tr(t("Associated object: "))), o.toString())));
                src.msg(src.fmt().combined(src.fmt().hl(src.fmt().tr(t("Active Contexts: "))), subject.getActiveContexts()));
                src.msg(src.fmt().combined(src.fmt().hl(src.fmt().tr(t("Active & Used Contexts: "))), subject.getUsedContextValues().join()));
            }
            if (!data.getAllPermissions().isEmpty() || !data.getAllDefaultValues().isEmpty()) {
                src.msg(src.fmt().hl(src.fmt().tr(t("Permissions:"))));
                printPermissions(src, data);
            }
            if (!transientData.getAllPermissions().isEmpty() || !transientData.getAllDefaultValues().isEmpty()) {
                src.msg(src.fmt().hl(src.fmt().tr(t("Transient permissions:"))));
                printPermissions(src, transientData);
            }

            if (!data.getAllOptions().isEmpty()) {
                src.msg(src.fmt().hl(src.fmt().tr(t("Options:"))));
                printOptions(src, data);
            }
            if (!transientData.getAllOptions().isEmpty()) {
                src.msg(src.fmt().hl(src.fmt().tr(t("Transient options:"))));
                printOptions(src, transientData);
            }

            if (!data.getAllParents().isEmpty()) {
                src.msg(src.fmt().hl(src.fmt().tr(t("Parents:"))));
                printParents(src, data);
            }
            if (!transientData.getAllParents().isEmpty()) {
                src.msg(src.fmt().hl(src.fmt().tr(t("Transient parents:"))));
                printParents(src, transientData);
            }

        }

        private <TextType> void printPermissions(Commander<TextType> src, ImmutableSubjectData data) {
            Set<Set<ContextValue<?>>> targetContexts = new HashSet<>();
            targetContexts.addAll(data.getAllPermissions().keySet());
            targetContexts.addAll(data.getAllDefaultValues().keySet());

            for (Set<ContextValue<?>> entry : targetContexts) {
                src.msg(src.fmt().combined(INDENT, formatContexts(src, entry), ":"));
                src.msg(src.fmt().combined(DOUBLE_INDENT, src.fmt().hl(src.fmt().tr(t("Default permission: %s", data.getDefaultValue(entry))))));
                for (Map.Entry<String, Integer> ent : data.getPermissions(entry).entrySet()) {
                    src.msg(src.fmt().combined(DOUBLE_INDENT, src.fmt().permission(ent.getKey(), ent.getValue())));
                }
            }
        }

        private <TextType> void printOptions(Commander<TextType> src, ImmutableSubjectData data) {
            for (Map.Entry<Set<ContextValue<?>>, Map<String, String>> ent : data.getAllOptions().entrySet()) {
                src.msg(src.fmt().combined(INDENT, formatContexts(src, ent.getKey()), ":"));
                for (Map.Entry<String, String> option : ent.getValue().entrySet()) {
                    src.msg(src.fmt().combined(DOUBLE_INDENT, src.fmt().option(option.getKey(), option.getValue())));
                }
            }
        }

        private <TextType> void printParents(Commander<TextType> src, ImmutableSubjectData data) {
            for (Map.Entry<Set<ContextValue<?>>, List<Map.Entry<String, String>>> ent : data.getAllParents().entrySet()) {

                src.msg(src.fmt().combined(INDENT, formatContexts(src, ent.getKey()), ":"));
                for (Map.Entry<String, String> parent : ent.getValue()) {
                    src.msg(src.fmt().combined(DOUBLE_INDENT, src.fmt().subject(parent)));
                }
            }
        }
    }
}
