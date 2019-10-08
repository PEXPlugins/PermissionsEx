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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import ca.stellardrift.permissionsex.PermissionsEx;
import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.data.SubjectDataReference;
import ca.stellardrift.permissionsex.util.Translatable;
import ca.stellardrift.permissionsex.util.command.CommandContext;
import ca.stellardrift.permissionsex.util.command.CommandException;
import ca.stellardrift.permissionsex.util.command.CommandSpec;
import ca.stellardrift.permissionsex.util.command.Commander;
import ca.stellardrift.permissionsex.util.command.args.CommandElement;

import java.util.Set;

import static ca.stellardrift.permissionsex.util.Translations.t;
import static ca.stellardrift.permissionsex.util.command.args.GameArguments.permission;
import static ca.stellardrift.permissionsex.util.command.args.GenericArguments.*;

public class PermissionsCommands {
    private PermissionsCommands() {}

    public static CommandElement permissionValue(Translatable key) {
        return firstParsing(integer(key), bool(key), choices(key, ImmutableMap.of("none", 0, "null", 0, "unset", 0)));
    }

    public static CommandSpec getPermissionCommand(final PermissionsEx pex) {
        return CommandSpec.builder()
                .setAliases("permission", "permissions", "perm", "perms", "p")
                .setArguments(seq(permission(t("key"), pex), permissionValue(t("value"))))
                .setExecutor(new PermissionsExExecutor(pex) {
                    @Override
                    public <TextType> void execute(Commander<TextType> src, CommandContext args) throws CommandException {
                        SubjectDataReference ref = getDataRef(src, args, "permissionsex.permission.set");
                        Set<ContextValue<?>> contexts = ImmutableSet.copyOf(args.getAll("context"));

                        final String key = args.getOne("key");
                        Object value = args.getOne("value");
                        if (value instanceof Boolean) {
                            value = ((Boolean) value) ? 1 : -1;
                        }
                        int intVal = (Integer) value;

                        messageSubjectOnFuture(
                                ref.update(old -> old.setPermission(contexts, key, intVal)), src,
                                t("Set permission %s for %s in %s context", src.fmt().permission(key, intVal), src.fmt().hl(src.fmt().subject(ref)), formatContexts(src, contexts)));
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
                        SubjectDataReference ref = getDataRef(src, args, "permissionsex.permission.set-default");
                        Set<ContextValue<?>> contexts = ImmutableSet.copyOf(args.getAll("context"));

                        Object value = args.getOne("value");
                        if (value instanceof Boolean) {
                            value = ((Boolean) value) ? 1 : -1;
                        }
                        int intVal = (Integer) value;

                        messageSubjectOnFuture(
                                ref.update(old -> old.setDefaultValue(contexts, intVal)), src,
                                t("Set default permission to %s for %s in %s context", intVal, src.fmt().hl(src.fmt().subject(ref)), formatContexts(src, contexts)));
                    }
                })
                .build();
    }
}
