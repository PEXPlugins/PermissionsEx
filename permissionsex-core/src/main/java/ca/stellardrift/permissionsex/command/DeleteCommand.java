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
import ca.stellardrift.permissionsex.data.SubjectCache;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import ca.stellardrift.permissionsex.util.command.CommandContext;
import ca.stellardrift.permissionsex.util.command.CommandException;
import ca.stellardrift.permissionsex.util.command.CommandSpec;
import ca.stellardrift.permissionsex.util.command.Commander;

import static ca.stellardrift.permissionsex.util.Translations.t;

/**
 * Command that deletes all data for a subject
 */
public class DeleteCommand {
    public static CommandSpec getDeleteCommand(PermissionsEx pex) {
        return CommandSpec.builder()
                .setAliases("delete", "del", "remove", "rem")
                .setExecutor(new PermissionsExExecutor(pex) {
                    @Override
                    public <TextType> void execute(Commander<TextType> src, CommandContext args) throws CommandException {
                        CalculatedSubject subject = subjectOrSelf(src, args);
                        checkSubjectPermission(src, subject.getIdentifier(), "permissionsex.delete");
                        SubjectCache cache = args.hasAny("transient") ? subject.transientData().getCache() : subject.data().getCache();
                        messageSubjectOnFuture(cache.isRegistered(subject.getIdentifier().getValue())
                                .thenCompose(registered -> {
                                    if (!registered) {
                                        throw new RuntimeCommandException(t("Subject %s does not exist!", src.fmt().subject(subject)));
                                    }
                                    return cache.remove(subject.getIdentifier().getValue());
                                }), src, t("Successfully deleted data for subject %s", src.fmt().subject(subject)));
                    }
                })
                .build();
    }
}
