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

import ca.stellardrift.permissionsex.context.ContextDefinitionProvider;
import ca.stellardrift.permissionsex.minecraft.MinecraftPermissionsEx;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import ca.stellardrift.permissionsex.subject.SubjectRef;
import ca.stellardrift.permissionsex.subject.SubjectTypeCollection;
import net.kyori.adventure.text.BuildableComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.concurrent.ExecutionException;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;

/**
 * Message formatting parameters for the current engine instance.
 */
public abstract class MessageFormatter {
    public static final TextColor DEFAULT_MESSAGE_COLOR = NamedTextColor.DARK_AQUA;
    public static final TextColor DEFAULT_HIGHLIGHT_COLOR = NamedTextColor.AQUA;
    public static final Component EQUALS_SIGN = text("=", NamedTextColor.GRAY);
    public static final Component SLASH = text("/");

    private final MinecraftPermissionsEx<?> manager;
    private final TextColor highlightColor;
    private final TextColor messageColor;

    /**
     * Create a new formatter with default message and highlight colours.
     *
     * @param manager the permissions manager
     */
    protected MessageFormatter(final MinecraftPermissionsEx<?> manager) {
        this(manager, DEFAULT_MESSAGE_COLOR, DEFAULT_HIGHLIGHT_COLOR);
    }

    protected MessageFormatter(
            final MinecraftPermissionsEx<?> manager,
            final TextColor messageColor,
            final TextColor highlightColor) {
        this.manager = manager;
        this.messageColor = messageColor;
        this.highlightColor = highlightColor;
    }

    final MinecraftPermissionsEx<?> manager() {
        return this.manager;
    }

    /**
     * Get the colour to be used for standard command responses.
     *
     * @return the response color
     */
    public final TextColor responseColor() {
        return this.messageColor;
    }

    /**
     * The color to be used for highlighted parts of messages.
     *
     * @return the highlight color
     */
    public final TextColor highlightColor() {
        return this.highlightColor;
    }

    /**
     * Given a command in standard format, correct it to refer to specifically the proxy format.
     *
     * @param cmd the original command
     * @return the transformed command
     */
    protected String transformCommand(final String cmd) {
        return cmd;
    }

    protected <I> @Nullable String friendlyName(final SubjectRef<I> reference) {
        return null;
    }

    public final Component subject(final CalculatedSubject subject) {
        return this.subject(subject.identifier());
    }

    /**
     * Print the subject in a user-friendly manner. May link to the subject info printout
     *
     * @param subject The subject to show
     * @return the formatted value
     */
    public <I> Component subject(final SubjectRef<I> subject) {
        final SubjectTypeCollection<I> type = this.manager.engine().subjects(subject.type());
        final String serializedIdent = subject.type().serializeIdentifier(subject.identifier());
        @Nullable String name = this.friendlyName(subject);
        if (name == null) {
            try {
                name = type.persistentData().data(subject.identifier(), null).get()
                        .segment(ContextDefinitionProvider.GLOBAL_CONTEXT).options().get("name");
            } catch (final ExecutionException | InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }

        final Component nameText;
        if (name != null) {
            nameText = text()
                    .append(text(serializedIdent, NamedTextColor.GRAY))
                    .append(SLASH)
                    .append(text(name))
                    .build();
        } else {
            nameText = text(serializedIdent);
        }

        return text()
                .append(text(subject.type().name()).decorate(TextDecoration.BOLD))
                .append(space())
                .append(nameText)
                .hoverEvent(HoverEvent.showText(Messages.FORMATTER_BUTTON_INFO_PROMPT.tr()))
                .clickEvent(ClickEvent.runCommand(transformCommand("/pex " + subject.type().name() + ' ' + serializedIdent + " info")))
                .build();
    }

    /**
     * Create a clickable button that will execute a command or suggest a command to be executed
     *
     * @param type    The style of button to present
     * @param tooltip A tooltip to optionally show when hovering over a button
     * @param command The command to execute
     * @param execute Whether the command provided will be executed or only added to the user's input
     * @return the formatted text
     */
    public <C extends BuildableComponent<C, B>, B extends ComponentBuilder<C, B>> Component button(
            B builder,
            ButtonType type,
            final @Nullable Component tooltip,
            final String command,
            final boolean execute
    ) {
        final TextColor buttonColor;
        switch (type) {
            case POSITIVE:
                buttonColor = NamedTextColor.GREEN;
                break;
            case NEGATIVE:
                buttonColor = NamedTextColor.RED;
                break;
            default:
                buttonColor = this.highlightColor;
        }
        builder.color(buttonColor);

        if (tooltip != null) {
            builder.hoverEvent(HoverEvent.showText(tooltip));
        }

        if (execute) {
            builder.clickEvent(ClickEvent.runCommand(transformCommand(command)));
        } else {
            builder.clickEvent(ClickEvent.suggestCommand(transformCommand(command)));
        }
        return builder.build();
    }


    public final <C extends BuildableComponent<C, B>, B extends ComponentBuilder<C, B>> B header(final B builder) {
        return builder.decoration(TextDecoration.BOLD, true);
    }

    public final <C extends BuildableComponent<C, B>, B extends ComponentBuilder<C, B>> B hl(final B builder) {
        return builder.color(this.highlightColor);
    }

    public final Style.Builder header(final Style.Builder builder) {
        return builder.decoration(TextDecoration.BOLD, true);
    }

    public final Style.Builder hl(final Style.Builder builder) {
        return builder.color(this.highlightColor);
    }
}
