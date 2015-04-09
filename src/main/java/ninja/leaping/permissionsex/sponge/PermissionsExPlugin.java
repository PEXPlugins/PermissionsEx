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
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.inject.Inject;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import ninja.leaping.permissionsex.ImplementationInterface;
import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.exception.PEBKACException;
import ninja.leaping.permissionsex.config.ConfigTransformations;
import ninja.leaping.permissionsex.config.PermissionsExConfiguration;
import ninja.leaping.permissionsex.exception.PermissionsLoadingException;
import ninja.leaping.permissionsex.util.Translatable;
import ninja.leaping.permissionsex.util.command.CommandException;
import ninja.leaping.permissionsex.util.command.CommandExecutor;
import ninja.leaping.permissionsex.util.command.CommandContext;
import ninja.leaping.permissionsex.util.command.Commander;
import ninja.leaping.permissionsex.util.command.CommandSpec;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.event.Subscribe;
import org.spongepowered.api.event.entity.player.PlayerJoinEvent;
import org.spongepowered.api.event.entity.player.PlayerQuitEvent;
import org.spongepowered.api.event.state.PreInitializationEvent;
import org.spongepowered.api.event.state.ServerStoppingEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.service.ProviderExistsException;
import org.spongepowered.api.service.ServiceManager;
import org.spongepowered.api.service.ServiceReference;
import org.spongepowered.api.service.config.ConfigDir;
import org.spongepowered.api.service.config.DefaultConfig;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.context.Context;
import org.spongepowered.api.service.permission.context.ContextCalculator;
import org.spongepowered.api.service.scheduler.AsynchronousScheduler;
import org.spongepowered.api.service.sql.SqlService;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.util.command.CommandMapping;
import org.spongepowered.api.util.command.CommandSource;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;

import static ninja.leaping.permissionsex.util.Translations._;
import static ninja.leaping.permissionsex.util.command.args.GenericArguments.*;

/**
 * PermissionsEx plugin
 */
@NonnullByDefault
@Plugin(id = PomData.ARTIFACT_ID, name = PomData.NAME, version = PomData.VERSION)
public class PermissionsExPlugin implements PermissionService, ImplementationInterface {

    private ServiceReference<SqlService> sql;
    private ServiceReference<AsynchronousScheduler> scheduler;
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

    private static String lf(Translatable trans) {
        return trans.translateFormatted(Locale.getDefault());
    }

