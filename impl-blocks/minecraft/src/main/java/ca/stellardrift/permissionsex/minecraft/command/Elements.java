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

import ca.stellardrift.permissionsex.PermissionsEngine;
import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.impl.util.PCollections;
import ca.stellardrift.permissionsex.minecraft.command.argument.Parsers;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.flags.CommandFlag;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.execution.CommandExecutionHandler;
import cloud.commandframework.minecraft.extras.RichDescription;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.reactive.Publisher;
import org.spongepowered.configurate.reactive.Subscriber;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Assorted pieces of command handling, used across commands.
 */
public final class Elements {

    public static final CommandFlag<Void> FLAG_TRANSIENT = CommandFlag.newBuilder("transient")
        .withAliases("t")
        .withDescription(RichDescription.of(Messages.COMMON_TRANSIENT_DESCRIPTION))
        .build();

    public static CommandFlag<ContextValue<?>> FLAG_CONTEXT = CommandFlag.newBuilder("contexts")
        .withAliases("c")
        .withArgument(CommandArgument.<Commander, ContextValue<?>>ofType(new TypeToken<ContextValue<?>>() {}, "value")
            .withParser(Parsers.contextValue()))
        .withDescription(RichDescription.of(Messages.COMMON_CONTEXT_DESCRIPTION))
        .build();

    public static Set<ContextValue<?>> contexts(final CommandContext<?> context) {
        final @Nullable ContextValue<?> single = context.flags().get(FLAG_CONTEXT.getName());
        if (single == null) {
            return PCollections.set();
        } else {
            return PCollections.set(single);
        }
    }

    /**
     * To be used with {@link CompletableFuture#whenComplete(BiConsumer)}
     */
    public static <V> BiConsumer<V, Throwable> messageSender(
            final Commander src,
            final Component message
    ) {
        return messageSender(src, send -> send.accept(message));
    }

    /**
     * Message a subject with the result of a completable future.
     *
     * <p>intended to be used with {@link CompletableFuture#whenComplete(BiConsumer)}</p>
     *
     * @param src the commander to receive the response
     * @param callback the callback to execute on completion
     * @param <V> upstream input value
     * @return a handler function for a future
     */
    public static <V> BiConsumer<@Nullable V, @Nullable Throwable> messageSender(
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
                    src.error(((CommandException) err).componentMessage());
                } else {
                    // TODO: err.getMessage() can be null
                    src.error(Messages.EXECUTOR_ERROR_ASYNC_TASK.tr(err.getClass().getSimpleName(), err.getMessage()), err);
                    src.formatter().manager().engine().logger().error(Messages.EXECUTOR_ERROR_ASYNC_TASK_CONSOLE.tr(src.name()), err);
                }
            } else {
                callback.accept(src::sendMessage);
            }
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

    @FunctionalInterface
    public interface PexExecutor<C> {
        void execute(C sender, PermissionsEngine engine, CommandContext<C> ctx);
    }

    public static <C> CommandExecutionHandler<C> handler(final PexExecutor<C> executor) {
        return ctx -> executor.execute(ctx.getSender(), ctx.get(PEXCommandPreprocessor.PEX_ENGINE), ctx);
    }

    public static <C> BiFunction<CommandContext<C>, String, List<String>> engineCompletions(final BiFunction<PermissionsEngine, String, List<String>> completer) {
        return (ctx, input) -> {
            final PermissionsEngine engine = ctx.get(PEXCommandPreprocessor.PEX_ENGINE);
            return completer.apply(engine, input);
        };
    }

    private Elements() {
    }

}
