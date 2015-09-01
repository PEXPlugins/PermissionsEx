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

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import ninja.leaping.permissionsex.ImplementationInterface;
import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.data.ImmutableSubjectData;
import ninja.leaping.permissionsex.data.SubjectCache;
import ninja.leaping.permissionsex.exception.PEBKACException;
import ninja.leaping.permissionsex.config.ConfigTransformations;
import ninja.leaping.permissionsex.config.PermissionsExConfiguration;
import ninja.leaping.permissionsex.exception.PermissionsLoadingException;
import ninja.leaping.permissionsex.util.Translatable;
import ninja.leaping.permissionsex.util.Util;
import ninja.leaping.permissionsex.util.command.CommandException;
import ninja.leaping.permissionsex.util.command.CommandExecutor;
import ninja.leaping.permissionsex.util.command.CommandContext;
import ninja.leaping.permissionsex.util.command.Commander;
import ninja.leaping.permissionsex.util.command.CommandSpec;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.GameProfile;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.event.Subscribe;
import org.spongepowered.api.event.entity.player.PlayerJoinEvent;
import org.spongepowered.api.event.entity.player.PlayerQuitEvent;
import org.spongepowered.api.event.network.GameClientAuthEvent;
import org.spongepowered.api.event.state.PreInitializationEvent;
import org.spongepowered.api.event.state.ServerStoppedEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.ProviderExistsException;
import org.spongepowered.api.service.ServiceManager;
import org.spongepowered.api.service.ServiceReference;
import org.spongepowered.api.service.config.ConfigDir;
import org.spongepowered.api.service.config.DefaultConfig;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.context.ContextCalculator;
import org.spongepowered.api.service.profile.GameProfileResolver;
import org.spongepowered.api.service.scheduler.SchedulerService;
import org.spongepowered.api.service.sql.SqlService;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.util.command.CommandMapping;
import org.spongepowered.api.util.command.CommandSource;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.Function;

import static ninja.leaping.permissionsex.sponge.SpongeTranslations.t;

/**
 * PermissionsEx plugin
 */
@NonnullByDefault
@Plugin(id = PomData.ARTIFACT_ID, name = PomData.NAME, version = PomData.VERSION)
public class PermissionsExPlugin implements PermissionService, ImplementationInterface {

    private ServiceReference<SqlService> sql;
    private ServiceReference<SchedulerService> scheduler;
    @Inject private ServiceManager services;
    @Inject private Logger logger;
    @Inject @ConfigDir(sharedRoot = false) private File configDir;
    @Inject @DefaultConfig(sharedRoot = false) private ConfigurationLoader<CommentedConfigurationNode> configLoader;
    @Inject private Game game;

    @Nullable
    private volatile PermissionsEx manager;
    private PermissionsExConfiguration config;
    private ConfigurationNode rawConfig;

    private final List<ContextCalculator> contextCalculators = new CopyOnWriteArrayList<>();
    private final ConcurrentMap<String, Function<String, Optional<CommandSource>>> commandSourceProviders = new ConcurrentHashMap<>();
    private final LoadingCache<String, PEXSubjectCollection> subjectCollections = CacheBuilder.newBuilder().build(new CacheLoader<String, PEXSubjectCollection>() {
        @Override
        public PEXSubjectCollection load(String type) throws Exception {
            return new PEXSubjectCollection(type, PermissionsExPlugin.this);
        }
    });
    private PEXSubject defaults;
    private final PEXContextCalculator contextCalculator = new PEXContextCalculator();
    private final Map<String, Function<String, String>> nameTransformerMap = new ConcurrentHashMap<>();
    private final Map<String, PEXPermissionDescription> descriptions = new ConcurrentHashMap<>();
    private Executor spongeExecutor = runnable -> {
        scheduler.ref().get()
                .createTaskBuilder()
                .async()
                .execute(runnable)
                .submit(PermissionsExPlugin.this);
    };

    private static String lf(Translatable trans) {
        return trans.translateFormatted(Locale.getDefault());
    }

