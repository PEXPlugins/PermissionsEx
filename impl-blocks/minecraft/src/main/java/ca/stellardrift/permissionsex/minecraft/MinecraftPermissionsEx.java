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
package ca.stellardrift.permissionsex.minecraft;

import ca.stellardrift.permissionsex.PermissionsEngine;
import ca.stellardrift.permissionsex.PermissionsEngineBuilder;
import ca.stellardrift.permissionsex.datastore.DataStoreFactory;
import ca.stellardrift.permissionsex.impl.PermissionsEx;
import ca.stellardrift.permissionsex.exception.PermissionsLoadingException;
import ca.stellardrift.permissionsex.impl.util.PCollections;
import ca.stellardrift.permissionsex.minecraft.command.CallbackController;
import ca.stellardrift.permissionsex.minecraft.command.CommandException;
import ca.stellardrift.permissionsex.minecraft.command.CommandRegistrationContext;
import ca.stellardrift.permissionsex.minecraft.command.Commander;
import ca.stellardrift.permissionsex.minecraft.command.MessageFormatter;
import ca.stellardrift.permissionsex.minecraft.command.PEXCommandPreprocessor;
import ca.stellardrift.permissionsex.minecraft.command.definition.PermissionsExCommand;
import ca.stellardrift.permissionsex.minecraft.command.definition.RankingCommands;
import ca.stellardrift.permissionsex.minecraft.profile.ProfileApiResolver;
import ca.stellardrift.permissionsex.subject.InvalidIdentifierException;
import ca.stellardrift.permissionsex.subject.SubjectType;
import ca.stellardrift.permissionsex.subject.SubjectTypeCollection;
import cloud.commandframework.CommandManager;
import cloud.commandframework.CommandTree;
import cloud.commandframework.exceptions.CommandExecutionException;
import cloud.commandframework.execution.AsynchronousCommandExecutionCoordinator;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.minecraft.extras.MinecraftExceptionHandler;
import com.google.errorprone.annotations.DoNotCall;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.util.ComponentMessageThrowable;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.pcollections.PMap;
import org.slf4j.Logger;
import org.spongepowered.configurate.util.CheckedFunction;

import javax.sql.DataSource;
import java.io.Closeable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ca.stellardrift.permissionsex.impl.PermissionsEx.GLOBAL_CONTEXT;
import static ca.stellardrift.permissionsex.minecraft.command.Formats.message;
import static java.util.Objects.requireNonNull;
import static net.kyori.adventure.text.Component.text;

/**
 * An implementation of the Minecraft-specific parts of PermissionsEx
 *
 * @since 2.0.0
 */
public final class MinecraftPermissionsEx<T> implements Closeable {

    private static final String SUBJECTS_USER = "user";
    private static final String SUBJECTS_GROUP = "group";

    private final PermissionsEx<T> engine;
    private final SubjectType<UUID> users;
    private final SubjectType<String> groups;
    private final ProfileApiResolver resolver;
    private final CallbackController callbacks;
    private final @Nullable CommandManager<Commander> commands;
    private final String commandPrefix;
    private final @Nullable Consumer<CommandRegistrationContext> commandContributor;
    private final MessageFormatter formatter;
    private final Map<BaseDirectoryScope, Path> baseDirectories;

    /**
     * Create a new builder for a Minecraft permissions engine.
     *
     * @param <V>    platform configuration type
     * @return the builder
     */
    public static <V> Builder<V> builder() {
        return new Builder<>();
    }