    @Subscribe
    public void onPreInit(PreInitializationEvent event) throws PEBKACException {
        logger.info(lf(_("Pre-init of %s v%s", PomData.NAME, PomData.VERSION)));
        sql = services.potentiallyProvide(SqlService.class);
        scheduler = services.potentiallyProvide(AsynchronousScheduler.class);

        try {
            convertFromBukkit();
            configDir.mkdirs();
            reloadSync();
        } catch (PEBKACException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(lf(_("Error occurred while enabling %s", PomData.NAME)), e);
        }

        try {
            PermissionsExConfiguration.MAPPER.bind(config).serialize(rawConfig);
            configLoader.save(rawConfig);
        } catch (IOException | ObjectMappingException e) {
            throw new RuntimeException(e);
        }
        defaults = getSubjects(manager.getDefaultIdentifier().getKey()).get(manager.getDefaultIdentifier().getValue());

        setCommandSourceProvider(getUserSubjects(), new Function<String, Optional<CommandSource>>() {
            @Override
            @SuppressWarnings("unchecked")
            public Optional<CommandSource> apply(@Nullable String s) {
                if (s == null) {
                    return Optional.absent();
                }

                UUID uid;
                try {
                    uid = UUID.fromString(s);
                } catch (IllegalArgumentException ex) {
                    return Optional.absent();
                }

                // Yeah, java generics are stupid
                return (Optional) game.getServer().getPlayer(uid);

            }
        });

        setCommandSourceProvider(getSubjects(PermissionService.SUBJECTS_SYSTEM), new Function<String, Optional<CommandSource>>() {
            @Nullable
            @Override
            public Optional<CommandSource> apply(@Nullable String input) {
                switch (input) {
                    case "Server":
                        break;
                    case "RCON":
                        break;
                }
                return Optional.absent();
            }
        });

        registerContextCalculator(contextCalculator);

        // Registering the PEX service *must* occur after the plugin has been completely initialized
        try {
            services.setProvider(this, PermissionService.class, this);
        } catch (ProviderExistsException e) {
            manager.close();
            throw new PEBKACException(_("Your appear to already be using a different permissions plugin: %s", e.getMessage()));
        }

        /*
            Commands api todo items:
            - write command flags
            - handle rolling back CommandContexts -- use a custom immutable data structure for this
            - write PEX commands
            - implement into Sponge
         */
        this.registerCommand(
                CommandSpec.builder()
                        .setAliases("pextest")
                        .setDescription(_("A simple test command"))
                        .setArguments(flags()
                                .flag("a")
                                .buildWith(seq(string(_("first")), optional(choices(_("second"), ImmutableMap.of("first", true, "second", false))))))
                        .setExecutor(new CommandExecutor() {
                            @Override
                            public <TextType> void execute(Commander<TextType> src, CommandContext args) throws CommandException {
                                src.msg(_("Source locale: %s", src.getLocale()));
                                src.msg(_("You are: %s", src.fmt().subject(src.getSubjectIdentifier().get())));
                                src.msg(_("Your command ran!!"));
                                for (Map.Entry<Set<Context>, Map<String, Boolean>> entry : getDefaultData().getAllPermissions().entrySet()) {
                                    src.msg(_("Default in contexts: %s", entry.getKey().toString()));
                                    for (Map.Entry<String, Boolean> ent : entry.getValue().entrySet()) {
                                        src.msg(src.fmt().permission(ent.getKey(), ent.getValue() ? 1 : -1));
                                    }
                                }
                                src.msg(_("Has flag %s: %s", "a", String.valueOf(args.getOne("a"))));
                                src.msg(_("Has flag %s: %s", "-a", String.valueOf(args.getOne("-a"))));
                                src.msg(_("First argument: %s", args.getAll("first")));
                                src.msg(_("Second (optional) argument: %s", String.valueOf(args.getAll("second"))));
                                src.msg(_("Has permission: %s", src.fmt().booleanVal(src.hasPermission("permissionsex.test.check"))));
                            }
                        })
                        .build());
    }

    @Subscribe
    public void disable(ServerStoppingEvent event) {
        logger.debug(lf(_("Disabling %s", PomData.NAME)));
        if (manager != null) {
            manager.close();
            manager = null;
        }
    }

    @Subscribe
    public void onPlayerJoin(PlayerJoinEvent event) {
        final String identifier = event.getPlayer().getIdentifier();
        final PEXSubject subject = getUserSubjects().get(identifier);
        if (getUserSubjects().hasRegistered(identifier)) {
            if (!event.getPlayer().getName().equals(subject.getOption(SubjectData.GLOBAL_CONTEXT, "name").orNull())) {
                subject.getData().setOption(SubjectData.GLOBAL_CONTEXT, "name", event.getPlayer().getName());
            }
        }
    }

    @Subscribe
    public void onPlayerQuit(PlayerQuitEvent event) {
        getUserSubjects().uncache(event.getPlayer().getIdentifier());
    }

    static ConfigurationNode loadDefaultConfiguration() throws IOException {
        final URL defaultConfig = PermissionsExPlugin.class.getResource("default.conf");
        if (defaultConfig == null) {
            throw new Error(lf(_("Default config file is not present in jar!")));
        }
        HoconConfigurationLoader fallbackLoader = HoconConfigurationLoader.builder().setURL(defaultConfig).build();
        return fallbackLoader.load();

    }

