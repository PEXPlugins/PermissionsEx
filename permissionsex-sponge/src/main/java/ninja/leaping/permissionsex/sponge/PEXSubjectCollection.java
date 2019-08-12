/**
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
package ninja.leaping.permissionsex.sponge;

import com.github.benmanes.caffeine.cache.AsyncCacheLoader;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.subject.CalculatedSubject;
import ninja.leaping.permissionsex.subject.SubjectType;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectReference;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.util.Tristate;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Subject collection
 */
class PEXSubjectCollection implements SubjectCollection {
    private final String identifier;
    private final PermissionsExPlugin plugin;
    private final SubjectType collection;
    private PEXSubject defaults;

    private final AsyncLoadingCache<String, PEXSubject> subjectCache;
    private PEXSubjectCollection(final String identifier, final PermissionsExPlugin plugin) {
        this.identifier = identifier;
        this.plugin = plugin;
        this.collection = plugin.getManager().getSubjects(identifier);
        this.subjectCache = Caffeine.newBuilder().executor(plugin.getAsyncExecutor()).buildAsync(new AsyncCacheLoader<String, PEXSubject>() {
                @Override
                public CompletableFuture<PEXSubject> asyncLoad(String key, Executor executor) {
                    return PEXSubject.load(key, PEXSubjectCollection.this);
                }
            });
    }

    public static CompletableFuture<PEXSubjectCollection> load(final String identifier, final PermissionsExPlugin plugin) {
        PEXSubjectCollection ret = new PEXSubjectCollection(identifier, plugin);
        CompletableFuture<Subject> defaultFuture = identifier.equals(PermissionsEx.SUBJECTS_DEFAULTS) ? ret.loadSubject(PermissionsEx.SUBJECTS_DEFAULTS) 
            : plugin.loadCollection(PermissionsEx.SUBJECTS_DEFAULTS).thenCompose(coll -> coll.loadSubject(identifier));
        return defaultFuture.thenApply(defSubj -> {
            ret.defaults = (PEXSubject) defSubj;
            return ret;
        });
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    PermissionsExPlugin getPlugin() {
        return this.plugin;
    }

    @Override
    public Predicate<String> getIdentifierValidityPredicate() {
        return this.collection.getTypeInfo()::isNameValid;
    }

    @Override
    public Optional<Subject> getSubject(String identifier) {
        try {
            CompletableFuture<PEXSubject> future = this.subjectCache.getIfPresent(identifier);
            return future == null ? Optional.empty() : Optional.ofNullable(future.get());
        } catch (InterruptedException | ExecutionException e) {
            return Optional.empty();
        }
    }

    @Override
    public CompletableFuture<Boolean> hasSubject(String identifier) {
        return this.collection.persistentData().isRegistered(identifier);
    }

    @Override
    public CompletableFuture<Subject> loadSubject(String identifier) {
        return this.subjectCache.get(identifier).thenApply(x -> x);
    }

    @Override
    public CompletableFuture<Map<String, Subject>> loadSubjects(Set<String> identifiers) {
        return this.subjectCache.getAll(identifiers).thenApply(map -> Maps.transformValues(map, x -> x));
    }

    @Override
    public SubjectReference newSubjectReference(String subjectIdentifier) {
        return new PEXSubjectReference(subjectIdentifier, this.identifier, plugin);
    }

    @Override
    public CompletableFuture<Map<SubjectReference, Boolean>> getAllWithPermission(String permission) {
        return getAllWithPermissionValue(permission).thenApply(map -> Maps.transformValues(map, x -> x > 0));
    }

    @Override
    public CompletableFuture<Map<SubjectReference, Boolean>> getAllWithPermission(Set<Context> contexts, String permission) {
        return getAllWithPermissionValue(contexts, permission).thenApply(map -> Maps.transformValues(map, x -> x > 0));
    }

    @Override
    public Map<Subject, Boolean> getLoadedWithPermission(String permission) {
        return Maps.transformValues(getLoadedWithPermissionValue(permission), x -> x > 0);
    }

    @Override
    public Map<Subject, Boolean> getLoadedWithPermission(Set<Context> contexts, String permission) {
        return Maps.transformValues(getLoadedWithPermissionValue(contexts, permission), x -> x > 0);
    }

    @Override
    public void suggestUnload(String identifier) {
        subjectCache.synchronous().invalidate(identifier);
        collection.uncache(identifier);
    }

    @Override
    public CompletableFuture<Set<String>> getAllIdentifiers() {
        return CompletableFuture.completedFuture(collection.getAllIdentifiers());
    }

    @Override
    public Collection<Subject> getLoadedSubjects() {
        return ImmutableSet.copyOf(getActiveSubjects());
    }

    Iterable<PEXSubject> getActiveSubjects() {
        return subjectCache.synchronous().asMap().values();
    }

    @Override
    public Map<Subject, Integer> getLoadedWithPermissionValue(String permission) {
        return getLoadedWithPermissionValue(null, permission);
    }

    @Override
    public Map<Subject, Integer> getLoadedWithPermissionValue(Set<Context> contexts, String permission) {
        final ImmutableMap.Builder<Subject, Integer> ret = ImmutableMap.builder();
        for (PEXSubject subject : subjectCache.synchronous().asMap().values()) {
                int permissionValue = subject.getPermission(contexts == null ? subject.getActiveContexts() : contexts, permission);
                if (permissionValue != 0) {
                    ret.put(subject, permissionValue);
                }
        }
        return ret.build();
    }

    @Override
    public CompletableFuture<Map<SubjectReference, Integer>> getAllWithPermissionValue(String permission) {
        return getAllWithPermissionValue(ImmutableSet.of(), permission); // TODO: make this use active contexts
    }

    @Override
    public CompletableFuture<Map<SubjectReference, Integer>> getAllWithPermissionValue(Set<Context> contexts,
            String permission) {
        Set<String> raw = this.collection.getAllIdentifiers();
        CompletableFuture<CalculatedSubject>[] futures = new CompletableFuture[raw.size()];
        int i = 0;
        for (String ident : raw) {
            if (i >= futures.length) {
                break; // TODO: acknowlege this error somehow?
            }
            futures[i++] = this.collection.get(ident);
        }

        return CompletableFuture.allOf(futures).thenApply(x -> {
            return Arrays.stream(futures).map(f -> {
                try {
                    return f.get();
                } catch (InterruptedException | ExecutionException e) {
                    return null;
                }
            }).map(subj -> {
                final int perm = subj.getPermission(PEXSubjectData.parSet(contexts), permission);
                return Maps.immutableEntry((SubjectReference) subj.getIdentifier(), perm);
            }).filter(ent -> ent.getValue() != null)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        });
    }

    /**
     * Get the subject that provides defaults for subjects of this type. This subject is placed at the root of any inheritance tree involving subjects of this type.
     *
     * @return The subject holding defaults
     */
    @Override
    public Subject getDefaults() {
        return this.defaults;
    }

    public CompletableFuture<CalculatedSubject> getCalculatedSubject(String identifier) {
            return this.collection.get(identifier);
    }

    SubjectType getType() {
        return this.collection;
    }
}
