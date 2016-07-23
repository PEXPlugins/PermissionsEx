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
import ninja.leaping.permissionsex.data.DataSegment;
import ninja.leaping.permissionsex.data.ImmutableSubjectData;
import ninja.leaping.permissionsex.data.SubjectRef;
import ninja.leaping.permissionsex.subject.CalculatedSubject;
import ninja.leaping.permissionsex.util.Translatable;
import ninja.leaping.permissionsex.util.Tristate;
import ninja.leaping.permissionsex.util.command.CommandContext;
import ninja.leaping.permissionsex.util.command.CommandException;
import ninja.leaping.permissionsex.util.command.CommandSpec;
import ninja.leaping.permissionsex.util.command.Commander;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static ninja.leaping.permissionsex.util.Translations.t;

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
            printSegments(src, t("Permissions:"), t("Transient permissions"), data, transientData,
                    seg -> !seg.getPermissions().isEmpty() || seg.getPermissionDefault() != Tristate.UNDEFINED,
                    (msg, seg) -> {
                        msg.accept(src.fmt().hl(src.fmt().tr(t("Default permission: %s", seg.getPermissionDefault()))));
                        for (Map.Entry<String, Tristate> ent : seg.getPermissions().entrySet()) {
                            msg.accept(src.fmt().permission(ent.getKey(), ent.getValue()));
                        }
                    });

            printSegments(src, t("Options:"), t("Transient options:"), data, transientData,
                     seg -> !seg.getOptions().isEmpty(),
                    (msg, seg) -> {
                        for (Map.Entry<String, String> option : seg.getOptions().entrySet()) {
                            msg.accept(src.fmt().option(option.getKey(), option.getValue()));
                        }
                    });

            printSegments(src, t("Parents:"), t("Transient parents:"), data, transientData,
                    seg -> !seg.getParents().isEmpty(),
                    (msg, seg) -> {
                        for (SubjectRef parent : seg.getParents()) {
                            msg.accept(src.fmt().subject(parent));
                        }
                    });
        }

        private <TextType> void printSegments(Commander<TextType> src, Translatable heading, Translatable transientHeading, ImmutableSubjectData data, ImmutableSubjectData transientData, Predicate<DataSegment> segmentNonEmpty, BiConsumer<Consumer<TextType>, DataSegment> print) {
            printSegmentsSingle(src, heading, data, segmentNonEmpty, print);
            printSegmentsSingle(src, transientHeading, transientData, segmentNonEmpty, print);
        }

        private <TextType> void printSegmentsSingle(Commander<TextType> src, Translatable heading, ImmutableSubjectData data, Predicate<DataSegment> segmentNonEmpty, BiConsumer<Consumer<TextType>, DataSegment> print) {
            boolean printed = false;
            for (DataSegment segment : data.getAllSegments()) {
                if (segmentNonEmpty.test(segment)) {
                    if (!printed) {
                        src.msg(src.fmt().hl(src.fmt().tr(heading)));
                        printed = true;
                    }
                    src.msg(src.fmt().combined(INDENT, formatSegmentKey(src, segment.getKey()), ":"));
                    print.accept(msg -> src.msg(src.fmt().combined(DOUBLE_INDENT)), segment);
                }
            }
        }
    }
}
