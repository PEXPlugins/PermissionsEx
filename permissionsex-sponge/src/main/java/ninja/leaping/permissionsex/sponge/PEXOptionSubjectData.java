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
import com.google.common.util.concurrent.ListenableFuture;
import ninja.leaping.permissionsex.data.Caching;
import ninja.leaping.permissionsex.data.ImmutableSubjectData;
import ninja.leaping.permissionsex.data.SubjectCache;
import ninja.leaping.permissionsex.data.SubjectDataReference;
import ninja.leaping.permissionsex.util.NonNullFunction;
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
class PEXOptionSubjectData implements OptionSubjectData, Caching<ImmutableSubjectData> {
    private final PermissionsExPlugin plugin;
    private final String identifier;
    private SubjectDataReference data;
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

    private boolean wasSuccess(ListenableFuture<ImmutableSubjectData> future) {
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

    @Override
    public void clearCache(ImmutableSubjectData newData) {
        synchronized (parentsCache) {
            parentsCache.clear();
        }
    }

    void updateCache(SubjectCache newCache) throws ExecutionException {
        this.data = SubjectDataReference.forSubject(identifier, newCache, this);
        clearCache(this.data.get());
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
        return wasSuccess(data.update(new NonNullFunction<ImmutableSubjectData, ImmutableSubjectData>() {
            @Override
            public ImmutableSubjectData applyNonNull(ImmutableSubjectData input) {
                return input.setOption(parSet(contexts), key, value);
            }
        }));
    }

    @Override
    public boolean clearOptions(final Set<Context> contexts) {
        return wasSuccess(data.update(new NonNullFunction<ImmutableSubjectData, ImmutableSubjectData>() {
            @Override
            public ImmutableSubjectData applyNonNull(ImmutableSubjectData input) {
                return input.clearOptions(parSet(contexts));
            }
        }));
    }

    @Override
    public boolean clearOptions() {
        return wasSuccess(data.update(new NonNullFunction<ImmutableSubjectData, ImmutableSubjectData>() {
            @Override
            public ImmutableSubjectData applyNonNull(ImmutableSubjectData input) {
                return input.clearOptions();
            }
        }));
    }

    @Override
    public Map<Set<Context>, Map<String, Boolean>> getAllPermissions() {
        return Maps.transformValues(tKeys(data.get().getAllPermissions()), new Function<Map<String, Integer>, Map<String, Boolean>>() {
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
        return Maps.transformValues(data.get().getPermissions(parSet(set)), new Function<Integer, Boolean>() {
            @Nullable
            @Override
            public Boolean apply(@Nullable Integer integer) {
                return integer != null && integer > 0;
            }
        });
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

        return wasSuccess(data.update(new NonNullFunction<ImmutableSubjectData, ImmutableSubjectData>() {
            @Override
            public ImmutableSubjectData applyNonNull(ImmutableSubjectData input) {
                return input.setPermission(parSet(set), s, val);
            }
        }));
    }

    @Override
    public boolean clearPermissions() {
        return wasSuccess(data.update(new NonNullFunction<ImmutableSubjectData, ImmutableSubjectData>() {
            @Override
            public ImmutableSubjectData applyNonNull(ImmutableSubjectData input) {
                return input.clearPermissions();
            }
        }));
    }

    @Override
    public boolean clearPermissions(final Set<Context> set) {
        return wasSuccess(data.update(new NonNullFunction<ImmutableSubjectData, ImmutableSubjectData>() {
            @Override
            public ImmutableSubjectData applyNonNull(ImmutableSubjectData input) {
                return input.clearPermissions(parSet(set));
            }
        }));
    }

    @Override
    public Map<Set<Context>, List<Subject>> getAllParents() {
        synchronized (parentsCache) {
            for (Set<Map.Entry<String, String>> set : data.get().getActiveContexts()) {
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
                List<Map.Entry<String, String>> rawParents = data.get().getParents(set);
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
    public boolean addParent(final Set<Context> set, final Subject subject) {
        return wasSuccess(data.update(new NonNullFunction<ImmutableSubjectData, ImmutableSubjectData>() {
            @Override
            public ImmutableSubjectData applyNonNull(ImmutableSubjectData input) {
                return input.addParent(parSet(set), subject.getContainingCollection().getIdentifier(), subject.getIdentifier());
            }
        }));
    }

    @Override
    public boolean removeParent(final Set<Context> set, final Subject subject) {
        return wasSuccess(data.update(new NonNullFunction<ImmutableSubjectData, ImmutableSubjectData>() {
            @Override
            public ImmutableSubjectData applyNonNull(ImmutableSubjectData input) {
                return input.removeParent(parSet(set), subject.getContainingCollection().getIdentifier(), subject.getIdentifier());
            }
        }));
    }

    @Override
    public boolean clearParents() {
        return wasSuccess(data.update(new NonNullFunction<ImmutableSubjectData, ImmutableSubjectData>() {
            @Override
            public ImmutableSubjectData applyNonNull(ImmutableSubjectData input) {
                return input.clearParents();
            }
        }));
    }

    @Override
    public boolean clearParents(final Set<Context> set) {
        return wasSuccess(data.update(new NonNullFunction<ImmutableSubjectData, ImmutableSubjectData>() {
            @Override
            public ImmutableSubjectData applyNonNull(ImmutableSubjectData input) {
                return input.clearParents(parSet(set));
            }
        }));
    }
}
