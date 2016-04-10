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
import ninja.leaping.permissionsex.data.SubjectDataReference;
import ninja.leaping.permissionsex.util.command.CommandContext;
import ninja.leaping.permissionsex.util.command.CommandException;
import ninja.leaping.permissionsex.util.command.CommandSpec;
import ninja.leaping.permissionsex.util.command.Commander;

import java.util.Map;
import java.util.Set;

import static ninja.leaping.permissionsex.util.Translations.t;
import static ninja.leaping.permissionsex.util.command.args.GameArguments.option;
import static ninja.leaping.permissionsex.util.command.args.GenericArguments.*;

public class OptionCommands {
    private OptionCommands() {}
    public static CommandSpec getOptionCommand(PermissionsEx pex) {
        return CommandSpec.builder()
                .setAliases("options", "option", "opt", "o")
                .setArguments(seq(option(t("key"), pex), optional(string(t("value")))))
                .setExecutor(new PermissionsExExecutor(pex) {
                    @Override
                    public <TextType> void execute(Commander<TextType> src, CommandContext args) throws CommandException {
                        SubjectDataReference ref = getDataRef(src, args, "permissionsex.option.set");
                        Set<Map.Entry<String, String>> contexts = ImmutableSet.copyOf(args.<Map.Entry<String, String>>getAll("context"));
                        final String key = args.getOne("key");
                        final String value = args.getOne("value");
                        if (value == null) {
                            messageSubjectOnFuture(ref.update(old -> old.setOption(contexts, key, null)), src,
                                    t("Unset option '%s' for %s in %s context", key, src.fmt().hl(src.fmt().subject(ref)), formatContexts(src, contexts)));
                        } else {
                            messageSubjectOnFuture(
                                    ref.update(old -> old.setOption(contexts, key, value)), src,
                                    t("Set option %s for %s in %s context", src.fmt().option(key, value), src.fmt().hl(src.fmt().subject(ref)), formatContexts(src, contexts)));
                        }
                    }
                })
                .build();

    }
}
