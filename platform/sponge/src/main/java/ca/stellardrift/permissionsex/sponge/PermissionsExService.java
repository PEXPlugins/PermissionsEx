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

import ca.stellardrift.permissionsex.PermissionsEngine;
import ca.stellardrift.permissionsex.impl.PermissionsEx;
import ca.stellardrift.permissionsex.impl.util.CachingValue;
import ca.stellardrift.permissionsex.impl.util.PCollections;
import ca.stellardrift.permissionsex.subject.SubjectType;
import ca.stellardrift.permissionsex.subject.SubjectTypeCollection;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Server;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectReference;
import org.spongepowered.plugin.PluginContainer;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

public class PermissionsExService implements PermissionService {
    private final Server server;
    private final PermissionsExPlugin plugin;
    private final Timings timings;

    // Sponge registrations
    private final List<ContextCalculator<Subject>> contextCalculators = new CopyOnWriteArrayList<>();
    private final Map<String, PEXPermissionDescription> descriptions = new ConcurrentHashMap<>();

    // Caches of mapped types
    private final AsyncLoadingCache<SubjectType<?>, PEXSubjectCollection<?>> subjectCollections;
    private final PEXSubject defaults;

    PermissionsExService(final Server server, final PermissionsExPlugin plugin) {
        this.server = server;
        this.plugin = plugin;
        this.timings = new Timings(plugin.container());
        this.subjectCollections = Caffeine.newBuilder().executor(plugin.scheduler())
            .buildAsync((type, $) -> PEXSubjectCollection.load(type, this));
        this.defaults = (PEXSubject) loadCollection(plugin.engine().defaults().type())
            .thenCompose(coll -> coll.loadSubject(plugin.engine().defaults().type().name()))
            .join();
    }

    Timings timings() {
        return this.timings;
    }

    List<ContextCalculator<Subject>> contextCalculators() {
        return this.contextCalculators;
    }

    PermissionsEx<?> manager() {
        return this.plugin.manager().engine();
    }

    SubjectType<?> subjectTypeFromIdentifier(final String identifier) {
        final @Nullable SubjectType<?> existing = this.manager().subjectType(identifier);
        if (existing != null) {
            return existing;
        }

        // There's nothing registered, but Sponge doesn't have a concept of types, so we'll create a fallback string type
        return SubjectType.stringIdentBuilder(identifier).build();
    }

    Stream<PEXSubject> allActiveSubjects() {
        return subjectCollections.synchronous().asMap().values().stream().flatMap(PEXSubjectCollection::activeSubjects);
    }

    <V> CachingValue<V> tickBasedCachingValue(final long deltaTicks, final Supplier<V> update) {
        return new CachingValue<>(this.server::runningTimeTicks, deltaTicks, update);
    }

    @Override
    public SubjectCollection userSubjects() {
        return this.collection(plugin.users().type());
    }

    @Override
    public SubjectCollection groupSubjects() {
        return this.collection(plugin.groups().type());
    }

    @Override
    public Subject defaults() {
        return this.defaults;
    }

    @Override
    public Predicate<String> identifierValidityPredicate() {
        return $ -> true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    <I> CompletableFuture<PEXSubjectCollection<I>> loadCollection(final SubjectType<I> type) {
        return (CompletableFuture) this.subjectCollections.get(type);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public CompletableFuture<SubjectCollection> loadCollection(final String identifier) {
        return (CompletableFuture) this.subjectCollections.get(this.subjectTypeFromIdentifier(identifier));
    }

    @SuppressWarnings("unchecked")
    <I> PEXSubjectCollection<I> collection(final SubjectType<I> type) {
        return (PEXSubjectCollection<I>) this.subjectCollections.get(type).join();
    }

    @Override
    public Optional<SubjectCollection> collection(final String identifier) {
        final @Nullable CompletableFuture<PEXSubjectCollection<?>> collectionFuture = subjectCollections.getIfPresent(this.subjectTypeFromIdentifier(identifier));
        return collectionFuture == null ? Optional.empty() : Optional.of(collectionFuture.join());
    }

    @Override
    public CompletableFuture<Boolean> hasCollection(final String identifier) {
        return CompletableFuture.completedFuture(true); // TODO: Check if subject type registered
    }

    @Override
    public Map<String, SubjectCollection> loadedCollections() {
        return PCollections.asMap(subjectCollections.synchronous().asMap(), (k, $) -> k.name(), ($, v) -> v);
    }

    @Override
    public CompletableFuture<Set<String>> allIdentifiers() {
        return CompletableFuture.completedFuture(PCollections.asSet(this.manager().knownSubjectTypes(), SubjectType::name));
    }

    @Override
    public SubjectReference newSubjectReference(final String collectionIdentifier, final String subjectIdentifier) {
        return new PEXSubjectReference<>(this.subjectTypeFromIdentifier(collectionIdentifier), subjectIdentifier, this);
    }

    @Override
    public PermissionDescription.Builder newDescriptionBuilder(final PluginContainer plugin) {
        return new PEXPermissionDescription.Builder(requireNonNull(plugin, "plugin"), this);
    }

    void registerDescription(final PEXPermissionDescription description, final Map<String, Integer> ranks) {
        descriptions.put(description.id(), description);
        final SubjectTypeCollection<String> coll = this.plugin.roleTemplates();
        for (Map.Entry<String, Integer> entry : ranks.entrySet()) {
            coll.transientData().update(entry.getKey(),  input ->
                input.withSegment(PermissionsEngine.GLOBAL_CONTEXT, it -> it.withPermission(description.id(), entry.getValue()))
            );
        }
    }

    @Override
    public Optional<PermissionDescription> description(final String permission) {
        return Optional.ofNullable(this.descriptions.get(permission));
    }

    @Override
    public Collection<PermissionDescription> descriptions() {
        return PCollections.narrow(PCollections.asSet(this.descriptions.values()));
    }

    @Override
    public void registerContextCalculator(final ContextCalculator<Subject> calculator) {
        this.contextCalculators.add(requireNonNull(calculator, "calculator"));
    }

}
