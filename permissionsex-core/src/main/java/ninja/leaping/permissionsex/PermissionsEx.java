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
package ninja.leaping.permissionsex;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.sk89q.squirrelid.resolver.CacheForwardingService;
import com.sk89q.squirrelid.resolver.HttpRepositoryService;
import com.sk89q.squirrelid.resolver.ProfileService;
import ninja.leaping.permissionsex.backend.DataStore;
import ninja.leaping.permissionsex.backend.memory.MemoryDataStore;
import ninja.leaping.permissionsex.command.PermissionsExCommands;
import ninja.leaping.permissionsex.command.RankingCommands;
import ninja.leaping.permissionsex.config.PermissionsExConfiguration;
import ninja.leaping.permissionsex.data.CacheListenerHolder;
import ninja.leaping.permissionsex.data.Caching;
import ninja.leaping.permissionsex.logging.TranslatableLogger;
import ninja.leaping.permissionsex.subject.CalculatedSubject;
import ninja.leaping.permissionsex.data.ContextInheritance;
import ninja.leaping.permissionsex.data.ImmutableSubjectData;
import ninja.leaping.permissionsex.data.RankLadderCache;
import ninja.leaping.permissionsex.data.SubjectCache;
import ninja.leaping.permissionsex.subject.SubjectDataBaker;
import ninja.leaping.permissionsex.exception.PermissionsLoadingException;
import ninja.leaping.permissionsex.util.PEXProfileCache;
import ninja.leaping.permissionsex.util.Translatable;
import ninja.leaping.permissionsex.util.Util;
import ninja.leaping.permissionsex.util.command.CommandSpec;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.Function;

import static ninja.leaping.permissionsex.util.Translations.*;

public class PermissionsEx implements ImplementationInterface, Caching<ContextInheritance> {
    public static final String SUBJECTS_USER = "user";
    public static final String SUBJECTS_GROUP = "group";
    public static final ImmutableSet<Map.Entry<String, String>> GLOBAL_CONTEXT = ImmutableSet.of();

    private static final Map.Entry<String, String> DEFAULT_IDENTIFIER = Maps.immutableEntry("system", "default");
    private final PermissionsExConfiguration config;
    private final TranslatableLogger logger;
    private final ImplementationInterface impl;
    private DataStore activeDataStore;
    private final ConcurrentMap<String, SubjectCache> subjectCaches = new ConcurrentHashMap<>(), transientSubjectCaches = new ConcurrentHashMap<>();
    private final RankLadderCache rankLadderCache;
    private final LoadingCache<Map.Entry<String, String>, CalculatedSubject> calculatedSubjects = CacheBuilder.newBuilder().maximumSize(512).build(new CacheLoader<Map.Entry<String, String>, CalculatedSubject>() {
        @Override
        public CalculatedSubject load(Map.Entry<String, String> key) throws Exception {
            return new CalculatedSubject(SubjectDataBaker.inheritance(), key, PermissionsEx.this);
        }
    });
    private volatile ContextInheritance cachedInheritance;
    private final CacheListenerHolder<Boolean, ContextInheritance> cachedInheritanceListeners = new CacheListenerHolder<>();
    private final MemoryDataStore transientData;
    private ProfileService uuidService;
    private volatile boolean debug;

    public PermissionsEx(final PermissionsExConfiguration config, ImplementationInterface impl) throws PermissionsLoadingException {
        this.config = config;
        this.impl = impl;
        this.logger = TranslatableLogger.forLogger(impl.getLogger());
        this.debug = config.isDebugEnabled();
        this.uuidService = HttpRepositoryService.forMinecraft();
        this.transientData = new MemoryDataStore();
        this.transientData.initialize(this);
        this.activeDataStore = config.getDefaultDataStore();
        this.activeDataStore.initialize(this);
        getSubjects(SUBJECTS_GROUP).cacheAll();
        this.rankLadderCache = new RankLadderCache(this.activeDataStore);
        convertUuids();

        // Now that initialization is complete
        uuidService = new CacheForwardingService(uuidService, new PEXProfileCache(getSubjects(SUBJECTS_USER)));
        registerCommand(PermissionsExCommands.createRootCommand(this));
        registerCommand(RankingCommands.getPromoteCommand(this));
        registerCommand(RankingCommands.getDemoteCommand(this));
    }

