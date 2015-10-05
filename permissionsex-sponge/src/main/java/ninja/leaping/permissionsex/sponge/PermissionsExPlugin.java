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
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import ninja.leaping.permissionsex.ImplementationInterface;
import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.config.FilePermissionsExConfiguration;
import ninja.leaping.permissionsex.data.ImmutableSubjectData;
import ninja.leaping.permissionsex.data.SubjectCache;
import ninja.leaping.permissionsex.exception.PEBKACException;
import ninja.leaping.permissionsex.exception.PermissionsLoadingException;
import ninja.leaping.permissionsex.logging.TranslatableLogger;
import ninja.leaping.permissionsex.util.command.CommandSpec;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.GameProfile;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStoppedServerEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
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
import org.spongepowered.api.util.command.CommandSource;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
    private final TranslatableLogger logger;
    @Inject @ConfigDir(sharedRoot = false) private File configDir;
    @Inject @DefaultConfig(sharedRoot = false) private ConfigurationLoader<CommentedConfigurationNode> configLoader;
    @Inject private Game game;

    private PermissionsEx manager;

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
    private final Map<String, PEXPermissionDescription> descriptions = new ConcurrentHashMap<>();
    private Executor spongeExecutor = runnable -> scheduler.ref().get()
            .createTaskBuilder()
            .async()
            .execute(runnable)
            .submit(PermissionsExPlugin.this);

    @Inject
    PermissionsExPlugin(Logger logger) {
        this.logger = TranslatableLogger.forLogger(logger);
    }

    @Listener
    public void onPreInit(GamePreInitializationEvent event) throws PEBKACException {
        logger.info(t("Pre-init of %s v%s", PomData.NAME, PomData.VERSION));
        sql = services.potentiallyProvide(SqlService.class);
        scheduler = services.potentiallyProvide(SchedulerService.class);

        try {
            convertFromBukkit();
            configDir.mkdirs();
            this.manager = new PermissionsEx(FilePermissionsExConfiguration.fromLoader(this.configLoader), this);
        } catch (Exception e) {
            throw new RuntimeException(t("Error occurred while enabling %s", PomData.NAME).translateFormatted(logger.getLogLocale()), e);
        }
        defaults = getSubjects(PermissionsEx.SUBJECTS_DEFAULTS).get(PermissionsEx.SUBJECTS_DEFAULTS);

        setCommandSourceProvider(getUserSubjects(), name -> {
            UUID uid;
            try {
                uid = UUID.fromString(name);
            } catch (IllegalArgumentException ex) {
                return Optional.empty();
            }

            // Yeah, java generics are stupid
            return (Optional) game.getServer().getPlayer(uid);

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
        manager.registerNameTransformer(PermissionService.SUBJECTS_USER, input -> {
            try {
                UUID.fromString(input);
                return input;
            } catch (IllegalArgumentException ex) {
                Optional<Player> player = game.getServer().getPlayer(input);
                if (player.isPresent()) {
                    return player.get().getUniqueId().toString();
                } else {
                    Optional<GameProfileResolver> res = game.getServiceManager().provide(GameProfileResolver.class);
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

    @Listener
    public void cacheUserAsync(ClientConnectionEvent.Auth event) {
        try {
            getManager().getCalculatedSubject(PermissionsEx.SUBJECTS_USER, event.getProfile().getUniqueId().toString());
        } catch (PermissionsLoadingException e) {
            logger.warn(t("Error while loading data for user %s/%s during prelogin: %s", event.getProfile().getName(), event.getProfile().getUniqueId().toString(), e.getMessage()), e);
        }
    }

    @Listener
    public void disable(GameStoppedServerEvent event) {
        logger.debug(t("Disabling %s", PomData.NAME));
        PermissionsEx manager = this.manager;
        if (manager != null) {
            manager.close();
        }
    }

    @Listener
    public void onPlayerJoin(final ClientConnectionEvent.Join event) {
        final String identifier = event.getTargetEntity().getIdentifier();
        final SubjectCache cache = getManager().getSubjects(PermissionsEx.SUBJECTS_USER);
        if (cache.isRegistered(identifier)) {
            cache.update(identifier, input -> {
                if (event.getTargetEntity().getName().equals(input.getOptions(PermissionsEx.GLOBAL_CONTEXT).get("name"))) {
                    return input;
                } else {
                    return input.setOption(PermissionsEx.GLOBAL_CONTEXT, "name", event.getTargetEntity().getName());
                }
            });
        }
    }

    @Listener
    public void onPlayerQuit(ClientConnectionEvent.Disconnect event) {
        getUserSubjects().uncache(event.getTargetEntity().getIdentifier());
    }


    private void convertFromBukkit() throws IOException {
        File bukkitConfigDir = new File("plugins/PermissionsEx");
        if (bukkitConfigDir.isDirectory() && !configDir.isDirectory()) {
            logger.info(t("Migrating configuration data from Bukkit"));
            if (!bukkitConfigDir.renameTo(configDir)) {
                throw new IOException(t("Unable to move Bukkit configuration directory to location for Sponge!").translateFormatted(logger.getLogLocale()));
            }
        }
        File bukkitConfigFile = new File(configDir, "config.yml");
        if (bukkitConfigFile.isFile()) {
            ConfigurationLoader<ConfigurationNode> yamlReader = YAMLConfigurationLoader.builder().setFile(bukkitConfigFile).build();
            ConfigurationNode bukkitConfig = yamlReader.load();
            configLoader.save(bukkitConfig);
            if (!bukkitConfigFile.renameTo(new File(configDir, "config.yml.bukkit"))) {
                logger.warn(t("Could not rename old Bukkit configuration file to old name"));
            }
        }
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
            logger.error(t("Unable to get subject collection for type %s", identifier), e);
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
    public Optional<PermissionDescription.Builder> newDescriptionBuilder(Object instance) {
        Optional<PluginContainer> container = this.game.getPluginManager().fromInstance(instance);
        if (!container.isPresent()) {
            throw new IllegalArgumentException("Provided plugin did not have an associated plugin instance. Are you sure it's your plugin instance?");
        }
        return Optional.of(new PEXPermissionDescription.Builder(container.get(), this));
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
    public Optional<PermissionDescription> getDescription(String s) {
        return Optional.ofNullable(this.descriptions.get(s));
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
            logger.error(t("Unable to get data source for jdbc url %s", url), e);
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
        return ImmutableSet.of();
    }

    @Override
    public String getVersion() {
        return PomData.VERSION;
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