    MinecraftPermissionsEx(final Builder<T> builder) throws PermissionsLoadingException {
        this.baseDirectories = builder.baseDirectories;
        this.engine = (PermissionsEx<T>) builder.buildEngine();
        this.resolver = ProfileApiResolver.resolver(this.engine.asyncExecutor());
        this.callbacks = new CallbackController();
        this.commands = builder.commandManagerMaker == null ? null : builder.commandManagerMaker.apply(
            AsynchronousCommandExecutionCoordinator.<Commander>newBuilder()
                .withAsynchronousParsing()
                .withExecutor(this.engine.asyncExecutor())
                .build()
        );
        this.commandPrefix = builder.commandPrefix;
        this.commandContributor = builder.commandContributor;
        final Predicate<UUID> opProvider = builder.opProvider;
        this.users = SubjectType.builder(SUBJECTS_USER, UUID.class)
            .serializedBy(UUID::toString)
            .deserializedBy(id -> {
                try {
                    return UUID.fromString(id);
                } catch (final IllegalArgumentException ex) {
                    throw new InvalidIdentifierException(id);
                }
            })
            .friendlyNameResolvedBy(builder.cachedUuidResolver)
            .undefinedValues(opProvider::test)
            .associatedObjects(builder.playerProvider)
            .build();

        // TODO: force group names to be lower-case?
        this.groups = SubjectType.stringIdentBuilder(SUBJECTS_GROUP).build();

        convertUuids();

        // Initialize subject types
        users();
        groups().cacheAll();

        this.formatter = builder.formatterProvider.apply(this);
        this.configureCommandManager();
    }

    /**
     * Get the engine backing this PermissionsEx instance.
     *
     * @return the backing engine
     * @since 2.0.0
     */
    public PermissionsEx<T> engine() {
        return this.engine;
    }

    /**
     * Get user subjects.
     *
     * <p>User subject identifiers are UUIDs.</p>
     *
     * @return the collection of user subjects
     * @since 2.0.0
     */
    public SubjectTypeCollection<UUID> users() {
        return this.engine.subjects(this.users);
    }

    /**
     * Get group subjects.
     *
     * <p>Group subject identifiers are any string.</p>
     *
     * @return the collection of group subjects
     * @since 2.0.0
     */
    public SubjectTypeCollection<String> groups() {
        return this.engine.subjects(this.groups);
    }

    /**
     * Get the command callback controller for this permissions instance
     *
     * @return The callback controller
     */
    public CallbackController callbackController() {
        return this.callbacks;
    }

    /**
     * Describe this PermissionsEx implementation.
     *
     * @param receiver the receiver for the messages
     * @param verbose  whether verbose information should be printed
     */
    public void describe(final Audience receiver, final boolean verbose) {
        receiver.sendMessage(text()
            .content("PermissionsEx v")
            .append(text(this.engine.version()))); // highlight? make message formatter a manager thing?

        receiver.sendMessage(Messages.DESCRIBE_RESPONSE_ACTIVE_DATA_STORE.tr(this.engine.config().getDefaultDataStore().identifier()));
        receiver.sendMessage(Messages.DESCRIBE_RESPONSE_AVAILABLE_DATA_STORES.tr(DataStoreFactory.all().keySet().toString()));
        receiver.sendMessage(Component.empty());
        if (verbose) {
            receiver.sendMessage(this.formatter.header(Messages.DESCRIBE_BASEDIRS_HEADER.bTr()).build());
            receiver.sendMessage(Messages.DESCRIBE_BASEDIRS_CONFIG.tr(this.baseDirectory(BaseDirectoryScope.CONFIG)));
            receiver.sendMessage(Messages.DESCRIBE_BASEDIRS_JAR.tr(this.baseDirectory(BaseDirectoryScope.JAR)));
            receiver.sendMessage(Messages.DESCRIBE_BASEDIRS_SERVER.tr(this.baseDirectory(BaseDirectoryScope.SERVER)));
            receiver.sendMessage(Messages.DESCRIBE_BASEDIRS_WORLDS.tr(this.baseDirectory(BaseDirectoryScope.WORLDS)));
        }
    }

    /**
     * Get a game-specific base directory for a certain socpe.
     *
     * @param scope the scope
     * @return a base directory
     * @since 2.0.0
     */
    public Path baseDirectory(final BaseDirectoryScope scope) {
        final @Nullable Path baseDir = this.baseDirectories.get(scope);
        return baseDir == null ? this.engine.baseDirectory() : baseDir;
    }

    /**
     * Get a message formatter that can be used for styling user output.
     *
     * @return the formatter
     */
    public MessageFormatter messageFormatter() {
        return this.formatter;
    }

