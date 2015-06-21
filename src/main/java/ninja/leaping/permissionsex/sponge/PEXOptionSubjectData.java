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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import ninja.leaping.permissionsex.data.Caching;
import ninja.leaping.permissionsex.data.ImmutableOptionSubjectData;
import ninja.leaping.permissionsex.data.SubjectCache;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.context.Context;
import org.spongepowered.api.service.permission.option.OptionSubjectData;
import org.spongepowered.api.util.Tristate;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

/**
 * Wrapper around ImmutableSubjectData that writes to backend each change
 */
public class PEXOptionSubjectData implements OptionSubjectData, Caching<ImmutableOptionSubjectData> {
    private final PermissionsExPlugin plugin;
    private volatile SubjectCache cache;
    private final String identifier;
    private volatile ImmutableOptionSubjectData data;
    private final ConcurrentMap<Set<Map.Entry<String, String>>, List<Subject>> parentsCache = new ConcurrentHashMap<>();

    public PEXOptionSubjectData(SubjectCache cache, String identifier, PermissionsExPlugin plugin) throws ExecutionException {
        this.plugin = plugin;
        this.identifier = identifier;
        updateCache(cache);
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
            ret.put(ImmutableSet.copyOf(Iterables.transform(ent.getKey(), new Function<Map.Entry<String, String>, Context>() {
                @Nullable
                @Override
                public Context apply(@Nullable Map.Entry<String, String> input) {
                    if (input == null) {
                        return null;
                    }

                    return input instanceof Context ? (Context) input : new Context(input.getKey(), input.getValue());
                }
            })), ent.getValue());
        }
        return ret.build();
    }

    private boolean updateIfChanged(ImmutableOptionSubjectData old, @Nullable ImmutableOptionSubjectData newData) {
        if (newData == null) {
            return false; // Change unsuccessful
        } else if (old == newData) {
            return false; // Nothing to do?
        }

        cache.update(identifier, newData);
        return true;
    }

    @Override
    public void clearCache(ImmutableOptionSubjectData newData) {
        synchronized (parentsCache) {
            this.data = newData;
            parentsCache.clear();
        }
    }

    void updateCache(SubjectCache newCache) throws ExecutionException {
        this.cache = newCache;
        clearCache(newCache.getData(identifier, this));
    }

    @Override
    public Map<Set<Context>, Map<String, String>> getAllOptions() {
        return tKeys(this.data.getAllOptions());
    }

    @Override
    public Map<String, String> getOptions(Set<Context> contexts) {
        return this.data.getOptions(parSet(contexts));
    }

    @Override
    public boolean setOption(Set<Context> contexts, String key, String value) {
        return updateIfChanged(data, data.setOption(parSet(contexts), key, value));
    }

    @Override
    public boolean clearOptions(Set<Context> contexts) {
        return updateIfChanged(data, data.clearOptions(parSet(contexts)));
    }

    @Override
    public boolean clearOptions() {
        return updateIfChanged(data, data.clearOptions());
    }

    @Override
    public Map<Set<Context>, Map<String, Boolean>> getAllPermissions() {
        return Maps.transformValues(tKeys(data.getAllPermissions()), new Function<Map<String, Integer>, Map<String, Boolean>>() {
            @Nullable
            @Override
            public Map<String, Boolean> apply(@Nullable Map<String, Integer> stringIntegerMap) {
                if (stringIntegerMap == null) {
                    return ImmutableMap.of();
                }

                return Maps.transformValues(stringIntegerMap, new Function<Integer, Boolean>() {
                    @Nullable
                    @Override
                    public Boolean apply(@Nullable Integer integer) {
                        return integer != null && integer > 0;
                    }
                });
            }
        });
    }

    @Override
    public Map<String, Boolean> getPermissions(Set<Context> set) {
        return Maps.transformValues(data.getPermissions(parSet(set)), new Function<Integer, Boolean>() {
            @Nullable
            @Override
            public Boolean apply(@Nullable Integer integer) {
                return integer != null && integer > 0;
            }
        });
    }

    @Override
    public boolean setPermission(Set<Context> set, String s, Tristate tristate) {
        int val;
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

        return updateIfChanged(data, data.setPermission(parSet(set), s, val));
    }

    @Override
    public boolean clearPermissions() {
        return updateIfChanged(data, data.clearPermissions());
    }

    @Override
    public boolean clearPermissions(Set<Context> set) {
        return updateIfChanged(data, data.clearPermissions(parSet(set)));
    }

    @Override
    public Map<Set<Context>, List<Subject>> getAllParents() {
        synchronized (parentsCache) {
            for (Set<Map.Entry<String, String>> set : data.getActiveContexts()) {
                getParentsInternal(set);
            }
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
                List<Map.Entry<String, String>> rawParents = data.getParents(set);
                if (rawParents == null) {
                    parents = ImmutableList.of();
                } else {
                    parents = new ArrayList<>(rawParents.size());
                    for (Map.Entry<String, String> ent : rawParents) {
                        parents.add(plugin.getSubjects(ent.getKey()).get(ent.getValue()));
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
    public boolean addParent(Set<Context> set, Subject subject) {
        return updateIfChanged(data, data.addParent(parSet(set), subject.getContainingCollection().getIdentifier(), subject.getIdentifier()));
    }

    @Override
    public boolean removeParent(Set<Context> set, Subject subject) {
        return updateIfChanged(data, data.removeParent(parSet(set), subject.getContainingCollection().getIdentifier(), subject.getIdentifier()));
    }

    @Override
    public boolean clearParents() {
        return updateIfChanged(data, data.clearParents());
    }

    @Override
    public boolean clearParents(Set<Context> set) {
        return updateIfChanged(data, data.clearParents(parSet(set)));
    }

    public ImmutableOptionSubjectData getCurrent() {
        return this.data;
    }
}
