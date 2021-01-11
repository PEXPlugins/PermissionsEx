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
package ca.stellardrift.permissionsex.impl.subject;

import ca.stellardrift.permissionsex.impl.PermissionsEx;
import ca.stellardrift.permissionsex.context.ContextDefinition;
import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.impl.util.PCollections;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import ca.stellardrift.permissionsex.subject.ImmutableSubjectData;
import ca.stellardrift.permissionsex.subject.SubjectRef;
import ca.stellardrift.permissionsex.impl.util.CachingValue;
import ca.stellardrift.permissionsex.util.NodeTree;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * This is a holder that maintains the current subject data state
 */
public class CalculatedSubjectImpl<I> implements Consumer<ImmutableSubjectData>, CalculatedSubject {
    private final SubjectDataBaker baker;
    private final SubjectRef<I> identifier;
    private final SubjectTypeCollectionImpl<I> type;
    private @MonotonicNonNull ToDataSubjectRefImpl<I> ref;
    private @MonotonicNonNull ToDataSubjectRefImpl<I> transientRef;

    private final AsyncLoadingCache<Set<ContextValue<?>>, BakedSubjectData> data;
    private final Set<Consumer<CalculatedSubject>> updateListeners = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private @MonotonicNonNull CachingValue<Set<ContextValue<?>>> activeContexts;

    CalculatedSubjectImpl(
            final SubjectDataBaker baker,
            final SubjectRef<I> identifier,
            final SubjectTypeCollectionImpl<I> type) {
        this.baker = baker;
        this.identifier = identifier;
        this.type = type;
        this.data = Caffeine.newBuilder()
                .maximumSize(32)
                .expireAfterAccess(1, TimeUnit.MINUTES)
                .executor(type.engine().asyncExecutor())
                .buildAsync((key, executor) -> this.baker.bake(CalculatedSubjectImpl.this, key));
    }

    void initialize(ToDataSubjectRefImpl<I> persistentRef, ToDataSubjectRefImpl<I> transientRef) {
        this.ref = persistentRef;
        this.transientRef = transientRef;
        this.activeContexts = CachingValue.timeBased(50L, () -> {
            Set<ContextValue<?>> acc = new HashSet<>();
            for (ContextDefinition<?> contextDefinition : getManager().registeredContextTypes()) {
                handleAccumulateSingle(contextDefinition, acc);
            }
            return acc;
        });
    }

    @Override
    public SubjectRef<I> identifier() {
        return identifier;
    }

    @Override
    public SubjectTypeCollectionImpl<I> containingType() {
        return this.type;
    }

    PermissionsEx<?> getManager() {
        return this.type.engine();
    }

    /**
     * Get the calculated data for a specific context set
     *
     * @param contexts The contexts to get data in. These will be processed for combinations
     * @return The baked subject data
     */
    private BakedSubjectData getData(Set<ContextValue<?>> contexts) {
        Objects.requireNonNull(contexts, "contexts");
        return data.synchronous().get(PCollections.asSet(contexts));
    }

    @Override
    public NodeTree permissions(Set<ContextValue<?>> contexts) {
        return getData(contexts).permissions();
    }

    @Override
    public Map<String, String> options(Set<ContextValue<?>> contexts) {
        return getData(contexts).options();
    }

    @Override
    public List<SubjectRef<?>> parents(Set<ContextValue<?>> contexts) {
        List<SubjectRef<?>> parents = getData(contexts).parents();
        getManager().getNotifier().onParentCheck(identifier(), contexts, parents);
        return parents;
    }

    /**
     * Contexts that have been queried recently enough to still be cached
     * @return A set of context sets that are in the lookup cache
     */
    private Set<Set<ContextValue<?>>> getCachedContexts() {
        return data.synchronous().asMap().keySet();
    }

    @Override
    public Set<ContextValue<?>> activeContexts() {
        if (this.activeContexts == null) {
            throw new IllegalStateException("This subject has not yet been initialized! This is normally done before the future provided by PEX completes.");
        }
        return new HashSet<>(this.activeContexts.get());
    }

    @Override
    public CompletableFuture<Set<ContextValue<?>>> usedContextValues() {
        return getManager().usedContextTypes().thenApply(defs -> {
            Set<ContextValue<?>> acc = new HashSet<>();
            defs.forEach(def -> handleAccumulateSingle(def, acc));
            return acc;
        });
    }

    private <T> void handleAccumulateSingle(ContextDefinition<T> def, Set<ContextValue<?>> acc) {
        def.accumulateCurrentValues(this, val -> acc.add(def.createValue(val)));
    }

    @Override
    public int permission(Set<ContextValue<?>> contexts, String permission) {
        int ret = permissions(contexts).get(Objects.requireNonNull(permission, "permission"));
        getManager().getNotifier().onPermissionCheck(identifier(), contexts, permission, ret);
        return ret;
    }

    @Override
    public boolean hasPermission(Set<ContextValue<?>> contexts, String permission) {
        final int perm = permission(contexts, permission);
        if (perm == 0) {
            return containingType().type().undefinedPermissionValue(this.identifier.identifier());
        } else {
            return perm > 0;
        }
    }

    @Override
    public Optional<String> option(Set<ContextValue<?>> contexts, String option) {
        final @Nullable String val = options(contexts).get(Objects.requireNonNull(option, "option"));
        getManager().getNotifier().onOptionCheck(identifier(), contexts, option, val);
        return Optional.ofNullable(val);
    }

    @Override
    public ConfigurationNode optionNode(Set<ContextValue<?>> contexts, String option) {
        String val = options(contexts).get(Objects.requireNonNull(option, "option"));
        getManager().getNotifier().onOptionCheck(identifier(), contexts, option, val);
        return BasicConfigurationNode.root().raw(val);
    }

    /**
     * Access this subject's persistent data
     *
     * @return A reference to the persistent data of this subject
     */
    @Override
    public ToDataSubjectRefImpl<I> data() {
        return this.ref;
    }

    @Override
    public ToDataSubjectRefImpl<I> transientData() {
        return this.transientRef;
    }

    @Override
    public @Nullable Object associatedObject() {
        return this.type.type().getAssociatedObject(this.identifier.identifier());
    }

    @Override
    public void registerListener(Consumer<CalculatedSubject> listener) {
        updateListeners.add(Objects.requireNonNull(listener));
    }

    @Override
    public void unregisterListener(Consumer<CalculatedSubject> listener) {
        updateListeners.remove(Objects.requireNonNull(listener));
    }

    @Override
    public void accept(ImmutableSubjectData newData) {
        data.synchronous().invalidateAll();
        getManager().loadedSubjectTypes().stream()
                .flatMap(type -> type.activeSubjects().stream())
                .map(it -> (CalculatedSubjectImpl<?>) it)
                .filter(subj -> {
                    for (Set<ContextValue<?>> ent : subj.getCachedContexts()) {
                        if (subj.parents(ent).contains(this.identifier)) {
                            return true;
                        }
                    }
                    return false;
                })
                .forEach(subj -> subj.data.synchronous().invalidateAll());
        updateListeners.forEach(listener -> listener.accept(this));
    }

}
