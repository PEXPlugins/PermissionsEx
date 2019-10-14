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

package ca.stellardrift.permissionsex.backend;

import ca.stellardrift.permissionsex.PermissionsEx;
import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.data.CacheListenerHolder;
import ca.stellardrift.permissionsex.data.ContextInheritance;
import ca.stellardrift.permissionsex.data.ImmutableSubjectData;
import ca.stellardrift.permissionsex.exception.PermissionsLoadingException;
import ca.stellardrift.permissionsex.rank.RankLadder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import kotlin.Pair;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMapper;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

import static ca.stellardrift.permissionsex.util.Translations.t;

/**
 * Base implementation of a data store that provides common points for other data stores to hook into.
 */
@NonNull
public abstract class AbstractDataStore implements DataStore {
    private PermissionsEx manager;
    private final Factory factory;
    protected final CacheListenerHolder<Map.Entry<String, String>, ImmutableSubjectData> listeners = new CacheListenerHolder<>();
    protected final CacheListenerHolder<String, RankLadder> rankLadderListeners = new CacheListenerHolder<>();
    protected final CacheListenerHolder<Boolean, ContextInheritance> contextInheritanceListeners = new CacheListenerHolder<>();

    protected AbstractDataStore(Factory factory) {
        if (!factory.expectedClazz.equals(getClass())) {
            throw new ExceptionInInitializerError("Data store factory for wrong class " + factory.expectedClazz + " provided to a " + getClass());
        }
        this.factory = factory;
    }

    protected PermissionsEx getManager() {
        return this.manager;
    }

    @Override
    public final boolean initialize(PermissionsEx core) throws PermissionsLoadingException {
        this.manager = core;
        initializeInternal();
        return true;
    }

    protected abstract void initializeInternal() throws PermissionsLoadingException;

    @Override
    public final Mono<ImmutableSubjectData> getData(String type, String identifier, Consumer<ImmutableSubjectData> listener) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(identifier, "identifier");

        Mono<ImmutableSubjectData> ret = getDataInternal(type, identifier);
        return ret.doOnSuccess(data -> {
            if (listener != null) {
                listeners.addListener(Maps.immutableEntry(type, identifier), listener);
            }
        });
    }

    @Override
    public final Mono<ImmutableSubjectData> setData(String type, String identifier, ImmutableSubjectData data) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(identifier, "identifier");

        final Map.Entry<String, String> lookupKey = Maps.immutableEntry(type, identifier);
        return setDataInternal(type, identifier, data)
                .doOnSuccess(newData -> {
                    if (newData != null) {
                        listeners.call(lookupKey, newData);
                    }
                });
    }

    protected <T> Mono<T> runAsync(Callable<T> func) {
        return Mono.fromCallable(func)
                .subscribeOn(getManager().getScheduler());
    }

    /**
     * Apply default data when creating a new file.
     *
     * This consists of
     * <ul>
     *     <li>Modifying default data to give all permissions to a user connecting locally</li>
     * </ul>
     */
    protected final void applyDefaultData() {
        getData(PermissionsEx.SUBJECTS_DEFAULTS, PermissionsEx.SUBJECTS_DEFAULTS, null)
                .subscribe(data -> setData(PermissionsEx.SUBJECTS_DEFAULTS, PermissionsEx.SUBJECTS_DEFAULTS,
                        data.setDefaultValue(ImmutableSet.of(new ContextValue<>("localip", "127.0.0.1")), 1)
                        )).dispose();
    }

    protected abstract Mono<ImmutableSubjectData> getDataInternal(String type, String identifier);

    protected abstract Mono<ImmutableSubjectData> setDataInternal(String type, String identifier, ImmutableSubjectData data);

    @Override
    public final Flux<Pair<String, ImmutableSubjectData>> getAll(final String type) {
        Objects.requireNonNull(type, "type");
        return getAllIdentifiers(type).flatMap(ident -> {
            return getData(type, ident, null).map(data -> new Pair<>(ident, data));
        });
    }

    @Override
    public final <T, P extends Publisher<T>> P performBulkOperation(final Function<DataStore, P> function) {
        return function.apply(this);
        // return Util.asyncFailableFuture(() -> performBulkOperationSync(function), getManager().getAsyncExecutor()); // TODO  fix this?
    }

    @Override
    public final Mono<RankLadder> getRankLadder(String ladderName, Consumer<RankLadder> listener) {
        Objects.requireNonNull(ladderName, "ladderName");
        Mono<RankLadder> ret = getRankLadderInternal(ladderName).cache();
        return ret.doOnSuccess(ladder -> {
            rankLadderListeners.addListener(ladderName.toLowerCase(), listener);
        });
    }

    @Override
    public final Mono<RankLadder> setRankLadder(final String identifier, RankLadder ladder) {
        Mono<RankLadder> ret = setRankLadderInternal(identifier, ladder).cache();
        return ret.doOnSuccess(newData -> {
            if (newData != null) {
                rankLadderListeners.call(identifier, newData);
            }
        });
    }


    protected abstract Mono<RankLadder> getRankLadderInternal(String ladder);
    protected abstract Mono<RankLadder> setRankLadderInternal(String ladder, RankLadder newLadder);

    @Override
    public final Mono<ContextInheritance> getContextInheritance(Consumer<ContextInheritance> listener) {
        Mono<ContextInheritance> inheritance = getContextInheritanceInternal().cache();
        if (listener != null) {
            contextInheritanceListeners.addListener(true, listener);
        }
        return inheritance;
    }


    @Override
    public final Mono<ContextInheritance> setContextInheritance(ContextInheritance contextInheritance) {
        return setContextInheritanceInternal(contextInheritance).doOnSuccess(newData -> {
            if (newData != null) {
                contextInheritanceListeners.call(true, newData);
            }
        });
    }

    protected abstract Mono<ContextInheritance> getContextInheritanceInternal();
    protected abstract Mono<ContextInheritance> setContextInheritanceInternal(ContextInheritance contextInheritance);

    /**
     * Internally perform a bulk operation. Safe to call blocking operations from this method -- we're running it asyncly.
     *
     * @param function The function to run
     * @param <T> The
     * @return
     * @throws Exception
     */
    protected abstract <T> T performBulkOperationSync(Function<DataStore, T> function) throws Exception;

    @Override
    @SuppressWarnings("unchecked") // Correct types are verified in the constructor
    public String serialize(ConfigurationNode node) throws PermissionsLoadingException {
        Objects.requireNonNull(node, "node");
        try {
            ((ObjectMapper) factory.mapper).bind(this).serialize(node);
        } catch (ObjectMappingException e) {
            throw new PermissionsLoadingException(t("Error while serializing backend %s", node.getKey()), e);
        }
        return factory.type;
    }

    public static class Factory implements DataStoreFactory {
        private final String type;
        private final Class<? extends AbstractDataStore> expectedClazz;
        private final ObjectMapper<? extends AbstractDataStore> mapper;

        public Factory(final String type, Class<? extends AbstractDataStore> clazz) {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(clazz, "clazz");
            this.type = type;
            this.expectedClazz = clazz;
            try {
                mapper = ObjectMapper.forClass(clazz);
            } catch (ObjectMappingException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        @Override
        public DataStore createDataStore(String identifier, ConfigurationNode config) throws PermissionsLoadingException {
            try {
                return mapper.bindToNew().populate(config);
            } catch (ObjectMappingException e) {
                throw new PermissionsLoadingException(t("Error while deserializing backend %s", identifier), e);
            }
        }
    }
}
