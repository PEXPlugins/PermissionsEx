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
import ninja.leaping.permissionsex.data.SubjectCache;
import ninja.leaping.permissionsex.util.command.CommandContext;
import ninja.leaping.permissionsex.util.command.CommandException;
import ninja.leaping.permissionsex.util.command.CommandSpec;
import ninja.leaping.permissionsex.util.command.Commander;

import java.util.Map;

import static ninja.leaping.permissionsex.util.Translations._;

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
                        Map.Entry<String, String> subject = subjectOrSelf(src, args);
                        checkSubjectPermission(src, subject, "permissionsex.delete");
                        SubjectCache cache = args.hasAny("transient") ? pex.getTransientSubjects(subject.getKey()) : pex.getSubjects(subject.getKey());
                        if (!cache.isRegistered(subject.getValue())) {
                            throw new CommandException(_("Subject %s does not exist!", src.fmt().subject(subject)));
                        }
                        messageSubjectOnFuture(cache.update(subject.getValue(), null), src, _("Successfully deleted data for subject %s", src.fmt().subject(subject)));
                    }
                })
                .build();
    }
}
