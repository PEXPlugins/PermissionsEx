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
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
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
import ninja.leaping.permissionsex.util.command.Command;
import ninja.leaping.permissionsex.util.command.CommandContext;
import ninja.leaping.permissionsex.util.command.Commander;
import ninja.leaping.permissionsex.util.command.args.CommandSpec;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
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
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.util.command.CommandCallable;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.util.event.Subscribe;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Collections;
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

import static ninja.leaping.permissionsex.util.Translations.tr;
import static ninja.leaping.permissionsex.util.command.args.GenericArguments.*;
import static ninja.leaping.permissionsex.util.command.args.GameArguments.*;

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

    private String dtr(String text, Object... args) {
        return String.format(Locale.getDefault(), tr(text).translate(Locale.getDefault()), args);
    }

    @Subscribe
    public void onPreInit(PreInitializationEvent event) throws PEBKACException {
        logger.info("Pre-init of " + PomData.NAME + " v" + PomData.VERSION);
        sql = services.potentiallyProvide(SqlService.class);
        scheduler = services.potentiallyProvide(AsynchronousScheduler.class);

        try {
            convertFromBukkit();
            configDir.mkdirs();
            reloadSync();
        } catch (PEBKACException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while enabling " + PomData.NAME, e);
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
            throw new PEBKACException(tr("Your appear to already be using a different permissions plugin: %s"), e.getMessage());
        }

        this.registerCommand(new Command(
                CommandSpec.builder()
                .setAliases("pex", "pextest")
                .setDescription(tr("A simple test command"))
                .setArguments(seq(string("first"), optional(string("second"))))
                .build()
        ) {
            @Override
            protected <TextType> void execute(Commander<TextType> cmd, CommandContext args) throws ninja.leaping.permissionsex.util.command.CommandException {
                cmd.msg(tr("Source locale: %s"), "unknown");
                cmd.msg(tr("You are: %s"), cmd.getName()); //cmd.fmt().subject(Maps.immutableEntry(cmd.getSubject().getIdentifier().getKey(), cmd.getSubject().getIdentifier().getValue())));
                cmd.msg(tr("Your command ran!!"));
                for (Map.Entry<Set<Context>, Map<String, Boolean>> entry : getDefaultData().getAllPermissions().entrySet()) {
                    cmd.msg(tr("Default in contexts: %s"), entry.getKey().toString());
                    for (Map.Entry<String, Boolean> ent : entry.getValue().entrySet()) {
                        //source.sendMessage(cmd.fmt().permission(ent.getKey(), ent.getValue() ? 1 : -1)); // TODO: How does this translate?
                    }

                }
                cmd.msg(tr("First argument: %s"), args.getArg("first"));
                cmd.msg(tr("Second (optional) argument: %s"), String.valueOf(args.getArg("second")));
                cmd.msg(tr("Has permission: %s"), cmd.fmt().booleanVal(cmd.hasPermission("permissionsex.test.check")));

            }
        });
    }

    @Subscribe
    public void disable(ServerStoppingEvent event) {
        logger.debug("Disabling " + PomData.NAME);
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
            throw new Error("Default config file is not present in jar!");
        }
        HoconConfigurationLoader fallbackLoader = HoconConfigurationLoader.builder().setURL(defaultConfig).build();
        return fallbackLoader.load();

    }

    private void convertFromBukkit() throws IOException {
        File bukkitConfigDir = new File("plugins/PermissionsEx");
        if (bukkitConfigDir.isDirectory() && !configDir.isDirectory()) {
            logger.info("Migrating configuration data from Bukkit");
            if (!bukkitConfigDir.renameTo(configDir)) {
                throw new IOException("Unable to move Bukkit configuration directory to location for Sponge!");
            }
        }
        File bukkitConfigFile = new File(configDir, "config.yml");
        if (bukkitConfigFile.isFile()) {
            ConfigurationLoader<ConfigurationNode> yamlReader = YAMLConfigurationLoader.builder().setFile(bukkitConfigFile).build();
            ConfigurationNode bukkitConfig = yamlReader.load();
            ConfigTransformations.fromBukkit().apply(bukkitConfig);
            configLoader.save(bukkitConfig);
            if (!bukkitConfigFile.renameTo(new File(configDir, "config.yml.bukkit"))) {
                logger.warn("Could not rename old Bukkit configuration file to old name");
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
            manager = new PermissionsEx(config, this);
            if (oldManager != null) {
                oldManager.close();
            }
            contextCalculator.update(config);
            for (PEXSubjectCollection collection : subjectCollections.asMap().values()) {
                collection.updateCaches();
            }
        } catch (IOException e) {
            throw new PEBKACException(tr("Error while loading configuration: %s"), e.getLocalizedMessage());
        } catch (ExecutionException e) {
            throw new PermissionsLoadingException(tr("Unable to reload!"), e);
        }
    }

    public ListenableFuture<Void> reload() {
        return ListenableFutureTask.create(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                reloadSync();
                return null;
            }
        });
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
            logger.error("Unable to get subject collection for type " + identifier, e);
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
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
            logger.error(dtr("Unable to get data source for jdbc url %s", url), e);
            return null;
        }
    }

    @Override
    public void executeAsyncronously(Runnable run) {
        scheduler.ref().get().runTask(PermissionsExPlugin.this, run);
    }

    @Override
    public void registerCommand(Command command) {
        game.getCommandDispatcher().register(this, new PEXSpongeCommand(command, this), command.getSpec().getAliases());
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
