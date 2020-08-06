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

package ca.stellardrift.permissionsex.subject;

import ca.stellardrift.permissionsex.PermissionsEx;
import ca.stellardrift.permissionsex.context.ContextDefinition;
import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.data.ImmutableSubjectData;
import ca.stellardrift.permissionsex.data.SubjectDataReference;
import ca.stellardrift.permissionsex.logging.PermissionCheckNotifier;
import ca.stellardrift.permissionsex.util.CachingValue;
import ca.stellardrift.permissionsex.util.CachingValues;
import ca.stellardrift.permissionsex.util.NodeTree;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.SimpleConfigurationNode;
import ninja.leaping.configurate.reference.ConfigurationReference;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * This is a holder that maintains the current subject data state
 */
public class CalculatedSubject implements Consumer<ImmutableSubjectData> {
    private final SubjectDataBaker baker;
    private final Map.Entry<String, String> identifier;
    private final SubjectType type;
    private SubjectDataReference ref, transientRef;

    private final AsyncLoadingCache<Set<ContextValue<?>>, BakedSubjectData> data;
    private final Set<Consumer<CalculatedSubject>> updateListeners = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private CachingValue<Set<ContextValue<?>>> activeContexts;

    CalculatedSubject(SubjectDataBaker baker, Map.Entry<String, String> identifier, SubjectType type) {
        this.baker = Preconditions.checkNotNull(baker, "baker");
        this.identifier = Preconditions.checkNotNull(identifier, "identifier");
        this.type = Preconditions.checkNotNull(type, "type");
        this.data = Caffeine.newBuilder()
                .maximumSize(32)
                .expireAfterAccess(1, TimeUnit.MINUTES)
                .executor(type.getManager().getAsyncExecutor())
                .buildAsync(((key, executor) -> this.baker.bake(CalculatedSubject.this, key)));
    }

    void initialize(SubjectDataReference persistentRef, SubjectDataReference transientRef) {
        this.ref = persistentRef;
        this.transientRef = transientRef;
        this.activeContexts = CachingValues.cachedByTime(50L, () -> {
            Set<ContextValue<?>> acc = new HashSet<>();
            for (ContextDefinition<?> contextDefinition : getManager().getRegisteredContextTypes()) {
                handleAccumulateSingle(contextDefinition, acc);
            }
            return acc;
        });
    }

    /**
     * Get the identifier for this subject, as a map entry where the key is the identifier for this subject's type
     * and the value is the specific identifier for this subject.
     *
     * @return The identifier
     */
    public Map.Entry<String, String> getIdentifier() {
        return identifier;
    }

    /**
     * Get the subject type holding this calculated subject
     * @return The subject type
     */
    public SubjectType getType() {
        return this.type;
    }

    PermissionsEx<?> getManager() {
        return this.type.getManager();
    }

    /**
     * Get the calculated data for a specific context set
     *
     * @param contexts The contexts to get data in. These will be processed for combinations
     * @return The baked subject data
     */
    private BakedSubjectData getData(Set<ContextValue<?>> contexts) {
        Preconditions.checkNotNull(contexts, "contexts");
        return data.synchronous().get(ImmutableSet.copyOf(contexts));
    }

    /**
     * Get the permissions tree in this subject's active contexts
     *
     * @return A node tree with the calculated permissions
     */
    public NodeTree getPermissions() {
        return getData(getActiveContexts()).getPermissions();
    }

    /**
     * Get the permissions tree for a certain set of contexts
     *
     * @param contexts The contexts to get permissions in
     * @return A node tree with the calculated permissions
     */
    public NodeTree getPermissions(Set<ContextValue<?>> contexts) {
        return getData(contexts).getPermissions();
    }

    /**
     * Get all options for this subject's active contexts. Options are plain strings and therefore do not have wildcard handling.
     *
     * @return A map of option keys to values
     */
    public Map<String, String> getOptions() {
        return getOptions(getActiveContexts());
    }

