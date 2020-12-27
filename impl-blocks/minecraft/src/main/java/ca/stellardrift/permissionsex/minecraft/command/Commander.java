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
package ca.stellardrift.permissionsex.minecraft.command;

import ca.stellardrift.permissionsex.subject.SubjectRef;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identified;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.BuildableComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

import static net.kyori.adventure.text.Component.text;

/**
 * An actor that can perform commands and receive messages.
 */
public interface Commander extends ForwardingAudience.Single {

    /**
     * The display name for the actor, to be used in any potential action logging.
     *
     * @return the name
     */
    Component name();

    /**
     * A reference to the subject used
     * @return a subject identifier
     */
    @Nullable SubjectRef<?> subjectIdentifier();

    /**
     * A formatter providing formatting options for messages sent to this commander.
     *
     * @return the formatter
     */
    MessageFormatter formatter();

    /**
     * Get whether this commander has a certain permission.
     *
     * <p>This result might not match checking the subject identified by {@link #subjectIdentifier()}
     * due to additional context that may be present in command execution.</p>
     *
     * @param permission the permission to test
     * @return whether this subject has a certain permission
     */
    boolean hasPermission(final String permission);

    default boolean hasPermission(final Permission permission) {
        return this.hasPermission(permission.value());
    }

    /**
     * Check a permission specialized for a certain subject type.
     *
     * @param basePermission the base permission to use
     * @param subject the specific subject to validate
     * @throws CommandException thrown if the permission check fails
     */
    default void checkSubjectPermission(final String basePermission, final SubjectRef<?> subject) throws CommandException {
        if (!hasPermission(basePermission + '.' + subject.type().name() + '.' + subject.serializedIdentifier()) // permission to act on others
                && (!Objects.equals(subject, this.subjectIdentifier()) || !hasPermission(basePermission + ".own"))) { // specialized permission when acting on self
            throw new CommandException(Messages.EXECUTOR_ERROR_NO_PERMISSION.tr());
        }
    }


    /**
     * {@inheritDoc}
     *
     * <p>The message should be colored the appropriate output colour if it does not yet have a colour</p>
     */
    @Override
    default void sendMessage(@NonNull Identified source, @NonNull Component message, @NonNull MessageType type) {
        this.audience().sendMessage(source, message.colorIfAbsent(this.formatter().responseColor()), type);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The message should be colored the appropriate output colour if it does not yet have a colour</p>
     */
    @Override
    default void sendMessage(@NonNull Identity source, @NonNull Component message, @NonNull MessageType type) {
        this.audience().sendMessage(source, message.colorIfAbsent(this.formatter().responseColor()), type);
    }

    /**
     * Send debug text.
     *
     * @param text the text that will be sent
     */
    default void debug(final ComponentLike text) {
        sendMessage(text.asComponent().colorIfAbsent(NamedTextColor.GRAY));
    }

    default void error(final ComponentLike text) {
        this.error(text, null);
    }

    default void error(final ComponentLike text, final @Nullable Throwable error) {
        if (error != null && hasPermission("permissionsex.show-stacktrace-on-hover")) {
            // We can do a hover stacktrace
            final TextComponent.Builder base = text().content("The error that occurred was:");
            for (final StackTraceElement line : error.getStackTrace()) {
                base.append(Component.newline())
                        .append(text(line.toString().replace("\t", "    ")));
            }
             this.sendMessage(Component.text().append(text).color(NamedTextColor.RED).hoverEvent(base.build()));
        } else {
           this.sendMessage(text.asComponent().colorIfAbsent(NamedTextColor.RED));
        }
    }

    default void sendPaginated(
            final Component title,
            final Iterable<? extends ComponentLike> lines
    ) {
        this.sendPaginated(title, null, lines);
    }

    default void sendPaginated(
            final Component title,
            final @Nullable Component header,
            final Iterable<? extends ComponentLike> lines
    ) {
        final Component marker = Component.text("#");
        this.sendMessage(Component.join(Component.space(), Arrays.asList(marker, title, marker)));
        if (header != null) {
            this.sendMessage(header);
        }
        lines.forEach(this::sendMessage);
        this.sendMessage(Component.text("#############################"));
    }

    /**
     * Adds a click event to the provided component builder
     *
     * @param callback The function to call
     * @return The updated text
     */
    default  <C extends BuildableComponent<C, B>, B extends ComponentBuilder<C, B>> B callback(final B builder, final Consumer<Commander> callback) {
        final String command = this.formatter().manager().callbackController().registerCallback(this, callback);
        return builder.decoration(TextDecoration.UNDERLINED, true)
                .color(this.formatter().highlightColor())
                .clickEvent(ClickEvent.runCommand(this.formatter().transformCommand(command)));
    }

}
