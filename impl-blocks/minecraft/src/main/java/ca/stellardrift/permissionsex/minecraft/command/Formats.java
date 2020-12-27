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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.reactive.Publisher;
import org.spongepowered.configurate.reactive.Subscriber;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static net.kyori.adventure.text.Component.text;

/**
 * Message creators for use in command output.
 */
public final class Formats {

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

    public static Component option(final String option, final String value) {
        return text()
                .content(option)
                .append(MessageFormatter.EQUALS_SIGN)
                .append(text(value))
                .build();
    }

    /**
     * To be used with {@link CompletableFuture#handle(BiFunction)}
     */
    public static <V, U> BiFunction<V, Throwable, U> messageSender(
            final Commander src,
            final Component message
    ) {
        return messageSender(src, send -> send.accept(message));
    }

    /**
     * Message a subject with the result of a completable future.
     *
     * <p>Intended to be used with {@link CompletableFuture#handle(BiFunction)}</p>
     *
     * @param src the commander to receive the response
     * @param callback the callback to execute on completion
     * @param <V> upstream input value
     * @param <U> downstream output value. will always receive null
     * @return a handler function for a future
     */
    public static <V, U> BiFunction<@Nullable V, @Nullable Throwable, U> messageSender(
            final Commander src,
            final Consumer<Consumer<Component>> callback
    ) {

        return (result, err) -> {
            if (err != null) {
                final Throwable cause = err.getCause();
                if (err instanceof CompletionException && cause != null) {
                    err = cause;
                }
                if (err instanceof CommandException) {
                    src.error(((CommandException) err).getComponent());
                } else {
                    // TODO: err.getMessage() can be null
                    src.error(Messages.EXECUTOR_ERROR_ASYNC_TASK.tr(err.getClass().getSimpleName(), err.getMessage()), err);
                    src.formatter().manager().engine().logger().error(Messages.EXECUTOR_ERROR_ASYNC_TASK_CONSOLE.tr(src.name()), err);
                }
            } else {
                callback.accept(src::sendMessage);
            }
            return null;
        };
    }

    public static <V> CompletableFuture<V> toCompletableFuture(final Publisher<V> publisher) {
        final CompletableFuture<V> ret = new CompletableFuture<>();
        publisher.subscribe(new Subscriber<V>() {
            @Override
            public void submit(final V item) {
                ret.complete(item);
            }

            @Override
            public void onError(final Throwable ex) {
                ret.completeExceptionally(ex);
            }
        });
        return ret;
    }
}
