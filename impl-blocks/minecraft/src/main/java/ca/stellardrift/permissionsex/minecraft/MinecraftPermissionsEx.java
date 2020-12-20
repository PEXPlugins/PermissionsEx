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

import ca.stellardrift.permissionsex.ImplementationInterface;
import ca.stellardrift.permissionsex.PermissionsEx;
import ca.stellardrift.permissionsex.config.PermissionsExConfiguration;
import ca.stellardrift.permissionsex.exception.PermissionsLoadingException;
import ca.stellardrift.permissionsex.minecraft.profile.ProfileApiResolver;
import ca.stellardrift.permissionsex.subject.InvalidIdentifierException;
import ca.stellardrift.permissionsex.subject.SubjectType;
import ca.stellardrift.permissionsex.subject.SubjectTypeCollection;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import reactor.core.publisher.Mono;

import java.io.Closeable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static ca.stellardrift.permissionsex.Messages.*;
import static ca.stellardrift.permissionsex.PermissionsEx.GLOBAL_CONTEXT;
import static java.util.Objects.requireNonNull;

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

    /**
     * Create a new builder for a Minecraft permissions engine.
     *
     * @param config the configuration for the engine
     * @param <V> platform configuration type
     * @return the builder
     */
    public static <V> Builder<V> builder(final PermissionsExConfiguration<V> config) {
        return new Builder<>(config);
    }

    MinecraftPermissionsEx(final Builder<T> builder) throws PermissionsLoadingException {
        this.engine = new PermissionsEx<>(builder.config, builder.implementation);
        this.resolver = ProfileApiResolver.resolver(this.engine.asyncExecutor());
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
    }

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

    private void convertUuids() {
        try {
            InetAddress.getByName("api.mojang.com");
            // Low-lever operation
            this.engine.doBulkOperation(store -> {
                Set<String> toConvert = store.getAllIdentifiers(SUBJECTS_USER).stream()
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
                    engine.logger().info(UUIDCONVERSION_BEGIN.toComponent());
                } else {
                    return CompletableFuture.completedFuture(0L);
                }

                return this.resolver.resolveByName(toConvert)
                        .filterWhen(profile -> {
                            final String newIdentifier = profile.uuid().toString();
                            final String lookupName = profile.name();
                            final Mono<Boolean> newRegistered = Mono.fromCompletionStage(store.isRegistered(SUBJECTS_USER, newIdentifier));
                            final Mono<Boolean> oldRegistered = Mono.zip(Mono.fromCompletionStage(store.isRegistered(SUBJECTS_USER, lookupName)),
                                    Mono.fromCompletionStage(store.isRegistered(SUBJECTS_USER, lookupName.toLowerCase(Locale.ROOT))), (a, b) -> a || b);
                            return Mono.zip(newRegistered, oldRegistered, (n, o) -> {
                                if (n) {
                                    this.engine.logger().warn(UUIDCONVERSION_ERROR_DUPLICATE.toComponent(newIdentifier));
                                    return false;
                                } else {
                                    return o;
                                }
                            });
                        }).flatMap(profile -> {
                            final String newIdentifier = profile.uuid().toString();
                            return Mono.fromCompletionStage(store.getData(SUBJECTS_USER, profile.name(), null)
                                    .thenCompose(oldData -> store.setData(SUBJECTS_USER, newIdentifier, oldData.setOption(GLOBAL_CONTEXT, "name", profile.name()))
                                            .thenAccept(result -> store.setData(SUBJECTS_USER, profile.name(), null)
                                                    .exceptionally(t -> {
                                                        t.printStackTrace();
                                                        return null;
                                                    }))));
                        }).count().toFuture();
            }).thenAccept(result -> {
                if (result != null && result > 0) {
                    engine.logger().info(UUIDCONVERSION_END.toComponent(result));
                }
            }).exceptionally(t -> {
                engine.logger().error(UUIDCONVERSION_ERROR_GENERAL.toComponent(), t);
                return null;
            });
        } catch (final UnknownHostException ex) {
            engine.logger().warn(UUIDCONVERSION_ERROR_DNS.toComponent());
        }
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
    public static final class Builder<C> {
        private final PermissionsExConfiguration<C> config;
        private @MonotonicNonNull ImplementationInterface implementation;
        private Function<String, @Nullable UUID> cachedUuidResolver = $ -> null;
        private Predicate<UUID> opProvider = $ -> false;
        private Function<UUID, @Nullable ?> playerProvider = $ -> null;

        Builder(final PermissionsExConfiguration<C> configInstance) {
            this.config = requireNonNull(configInstance, "config");
        }

        /**
         * Set the implementation interface to be used.
         *
         * @param impl the implementation interface
         * @return this builder
         * @since 2.0.0
         */
        public Builder<C> implementationInterface(final ImplementationInterface impl) {
            this.implementation = requireNonNull(impl, "impl");
            return this;
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
         * Build an engine.
         *
         * <p>The implementation interface must have been set.</p>
         *
         * @return a new instance
         * @throws PermissionsLoadingException if unable to load initial data
         * @since 2.0.0
         */
        public MinecraftPermissionsEx<C> build() throws PermissionsLoadingException {
            requireNonNull(this.implementation, "An ImplementationInterface must be provided");
            return new MinecraftPermissionsEx<>(this);
        }
    }
}