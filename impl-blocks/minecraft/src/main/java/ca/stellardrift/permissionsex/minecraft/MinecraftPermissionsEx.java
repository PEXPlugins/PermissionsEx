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

import ca.stellardrift.permissionsex.PermissionsEx;
import ca.stellardrift.permissionsex.data.SubjectCache;
import ca.stellardrift.permissionsex.minecraft.profile.ProfileApiResolver;
import ca.stellardrift.permissionsex.subject.SubjectType;
import reactor.core.publisher.Mono;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static ca.stellardrift.permissionsex.Messages.*;
import static ca.stellardrift.permissionsex.PermissionsEx.GLOBAL_CONTEXT;

/**
 * An implementation of the Minecraft-specific parts of PermissionsEx
 *
 * @since 2.0.0
 */
public class MinecraftPermissionsEx<T> implements Closeable {
    public static final String SUBJECTS_USER = "user";
    public static final String SUBJECTS_GROUP = "group";

    private final PermissionsEx<T> engine;
    private final ProfileApiResolver resolver;

    public MinecraftPermissionsEx(final PermissionsEx<T> engine) {
        this.engine = engine;
        this.resolver = ProfileApiResolver.resolver(engine.getAsyncExecutor());

        convertUuids();
        groups().cacheAll();
    }

    public PermissionsEx<T> engine() {
        return this.engine;
    }

    public SubjectType users() {
        return this.engine.getSubjects(SUBJECTS_USER);
    }

    public SubjectType groups() {
        return this.engine.getSubjects(SUBJECTS_GROUP);
    }

    private void convertUuids() {
        try {
            InetAddress.getByName("api.mojang.com");
            this.engine.performBulkOperation(() -> {
                final SubjectCache users = this.users().persistentData();
                Set<String> toConvert = users.getAllIdentifiers().stream()
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
                    engine.getLogger().info(UUIDCONVERSION_BEGIN.toComponent());
                } else {
                    return CompletableFuture.completedFuture(0L);
                }

                return this.resolver.resolveByName(toConvert)
                        .filterWhen(profile -> {
                            final String newIdentifier = profile.uuid().toString();
                            final String lookupName = profile.name();
                            final Mono<Boolean> newRegistered = Mono.fromCompletionStage(users.isRegistered(newIdentifier));
                            final Mono<Boolean> oldRegistered = Mono.zip(Mono.fromCompletionStage(users.isRegistered(lookupName)),
                                    Mono.fromCompletionStage(users.isRegistered(lookupName.toLowerCase(Locale.ROOT))), (a, b) -> a || b);
                            return Mono.zip(newRegistered, oldRegistered, (n, o) -> {
                                if (n) {
                                    this.engine.getLogger().warn(UUIDCONVERSION_ERROR_DUPLICATE.toComponent(newIdentifier));
                                    return false;
                                } else {
                                    return o;
                                }
                            });
                        }).flatMap(profile -> {
                            final String newIdentifier = profile.uuid().toString();
                            return Mono.fromCompletionStage(users.getData(profile.name(), null)
                                    .thenCompose(oldData -> users.set(newIdentifier, oldData.setOption(GLOBAL_CONTEXT, "name", profile.name()))
                                            .thenAccept(result -> users.set(profile.name(), null)
                                                    .exceptionally(t -> {
                                                        t.printStackTrace();
                                                        return null;
                                                    }))));
                        }).count().toFuture();
            }).thenAccept(result -> {
                if (result != null && result > 0) {
                    engine.getLogger().info(UUIDCONVERSION_END.toComponent(result));
                }
            }).exceptionally(t -> {
                engine.getLogger().error(UUIDCONVERSION_ERROR_GENERAL.toComponent(), t);
                return null;
            });
        } catch (final UnknownHostException ex) {
            engine.getLogger().warn(UUIDCONVERSION_ERROR_DNS.toComponent());
        }
    }

    @Override
    public void close() {
        this.engine.close();
    }
}
