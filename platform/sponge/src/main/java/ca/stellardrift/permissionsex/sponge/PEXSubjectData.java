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

import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.impl.util.PCollections;
import ca.stellardrift.permissionsex.subject.Segment;
import ca.stellardrift.permissionsex.subject.SubjectRef;
import ca.stellardrift.permissionsex.util.Change;
import com.google.common.collect.Maps;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.SubjectReference;
import org.spongepowered.api.util.Tristate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.Objects.requireNonNull;

public class PEXSubjectData implements SubjectData {
    private final SubjectRef.ToData<?> data;
    private final PEXSubject subject;
    private final ConcurrentMap<Set<ContextValue<?>>, List<SubjectReference>> parentsCache = new ConcurrentHashMap<>();


    PEXSubjectData(final SubjectRef.ToData<?> data, final PEXSubject subject) {
        this.data = data;
        this.subject = subject;
        data.onUpdate($ -> this.clearCache());
    }

    PermissionsExService service() {
        return this.subject.getContainingCollection().service();
    }

    private void clearCache() {
        synchronized(this.parentsCache) {
            this.parentsCache.clear();
        }
    }

    private <T> Map<Set<Context>, T> keysToSponge(final Map<Set<ContextValue<?>>, T> original) {
        return PCollections.asMap(original, (k, $) -> Contexts.toSponge(k), ($, v) -> v);
    }

    @Override
    public Subject getSubject() {
        return this.subject;
    }

    @Override
    public boolean isTransient() {
        return this == this.subject.getTransientSubjectData();
    }

    @Override
    public Map<Set<Context>, Map<String, Boolean>> getAllPermissions() {
        return keysToSponge(data.get().mapSegmentValues(it -> Maps.transformValues(it.permissions(), v -> v > 0)));
    }

    @Override
    public Map<String, Boolean> getPermissions(final Set<Context> contexts) {
        return Maps.transformValues(this.data.get().segment(Contexts.toPex(contexts, service().manager()))
            .permissions(), v -> v > 0);
    }

    @Override
    public CompletableFuture<Boolean> setPermission(
        final Set<Context> contexts,
        final String permission,
        final Tristate value
    ) {
        final int intVal;
        switch (value) {
            case TRUE: intVal = 1; break;
            case FALSE: intVal = -1; break;
            case UNDEFINED: intVal = 0; break;
            default: throw new IllegalStateException("Unknown tristate provided: " + value);
        }
        return data.update(Contexts.toPex(contexts, service().manager()), it -> it.withPermission(permission, intVal))
            .thenApply( $ -> true);
    }

    @Override
    public CompletableFuture<Boolean> clearPermissions() {
        return data.update(it -> it.withSegments(($, segment) -> segment.withoutPermissions()))
            .thenApply(Change::changed);
    }

    @Override
    public CompletableFuture<Boolean> clearPermissions(final Set<Context> contexts) {
        return data.update(Contexts.toPex(contexts, service().manager()), Segment::withoutPermissions)
            .thenApply(Change::changed);
    }

    @Override
    public Map<Set<Context>, List<SubjectReference>> getAllParents() {
        synchronized(parentsCache) {
            this.data.get().activeContexts().forEach(this::getParentsInternal);
            return keysToSponge(this.parentsCache);
        }
    }

    @Override
    public List<SubjectReference> getParents(final Set<Context> contexts) {
        return this.getParentsInternal(Contexts.toPex(contexts, service().manager()));
    }

    private List<SubjectReference> getParentsInternal(final Set<ContextValue<?>> contexts) {
        final List<SubjectReference> existing = this.parentsCache.get(contexts);
        if (existing != null) {
            return existing;
        }
        final List<SubjectReference> parents;
        synchronized(this.parentsCache) {
            final List<? extends SubjectRef<?>> rawParents = this.data.get().segment(contexts).parents();
            parents = rawParents == null ? PCollections.vector() : PCollections.asVector(rawParents, it -> PEXSubjectReference.asSponge(it, this.service()));
            final @Nullable List<SubjectReference> existingParents = this.parentsCache.putIfAbsent(contexts, parents);
            if (existingParents != null) {
                return existingParents;
            }
        }
        return parents;
    }

    @Override
    public CompletableFuture<Boolean> addParent(final Set<Context> contexts, final SubjectReference parent) {
        requireNonNull(contexts, "contexts");
        requireNonNull(parent, "parent");
        final PEXSubjectReference<?> ref = PEXSubjectReference.asPex(parent, this.service()); // validate subject reference
        return data.update(Contexts.toPex(contexts, service().manager()), it -> it.plusParent(ref))
            .thenApply(Change::changed);
    }

    @Override
    public CompletableFuture<Boolean> removeParent(final Set<Context> contexts, final SubjectReference parent) {
        requireNonNull(parent, "parent");
        return data.update(Contexts.toPex(contexts, service().manager()), it -> it.minusParent(PEXSubjectReference.asPex(parent, this.service())))
            .thenApply(Change::changed);
    }

    @Override
    public CompletableFuture<Boolean> clearParents() {
        return data.update(it -> it.withSegments(($, segment) -> segment.withoutParents()))
            .thenApply(Change::changed);
    }

    @Override
    public CompletableFuture<Boolean> clearParents(final Set<Context> contexts) {
        return data.update(Contexts.toPex(contexts, service().manager()), Segment::withoutParents)
            .thenApply(Change::changed);
    }

    @Override
    public Map<Set<Context>, Map<String, String>> getAllOptions() {
        return keysToSponge(this.data.get().mapSegmentValues(Segment::options));
    }

    @Override
    public Map<String, String> getOptions(final Set<Context> contexts) {
        return data.get().segment(Contexts.toPex(contexts, service().manager())).options();
    }

    @Override
    public CompletableFuture<Boolean> setOption(
        final Set<Context> contexts,
        final String key,
        final @Nullable String value
    ) {
        return data.update(Contexts.toPex(contexts, service().manager()),
            it -> value == null ? it.withoutOption(key) : it.withOption(key, value))
            .thenApply(Change::changed);
    }

    @Override
    public CompletableFuture<Boolean> clearOptions() {
        return data.update(it -> it.withSegments(($, segment) -> segment.withoutOptions()))
            .thenApply(Change::changed);
    }

    @Override
    public CompletableFuture<Boolean> clearOptions(final Set<Context> contexts) {
        return data.update(Contexts.toPex(contexts, service().manager()), Segment::withoutOptions)
            .thenApply(Change::changed);
    }

}
