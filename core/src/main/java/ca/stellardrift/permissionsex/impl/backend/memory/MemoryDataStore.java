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
package ca.stellardrift.permissionsex.impl.backend.memory;

import ca.stellardrift.permissionsex.impl.PermissionsEx;
import ca.stellardrift.permissionsex.impl.backend.AbstractDataStore;
import ca.stellardrift.permissionsex.impl.config.FilePermissionsExConfiguration;
import ca.stellardrift.permissionsex.datastore.DataStore;
import ca.stellardrift.permissionsex.datastore.DataStoreFactory;
import ca.stellardrift.permissionsex.datastore.StoreProperties;
import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.context.ContextInheritance;
import ca.stellardrift.permissionsex.exception.PermissionsLoadingException;
import ca.stellardrift.permissionsex.impl.util.PCollections;
import ca.stellardrift.permissionsex.subject.ImmutableSubjectData;
import ca.stellardrift.permissionsex.impl.rank.FixedRankLadder;
import ca.stellardrift.permissionsex.rank.RankLadder;
import ca.stellardrift.permissionsex.subject.SubjectRef;
import com.google.auto.service.AutoService;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;
import org.spongepowered.configurate.util.UnmodifiableCollections;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.spongepowered.configurate.util.UnmodifiableCollections.immutableMapEntry;

/**
 * A data store backed entirely in memory
 */
public class MemoryDataStore extends AbstractDataStore<MemoryDataStore, MemoryDataStore.Config> {
    @ConfigSerializable
    static class Config {
        @Setting
        @Comment("Whether or not this data store will store subjects being set")
        boolean track = true;
    }

    @AutoService(DataStoreFactory.class)
    public static final class Factory extends AbstractDataStore.Factory<MemoryDataStore, Config> {
        static final String TYPE = "memory";

        public Factory() {
            super(TYPE, Config.class, MemoryDataStore::new);
        }
    }


    private final ConcurrentMap<Map.Entry<String, String>, ImmutableSubjectData> data = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, RankLadder> rankLadders = new ConcurrentHashMap<>();
    private volatile ContextInheritance inheritance = new MemoryContextInheritance();

    public static MemoryDataStore create(final String identifier) {
        try {
            return (MemoryDataStore) DataStoreFactory.forType(Factory.TYPE).create(identifier, BasicConfigurationNode.root(FilePermissionsExConfiguration.PEX_OPTIONS));
        } catch (final PermissionsLoadingException ex) {
            // Not possible to have loading errors when we're not loading anything
            throw new RuntimeException(ex);
        }
    }

    public MemoryDataStore(final StoreProperties<Config> properties) {
        super(properties);
    }

    @Override
    protected boolean initializeInternal() {
        return false; // we never have any starting data
    }

    @Override
    public void close() {
    }

    @Override
    public CompletableFuture<ImmutableSubjectData> getDataInternal(final String type, final String identifier) {
        final Map.Entry<String, String> key = immutableMapEntry(type, identifier);
        ImmutableSubjectData ret = data.get(key);
        if (ret == null) {
            ret = new MemorySubjectData();
            if (config().track) {
                final @Nullable ImmutableSubjectData existingData = data.putIfAbsent(key, ret);
                if (existingData != null) {
                    ret = existingData;
                }
            }
        }
        return completedFuture(ret);
    }

    @Override
    public CompletableFuture<ImmutableSubjectData> setDataInternal(final String type, final String identifier, final ImmutableSubjectData data) {
        if (config().track) {
            this.data.put(immutableMapEntry(type, identifier), data);
        }
        return completedFuture(data);
    }

    @Override
    protected CompletableFuture<RankLadder> getRankLadderInternal(String name) {
        RankLadder ladder = rankLadders.get(name.toLowerCase());
        if (ladder == null) {
            ladder = new FixedRankLadder(name, PCollections.vector());
        }
        return completedFuture(ladder);
    }

    @Override
    protected CompletableFuture<RankLadder> setRankLadderInternal(String ladder, RankLadder newLadder) {
        this.rankLadders.put(ladder, newLadder);
        return completedFuture(newLadder);
    }

    private <T> CompletableFuture<T> completedFuture(T i) {
        return CompletableFuture.supplyAsync(() -> i, getManager().asyncExecutor());
    }

    @Override
    public CompletableFuture<Boolean> isRegistered(String type, String identifier) {
        return completedFuture(data.containsKey(immutableMapEntry(type, identifier)));
    }

    @Override
    public Stream<String> getAllIdentifiers(final String type) {
        return data.keySet().stream()
                .filter(inp -> inp.getKey().equals(type))
                .map(Map.Entry::getValue);
    }

    @Override
    public Set<String> getRegisteredTypes() {
        return this.data.keySet().stream()
                .map(Map.Entry::getKey)
                .collect(PCollections.toPSet());
    }

    @Override
    public CompletableFuture<Set<String>> getDefinedContextKeys() {
        return CompletableFuture.completedFuture(data.values().stream()
                .flatMap(data -> data.activeContexts().stream())
                .flatMap(Collection::stream)
                .map(ContextValue::key)
                .collect(Collectors.toSet()));
    }

    @Override
    public Stream<Map.Entry<SubjectRef<?>, ImmutableSubjectData>> getAll() {
        return this.data.entrySet().stream()
                .map(entry -> immutableMapEntry(((PermissionsEx<?>) this.getManager()).deserializeSubjectRef(entry.getKey()), entry.getValue()));
    }

    @Override
    public Stream<String> getAllRankLadders() {
        return this.rankLadders.keySet().stream();
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
    public CompletableFuture<ContextInheritance> setContextInheritanceInternal(final ContextInheritance inheritance) {
        this.inheritance = inheritance;
        return completedFuture(this.inheritance);
    }

    @Override
    protected <T> T performBulkOperationSync(Function<DataStore, T> function) {
        return function.apply(this);
    }
}