    /**
     * Get all options for a certain set of contexts. Options are plain strings and therefore do not have wildcard handling.
     *
     * @param contexts The contexts to query
     * @return A map of option keys to values
     */
    public Map<String, String> getOptions(Set<ContextValue<?>> contexts) {
        return getData(contexts).getOptions();
    }

    /**
     * Get a list of all parents inheriting from this subject
     * @param contexts The contexts to check
     * @return The list of parents that apply to this subject
     */
    public List<Map.Entry<String, String>> getParents(Set<ContextValue<?>> contexts) {
        List<Map.Entry<String, String>> parents = getData(contexts).getParents();
        getManager().getNotifier().onParentCheck(getIdentifier(), contexts, parents);
        return parents;
    }

    /**
     * Get a list of all parents inheriting from this subject in the active contexts
     * @return The list of parents that apply to this subject
     */
    public List<Map.Entry<String, String>> getParents() {
        return getParents(getActiveContexts());
    }

    /**
     * Contexts that have been queried recently enough to still be cached
     * @return A set of context sets that are in the lookup cache
     */
    private Set<Set<ContextValue<?>>> getCachedContexts() {
        return data.synchronous().asMap().keySet();
    }

    public Set<ContextValue<?>> getActiveContexts() {
        if (activeContexts == null) {
            throw new IllegalStateException("This subject has not yet been initialized! This is normally done before the future provided by PEX completes.");
        }
        return new HashSet<>(activeContexts.get());
    }

    public CompletableFuture<Set<ContextValue<?>>> getUsedContextValues() {
        return getManager().getUsedContextTypes().thenApply(defs -> {
            Set<ContextValue<?>> acc = new HashSet<>();
            defs.forEach(def -> handleAccumulateSingle(def, acc));
            return acc;
        });
    }

    private <T> void handleAccumulateSingle(ContextDefinition<T> def, Set<ContextValue<?>> acc) {
        def.accumulateCurrentValues(this, val -> {
            acc.add(def.createValue(val));
            return null;
        });
    }

    /**
     * Query a specific permission in a certain set of contexts.
     * This method takes into account context and wildcard inheritance calculations for any permission.
     *
     * Any checks made through this method will be logged by the {@link PermissionCheckNotifier}
     * registered with the PEX engine.
     *
     * @param contexts The contexts to check in
     * @param permission The permission to query
     * @return The permission value. &lt;0 evaluates to false, 0 is undefined, and &gt;0 evaluates to true.
     */
    public int getPermission(Set<ContextValue<?>> contexts, String permission) {
        int ret = getPermissions(contexts).get(Preconditions.checkNotNull(permission, "permission"));
        getManager().getNotifier().onPermissionCheck(getIdentifier(), contexts, permission, ret);
        return ret;
    }

    /**
     * Query a specific permission in this subject's active contexts
     * This method takes into account context and wildcard inheritance calculations for any permission.
     *
     * Any checks made through this method will be logged by the {@link PermissionCheckNotifier}
     * registered with the PEX engine.
     *
     * @param permission The permission to query
     * @return The permission value. &lt;0 evaluates to false, 0 is undefined, and &gt;0 evaluates to true.
     */
    public int getPermission(String permission) {
        return getPermission(getActiveContexts(), permission);
    }

    /**
     * Query whether this subject has a specific permission in this subject's active contexts
     * This method takes into account context and wildcard inheritance calculations for any permission.
     *
     * Any checks made through this method will be logged by the {@link PermissionCheckNotifier}
     * registered with the PEX engine.
     *
     * @param permission The permission to query
     * @return Whether the subject has a true permissions value
     */
    public boolean hasPermission(String permission) {
        return getPermission(permission) > 0;
    }

