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
import ninja.leaping.permissionsex.util.command.CommandContext;
import ninja.leaping.permissionsex.util.command.CommandException;
import ninja.leaping.permissionsex.util.command.CommandSpec;
import ninja.leaping.permissionsex.util.command.Commander;

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
                        final String key = args.getOne("key");
                        final String value = args.getOne("value");
                        if (value == null) {
                            updateDataSegment(src, args, "permissionsex.option.set", seg -> seg.withoutOption(key),
                                    (subj, contexts) -> t("Unset option '%s' for %s in %s context", key, src.fmt().hl(src.fmt().subject(subj)), formatContexts(src, contexts)));
                        } else {
                            updateDataSegment(src, args, "permissionsex.option.set", seg -> seg.withOption(key, value),
                                    (subj, contexts) -> t("Set option %s for %s in %s context", src.fmt().option(key, value), src.fmt().hl(src.fmt().subject(subj)), formatContexts(src, contexts)));
                        }
                    }
                })
                .build();

    }
}
