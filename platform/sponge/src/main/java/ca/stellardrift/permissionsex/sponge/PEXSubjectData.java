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

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import ca.stellardrift.permissionsex.PermissionsEx;
import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.context.ContextDefinition;
import ca.stellardrift.permissionsex.data.Change;
import ca.stellardrift.permissionsex.data.ImmutableSubjectData;
import ca.stellardrift.permissionsex.data.SubjectDataReference;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.SubjectReference;
import org.spongepowered.api.util.Tristate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Wrapper around ImmutableSubjectData that writes to backend each change
 */
class PEXSubjectData implements SubjectData {
    private final PermissionsExPlugin plugin;
    private SubjectDataReference data;
    private final ConcurrentMap<Set<ContextValue<?>>, List<SubjectReference>> parentsCache = new ConcurrentHashMap<>();

    PEXSubjectData(SubjectDataReference data, PermissionsExPlugin plugin) {
        this.plugin = plugin;
        this.data = data;
        this.data.onUpdate(this::clearCache);
    }

    static Set<Context> contextsPexToSponge(Set<ContextValue<?>> input) {
        Set<Context> build = new HashSet<>();
        for (ContextValue<?> ctx : input) {
            build.add(new Context(ctx.getKey(), ctx.getRawValue()));
        }
        return build;
    }

    @Nullable
    private static <T> ContextValue<T> singleContextToPex(Context ctx, ContextDefinition<T> def) {
        T val = def.deserialize(ctx.getValue());
        return val == null ? null : def.createValue(val);
    }

    static Set<ContextValue<?>> contextsSpongeToPex(Set<Context> input, PermissionsEx<?> manager) {
       ImmutableSet.Builder<ContextValue<?>> builder = ImmutableSet.builder();
       for (Context ctx : input) {
           ContextDefinition<?> def = manager.getContextDefinition(ctx.getKey(), true);
           if (def == null) {
               throw new IllegalStateException("A fallback context value was expected!");
           }
           ContextValue<?> ctxVal = singleContextToPex(ctx, def);
           if (ctxVal != null) {
               builder.add(ctxVal);
           }
       }
       return builder.build();
    }

    private static <T> Map<Set<Context>, T> tKeys(Map<Set<ContextValue<?>>, T> input) {
        final ImmutableMap.Builder<Set<Context>, T> ret = ImmutableMap.builder();
        for (Map.Entry<Set<ContextValue<?>>, T> ent : input.entrySet()) {
            ret.put(contextsPexToSponge(ent.getKey()), ent.getValue());
        }
        return ret.build();
    }

    /**
     * Provide a boolean representation of success for the Sponge returns.
     * 
     * @param future The PEX-internal result
     * @return Whether or not the old data object is different from the new data object
     */
    private CompletableFuture<Boolean> boolSuccess(CompletableFuture<Change<ImmutableSubjectData>> future) {
        return future.thenApply(chg -> !Objects.equal(chg.getOld(), chg.getNew()));
    }

    private void clearCache(ImmutableSubjectData newData) {
        synchronized (parentsCache) {
            parentsCache.clear();
        }
    }

    @Override
    public Map<Set<Context>, Map<String, String>> getAllOptions() {
        return tKeys(this.data.get().getAllOptions());
    }

    @Override
    public Map<String, String> getOptions(Set<Context> contexts) {
        return this.data.get().getOptions(contextsSpongeToPex(contexts, plugin.getManager()));
    }

    @Override
    public CompletableFuture<Boolean> setOption(final Set<Context> contexts, final String key, @Nullable final String value) {
        return boolSuccess(data.update(input -> input.setOption(contextsSpongeToPex(contexts, plugin.getManager()), key, value)));
    }

    @Override
    public CompletableFuture<Boolean> clearOptions(final Set<Context> contexts) {
        return boolSuccess(data.update(input -> input.clearOptions(contextsSpongeToPex(contexts, plugin.getManager()))));
    }

