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

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.sk89q.squirrelid.Profile;
import com.sk89q.squirrelid.resolver.CacheForwardingService;
import com.sk89q.squirrelid.resolver.HttpRepositoryService;
import com.sk89q.squirrelid.resolver.ProfileService;
import ninja.leaping.permissionsex.backend.DataStore;
import ninja.leaping.permissionsex.backend.memory.MemoryDataStore;
import ninja.leaping.permissionsex.command.PermissionsExCommands;
import ninja.leaping.permissionsex.command.RankingCommands;
import ninja.leaping.permissionsex.config.PermissionsExConfiguration;
import ninja.leaping.permissionsex.data.Caching;
import ninja.leaping.permissionsex.data.CalculatedSubject;
import ninja.leaping.permissionsex.data.ImmutableOptionSubjectData;
import ninja.leaping.permissionsex.data.RankLadderCache;
import ninja.leaping.permissionsex.data.SubjectCache;
import ninja.leaping.permissionsex.exception.PermissionsLoadingException;
import ninja.leaping.permissionsex.rank.RankLadder;
import ninja.leaping.permissionsex.util.PEXProfileCache;
import ninja.leaping.permissionsex.util.Translatable;
import ninja.leaping.permissionsex.util.command.CommandSpec;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import static ninja.leaping.permissionsex.util.Translations.*;

public class PermissionsEx implements ImplementationInterface {
    private static final Map.Entry<String, String> DEFAULT_IDENTIFIER = Maps.immutableEntry("system", "default");
    private final PermissionsExConfiguration config;
    private final ImplementationInterface impl;
    private DataStore activeDataStore;
    private final ConcurrentMap<String, SubjectCache> subjectCaches = new ConcurrentHashMap<>(), transientSubjectCaches = new ConcurrentHashMap<>();
    private final RankLadderCache rankLadderCache;
    private final LoadingCache<Map.Entry<String, String>, CalculatedSubject> calculatedSubjects = CacheBuilder.newBuilder().maximumSize(512).build(new CacheLoader<Map.Entry<String, String>, CalculatedSubject>() {
        @Override
        public CalculatedSubject load(Map.Entry<String, String> key) throws Exception {
            return new CalculatedSubject(key, PermissionsEx.this);
        }
    });
    private final MemoryDataStore transientData;
    private ProfileService uuidService;
    private volatile boolean debug;

    private static String fLog(Translatable trans) {
        return trans.translateFormatted(Locale.getDefault());
    }

    public PermissionsEx(final PermissionsExConfiguration config, ImplementationInterface impl) throws PermissionsLoadingException {
        this.config = config;
        this.impl = impl;
        this.debug = config.isDebugEnabled();
        this.uuidService = HttpRepositoryService.forMinecraft();
        this.transientData = new MemoryDataStore();
        this.transientData.initialize(this);
        this.activeDataStore = config.getDefaultDataStore();
        this.activeDataStore.initialize(this);
        getSubjects("group").cacheAll();
        this.rankLadderCache = new RankLadderCache(this.activeDataStore);
        convertUuids();

        // Now that initialization is complete
        uuidService = new CacheForwardingService(uuidService, new PEXProfileCache(getSubjects("user")));
        registerCommand(PermissionsExCommands.createRootCommand(this));
        registerCommand(RankingCommands.getPromoteCommand(this));
        registerCommand(RankingCommands.getDemoteCommand(this));
    }