    private void convertUuids() {
        try {
            InetAddress.getByName("api.mojang.com");
            this.activeDataStore.performBulkOperation(input -> {
                Iterable<String> toConvert = Iterables.filter(input.getAllIdentifiers(SUBJECTS_USER), input1 -> {
                    if (input1 == null || input1.length() != 36) {
                        return true;
                    }
                    try {
                        UUID.fromString(input1);
                        return false;
                    } catch (IllegalArgumentException e) {
                        return true;
                    }
                });
                if (toConvert.iterator().hasNext()) {
                    getLogger().info(t("Trying to convert users stored by name to UUID"));
                } else {
                    return 0;
                }

                final ProfileService service = HttpRepositoryService.forMinecraft();
                try {
                    final int[] converted = {0};
                    service.findAllByName(toConvert, profile -> {
                        final String newIdentifier = profile.getUniqueId().toString();
                        if (input.isRegistered(SUBJECTS_USER, newIdentifier)) {
                            getLogger().warn(t("Duplicate entry for %s found while converting to UUID", newIdentifier + "/" + profile.getName()));
                            return false; // We already have a registered UUID, this is a duplicate.
                        }

                        String lookupName = profile.getName();
                        if (!input.isRegistered(SUBJECTS_USER, lookupName)) {
                            lookupName = lookupName.toLowerCase();
                        }
                        if (!input.isRegistered(SUBJECTS_USER, lookupName)) {
                            return false;
                        }
                        converted[0]++;

                        ImmutableSubjectData oldData = input.getData(SUBJECTS_USER, profile.getName(), null);
                        final String finalLookupName = lookupName;
                        input.setData(SUBJECTS_USER, newIdentifier, oldData.setOption(ImmutableSet.<Map.Entry<String, String>>of(), "name", profile.getName()))
                                .thenAccept(result -> input.setData(SUBJECTS_USER, finalLookupName, null)
                                        .exceptionally(t -> {
                                            t.printStackTrace();
                                            return null;
                                        }));
                        return true;
                    });
                    return converted[0];
                } catch (IOException | InterruptedException e) {
                    getLogger().error(t("Error while fetching UUIDs for users"), e);
                    return 0;
                }
            }).thenAccept(result -> {
                    if (result != null && result > 0) {
                        getLogger().info(tn("%s user successfully converted from name to UUID",
                                "%s users successfully converted from name to UUID!",
                                result, result));
                    }
                }).exceptionally(t -> {
                getLogger().error(t("Error converting users to UUID"), t);
                return null;
                });
        } catch (UnknownHostException e) {
            getLogger().warn(t("Unable to resolve Mojang API for UUID conversion. Do you have an internet connection? UUID conversion will not proceed (but may not be necessary)."));
        }

    }

    public SubjectCache getSubjects(String type) {
        Preconditions.checkNotNull(type, "type");
        SubjectCache cache = subjectCaches.get(type);
        if (cache == null) {
            cache = new SubjectCache(type, activeDataStore);
            SubjectCache newCache = subjectCaches.putIfAbsent(type, cache);
            if (newCache != null) {
                cache = newCache;
            }
        }
        return cache;
    }

    public SubjectCache getTransientSubjects(String type) {
        Preconditions.checkNotNull(type, "type");
        SubjectCache cache = transientSubjectCaches.get(type);
        if (cache == null) {
            cache = new SubjectCache(type, transientData);
            SubjectCache newCache = transientSubjectCaches.putIfAbsent(type, cache);
            if (newCache != null) {
                cache = newCache;
            }
        }
        return cache;
    }

    public void uncache(String type, String identifier) {
        SubjectCache cache = subjectCaches.get(type);
        if (cache != null) {
            cache.invalidate(identifier);
        }
        cache = transientSubjectCaches.get(type);
        if (cache != null) {
            cache.invalidate(identifier);
        }
        calculatedSubjects.invalidate(Maps.immutableEntry(type, identifier));
    }

    /**
     * Access rank ladders through a cached interface
     *
     * @return Access to rank ladders
     */
    public RankLadderCache getLadders() {
        return this.rankLadderCache;
    }

