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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import ninja.leaping.permissionsex.data.Change;
import ninja.leaping.permissionsex.data.ImmutableSubjectData;
import ninja.leaping.permissionsex.data.SubjectCache;
import ninja.leaping.permissionsex.data.SubjectDataReference;
import ninja.leaping.permissionsex.util.GuavaCollectors;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.option.OptionSubjectData;
import org.spongepowered.api.util.Tristate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

/**
 * Wrapper around ImmutableSubjectData that writes to backend each change
 */
class PEXOptionSubjectData implements OptionSubjectData {
    private final PermissionsExPlugin plugin;
    private final String identifier;
    private SubjectDataReference data;
    private final ConcurrentMap<Set<Map.Entry<String, String>>, List<Subject>> parentsCache = new ConcurrentHashMap<>();

    public PEXOptionSubjectData(SubjectDataReference data, String identifier, PermissionsExPlugin plugin) throws ExecutionException {
        this.plugin = plugin;
        this.identifier = identifier;
        this.data = data;
        this.data.onUpdate(this::clearCache);
    }

    /**
     * This is valid because all Contexts are Map.Entries
     *
     * @param input The input set
     * @return A properly casted set
     */
    @SuppressWarnings("unchecked")
    static Set<Map.Entry<String, String>> parSet(Set<Context> input) {
        return (Set) input;
    }

    private static <T> Map<Set<Context>, T> tKeys(Map<Set<Map.Entry<String, String>>, T> input) {
        final ImmutableMap.Builder<Set<Context>, T> ret = ImmutableMap.builder();
        for (Map.Entry<Set<Map.Entry<String, String>>, T> ent : input.entrySet()) {
            ret.put(ent.getKey().stream()
                    .map(ctx -> ctx instanceof Context ? (Context) ctx : new Context(ctx.getKey(), ctx.getValue()))
                    .collect(GuavaCollectors.toImmutableSet()), ent.getValue());
        }
        return ret.build();
    }

    private boolean wasSuccess(CompletableFuture<Change<ImmutableSubjectData>> future) {
        if (future.isDone()) {
            try {
                future.get();
                return true;
            } catch (InterruptedException | ExecutionException e) {
                return false;
            }
        } else {
            return true;
        }
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
        return this.data.get().getOptions(parSet(contexts));
    }

    @Override
    public boolean setOption(final Set<Context> contexts, final String key, final String value) {
        return wasSuccess(data.update(input -> input.setOption(parSet(contexts), key, value)));
    }

    @Override
    public boolean clearOptions(final Set<Context> contexts) {
        return wasSuccess(data.update(input -> input.clearOptions(parSet(contexts))));
    }

    @Override
    public boolean clearOptions() {
        return wasSuccess(data.update(ImmutableSubjectData::clearOptions));
    }

    @Override
    public Map<Set<Context>, Map<String, Boolean>> getAllPermissions() {
        return Maps.transformValues(tKeys(data.get().getAllPermissions()),
                map -> Maps.transformValues(map, i -> i > 0));
    }

    @Override
    public Map<String, Boolean> getPermissions(Set<Context> set) {
        return Maps.transformValues(data.get().getPermissions(parSet(set)), value -> value > 0);
    }

    @Override
    public boolean setPermission(final Set<Context> set, final String s, Tristate tristate) {
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

        return wasSuccess(data.update(input -> input.setPermission(parSet(set), s, val)));
    }

    @Override
    public boolean clearPermissions() {
        return wasSuccess(data.update(ImmutableSubjectData::clearPermissions));
    }

    @Override
    public boolean clearPermissions(final Set<Context> set) {
        return wasSuccess(data.update(input -> input.clearPermissions(parSet(set))));
    }

    @Override
    public Map<Set<Context>, List<Subject>> getAllParents() {
        synchronized (parentsCache) {
            data.get().getActiveContexts().forEach(this::getParentsInternal);
            return tKeys(parentsCache);
        }
    }

    @Override
    public List<Subject> getParents(Set<Context> set) {
        return getParentsInternal(parSet(set));
    }

    public List<Subject> getParentsInternal(Set<Map.Entry<String, String>> set) {
        List<Subject> parents = parentsCache.get(set);
        if (parents == null) {
            synchronized (parentsCache) {
                List<Map.Entry<String, String>> rawParents = data.get().getParents(set);
                if (rawParents == null) {
                    parents = ImmutableList.of();
                } else {
                    parents = new ArrayList<>(rawParents.size());
                    for (Map.Entry<String, String> ent : rawParents) {
                        parents.add(plugin.getSubjects(ent.getKey()).get(ent.getValue())); // TODO: Parallelize
                    }
                }
                List<Subject> existingParents = parentsCache.putIfAbsent(set, parents);
                if (existingParents != null) {
                    parents = existingParents;
                }
            }
        }
        return parents;
    }

    @Override
    public boolean addParent(final Set<Context> set, final Subject subject) {
        return wasSuccess(data.update(input -> input.addParent(parSet(set), subject.getContainingCollection().getIdentifier(), subject.getIdentifier())));
    }

    @Override
    public boolean removeParent(final Set<Context> set, final Subject subject) {
        return wasSuccess(data.update(input -> input.removeParent(parSet(set), subject.getContainingCollection().getIdentifier(), subject.getIdentifier())));
    }

    @Override
    public boolean clearParents() {
        return wasSuccess(data.update(ImmutableSubjectData::clearParents));
    }

    @Override
    public boolean clearParents(final Set<Context> set) {
        return wasSuccess(data.update(input -> input.clearParents(parSet(set))));
    }
}