    @Subscribe
    public void onPreInit(PreInitializationEvent event) throws PEBKACException {
        logger.info(lf(t("Pre-init of %s v%s", PomData.NAME, PomData.VERSION)));
        sql = services.potentiallyProvide(SqlService.class);
        scheduler = services.potentiallyProvide(SchedulerService.class);

        try {
            convertFromBukkit();
            configDir.mkdirs();
            reloadSync();
        } catch (PEBKACException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(lf(t("Error occurred while enabling %s", PomData.NAME)), e);
        }

        try {
            rawConfig.setValue(PermissionsExConfiguration.TYPE, config);
            configLoader.save(rawConfig);
        } catch (IOException | ObjectMappingException e) {
            throw new RuntimeException(e);
        }

        defaults = getSubjects(manager.getDefaultIdentifier().getKey()).get(manager.getDefaultIdentifier().getValue());

        setCommandSourceProvider(getUserSubjects(), name -> {
            UUID uid;
            try {
                uid = UUID.fromString(name);
            } catch (IllegalArgumentException ex) {
                return Optional.empty();
            }

            // Yeah, java generics are stupid
            return Optional.ofNullable(game.getServer().getPlayer(uid).orNull());

        });

        setCommandSourceProvider(getSubjects(PermissionService.SUBJECTS_SYSTEM), input -> {
            switch (input) {
                case "Server":
                    return Optional.of(game.getServer().getConsole());
                case "RCON":
                    break;
            }
            return Optional.empty();
        });

        registerContextCalculator(contextCalculator);
        nameTransformerMap.put(PermissionService.SUBJECTS_USER, input -> {
            try {
                UUID.fromString(input);
                return input;
            } catch (IllegalArgumentException ex) {
                com.google.common.base.Optional<Player> player = game.getServer().getPlayer(input);
                if (player.isPresent()) {
                    return player.get().getUniqueId().toString();
                } else {
                    com.google.common.base.Optional<GameProfileResolver> res = game.getServiceManager().provide(GameProfileResolver.class);
                    if (res.isPresent()) {
                        for (GameProfile profile : res.get().match(input)) {
                            if (profile.getName().equalsIgnoreCase(input)) {
                                return profile.getUniqueId().toString();
                            }
                        }
                    }
                    return input;
                }
            }
        });

        // Registering the PEX service *must* occur after the plugin has been completely initialized
        try {
            services.setProvider(this, PermissionService.class, this);
        } catch (ProviderExistsException e) {
            manager.close();
            throw new PEBKACException(t("Your appear to already be using a different permissions plugin: %s", e.getMessage()));
        }
    }

    @Subscribe
    public void cacheUserAsync(GameClientAuthEvent event) {
        try {
            getManager().getCalculatedSubject(PermissionsEx.SUBJECTS_USER, event.getProfile().getUniqueId().toString());
        } catch (PermissionsLoadingException e) {
            logger.warn(lf(t("Error while loading data for user %s/%s during prelogin: %s", event.getProfile().getName(), event.getProfile().getUniqueId().toString(), e.getMessage())), e);
        }
    }

    @Subscribe
    public void disable(ServerStoppedEvent event) {
        logger.debug(lf(t("Disabling %s", PomData.NAME)));
        PermissionsEx manager = this.manager;
        if (manager != null) {
            manager.close();
        }
    }

