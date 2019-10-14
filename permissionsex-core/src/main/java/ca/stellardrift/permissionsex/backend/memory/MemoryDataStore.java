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

package ca.stellardrift.permissionsex.backend.memory;

import ca.stellardrift.permissionsex.backend.AbstractDataStore;
import ca.stellardrift.permissionsex.backend.DataStore;
import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.data.ContextInheritance;
import ca.stellardrift.permissionsex.data.ImmutableSubjectData;
import ca.stellardrift.permissionsex.rank.FixedRankLadder;
import ca.stellardrift.permissionsex.rank.RankLadder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import kotlin.Pair;
import ninja.leaping.configurate.objectmapping.Setting;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * A data store backed entirely in memory
 */
public class MemoryDataStore extends AbstractDataStore {
    public static final Factory FACTORY = new Factory("memory", MemoryDataStore.class);

    @Setting(comment = "Whether or not this data store will store subjects being set")
    private boolean track = true;

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
    public Mono<ImmutableSubjectData> getDataInternal(String type, String identifier) {
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
        return Mono.just(ret);
    }

    @Override
    public Mono<ImmutableSubjectData> setDataInternal(String type, String identifier, ImmutableSubjectData data) {
        if (track) {
            this.data.put(Maps.immutableEntry(type, identifier), data);
        }
        return Mono.just(data);
    }

    @Override
    protected Mono<RankLadder> getRankLadderInternal(String name) {
        RankLadder ladder = rankLadders.get(name.toLowerCase());
        if (ladder == null) {
            ladder = new FixedRankLadder(name, ImmutableList.<Map.Entry<String, String>>of());
        }
        return Mono.just(ladder);
    }

    @Override
    protected Mono<RankLadder> setRankLadderInternal(String ladder, RankLadder newLadder) {
        this.rankLadders.put(ladder, newLadder);
        return Mono.just(newLadder);
    }

    private <T> CompletableFuture<T> completedFuture(T i) {
        return CompletableFuture.supplyAsync(() -> i, getManager().getAsyncExecutor());
    }

    @Override
    public Mono<Boolean> isRegistered(String type, String identifier) {
        return Mono.just(data.containsKey(Maps.immutableEntry(type, identifier)));
    }

    @Override
    public Flux<String> getAllIdentifiers(final String type) {
        return Flux.fromIterable(data.keySet())
                .filter(ent -> ent.getKey().equals(type))
                .map(Map.Entry::getValue);
    }

    @Override
    public Flux<String> getRegisteredTypes() {
        return Flux.fromIterable(data.keySet())
                .map(Map.Entry::getKey);
    }

    @Override
    public Flux<String> getDefinedContextKeys() {
        return Flux.fromIterable(data.values())
                .flatMap(data -> Flux.fromIterable(data.getActiveContexts()))
                .flatMap(Flux::fromIterable)
                .map(ContextValue::getKey);
    }

    @Override
    public Flux<Pair<Map.Entry<String, String>, ImmutableSubjectData>> getAll() {
        return Flux.fromIterable(data.entrySet())
                .map(mapEnt -> new Pair<>(mapEnt.getKey(), mapEnt.getValue()));
    }

    @Override
    public Flux<String> getAllRankLadders() {
        return Flux.fromIterable(rankLadders.keySet());
    }

    @Override
    public Mono<Boolean> hasRankLadder(String ladder) {
        return Mono.just(rankLadders.containsKey(ladder.toLowerCase()));
    }

    @Override
    public Mono<ContextInheritance> getContextInheritanceInternal() {
        return Mono.just(this.inheritance);
    }

    @Override
    public Mono<ContextInheritance> setContextInheritanceInternal(ContextInheritance inheritance) {
        this.inheritance = inheritance;
        return Mono.just(this.inheritance);
    }

    @Override
    protected <T> T performBulkOperationSync(Function<DataStore, T> function) {
        return function.apply(this);
    }
}