    private void convertUuids() {
        try {
            InetAddress.getByName("api.mojang.com");
            Futures.addCallback(this.activeDataStore.performBulkOperation(new Function<DataStore, Integer>() {
                @Override
                public Integer apply(final DataStore input) {
                    Iterable<String> toConvert = Iterables.filter(input.getAllIdentifiers("user"), new Predicate<String>() {
                        @Override
                        public boolean apply(@Nullable String input) {
                            if (input == null || input.length() != 36) {
                                return true;
                            }
                            try {
                                UUID.fromString(input);
                                return false;
                            } catch (IllegalArgumentException e) {
                                return true;
                            }
                        }
                    });
                    if (toConvert.iterator().hasNext()) {
                        getLogger().info(fLog(_("Trying to convert users stored by name to UUID")));
                    } else {
                        return 0;
                    }

                    final ProfileService service = HttpRepositoryService.forMinecraft();
                    try {
                        final int[] converted = {0};
                        service.findAllByName(toConvert, new Predicate<Profile>() {
                            @Override
                            public boolean apply(Profile profile) {
                                final String newIdentifier = profile.getUniqueId().toString();
                                if (input.isRegistered("user", newIdentifier)) {
                                    getLogger().warn(fLog(_("Duplicate entry for %s found while converting to UUID", newIdentifier + "/" + profile.getName())));
                                    return false; // We already have a registered UUID, this is a duplicate.
                                }

                                String lookupName = profile.getName();
                                if (!input.isRegistered("user", lookupName)) {
                                    lookupName = lookupName.toLowerCase();
                                }
                                if (!input.isRegistered("user", lookupName)) {
                                    return false;
                                }
                                converted[0]++;

                                ImmutableOptionSubjectData oldData = input.getData("user", profile.getName(), null);
                                final String finalLookupName = lookupName;
                                Futures.addCallback(input.setData("user", newIdentifier, oldData.setOption(ImmutableSet.<Map.Entry<String, String>>of(), "name", profile.getName())), new FutureCallback<ImmutableOptionSubjectData>() {
                                    @Override
                                    public void onSuccess(@Nullable ImmutableOptionSubjectData result) {
                                        input.setData("user", finalLookupName, null);
                                    }

                                    @Override
                                    public void onFailure(Throwable t) {
                                        t.printStackTrace();
                                    }
                                });
                                return true;
                            }
                        });
                        return converted[0];
                    } catch (IOException | InterruptedException e) {
                        getLogger().error(fLog(_("Error while fetching UUIDs for users")), e);
                        return 0;
                    }
                }
            }), new FutureCallback<Integer>() {
                @Override
                public void onSuccess(@Nullable Integer result) {
                    if (result != null && result > 0) {
                        getLogger().info(fLog(_n("%s user successfully converted from name to UUID",
                                "%s users successfully converted from name to UUID!",
                                result, result)));
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    getLogger().error(fLog(_("Error converting users to UUID")), t);
                }
            });
        } catch (UnknownHostException e) {
            getLogger().warn(fLog(_("Unable to resolve Mojang API for UUID conversion. Do you have an internet connection? UUID conversion will not proceed (but may not be necessary).")));
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
    public ListenableFuture<Void> importDataFrom(String dataStoreIdentifier) {
        final DataStore expected = config.getDataStore(dataStoreIdentifier);
        if (expected == null) {
            return Futures.immediateFailedFuture(new IllegalArgumentException("Data store " + dataStoreIdentifier + " is not present"));
        }

        try {
            return activeDataStore.performBulkOperation(new Function<DataStore, ListenableFuture<Void>>() {
                @Override
                public ListenableFuture<Void> apply(@Nullable final DataStore store) {
                    return Futures.transform(Futures.allAsList(Iterables.transform(expected.getAll(), new Function<Map.Entry<Map.Entry<String, String>, ImmutableOptionSubjectData>, ListenableFuture<ImmutableOptionSubjectData>>() {
                        @Nullable
                        @Override
                        public ListenableFuture<ImmutableOptionSubjectData> apply(Map.Entry<Map.Entry<String, String>, ImmutableOptionSubjectData> input) {
                            return store.setData(input.getKey().getKey(), input.getKey().getValue(), input.getValue());
                        }
                    })), new Function<List<ImmutableOptionSubjectData>, Void>() {
                        @Nullable
                        @Override
                        public Void apply(@Nullable List<ImmutableOptionSubjectData> input) {
                            return null;
                        }
                    });
                }
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            return Futures.immediateFailedFuture(e);
        }
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
    public Logger getLogger() {
        return impl.getLogger();
    }

    @Override
    public DataSource getDataSourceForURL(String url) {
        return impl.getDataSourceForURL(url);
    }

    @Override
    public void executeAsyncronously(Runnable run) {
        impl.executeAsyncronously(run);
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
            throw new PermissionsLoadingException(_("While calculating subject data for %s:%s", type, identifier), e);
        }
    }

    public Iterable<? extends CalculatedSubject> getActiveCalculatedSubjects() {
        return Collections.unmodifiableCollection(calculatedSubjects.asMap().values());
    }
}
