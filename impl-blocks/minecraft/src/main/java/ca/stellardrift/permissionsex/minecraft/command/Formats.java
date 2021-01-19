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

import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.impl.util.PCollections;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.StaticArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.util.ComponentMessageThrowable;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.kyori.adventure.text.Component.text;

/**
 * Message creators for use in command output.
 */
public final class Formats {
    public static final Component COMMA = text(",");
    private static final Component BOOL_TRUE = Messages.FORMATTER_BOOLEAN_TRUE.bTr()
        .color(NamedTextColor.GREEN)
        .build();

    private static final Component BOOL_FALSE = Messages.FORMATTER_BOOLEAN_FALSE.bTr()
        .color(NamedTextColor.RED)
        .build();

    private Formats() {
    }

    /**
     * Represent a permission and its value in a user-visible way.
     *
     * The output will appear as {@code <permisison>=<value>}.
     *
     * @param permission the permission
     * @param value its value, listed and used for colouring
     * @return the formatted permission
     * @since 2.0.0
     */
    public static Component permission(final String permission, final int value) {
        final NamedTextColor color;
        if (value > 0) {
            color = NamedTextColor.GREEN;
        } else if (value < 0) {
            color = NamedTextColor.RED;
        } else {
            color = NamedTextColor.GRAY;
        }
        return text()
                .append(text(permission, color))
                .append(MessageFormatter.EQUALS_SIGN)
                .append(text(value))
                .build();
    }

    public static Component permissionValue(final int value) {
        final NamedTextColor color;
        if (value > 0) {
            color = NamedTextColor.GREEN;
        } else if (value < 0) {
            color = NamedTextColor.RED;
        } else {
            color = NamedTextColor.GRAY;
        }
        return text(value, color);
    }

    public static Component option(final String option, final String value) {
        return text()
                .content(option)
                .append(MessageFormatter.EQUALS_SIGN)
                .append(text(value))
                .build();
    }

    public static Component bool(final boolean value) {
        return value ? BOOL_TRUE : BOOL_FALSE;
    }

    public static Component contexts(final Set<ContextValue<?>> contexts) {
        if (contexts.isEmpty()) {
            return Messages.COMMON_ARGS_CONTEXT_GLOBAL.tr();
        } else {
            return Component.join(COMMA, contexts);
        }
    }

    /**
     * Format the specified command, filling in arguments as provided until the first unset
     * non-static argument is encountered.
     *
     * @param command the command to format
     * @param placeholders arguments to fill in
     * @return the formatted command
     */
    public static String formatCommand(final Command<?> command, final Map<CommandArgument<?, ?>, String> placeholders) {
        return formatCommand(command.getArguments(), placeholders);
    }

    /**
     * Format the specified command, filling in arguments as provided until the first unset
     * non-static argument is encountered.
     *
     * @param command the command to format
     * @param placeholders arguments to fill in
     * @return the formatted command
     */
    public static String formatCommand(final Command.Builder<?> command, final Map<CommandArgument<?, ?>, String> placeholders) {
        return formatCommand(command.build().getArguments(), placeholders); // TODO: can we do this without building the command?
    }

    /**
     * Format the specified command, filling in arguments as provided until the first unset
     * non-static argument is encountered.
     *
     * @param arguments the base arguments
     * @param placeholders arguments to fill in
     * @return the formatted command
     */
    private static String formatCommand(final List<? extends CommandArgument<?, ?>> arguments, final Map<CommandArgument<?, ?>, String> placeholders) {
        final StringBuilder builder = new StringBuilder("/");
        for (final CommandArgument<?, ?> argument : arguments) {
            if (argument instanceof StaticArgument<?>) {
                builder.append(argument.getName());
            } else {
                final @Nullable String value = placeholders.get(argument);
                if (value == null) {
                    break;
                }
                builder.append(value);
            }
            builder.append(" ");
        }
        return builder.toString();
    }

    public static String formatCommand(
            final Command<?> command,
            final CommandArgument<?, ?> arg1, final String val1
    ) {
        return formatCommand(command, PCollections.map(arg1, val1));
    }

    public static String formatCommand(
            final Command<?> command,
            final CommandArgument<?, ?> arg1, final String val1,
            final CommandArgument<?, ?> arg2, final String val2
    ) {
        return formatCommand(command, PCollections.<CommandArgument<?, ?>, String>map(arg1, val1).plus(arg2, val2));
    }

    public static Component message(final Throwable throwable) {
        if (throwable instanceof ComponentMessageThrowable) {
            final @Nullable Component msg =  ((ComponentMessageThrowable) throwable).componentMessage();
            return msg == null ? text("null") : msg;
        } else {
            return text(throwable.getMessage());
        }
    }

    public static TextColor lerp(final float pct, final TextColor from, final TextColor to) {
        // https://github.com/KyoriPowered/adventure/pull/215
        return TextColor.color(
            Math.round(from.red() + pct * (to.red() - from.red())),
            Math.round(from.green() + pct * (to.green() - from.green())),
            Math.round(from.blue() + pct * (to.blue() - from.blue()))
        );
    }

}
