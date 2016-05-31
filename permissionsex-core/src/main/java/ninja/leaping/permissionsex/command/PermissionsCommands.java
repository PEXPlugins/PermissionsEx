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

import com.google.common.collect.ImmutableMap;
import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.util.Translatable;
import ninja.leaping.permissionsex.util.Tristate;
import ninja.leaping.permissionsex.util.command.CommandContext;
import ninja.leaping.permissionsex.util.command.CommandException;
import ninja.leaping.permissionsex.util.command.CommandSpec;
import ninja.leaping.permissionsex.util.command.Commander;
import ninja.leaping.permissionsex.util.command.args.CommandElement;

import static ninja.leaping.permissionsex.util.Translations.t;
import static ninja.leaping.permissionsex.util.command.args.GameArguments.permission;
import static ninja.leaping.permissionsex.util.command.args.GenericArguments.*;

public class PermissionsCommands {
    private PermissionsCommands() {}

    public static CommandElement permissionValue(Translatable key) {
        return firstParsing(bool(key), enumValue(key, Tristate.class), choices(key, ImmutableMap.of("none", Tristate.FALSE, "null", Tristate.FALSE, "unset", Tristate.FALSE)));
    }

    public static CommandSpec getPermissionCommand(final PermissionsEx pex) {
        return CommandSpec.builder()
                .setAliases("permission", "permissions", "perm", "perms", "p")
                .setArguments(seq(permission(t("key"), pex), permissionValue(t("value"))))
                .setExecutor(new PermissionsExExecutor(pex) {
                    @Override
                    public <TextType> void execute(Commander<TextType> src, CommandContext args) throws CommandException {
                        final String key = args.getOne("key");
                        Object value = args.getOne("value");
                        if (value instanceof Boolean) {
                            value = ((Boolean) value) ? Tristate.TRUE : Tristate.FALSE;
                        }
                        Tristate intVal = (Tristate) value;
                        updateDataSegment(src, args, "permissionsex.permission.set", seg -> seg.withPermission(key, intVal),
                                (subj, contexts) -> t("Set permission %s for %s in %s context", src.fmt().permission(key, intVal), src.fmt().hl(src.fmt().subject(subj)), formatContexts(src, contexts)));
                    }
                })
                .build();
    }

    public static CommandSpec getPermissionDefaultCommand(final PermissionsEx pex) {
        return CommandSpec.builder()
                .setAliases("permission-default", "perms-def", "permsdef", "pdef", "pd", "default", "def")
                .setArguments(permissionValue(t("value")))
                .setExecutor(new PermissionsExExecutor(pex) {
                    @Override
                    public <TextType> void execute(Commander<TextType> src, CommandContext args) throws CommandException {
                        Object value = args.getOne("value");
                        if (value instanceof Boolean) {
                            value = ((Boolean) value) ? Tristate.TRUE : Tristate.FALSE;
                        }
                        Tristate intVal = (Tristate) value;

                        updateDataSegment(src, args, "permissionsex.permission.unset", seg -> seg.withDefaultValue(intVal),
                                (subj, contexts) -> t("Set default permission to %s for %s in %s context", intVal, src.fmt().hl(src.fmt().subject(subj)), formatContexts(src, contexts)));
                    }
                })
                .build();
    }
}
