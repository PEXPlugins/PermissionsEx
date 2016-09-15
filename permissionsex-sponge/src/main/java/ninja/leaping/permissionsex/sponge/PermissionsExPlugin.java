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
import ninja.leaping.permissionsex.exception.PEBKACException;
import ninja.leaping.permissionsex.logging.TranslatableLogger;
import ninja.leaping.permissionsex.subject.SubjectType;
import ninja.leaping.permissionsex.util.command.CommandContext;
import ninja.leaping.permissionsex.util.command.CommandException;
import ninja.leaping.permissionsex.util.command.CommandExecutor;
import ninja.leaping.permissionsex.util.command.CommandSpec;
import ninja.leaping.permissionsex.util.command.Commander;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStoppedServerEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Scheduler;
import org.spongepowered.api.service.ServiceManager;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.sql.SqlService;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
import static ninja.leaping.permissionsex.util.command.args.GenericArguments.string;

/**
 * PermissionsEx plugin
 */
@NonnullByDefault
@Plugin(id = PomData.ARTIFACT_ID, name = PomData.NAME, version = PomData.VERSION)
public class PermissionsExPlugin implements PermissionService, ImplementationInterface {

    private Optional<SqlService> sql;
    private Scheduler scheduler;
    @Inject private ServiceManager services;
    private final TranslatableLogger logger;
    @Inject @ConfigDir(sharedRoot = false) private Path configDir;
    @Inject @DefaultConfig(sharedRoot = false) private ConfigurationLoader<CommentedConfigurationNode> configLoader;
    @Inject private Game game;

    private PermissionsEx manager;

    private final List<ContextCalculator<Subject>> contextCalculators = new CopyOnWriteArrayList<>();
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
    private Executor spongeExecutor = runnable -> scheduler
            .createTaskBuilder()
            .async()
            .execute(runnable)
            .submit(PermissionsExPlugin.this);

    private Timings timings;

    @Inject
    PermissionsExPlugin(Logger logger) {
        this.logger = TranslatableLogger.forLogger(logger);
    }