    private void configureCommandManager() {
        if (this.commands == null) {
            return;
        }

        // Configure error handling
        new MinecraftExceptionHandler<Commander>()
            .withDefaultHandlers()
            .withHandler(MinecraftExceptionHandler.ExceptionType.ARGUMENT_PARSING, e ->
                Component.text("Invalid command argument: ", NamedTextColor.RED)
                    .append(message(e.getCause()).colorIfAbsent(NamedTextColor.GRAY)))
            .withDecorator(component -> component.colorIfAbsent(NamedTextColor.RED))
            .apply(this.commands, cmd -> cmd);

        this.commands.registerExceptionHandler(CommandException.class, (sender, err) -> {
            final @Nullable Component message = err.componentMessage();
            sender.error(message == null ? text("An unknown error occurred in this command") : message, err.getCause());
        });
        this.commands.registerExceptionHandler(CommandExecutionException.class, (sender, err) -> {
            final @Nullable Throwable cause = err.getCause();
            if (cause instanceof CommandException) {
                sender.error(((CommandException) cause).componentMessage(), cause.getCause());
            } else if (cause != null) {
                sender.error(Messages.COMMAND_ERROR_UNKNOWN.tr(), cause);
                this.engine.logger().error(Messages.COMMAND_ERROR_UNKNOWN_CONSOLE.tr(
                    /* sender */ sender.name(),
                    /* command */ "not yet implemented", // coming cloud 1.4.0
                    /* message */ ComponentMessageThrowable.getOrConvertMessage(cause)
                ));
            }
        });

        this.commands.registerCommandPreProcessor(new PEXCommandPreprocessor(this));

        // Register custom argument parsers
        if (this.hasBrigadier()) {
            BrigadierRegistration.registerArgumentTypes(this.commands);
        }

        // And register commands
        final CommandRegistrationContext regCtx = new CommandRegistrationContext(this.commandPrefix, this, this.commands);
        regCtx.push(regCtx.absoluteBuilder("permissionsex", "pex")
            .meta(CommandMeta.DESCRIPTION, "The command for controlling PermissionsEx"), child -> {
            PermissionsExCommand.register(child);
            this.callbackController().registerCommand(child);
        });

        regCtx.register(RankingCommands::promote, "promote", "prom", "rankup");
        regCtx.register(RankingCommands::demote, "demote", "dem", "rankdown");

        if (this.commandContributor != null) {
            this.commandContributor.accept(regCtx);
        }
    }

    private boolean hasBrigadier() {
        try {
            Class.forName("cloud.commandframework.brigadier.BrigadierManagerHolder");
            Class.forName("com.mojang.brigadier.CommandDispatcher");
            return true;
        } catch (final ClassNotFoundException ex) {
            return false;
        }
    }