    @Override
    public CompletableFuture<Boolean> clearOptions() {
        return boolSuccess(data.update(ImmutableSubjectData::clearOptions));
    }

    @Override
    public Map<Set<Context>, Map<String, Boolean>> getAllPermissions() {
        return Maps.transformValues(tKeys(data.get().getAllPermissions()),
                map -> Maps.transformValues(map, i -> i > 0));
    }

    @Override
    public Map<String, Boolean> getPermissions(Set<Context> contexts) {
        return Maps.transformValues(data.get().getPermissions(contextsSpongeToPex(contexts, plugin.getManager())), i -> i > 0);
    }

    @Override
    public CompletableFuture<Boolean> setPermission(final Set<Context> set, final String s, Tristate tristate) {
        final int val;
        switch (tristate) {
            case TRUE:
                val = 1;
                break;
            case FALSE:
                val = -1;
                break;
            case UNDEFINED:
                val = 0;
                break;
            default:
                throw new IllegalStateException("Unknown tristate provided " + tristate);
        }

        return data.update(input -> input.setPermission(contextsSpongeToPex(set, plugin.getManager()), s, val)).thenApply(x -> true);
    }

    @Override
    public CompletableFuture<Boolean> clearPermissions() {
        return boolSuccess(data.update(ImmutableSubjectData::clearPermissions));
    }

    @Override
    public CompletableFuture<Boolean> clearPermissions(final Set<Context> set) {
        return boolSuccess(data.update(input -> input.clearPermissions(contextsSpongeToPex(set, plugin.getManager()))));
    }

    @Override
    public Map<Set<Context>, List<SubjectReference>> getAllParents() {
        synchronized (parentsCache) {
            data.get().getActiveContexts().forEach(this::getParentsInternal);
            return tKeys(parentsCache);
        }
    }

    @Override
    public List<SubjectReference> getParents(Set<Context> set) {
        return getParentsInternal(contextsSpongeToPex(set, plugin.getManager()));
    }

    private List<SubjectReference> getParentsInternal(Set<ContextValue<?>> set) {
        List<SubjectReference> parents = parentsCache.get(set);
        if (parents == null) {
            synchronized (parentsCache) {
                List<Map.Entry<String, String>> rawParents = data.get().getParents(set);
                if (rawParents == null) {
                    parents = ImmutableList.of();
                } else {
                    parents = new ArrayList<>(rawParents.size());
                    for (Map.Entry<String, String> ent : rawParents) {
                        parents.add(plugin.newSubjectReference(ent.getKey(), ent.getValue()));
                    }
                }
                List<SubjectReference> existingParents = parentsCache.putIfAbsent(set, parents);
                if (existingParents != null) {
                    parents = existingParents;
                }
            }
        }
        return parents;
    }

    @Override
    public CompletableFuture<Boolean> addParent(final Set<Context> set, final SubjectReference subject) {
        PEXSubjectReference.of(subject, plugin); // validate subject reference
        return boolSuccess(data.update(input -> input.addParent(contextsSpongeToPex(set, plugin.getManager()), subject.getCollectionIdentifier(), subject.getSubjectIdentifier())));
    }

    @Override
    public CompletableFuture<Boolean> removeParent(final Set<Context> set, final SubjectReference subject) {
        return boolSuccess(data.update(input -> input.removeParent(contextsSpongeToPex(set, plugin.getManager()), subject.getCollectionIdentifier(), subject.getSubjectIdentifier())));
    }

    @Override
    public CompletableFuture<Boolean> clearParents() {
        return boolSuccess(data.update(ImmutableSubjectData::clearParents));
    }

    @Override
    public CompletableFuture<Boolean> clearParents(final Set<Context> set) {
        return boolSuccess(data.update(input -> input.clearParents(contextsSpongeToPex(set, plugin.getManager()))));
    }
}
