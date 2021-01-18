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
package ca.stellardrift.permissionsex.sponge;

import ca.stellardrift.permissionsex.impl.util.PCollections;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import ca.stellardrift.permissionsex.subject.SubjectType;
import ca.stellardrift.permissionsex.subject.SubjectTypeCollection;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Maps;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.pcollections.PMap;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectReference;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.configurate.util.UnmodifiableCollections;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

final class PEXSubjectCollection<I> implements SubjectCollection {
    private final SubjectType<I> type;
    private final PermissionsExService service;
    private final SubjectTypeCollection<I> implCache;
    private @MonotonicNonNull PEXSubject defaults;
    private final AsyncLoadingCache<String, PEXSubject> subjectCache;

    static <I> CompletableFuture<PEXSubjectCollection<?>> load(final SubjectType<I> identifier, final PermissionsExService service) {
        final PEXSubjectCollection<I> ret = new PEXSubjectCollection<>(identifier, service);
        final CompletableFuture<Subject> defaultFuture;
        if (Objects.equals(identifier, service.manager().defaults().type())) {
            defaultFuture = ret.loadSubject(service.manager().defaults().type().name());
        } else {
            defaultFuture = service.loadCollection(service.manager().defaults().type().name())
                .thenCompose(it -> it.loadSubject(identifier.name()));
        }
        return defaultFuture.thenApply(it -> {
            ret.defaults = (PEXSubject) it;
            return ret;
        });
    }

    PEXSubjectCollection(final SubjectType<I> type, final PermissionsExService service) {
        this.type = type;
        this.service = service;
        this.implCache = this.service.manager().subjects(type);
        this.subjectCache = Caffeine.newBuilder()
            .executor(this.service.manager().asyncExecutor())
            .buildAsync((key, $) -> PEXSubject.load(key, this));
    }

    PermissionsExService service() {
        return this.service;
    }

    CompletableFuture<CalculatedSubject> getCalculatedSubject(final String identifier) {
        return this.implCache.get(this.type.parseIdentifier(identifier));
    }

    Stream<PEXSubject> activeSubjects() {
        return this.subjectCache.synchronous().asMap().values().stream();
    }

    @Override
    public String getIdentifier() {
        return this.type.name();
    }

    @Override
    public Predicate<String> getIdentifierValidityPredicate() {
        return this.type::isIdentifierValid;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public CompletableFuture<Subject> loadSubject(final String identifier) {
        return (CompletableFuture) this.subjectCache.get(identifier);
    }

    @Override
    public Optional<Subject> getSubject(final String identifier) {
        final @Nullable CompletableFuture<PEXSubject> future = this.subjectCache.getIfPresent(identifier);
        if (future == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(future.get());
        } catch (final InterruptedException | ExecutionException ex) {
            // TODO: Log error
            return Optional.empty();
        }
    }

    @Override
    public CompletableFuture<Boolean> hasSubject(final String identifier) {
        return this.implCache.persistentData().isRegistered(this.type.parseIdentifier(identifier));
    }

    @Override
    @SuppressWarnings("rawtypes")
    public CompletableFuture<Map<String, Subject>> loadSubjects(final Set<String> identifiers) {
        final Map<String, CompletableFuture<Subject>> subjs = Maps.asMap(identifiers, this::loadSubject);
        return CompletableFuture.allOf(subjs.values().toArray(new CompletableFuture[0]))
            .thenApply($ -> PCollections.asMap(subjs, (k, $$) -> k, ($$, v) -> v.join()));
    }

    @Override
    public Collection<Subject> getLoadedSubjects() {
        return this.activeSubjects().collect(Collectors.toSet());
    }

    @Override
    public CompletableFuture<Set<String>> getAllIdentifiers() {
        return CompletableFuture.completedFuture(this.implCache.allIdentifiers()
                .map(this.type::serializeIdentifier)
                .collect(Collectors.toSet()));
    }

    @Override
    public SubjectReference newSubjectReference(final String subjectIdentifier) {
        return new PEXSubjectReference<>(this.type, this.type.parseIdentifier(subjectIdentifier), service);
    }

    @Override
    public CompletableFuture<Map<SubjectReference, Boolean>> getAllWithPermission(final String permission) {
        return getAllWithPermission(null, permission);
    }

    @Override
    public CompletableFuture<Map<SubjectReference, Boolean>> getAllWithPermission(
        final @Nullable Set<Context> contexts,
        final String permission
    ) {
        requireNonNull(permission, "permission");
        final Stream<I> raw = implCache.allIdentifiers();
        @SuppressWarnings("unchecked")
        final CompletableFuture<CalculatedSubject>[] futures = raw.map(this.implCache::get)
            .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(futures).thenApply($ ->
            Arrays.stream(futures)
                .map(CompletableFuture::join)
                .map(it -> {
                    final int perm = it.permission(contexts == null ? it.activeContexts() : Contexts.toPex(contexts, service.manager()), permission);
                    @Nullable Boolean bPerm = null;
                    if (perm > 0) {
                        bPerm = true;
                    } else if (perm < 0) {
                        bPerm = false;
                    }
                    if (bPerm == null) {
                        return null;
                    } else {
                        return UnmodifiableCollections.<SubjectReference, Boolean>immutableMapEntry(PEXSubjectReference.asSponge(it.identifier(), this.service()), bPerm);
                    }
                })
                .filter(Objects::nonNull)
                .collect(PCollections.toPMap())
        );
    }

    @Override
    public Map<Subject, Boolean> getLoadedWithPermission(final String permission) {
        return getLoadedWithPermission(null, permission);
    }

    @Override
    public Map<Subject, Boolean> getLoadedWithPermission(final @Nullable Set<Context> contexts, final String permission) {
        requireNonNull(permission, "permission");
        PMap<Subject, Boolean> ret = PCollections.map();
        for (final PEXSubject subject : this.subjectCache.synchronous().asMap().values()) {
            // TODO: Use CalculatedSubject here
            final Tristate permissionValue = subject.getPermissionValue(contexts == null ? subject.getActiveContexts() : contexts, permission);
            if (permissionValue != Tristate.UNDEFINED) {
                ret = ret.plus(subject, permissionValue.asBoolean());
            }
        }
        return ret;
    }

    @Override
    public Subject getDefaults() {
        return this.defaults;
    }

    @Override
    public void suggestUnload(final String identifier) {
        requireNonNull(identifier, "identifier");
        this.subjectCache.synchronous().invalidate(identifier);
        this.implCache.uncache(this.type.parseIdentifier(identifier));
    }

}