    private void convertUuids() {
        try {
            InetAddress.getByName("api.mojang.com");
        } catch (final UnknownHostException ex) {
            engine.logger().warn(Messages.UUIDCONVERSION_ERROR_DNS.tr());
        }

        // Low-level operation
        this.engine.doBulkOperation(store -> {
            final Set<String> toConvert = store.getAllIdentifiers(SUBJECTS_USER)
                .filter(ident -> {
                    if (ident.length() != 36) {
                        return true;
                    }
                    try {
                        UUID.fromString(ident);
                        return false;
                    } catch (IllegalArgumentException ex) {
                        return true;
                    }
                }).collect(Collectors.toSet());
            if (!toConvert.isEmpty()) {
                engine.logger().info(Messages.UUIDCONVERSION_BEGIN.tr());
            } else {
                return CompletableFuture.completedFuture(0L);
            }

            final AtomicInteger successCount = new AtomicInteger();
            final Stream<CompletableFuture<Void>> results = this.resolver.resolveByName(toConvert)
                .map(profile -> {
                    final String newIdentifier = profile.uuid().toString();
                    final String lookupName = profile.name();

                    // newRegistered <- registered(uuid)
                    final CompletableFuture<Boolean> newRegistered = store.isRegistered(SUBJECTS_USER, newIdentifier);
                    // oldRegistered <- registered(username || lowercaseUsername)
                    final CompletableFuture<Boolean> oldRegistered = store.isRegistered(SUBJECTS_USER, lookupName).thenCombine(
                        store.isRegistered(SUBJECTS_USER, lookupName.toLowerCase(Locale.ROOT)), (a, b) -> a || b
                    );

                    // shouldExecute <- !newRegistered && oldRegistered
                    final CompletableFuture<Boolean> shouldExecute = newRegistered.thenCombine(oldRegistered, (n, o) -> {
                        if (n) {
                            this.engine.logger().warn(Messages.UUIDCONVERSION_ERROR_DUPLICATE.tr(newIdentifier));
                            return false;
                        } else {
                            return o;
                        }
                    });

                    return shouldExecute.thenCompose(execute -> { // execute <- shouldExecute
                        if (!execute) {
                            return CompletableFuture.completedFuture(null);
                        }

                        // Actually move the data
                        return store.getData(SUBJECTS_USER, profile.name(), null)
                            .thenCompose(oldData -> store.setData(
                                SUBJECTS_USER,
                                newIdentifier,
                                oldData.withSegment(GLOBAL_CONTEXT, s -> s.withOption("name", profile.name()))
                            )
                                .thenAccept(result -> store.setData(SUBJECTS_USER, profile.name(), null)
                                    .whenComplete((value, err) -> {
                                        if (err != null) {
                                            err.printStackTrace();
                                        } else {
                                            successCount.getAndIncrement();
                                        }
                                    })));

                    });
                });

            @SuppressWarnings("unchecked")
            final CompletableFuture<Void>[] futureArray = results.toArray(CompletableFuture[]::new);
            return CompletableFuture.allOf(futureArray)
                .<Number>thenApply($ -> successCount.get());
        }).thenAccept(result -> {
            if (result != null && result.intValue() > 0) {
                engine.logger().info(Messages.UUIDCONVERSION_END.tr(result));
            }
        }).exceptionally(t -> {
            engine.logger().error(Messages.UUIDCONVERSION_ERROR_GENERAL.tr(), t);
            return null;
        });
    }

    @Override
    public void close() {
        this.engine.close();
    }

    /**
     * A builder for a Minecraft PermissionsEx engine.
     *
     * @since 2.0.0
     */
    public static final class Builder<C> implements PermissionsEngineBuilder {

        private final PermissionsEngineBuilder delegate;
        private Function<String, @Nullable UUID> cachedUuidResolver = $ -> null;
        private Predicate<UUID> opProvider = $ -> false;
        private Function<UUID, @Nullable ?> playerProvider = $ -> null;
        private @Nullable Function<Function<CommandTree<Commander>, CommandExecutionCoordinator<Commander>>, CommandManager<Commander>> commandManagerMaker;
        private String commandPrefix = "";
        private @Nullable Consumer<CommandRegistrationContext> commandContributor;
        private Function<MinecraftPermissionsEx<C>, MessageFormatter> formatterProvider = MessageFormatter::new;
        private PMap<BaseDirectoryScope, Path> baseDirectories = PCollections.map();

        Builder() {
            this.delegate = PermissionsEngine.builder();
        }

        /**
         * Provide a profile resolver to get player UUIDs from cache given a name.
         *
         * @param resolver the uuid resolver
         * @return this builder
         * @since 2.0.0
         */
        public Builder<C> cachedUuidResolver(final Function<String, @Nullable UUID> resolver) {
            this.cachedUuidResolver = requireNonNull(resolver, "uuid");
            return this;
        }

        /**
         * Set a predicate that will check whether a certain player UUID has op status.
         *
         * @param provider the op status provider
         * @return this builder
         * @since 2.0.0
         */
        public Builder<C> opProvider(final Predicate<UUID> provider) {
            this.opProvider = requireNonNull(provider, "provider");
            return this;
        }