    @Subscribe
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final String identifier = event.getEntity().getIdentifier();
        final SubjectCache cache = getManager().getSubjects(PermissionsEx.SUBJECTS_USER);
        if (cache.isRegistered(identifier)) {
            cache.update(identifier, input -> {
                    if (event.getEntity().getName().equals(input.getOptions(PermissionsEx.GLOBAL_CONTEXT).get("name"))) {
                        return input;
                    } else {
                        return input.setOption(PermissionsEx.GLOBAL_CONTEXT, "name", event.getEntity().getName());
                    }
            });
        }
    }

    @Subscribe
    public void onPlayerQuit(PlayerQuitEvent event) {
        getUserSubjects().uncache(event.getEntity().getIdentifier());
    }


    private void convertFromBukkit() throws IOException {
        File bukkitConfigDir = new File("plugins/PermissionsEx");
        if (bukkitConfigDir.isDirectory() && !configDir.isDirectory()) {
            logger.info(lf(t("Migrating configuration data from Bukkit")));
            if (!bukkitConfigDir.renameTo(configDir)) {
                throw new IOException(lf(t("Unable to move Bukkit configuration directory to location for Sponge!")));
            }
        }
        File bukkitConfigFile = new File(configDir, "config.yml");
        if (bukkitConfigFile.isFile()) {
            ConfigurationLoader<ConfigurationNode> yamlReader = YAMLConfigurationLoader.builder().setFile(bukkitConfigFile).build();
            ConfigurationNode bukkitConfig = yamlReader.load();
            configLoader.save(bukkitConfig);
            if (!bukkitConfigFile.renameTo(new File(configDir, "config.yml.bukkit"))) {
                logger.warn(lf(t("Could not rename old Bukkit configuration file to old name")));
            }
        }
    }

    private void reloadSync() throws PEBKACException, ObjectMappingException, PermissionsLoadingException {
        try {
            rawConfig = configLoader.load();
            ConfigurationNode fallbackConfig;
            try {
                fallbackConfig = PermissionsExConfiguration.loadDefaultConfiguration();
            } catch (IOException e) {
                throw new Error("PEX's default configuration could not be loaded!", e);
            }
            ConfigTransformations.versions().apply(rawConfig);
            rawConfig.mergeValuesFrom(fallbackConfig);
            config = rawConfig.getValue(PermissionsExConfiguration.TYPE);
            config.validate();
            PermissionsEx oldManager = manager;
            Set<CommandMapping> mappings = game.getCommandDispatcher().getOwnedBy(game.getPluginManager().fromInstance(this).get());
            for (CommandMapping mapping : mappings) { // Because the new PermissionsEx instance will register commands again, we have to unregister the old ones here
                game.getCommandDispatcher().removeMapping(mapping);
            }
            manager = new PermissionsEx(config, this);
            if (oldManager != null) {
                oldManager.close();
            }
            contextCalculator.update(config);
            for (PEXSubjectCollection collection : subjectCollections.asMap().values()) {
                collection.updateCaches();
            }
        } catch (IOException e) {
            throw new PEBKACException(t("Error while loading configuration: %s", e.getLocalizedMessage()));
        } catch (ExecutionException e) {
            throw new PermissionsLoadingException(t("Unable to reload!"), e);
        }
    }

    public CompletableFuture<Void> reload() {
        return Util.asyncFailableFuture(() -> {
            reloadSync();
            return null;
        }, getAsyncExecutor());
    }

    PermissionsEx getManager() {
        return this.manager;
    }

    @Override
    public PEXSubjectCollection getUserSubjects() {
        try {
            return subjectCollections.get(SUBJECTS_USER);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PEXSubjectCollection getGroupSubjects() {
        try {
            return subjectCollections.get(SUBJECTS_GROUP);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PEXOptionSubjectData getDefaultData() {
        return defaults.getTransientSubjectData();
    }

    public PEXSubject getDefaultSubject() {
        return defaults;
    }

    @Override
    public PEXSubjectCollection getSubjects(String identifier) {
        Preconditions.checkNotNull(identifier, "identifier");
        try {
            return subjectCollections.get(identifier);
        } catch (ExecutionException e) {
            logger.error(lf(t("Unable to get subject collection for type %s", identifier)), e);
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    // TODO: Get values from DataStore.getRegisteredTypes()
    public Map<String, SubjectCollection> getKnownSubjects() {
        return (Map) subjectCollections.asMap();
    }

    @Override
    public void registerContextCalculator(ContextCalculator calculator) {
        contextCalculators.add(calculator);
    }

    @Override
    public com.google.common.base.Optional<PermissionDescription.Builder> newDescriptionBuilder(Object instance) {
        com.google.common.base.Optional<PluginContainer> container = this.game.getPluginManager().fromInstance(instance);
        if (!container.isPresent()) {
            throw new IllegalArgumentException("Provided plugin did not have an associated plugin instance. Are you sure it's your plugin instance?");
        }
        return com.google.common.base.Optional.of(new PEXPermissionDescription.Builder(container.get(), this));
    }

    void registerDescription(final PEXPermissionDescription description, Map<String, Integer> ranks) {
        this.descriptions.put(description.getId(), description);
        final SubjectCache coll = getManager().getTransientSubjects(SUBJECTS_ROLE_TEMPLATE);
        for (final Map.Entry<String, Integer> rank : ranks.entrySet()) {
            try {
                coll.update(rank.getKey(), new Function<ImmutableSubjectData, ImmutableSubjectData>() {
                    @Nullable
                    @Override
                    public ImmutableSubjectData apply(@Nullable ImmutableSubjectData input) {
                        return Preconditions.checkNotNull(input).setPermission(PermissionsEx.GLOBAL_CONTEXT, description.getId(), rank.getValue());
                    }
                }).get();
            } catch (InterruptedException | ExecutionException e) {
                throw Throwables.propagate(e);
            }
        }
    }

    @Override
    public com.google.common.base.Optional<PermissionDescription> getDescription(String s) {
        return com.google.common.base.Optional.fromNullable(this.descriptions.get(s));
    }

    @Override
    public Collection<PermissionDescription> getDescriptions() {
        return ImmutableSet.<PermissionDescription>copyOf(this.descriptions.values());
    }

    public List<ContextCalculator> getContextCalculators() {
        return contextCalculators;
    }

    @Override
    public File getBaseDirectory() {
        return configDir;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    @Nullable
    public DataSource getDataSourceForURL(String url) {
        if (!sql.ref().isPresent()) {
            return null;
        }
        try {
            return sql.ref().get().getDataSource(url);
        } catch (SQLException e) {
            logger.error(lf(t("Unable to get data source for jdbc url %s", url)), e);
            return null;
        }
    }

    /**
     * Get an executor to run tasks asynchronously on.
     *
     * @return The async executor
     */
    @Override
    public Executor getAsyncExecutor() {
        return this.spongeExecutor;
    }

    @Override
    public void registerCommand(CommandSpec command) {
        game.getCommandDispatcher().register(this, new PEXSpongeCommand(command, this), command.getAliases());
    }

    @Override
    public Set<CommandSpec> getImplementationCommands() {
            return ImmutableSet.of(CommandSpec.builder()
                .setAliases("reload", "rel")
                .setDescription(t("Reload the PermissionsEx configuration"))
                .setPermission("permissionsex.reload")
                .setExecutor(new CommandExecutor() {
                    @Override
                    public <TextType> void execute(final Commander<TextType> src, CommandContext args) throws CommandException {
                        src.msg(t("Reloading PermissionsEx"));
                        reload().thenRun(() -> {
                            src.msg(t("The reload was successful"));
                        }).exceptionally(t -> {
                            src.error(t("An error occurred while reloading PEX: %s\n " +
                                    "Please see the server console for details", t.getLocalizedMessage()));
                            logger.error(lf(t("An error occurred while reloading PEX (triggered by %s's command): %s",
                                    src.getName(), t.getLocalizedMessage())), t);
                            return null;
                        });
                    }
                })
                .build());
    }

    @Override
    public String getVersion() {
        return PomData.VERSION;
    }

    @Override
    public Function<String, String> getNameTransformer(String type) {
        Function<String, String> xform = nameTransformerMap.get(type);
        if (xform == null) {
            xform = Function.identity();
        }
        return xform;
    }

    Function<String, Optional<CommandSource>> getCommandSourceProvider(String subjectCollection) {
        return commandSourceProviders.get(subjectCollection);
    }

    public void setCommandSourceProvider(PEXSubjectCollection subjectCollection, Function<String, Optional<CommandSource>> provider) {
        commandSourceProviders.put(subjectCollection.getIdentifier(), provider);
    }

    public Iterable<PEXSubject> getAllActiveSubjects() {
        return Iterables.concat(Iterables.transform(subjectCollections.asMap().values(), PEXSubjectCollection::getActiveSubjects));
    }

    public Game getGame() {
        return game;
    }
}
