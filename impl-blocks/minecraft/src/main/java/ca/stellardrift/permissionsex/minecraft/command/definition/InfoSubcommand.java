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

import ca.stellardrift.permissionsex.PermissionsEngine;
import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.minecraft.command.Commander;
import ca.stellardrift.permissionsex.minecraft.command.Formats;
import ca.stellardrift.permissionsex.minecraft.command.Permission;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import ca.stellardrift.permissionsex.subject.ImmutableSubjectData;
import ca.stellardrift.permissionsex.subject.Segment;
import ca.stellardrift.permissionsex.subject.SubjectRef;
import ca.stellardrift.permissionsex.util.TranslatableProvider;
import cloud.commandframework.Command;
import cloud.commandframework.meta.CommandMeta;
import net.kyori.adventure.text.BuildableComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static ca.stellardrift.permissionsex.minecraft.command.Elements.handler;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;

final class InfoSubcommand {

    private InfoSubcommand() {
    }

    static Command.Builder<Commander> register(final Command.Builder<Commander> builder,
                                               final SubjectRefProvider subjectProvider) {
        final Permission perm = Permission.pex("info");
        return builder
            .permission(perm)
            .meta(CommandMeta.DESCRIPTION, Messages.INFO_DESCRIPTION.key()) // TODO
            .handler(handler((source, engine, ctx) -> {
                final CalculatedSubject subject = subjectProvider.provideCalculated(ctx, perm);

                new Printer(source, engine, subject).printInfo();
            }));
    }

    static class Printer {
        private static final Component COLON = text(":");
        private static final Component INDENT = text("  ");
        private static final Component DOUBLE_INDENT = text("    ");

        private final Commander source;
        private final PermissionsEngine engine;
        private final CalculatedSubject subject;

        Printer(final Commander source, final PermissionsEngine engine, final CalculatedSubject subject) {
            this.source = source;
            this.engine = engine;
            this.subject = subject;
        }

        void printInfo() {
            final SubjectRef.ToData<?> transientData = subject.transientData();
            final SubjectRef.ToData<?> persistentData = subject.data();
            echo(this.source.formatter().header(Messages.INFO_HEADER.bTr(this.source.formatter().subject(subject))));
            if (this.engine.debugMode()) {
                final @Nullable Object associatedObject = subject.associatedObject();
                if (associatedObject != null) {
                    echo(hl(Messages.INFO_ASSOCIATED_OBJECT.bTr()).append(text(associatedObject.toString())));
                }
            }
            echo(hlKeyValue(Messages.INFO_ACTIVE_CONTEXTS.bTr(), text(subject.activeContexts().toString())));
            echo(hlKeyValue(Messages.INFO_ACTIVE_USED_CONTEXTS.bTr(), text(subject.usedContextValues().join().toString())));

            printPermissions(Messages.INFO_HEADER_PERMISSIONS, persistentData);
            printPermissions(Messages.INFO_HEADER_PERMISSIONS_TRANSIENT, transientData);

            printOptions(Messages.INFO_HEADER_OPTIONS, persistentData);
            printOptions(Messages.INFO_HEADER_OPTIONS_TRANSIENT, transientData);

            printParents(Messages.INFO_HEADER_PARENTS, persistentData);
            printParents(Messages.INFO_HEADER_PARENTS_TRANSIENT, transientData);

        }

        private void echoHead(final TranslatableProvider message) {
            this.source.sendMessage(this.source.formatter().hl(message.bTr()));
        }

        private <C extends BuildableComponent<C, B>, B extends ComponentBuilder<C, B>> B hl(final B builder) {
            return this.source.formatter().hl(builder);
        }

        private <C extends BuildableComponent<C, B>, B extends ComponentBuilder<C, B>> Component hlKeyValue(final B key, final Component value) {
            return TextComponent.ofChildren(this.source.formatter().hl(key), space(), value);
        }

        private void echo(final ComponentLike message) {
            this.source.sendMessage(message);
        }

        private void echo(final ComponentLike... message) {
            this.source.sendMessage(TextComponent.ofChildren(message));
        }

        private void printForSegments(final TranslatableProvider header, final SubjectRef.ToData<?> ref, final Predicate<Segment> filter, final Consumer<Segment> printer) {
            boolean first = true;
            final ImmutableSubjectData data = ref.get();
            for (final Map.Entry<? extends Set<ContextValue<?>>, Segment> entry : data.segments().entrySet()) {
                final Segment segment = entry.getValue();
                if (filter.test(segment)) {
                    if (first) {
                        this.echoHead(header);
                    }
                    first = false;
                    this.echo(INDENT, Formats.contexts(entry.getKey()), COLON);
                    printer.accept(segment);
                }
            }
        }

        private void printPermissions(final TranslatableProvider header, final SubjectRef.ToData<?> ref) {
            printForSegments(header, ref, seg -> !seg.permissions().isEmpty() || seg.fallbackPermission() != 0, segment -> {
                this.echo(DOUBLE_INDENT, hl(Messages.INFO_PERMISSIONS_DEFAULT.bTr(segment.fallbackPermission())));
                for (final Map.Entry<String, Integer> option : segment.permissions().entrySet()) {
                    this.echo(DOUBLE_INDENT, Formats.permission(option.getKey(), option.getValue()));
                }
            });
        }

        private void printOptions(final TranslatableProvider header, final SubjectRef.ToData<?> ref) {
            printForSegments(header, ref, seg -> !seg.options().isEmpty(), segment -> {
                for (final Map.Entry<String, String> option : segment.options().entrySet()) {
                    this.echo(DOUBLE_INDENT, Formats.option(option.getKey(), option.getValue()));
                }
            });
        }

        private void printParents(final TranslatableProvider header, final SubjectRef.ToData<?> ref) {
            printForSegments(header, ref, seg -> !seg.parents().isEmpty(), segment -> {
                for (final SubjectRef<?> parent : segment.parents()) {
                    this.echo(DOUBLE_INDENT, source.formatter().subject(parent));
                }
            });
        }
    }

}