        /**
         * Set a function that will look up players by UUID, to provide an associated object
         * for subjects.
         *
         * @param playerProvider the player provider
         * @return this builder
         * @since 2.0.0
         */
        public Builder<C> playerProvider(final Function<UUID, @Nullable ?> playerProvider) {
            this.playerProvider = requireNonNull(playerProvider, "playerProvider");
            return this;
        }

        /**
         * If commands should be registered, set the command manager to register with.
         *
         * @param manager the manager
         * @return this builder
         * @since 2.0.0
         */
        public Builder<C> commands(
            final Function<Function<CommandTree<Commander>, CommandExecutionCoordinator<Commander>>, CommandManager<Commander>> manager
        ) {
            this.commands(manager, "");
            return this;
        }

        /**
         * If commands should be registered, set the command manager to register with.
         *
         * @param manager the manager
         * @return this builder
         * @since 2.0.0
         */
        public Builder<C> commands(
            final Function<Function<CommandTree<Commander>, CommandExecutionCoordinator<Commander>>, CommandManager<Commander>> manager,
            final String commandPrefix
        ) {
            this.commandManagerMaker = requireNonNull(manager, "manager");
            this.commandPrefix = requireNonNull(commandPrefix, "commandPrefix");
            return this;
        }

        /**
         * Register a callback function that will contribute commands to be registered when PEX performs
         * command registration.
         *
         * @param contributor the contributor
         * @return this builder
         * @since 2.0.0
         */
        public Builder<C> commandContributor(final Consumer<CommandRegistrationContext> contributor) {
            this.commandContributor = requireNonNull(contributor, "contributor");
            return this;
        }

        /**
         * Set a message formatter to be used for instance-specific formatting.
         *
         * <p>This can be used to override the colour scheme.</p>
         *
         * @param formatterProvider a function that creates a message formatter for this provider
         * @return this builder
         * @since 2.0.0
         */
        public Builder<C> messageFormatter(final Function<MinecraftPermissionsEx<C>, MessageFormatter> formatterProvider) {
            this.formatterProvider = formatterProvider;
            return this;
        }

        @Override
        public Builder<C> configuration(final Path configFile) {
            this.delegate.configuration(configFile);
            return this;
        }

        @Override
        public Builder<C> baseDirectory(final Path baseDir) {
            this.delegate.baseDirectory(baseDir);
            return this.baseDirectory(BaseDirectoryScope.CONFIG, baseDir);
        }

        /**
         * Set the base directory in a certain game-specific scope.
         *
         * @param scope the scope to set a base directory in
         * @param baseDirectory the base directory
         * @return this builder
         * @since 2.0.0
         */
        public Builder<C> baseDirectory(final BaseDirectoryScope scope, final Path baseDirectory) {
            this.baseDirectories = this.baseDirectories.plus(scope, baseDirectory);
            return this;
        }

        @Override
        public Builder<C> logger(final Logger logger) {
            this.delegate.logger(logger);
            return this;
        }

        @Override
        public Builder<C> asyncExecutor(final Executor executor) {
            this.delegate.asyncExecutor(executor);
            return this;
        }

        @Override
        public Builder<C> databaseProvider(final CheckedFunction<String, @Nullable DataSource, SQLException> databaseProvider) {
            this.delegate.databaseProvider(databaseProvider);
            return this;
        }

        /**
         * This method should not be called directly.
         *
         * @return a new permissions engine
         * @throws PermissionsLoadingException when unable to load
         */
        @Override
        @Deprecated
        @DoNotCall
        public PermissionsEngine build() throws PermissionsLoadingException {
            throw new IllegalArgumentException("Call create() instead");
        }

        PermissionsEngine buildEngine() throws PermissionsLoadingException {
            return this.delegate.build();
        }

        /**
         * Build an engine.
         *
         * <p>The implementation interface must have been set.</p>
         *
         * @return a new instance
         * @throws PermissionsLoadingException if unable to load initial data
         * @since 2.0.0
         */
        public MinecraftPermissionsEx<C> create() throws PermissionsLoadingException {
            return new MinecraftPermissionsEx<>(this);
        }

    }

}