    /**
     * Query whether this subject has a specific permission in the provided contexts
     * This method takes into account context and wildcard inheritance calculations for any permission.
     *
     * Any checks made through this method will be logged by the {@link PermissionCheckNotifier}
     * registered with the PEX engine.
     *
     * @param contexts The contexts to query this permission in
     * @param permission The permission to query
     * @return Whether the subject has a true permissions value
     */
    public boolean hasPermission(Set<ContextValue<?>> contexts, String permission) {
        return getPermission(contexts, permission) > 0;
    }

    /**
     * Get an option that may be present for a certain subject in the subject's active contexts
     *
     * Any checks made through this method will be logged by the {@link PermissionCheckNotifier}
     * registered with the PEX engine.
     *
     * @param option The option to query
     * @return The option, if set
     */
    public Optional<String> getOption(String option) {
        return getOption(getActiveContexts(), option);
    }

    /**
     * Get an option that may be present for a certain subject
     *
     * Any checks made through this method will be logged by the {@link PermissionCheckNotifier}
     * registered with the PEX engine.
     *
     * @param contexts The contexts to check in
     * @param option The option to query
     * @return The option, if set
     */
    public Optional<String> getOption(Set<ContextValue<?>> contexts, String option) {
        String val = getOptions(contexts).get(Preconditions.checkNotNull(option, "option"));
        getManager().getNotifier().onOptionCheck(getIdentifier(), contexts, option, val);
        return Optional.ofNullable(val);
    }

    /**
     * Get an option from this subject. The value will be returned as a ConfigurationNode to allow easily accessing its data.
     *
     * Any checks made through this method will be logged by the {@link PermissionCheckNotifier}
     * registered with the PEX engine.
     *
     * @param option The option to query
     * @return The option, if set
     */
    public ConfigurationNode getOptionNode(String option) {
        return getOptionNode(getActiveContexts(), option);
    }

    /**
     * Get an option from this subject. The value will be returned as a ConfigurationNode to allow easily accessing its data.
     *
     * Any checks made through this method will be logged by the {@link PermissionCheckNotifier}
     * registered with the PEX engine.
     *
     * @param contexts The contexts to check in
     * @param option The option to query
     * @return The option, if set
     */
    public ConfigurationNode getOptionNode(Set<ContextValue<?>> contexts, String option) {
        String val = getOptions(contexts).get(Preconditions.checkNotNull(option, "option"));
        getManager().getNotifier().onOptionCheck(getIdentifier(), contexts, option, val);
        return ConfigurationNode.root().setValue(val);
    }

    /**
     * Access this subject's persistent data
     *
     * @return A reference to the persistent data of this subject
     */
    public SubjectDataReference data() {
        return this.ref;
    }

    /**
     * Access this subject's transient data
     *
     * @return A reference to the transient data of this subject
     */
    public SubjectDataReference transientData() {
        return this.transientRef;
    }

    public Optional<?> getAssociatedObject() {
        return this.type.getTypeInfo().getAssociatedObject(this.identifier.getValue());
    }

    /**
     * Register a listener that will receive updates to this subject.
     *
     * A reference to the listener will be held, so when updates are no longer needed the listener should be unregistered
     *
     * @param listener The listener
     */
    public void registerListener(Consumer<CalculatedSubject> listener) {
        updateListeners.add(Objects.requireNonNull(listener));
    }

    public void unregisterListener(Consumer<CalculatedSubject> listener) {
        updateListeners.remove(Objects.requireNonNull(listener));
    }

    /**
     * Internal use only. Cache updating listener
     *
     * @param newData Updated subject data object. Ignored
     */
    @Override
    public void accept(ImmutableSubjectData newData) {
        data.synchronous().invalidateAll();
        getManager().getActiveSubjectTypes().stream()
                .flatMap(type -> type.getActiveSubjects().stream())
                .filter(subj -> {
                    for (Set<ContextValue<?>> ent : subj.getCachedContexts()) {
                        if (subj.getParents(ent).contains(this.identifier)) {
                            return true;
                        }
                    }
                    return false;
                })
                .forEach(subj -> subj.data.synchronous().invalidateAll());
        updateListeners.forEach(listener -> listener.accept(this));
    }

}
