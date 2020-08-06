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

import ca.stellardrift.permissionsex.PermissionsEx;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import ca.stellardrift.permissionsex.subject.SubjectType;
import com.github.benmanes.caffeine.cache.AsyncCacheLoader;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectReference;
import org.spongepowered.api.util.Tristate;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
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
        Map<String, CompletableFuture<Subject>> subjs =  Maps.asMap(identifiers, ident -> loadSubject(Objects.requireNonNull(ident)));
        return CompletableFuture.allOf(subjs.values().toArray(new CompletableFuture[0]))
                .thenApply(none -> Maps.transformValues(subjs, CompletableFuture::join));
    }

    @Override
    public SubjectReference newSubjectReference(String subjectIdentifier) {
        return new PEXSubjectReference(subjectIdentifier, this.identifier, plugin);
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
    public Map<Subject, Boolean> getLoadedWithPermission(String permission) {
        return getLoadedWithPermission(null, permission);
    }

    @Override
    public Map<Subject, Boolean> getLoadedWithPermission(@Nullable Set<Context> contexts, String permission) {
        final ImmutableMap.Builder<Subject, Boolean> ret = ImmutableMap.builder();
        for (PEXSubject subject : subjectCache.synchronous().asMap().values()) {
                Tristate permissionValue = subject.getPermissionValue(contexts == null ? subject.getActiveContexts() : contexts, permission);
                if (permissionValue != Tristate.UNDEFINED) {
                    ret.put(subject, permissionValue.asBoolean());
                }
        }
        return ret.build();
    }

    @Override
    public CompletableFuture<Map<SubjectReference, Boolean>> getAllWithPermission(String permission) {
        return getAllWithPermission(null, permission);
    }

    @Override
    public CompletableFuture<Map<SubjectReference, Boolean>> getAllWithPermission(@Nullable Set<Context> contexts,
            String permission) {
        Set<String> raw = this.collection.getAllIdentifiers();
        @SuppressWarnings({"unchecked", "rawtypes"})
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
                    return (CalculatedSubject) null;
                }
            }).map(subj -> {
                final int perm = subj.getPermission(contexts == null ? subj.getActiveContexts() : PEXSubjectData.contextsSpongeToPex(contexts, this.plugin.getManager()), permission);
                Boolean bPerm = null;
                if (perm > 0) {
                    bPerm = true;
                } else if (perm < 0) {
                    bPerm = false;
                }
                return Maps.immutableEntry((SubjectReference) subj.getIdentifier(), bPerm);
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
