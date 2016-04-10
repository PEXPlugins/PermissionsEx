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
package ninja.leaping.permissionsex.backend.memory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.permissionsex.backend.AbstractDataStore;
import ninja.leaping.permissionsex.backend.DataStore;
import ninja.leaping.permissionsex.data.ContextInheritance;
import ninja.leaping.permissionsex.data.ImmutableSubjectData;
import ninja.leaping.permissionsex.rank.FixedRankLadder;
import ninja.leaping.permissionsex.rank.RankLadder;
import ninja.leaping.permissionsex.util.GuavaCollectors;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * A data store backed entirely in memory
 */
public class MemoryDataStore extends AbstractDataStore {
    public static final Factory FACTORY = new Factory("memory", MemoryDataStore.class);

    @Setting(comment = "Whether or not this data store will store subjects being set") private boolean track = true;

    private final ConcurrentMap<Map.Entry<String, String>, ImmutableSubjectData> data = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, RankLadder> rankLadders = new ConcurrentHashMap<>();
    private volatile ContextInheritance inheritance = new MemoryContextInheritance();

    public MemoryDataStore() {
        super(FACTORY);
    }

    @Override
    protected void initializeInternal() {
    }

    @Override
    public void close() {
    }

    @Override
    public CompletableFuture<ImmutableSubjectData> getDataInternal(String type, String identifier) {
        final Map.Entry<String, String> key = Maps.immutableEntry(type, identifier);
        ImmutableSubjectData ret = data.get(key);
        if (ret == null) {
            ret = new MemorySubjectData();
            if (track) {
                final ImmutableSubjectData existingData = data.putIfAbsent(key, ret);
                if (existingData != null) {
                    ret = existingData;
                }
            }
        }
        return completedFuture(ret);
    }

    @Override
    public CompletableFuture<ImmutableSubjectData> setDataInternal(String type, String identifier, ImmutableSubjectData data) {
        if (track) {
            this.data.put(Maps.immutableEntry(type, identifier), data);
        }

        return completedFuture(data);
    }

    @Override
    protected CompletableFuture<RankLadder> getRankLadderInternal(String name) {
        RankLadder ladder = rankLadders.get(name.toLowerCase());
        if (ladder == null) {
            ladder = new FixedRankLadder(name, ImmutableList.<Map.Entry<String, String>>of());
        }
        return completedFuture(ladder);
    }

    @Override
    protected CompletableFuture<RankLadder> setRankLadderInternal(String ladder, RankLadder newLadder) {
        this.rankLadders.put(ladder, newLadder);
        return completedFuture(newLadder);
    }

    @Override
    public CompletableFuture<Boolean> isRegistered(String type, String identifier) {
        return completedFuture(data.containsKey(Maps.immutableEntry(type, identifier)));
    }

    @Override
    public Set<String> getAllIdentifiers(final String type) {
        return data.keySet().stream()
                .filter(inp -> inp.getKey().equals(type))
                .map(Map.Entry::getValue)
                .collect(GuavaCollectors.toImmutableSet());
    }

    @Override
    public Set<String> getRegisteredTypes() {
        return ImmutableSet.copyOf(Iterables.transform(data.keySet(), Map.Entry::getKey));
    }

    @Override
    public Iterable<Map.Entry<Map.Entry<String, String>, ImmutableSubjectData>> getAll() {
        return Iterables.unmodifiableIterable(data.entrySet());
    }

    @Override
    public Iterable<String> getAllRankLadders() {
        return ImmutableSet.copyOf(rankLadders.keySet());
    }

    @Override
    public CompletableFuture<Boolean> hasRankLadder(String ladder) {
        return completedFuture(rankLadders.containsKey(ladder.toLowerCase()));
    }

    @Override
    public CompletableFuture<ContextInheritance> getContextInheritanceInternal() {
        return completedFuture(this.inheritance);
    }

    @Override
    public CompletableFuture<ContextInheritance> setContextInheritanceInternal(ContextInheritance inheritance) {
        this.inheritance = inheritance;
        return completedFuture(this.inheritance);
    }

    @Override
    protected  <T> T performBulkOperationSync(Function<DataStore, T> function) {
        return function.apply(this);
    }
}