    @Listener
    public void onPreInit(GamePreInitializationEvent event) throws PEBKACException {
        this.timings = new Timings(this);
        logger.info(t("Pre-init of %s v%s", PomData.NAME, PomData.VERSION));
        sql = services.provide(SqlService.class);
        scheduler = game.getScheduler();

        try {
            convertFromBukkit();
            convertFromLegacySpongeName();
            Files.createDirectories(configDir);
            this.manager = new PermissionsEx(FilePermissionsExConfiguration.fromLoader(this.configLoader), this);
        } catch (Exception e) {
            throw new RuntimeException(t("Error occurred while enabling %s", PomData.NAME).translateFormatted(logger.getLogLocale()), e);
        }

        defaults = getSubjects(PermissionsEx.SUBJECTS_DEFAULTS).get(PermissionsEx.SUBJECTS_DEFAULTS);

        setCommandSourceProvider(getUserSubjects(),name -> {
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
        getManager().getSubjects(SUBJECTS_USER).setTypeInfo(new UserSubjectTypeDescription(SUBJECTS_USER, this));

        registerFakeOpCommand("op", "minecraft.command.op");
        registerFakeOpCommand("deop", "minecraft.command.deop");


        // Registering the PEX service *must* occur after the plugin has been completely initialized
        if (!services.isRegistered(PermissionService.class)) {
            services.setProvider(this, PermissionService.class, this);
        } else {
            manager.close();
            throw new PEBKACException(t("Your appear to already be using a different permissions plugin!"));
        }
    }

    private void registerFakeOpCommand(String alias, String permission) {
        registerCommand(CommandSpec.builder()
                .setAliases(alias)
                .setPermission(permission)
                .setDescription(t("A dummy replacement for vanilla's operator commands"))
                .setArguments(string(t("user")))
                .setExecutor(new CommandExecutor() {
                    @Override
                    public <TextType> void execute(Commander<TextType> src, CommandContext ctx) throws CommandException {
                        throw new CommandException(t("PermissionsEx replaces the server op/deop commands. Use PEX commands to manage permissions instead!"));
                    }
                })
                .build());
    }

    @Listener
    public void cacheUserAsync(ClientConnectionEvent.Auth event) {
        try {
            getManager().getSubjects(PermissionsEx.SUBJECTS_USER).get(event.getProfile().getUniqueId().toString());
        } catch (Exception e) {
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
    public void onReload(GameReloadEvent event) {
        if (this.manager != null) {
            this.manager.reload();
        }
    }

    @Listener
    public void onPlayerJoin(final ClientConnectionEvent.Join event) {
        final String identifier = event.getTargetEntity().getIdentifier();
        final SubjectType cache = getManager().getSubjects(PermissionsEx.SUBJECTS_USER);
        cache.isRegistered(identifier).thenAccept(registered -> {
           if (registered)  {
               cache.persistentData().update(identifier, input -> {
                   if (event.getTargetEntity().getName().equals(input.getOptions(PermissionsEx.GLOBAL_CONTEXT).get("name"))) {
                       return input;
                   } else {
                       return input.setOption(PermissionsEx.GLOBAL_CONTEXT, "name", event.getTargetEntity().getName());
                   }
               });
           }
        });
    }

    @Listener
    public void onPlayerQuit(ClientConnectionEvent.Disconnect event) {
        getUserSubjects().uncache(event.getTargetEntity().getIdentifier());
    }

    public Timings getTimings() {
        return timings;
    }

    private void convertFromBukkit() throws IOException {
        Path bukkitConfigPath = Paths.get("plugins/PermissionsEx");
        if (Files.isDirectory(bukkitConfigPath) && isDirectoryEmpty(configDir)) {
            logger.info(t("Migrating configuration data from Bukkit"));
            Files.move(bukkitConfigPath, configDir, StandardCopyOption.REPLACE_EXISTING);
        }
        Path bukkitConfigFile = configDir.resolve("config.yml");
        if (Files.exists(bukkitConfigFile)) {
            ConfigurationLoader<ConfigurationNode> yamlReader = YAMLConfigurationLoader.builder().setPath(bukkitConfigFile).build();
            ConfigurationNode bukkitConfig = yamlReader.load();
            configLoader.save(bukkitConfig);
            Files.move(bukkitConfigFile, configDir.resolve("config.yml.bukkit"));
        }
    }

    private void convertFromLegacySpongeName() throws IOException {
        Path oldPath = configDir.resolveSibling("ninja.leaping.permissionsex"); // Old plugin ID

        if (Files.exists(oldPath) && isDirectoryEmpty(configDir)) {
            Files.move(oldPath, configDir, StandardCopyOption.REPLACE_EXISTING);
            Files.move(configDir.resolve("ninja.leaping.permissionsex.conf"), configDir.resolve(PomData.ARTIFACT_ID + ".conf"));
            logger.info(t("Migrated legacy sponge config directory to new location. Configuration is now located in %s", configDir.toString()));
        }
    }

    private boolean isDirectoryEmpty(Path dir) throws IOException {
        if (Files.exists(dir)) {
            return true;
        }
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(dir)) {
            return !dirStream.iterator().hasNext();
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
    public PEXSubject getDefaults() {
        return defaults;
    }

    @Override
    public PEXSubjectCollection getSubjects(String identifier) {
        Preconditions.checkNotNull(identifier, "identifier");
        try {
            return subjectCollections.get(identifier);
        } catch (ExecutionException e) {
            getLogger().error(t("Unable to get subject collection for type %s", identifier), e);
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
    public void registerContextCalculator(ContextCalculator<Subject> calculator) {
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
        final SubjectType coll = getManager().getSubjects(SUBJECTS_ROLE_TEMPLATE);
        for (final Map.Entry<String, Integer> rank : ranks.entrySet()) {
            try {
                coll.transientData().update(rank.getKey(), new Function<ImmutableSubjectData, ImmutableSubjectData>() {
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

    public List<ContextCalculator<Subject>> getContextCalculators() {
        return contextCalculators;
    }

    @Override
    public Path getBaseDirectory() {
        return configDir;
    }

    @Override
    public TranslatableLogger getLogger() {
        return logger;
    }

    @Override
    @Nullable
    public DataSource getDataSourceForURL(String url) {
        if (!sql.isPresent()) {
            return null;
        }
        try {
            return sql.get().getDataSource(this, url);
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
        game.getCommandManager().register(this, new PEXSpongeCommand(command, this), command.getAliases());
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
        return commandSourceProviders.getOrDefault(subjectCollection, k -> Optional.empty());
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
