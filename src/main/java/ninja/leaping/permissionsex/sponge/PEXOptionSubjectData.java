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
import com.google.common.collect.Maps;
import ninja.leaping.permissionsex.data.Caching;
import ninja.leaping.permissionsex.data.ImmutableOptionSubjectData;
import ninja.leaping.permissionsex.data.SubjectCache;
import ninja.leaping.permissionsex.sponge.option.OptionSubjectData;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.context.Context;
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
public class PEXOptionSubjectData implements OptionSubjectData, Caching {
    private final PermissionsExPlugin plugin;
    private final SubjectCache cache;
    private final String identifier;
    private volatile ImmutableOptionSubjectData data;
    private final ConcurrentMap<Set<Context>, List<Subject>> parentsCache = new ConcurrentHashMap<>();

    public PEXOptionSubjectData(SubjectCache cache, String identifier, PermissionsExPlugin plugin) throws ExecutionException {
        this.plugin = plugin;
        this.cache = cache;
        this.identifier = identifier;
        clearCache(cache.getData(identifier, this));
    }

    private boolean updateIfChanged(ImmutableOptionSubjectData old, ImmutableOptionSubjectData newData) {
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

    @Override
    public Map<Set<Context>, Map<String, String>> getAllOptions() {
        return this.data.getAllOptions();
    }

    @Override
    public Map<String, String> getOptions(Set<Context> contexts) {
        return this.data.getOptions(contexts);
    }

    @Override
    public boolean setOption(Set<Context> contexts, String key, String value) {
        return updateIfChanged(data, data.setOption(contexts, key, value));
    }

    @Override
    public boolean clearOptions(Set<Context> contexts) {
        return updateIfChanged(data, data.clearOptions(contexts));
    }

    @Override
    public boolean clearOptions() {
        return updateIfChanged(data, data.clearOptions());
    }

    @Override
    public Map<Set<Context>, Map<String, Boolean>> getAllPermissions() {
        return Maps.transformValues(data.getAllPermissions(), new Function<Map<String, Integer>, Map<String, Boolean>>() {
            @Nullable
            @Override
            public Map<String, Boolean> apply(Map<String, Integer> stringIntegerMap) {
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
        return Maps.transformValues(data.getPermissions(set), new Function<Integer, Boolean>() {
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

        return updateIfChanged(data, data.setPermission(set, s, val));
    }

    @Override
    public boolean clearPermissions() {
        return updateIfChanged(data, data.clearPermissions());
    }

    @Override
    public boolean clearPermissions(Set<Context> set) {
        return updateIfChanged(data, data.clearPermissions(set));
    }

    @Override
    public Map<Set<Context>, List<Subject>> getAllParents() {
        synchronized (parentsCache) {
            for (Set<Context> set : data.getActiveContexts()) {
                getParents(set);
            }
            return ImmutableMap.copyOf(parentsCache);
        }
    }

    @Override
    public List<Subject> getParents(Set<Context> set) {
        List<Subject> parents = parentsCache.get(set);
        if (parents == null) {
            synchronized (parentsCache) {
                List<Map.Entry<String, String>> rawParents = data.getParents(set);
                if (rawParents == null) {
                    parents = ImmutableList.of();
                } else {
                    parents = new ArrayList<>(rawParents.size());
                    for (Map.Entry<String, String> ent : rawParents) {
                        parents.add(plugin.getSubjects(ent.getKey()).get().get(ent.getValue()));
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
        return updateIfChanged(data, data.addParent(set, subject.getContainingCollection().getIdentifier(), subject.getIdentifier()));
    }

    @Override
    public boolean removeParent(Set<Context> set, Subject subject) {
        return updateIfChanged(data, data.removeParent(set, subject.getContainingCollection().getIdentifier(), subject.getIdentifier()));
    }

    @Override
    public boolean clearParents() {
        return updateIfChanged(data, data.clearParents());
    }

    @Override
    public boolean clearParents(Set<Context> set) {
        return updateIfChanged(data, data.clearParents(set));
    }

    public ImmutableOptionSubjectData getCurrent() {
        return this.data;
    }
}