    private void convertFromBukkit() throws IOException {
        File bukkitConfigDir = new File("plugins/PermissionsEx");
        if (bukkitConfigDir.isDirectory() && !configDir.isDirectory()) {
            logger.info(lf(_("Migrating configuration data from Bukkit")));
            if (!bukkitConfigDir.renameTo(configDir)) {
                throw new IOException(lf(_("Unable to move Bukkit configuration directory to location for Sponge!")));
            }
        }
        File bukkitConfigFile = new File(configDir, "config.yml");
        if (bukkitConfigFile.isFile()) {
            ConfigurationLoader<ConfigurationNode> yamlReader = YAMLConfigurationLoader.builder().setFile(bukkitConfigFile).build();
            ConfigurationNode bukkitConfig = yamlReader.load();
            ConfigTransformations.fromBukkit().apply(bukkitConfig);
            configLoader.save(bukkitConfig);
            if (!bukkitConfigFile.renameTo(new File(configDir, "config.yml.bukkit"))) {
                logger.warn(lf(_("Could not rename old Bukkit configuration file to old name")));
            }
        }
    }

    private void reloadSync() throws PEBKACException, ObjectMappingException, PermissionsLoadingException {
        try {
            rawConfig = configLoader.load();
            ConfigurationNode fallbackConfig;
            try {
                fallbackConfig = loadDefaultConfiguration();
            } catch (IOException e) {
                throw new Error("PEX's default configuration could not be loaded!", e);
            }
            rawConfig.mergeValuesFrom(fallbackConfig);
            config = PermissionsExConfiguration.MAPPER.bindToNew().populate(rawConfig);
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
            throw new PEBKACException(_("Error while loading configuration: %s", e.getLocalizedMessage()));
        } catch (ExecutionException e) {
            throw new PermissionsLoadingException(_("Unable to reload!"), e);
        }
    }

    public ListenableFuture<Void> reload() {
        ListenableFutureTask<Void> task = ListenableFutureTask.create(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                reloadSync();
                return null;
            }
        });
        executeAsyncronously(task);
        return task;
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
        return defaults.getTransientData();
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
            logger.error(lf(_("Unable to get subject collection for type %s", identifier)), e);
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
            logger.error(lf(_("Unable to get data source for jdbc url %s", url)), e);
            return null;
        }
    }

    @Override
    public void executeAsyncronously(Runnable run) {
        scheduler.ref().get().runTask(PermissionsExPlugin.this, run);
    }

    @Override
    public void registerCommand(CommandSpec command) {
        game.getCommandDispatcher().register(this, new PEXSpongeCommand(command, this), command.getAliases());
    }

    @Override
    public Set<CommandSpec> getImplementationCommands() {
            return ImmutableSet.of(CommandSpec.builder()
                .setAliases("reload", "rel")
                .setDescription(_("Reload the PermissionsEx configuration"))
                .setPermission("permissionsex.reload")
                .setExecutor(new CommandExecutor() {
                    @Override
                    public <TextType> void execute(final Commander<TextType> src, CommandContext args) throws CommandException {
                        src.msg(_("Reloading PermissionsEx"));
                        Futures.addCallback(reload(), new FutureCallback<Void>() {
                            @Override
                            public void onSuccess(@Nullable Void result) {
                                src.msg(_("The reload was successful"));
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                src.error(_("An error occurred while reloading PEX: %s\n " +
                                        "Please see the server console for details", t.getLocalizedMessage()));
                                logger.error(lf(_("An error occurred while reloading PEX (triggered by %s's command): %s",
                                        src.getName(), t.getLocalizedMessage())), t);
                            }
                        });
                    }
                })
                .build());
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
        return Iterables.concat(Iterables.transform(subjectCollections.asMap().values(), new Function<PEXSubjectCollection, Iterable<PEXSubject>>() {
            @Nullable
            @Override
            public Iterable<PEXSubject> apply(@Nullable PEXSubjectCollection input) {
                return input.getActiveSubjects();
            }
        }));
    }
}
