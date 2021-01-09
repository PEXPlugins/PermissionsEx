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
package ca.stellardrift.permissionsex.minecraft.command.definition;

import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.minecraft.command.CommandRegistrationContext;
import ca.stellardrift.permissionsex.minecraft.command.Commander;
import ca.stellardrift.permissionsex.minecraft.command.Elements;
import ca.stellardrift.permissionsex.minecraft.command.Formats;
import ca.stellardrift.permissionsex.minecraft.command.MessageFormatter;
import ca.stellardrift.permissionsex.minecraft.command.Permission;
import ca.stellardrift.permissionsex.minecraft.command.argument.Parsers;
import ca.stellardrift.permissionsex.subject.SubjectRef;
import ca.stellardrift.permissionsex.subject.SubjectType;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.CommandArgument;
import io.leangen.geantyref.TypeToken;

import java.util.Set;

final class ParentSubcommand {

    private ParentSubcommand() {
    }

    static void register(final CommandRegistrationContext ctx, final SubjectRefProvider provider) {
        ctx.register(build -> add(build, provider), "add", "a", "+");
        ctx.register(build -> remove(build, provider), "remove", "rem", "delete", "del", "-");
        ctx.register(build -> set(build, provider), "set", "replace", "=");
    }

    static Command.Builder<Commander> add(final Command.Builder<Commander> build, final SubjectRefProvider subjectProvider) {
        final Permission permission = Permission.pex("parent.add");
        final CommandArgument<Commander, SubjectType<?>> parentType = parentTypeArgument();
        final CommandArgument<Commander, ?> parentIdentifier = parentIdentifierArgument(parentType);
        final SubjectRefProvider parentProvider = SubjectRefProvider.of(parentType, parentIdentifier);
        return build
            .argument(parentType)
            .argument(parentIdentifier)
            .permission(permission)
            .handler(ctx -> {
                final SubjectRef.ToData<?> subject = subjectProvider.provideData(ctx, permission);
                final Set<ContextValue<?>> contexts = Elements.contexts(ctx);
                final SubjectRef<?> parent = parentProvider.provide(ctx);
                final MessageFormatter fmt = ctx.getSender().formatter();

                subject.update(contexts, segment -> segment.plusParent(parent))
                    .whenComplete(Elements.messageSender(ctx.getSender(), Messages.PARENT_ADD_SUCCESS.tr(
                        fmt.subject(parent),
                        fmt.subject(subject).style(fmt::hl),
                        Formats.contexts(contexts)
                    )));
            });
    }

    static Command.Builder<Commander> remove(final Command.Builder<Commander> build, final SubjectRefProvider subjectProvider) {
        final Permission permission = Permission.pex("parent.remove");
        final CommandArgument<Commander, SubjectType<?>> parentType = parentTypeArgument();
        final CommandArgument<Commander, ?> parentIdentifier = parentIdentifierArgument(parentType);
        final SubjectRefProvider parentProvider = SubjectRefProvider.of(parentType, parentIdentifier);

        return build
            .argument(parentType)
            .argument(parentIdentifier)
            .permission(permission)
            .handler(ctx -> {
                final SubjectRef.ToData<?> subject = subjectProvider.provideData(ctx, permission);
                final Set<ContextValue<?>> contexts = Elements.contexts(ctx);
                final SubjectRef<?> parent = parentProvider.provide(ctx);
                final MessageFormatter fmt = ctx.getSender().formatter();

                subject.update(contexts, segment -> segment.minusParent(parent))
                    .whenComplete(Elements.messageSender(ctx.getSender(), Messages.PARENT_REMOVE_SUCCESS.tr(
                        fmt.subject(parent),
                        fmt.subject(subject).style(fmt::hl),
                        Formats.contexts(contexts)
                    )));
            });
    }

    static Command.Builder<Commander> set(final Command.Builder<Commander> build, final SubjectRefProvider subjectProvider) {
        final Permission permission = Permission.pex("parent.set");
        final CommandArgument<Commander, SubjectType<?>> parentType = parentTypeArgument();
        final CommandArgument<Commander, ?> parentIdentifier = parentIdentifierArgument(parentType);
        final SubjectRefProvider parentProvider = SubjectRefProvider.of(parentType, parentIdentifier);

        return build
            .argument(parentType)
            .argument(parentIdentifier)
            .permission(permission)
            .handler(ctx -> {
                final SubjectRef.ToData<?> subject = subjectProvider.provideData(ctx, permission);
                final Set<ContextValue<?>> contexts = Elements.contexts(ctx);
                final SubjectRef<?> parent = parentProvider.provide(ctx);
                final MessageFormatter fmt = ctx.getSender().formatter();

                subject.update(contexts, segment -> segment.withoutParents().plusParent(parent))
                    .whenComplete(Elements.messageSender(ctx.getSender(), Messages.PARENT_SET_SUCCESS.tr(
                        fmt.subject(parent),
                        fmt.subject(subject).style(fmt::hl),
                        Formats.contexts(contexts)
                    )));
            });
    }

    private static CommandArgument<Commander, SubjectType<?>> parentTypeArgument() {
        return CommandArgument.<Commander, SubjectType<?>>ofType(new TypeToken<SubjectType<?>>() {}, "parent type")
            .withParser(Parsers.subjectType())
            .build();
    }

    private static CommandArgument<Commander, ?> parentIdentifierArgument(final CommandArgument<Commander, SubjectType<?>> parentType) {
        return CommandArgument.<Commander, Object>ofType(Object.class, "parent identifier")
            .withParser(Parsers.subjectIdentifier(data -> data.get(parentType)))
            .build();
    }

}