    /**
     * Imports data into the currently active backend from the backend identified by the provided identifier
     *
     * @param dataStoreIdentifier The identifier of the backend to import from
     * @return A future that completes once the import operation is complete
     */
    public CompletableFuture<Void> importDataFrom(String dataStoreIdentifier) {
        final DataStore expected = config.getDataStore(dataStoreIdentifier);
        if (expected == null) {
            return Util.failedFuture(new IllegalArgumentException("Data store " + dataStoreIdentifier + " is not present"));
        }
        try {
            expected.initialize(this);
        } catch (PermissionsLoadingException e) {
            return Util.failedFuture(e);
        }

        return activeDataStore.performBulkOperation(store -> {
                CompletableFuture<Void> ret = CompletableFuture.allOf(Iterables.toArray(Iterables.transform(expected.getAll(), input -> {
                    return store.setData(input.getKey().getKey(), input.getKey().getValue(), input.getValue());
                }), CompletableFuture.class)).thenCombine(store.setContextInheritance(expected.getContextInheritance(null)), (v, a) -> null);
                for (String ladder : store.getAllRankLadders()) {
                    ret = ret.thenCombine(store.setRankLadder(ladder, expected.getRankLadder(ladder, null)), (v, a) -> null);
                }
            return ret;
        }).thenCompose(val -> Util.failableFuture(val::get));
    }

    public Set<String> getRegisteredSubjectTypes() {
        return this.activeDataStore.getRegisteredTypes();
    }

    public void setDebugMode(boolean debug) {
        this.debug = debug;
    }

    public boolean hasDebugMode() {
        return this.debug;
    }

    public void close() {
        this.activeDataStore.close();
    }

    @Override
    public File getBaseDirectory() {
        return impl.getBaseDirectory();
    }

    @Override
    public TranslatableLogger getLogger() {
        return this.logger;
    }

    @Override
    public DataSource getDataSourceForURL(String url) {
        return impl.getDataSourceForURL(url);
    }

    /**
     * Get an executor to run tasks asynchronously on.
     *
     * @return The async executor
     */
    @Override
    public Executor getAsyncExecutor() {
        return impl.getAsyncExecutor();
    }

    @Override
    public void registerCommand(CommandSpec command) {
        impl.registerCommand(command);
    }

    @Override
    public Set<CommandSpec> getImplementationCommands() {
        return impl.getImplementationCommands();
    }

    @Override
    public String getVersion() {
        return impl.getVersion();
    }

    @Override
    public Function<String, String> getNameTransformer(String type) {
        return impl.getNameTransformer(type);
    }


    public PermissionsExConfiguration getConfig() {
        return this.config;
    }

    /**
     * Get the identifier to access default subject data
     *
     * @return The identifier referring to default subject data
     */
    public Map.Entry<String, String> getDefaultIdentifier() {
        return DEFAULT_IDENTIFIER;
    }

    public CalculatedSubject getCalculatedSubject(String type, String identifier) throws PermissionsLoadingException {
        try {
            return calculatedSubjects.get(Maps.immutableEntry(type, identifier));
        } catch (ExecutionException e) {
            throw new PermissionsLoadingException(t("While calculating subject data for %s:%s", type, identifier), e);
        }
    }

    public Iterable<? extends CalculatedSubject> getActiveCalculatedSubjects() {
        return Collections.unmodifiableCollection(calculatedSubjects.asMap().values());
    }

    public ContextInheritance getContextInheritance(Caching<ContextInheritance> listener) {
        if (this.cachedInheritance == null) {
            this.cachedInheritance = this.activeDataStore.getContextInheritance(this);
        }
        if (listener != null) {
            this.cachedInheritanceListeners.addListener(true, listener);
        }
        return this.cachedInheritance;

    }

    public CompletableFuture<ContextInheritance> setContextInheritance(ContextInheritance newInheritance) {
        return this.activeDataStore.setContextInheritance(newInheritance);
    }

    @Override
    public void clearCache(ContextInheritance newData) {
        this.cachedInheritance = newData;
        this.cachedInheritanceListeners.call(true, newData);
    }
}
