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

package ca.stellardrift.permissionsex.sponge;

import ca.stellardrift.permissionsex.BaseDirectoryScope;
import ca.stellardrift.permissionsex.ImplementationInterface;
import ca.stellardrift.permissionsex.PermissionsEx;
import ca.stellardrift.permissionsex.commands.commander.Permission;
import ca.stellardrift.permissionsex.commands.parse.CommandException;
import ca.stellardrift.permissionsex.commands.parse.CommandSpecKt;
import ca.stellardrift.permissionsex.config.FilePermissionsExConfiguration;
import ca.stellardrift.permissionsex.exception.PEBKACException;
import ca.stellardrift.permissionsex.exception.PermissionsException;
import ca.stellardrift.permissionsex.logging.FormattedLogger;
import ca.stellardrift.permissionsex.subject.FixedEntriesSubjectTypeDefinition;
import ca.stellardrift.permissionsex.subject.SubjectType;
import ca.stellardrift.permissionsex.util.CachingValue;
import ca.stellardrift.permissionsex.util.MinecraftProfile;
import ca.stellardrift.permissionsex.commands.parse.CommandExecutor;
import ca.stellardrift.permissionsex.commands.parse.CommandSpec;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
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
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.*;
import org.spongepowered.api.service.sql.SqlService;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static ca.stellardrift.permissionsex.commands.parse.ValuesKt.string;
import static java.util.Objects.requireNonNull;

/**
 * PermissionsEx plugin
 */
@NonnullByDefault
@Plugin(id = PomData.ARTIFACT_ID, name = PomData.NAME, version = PomData.VERSION, description = PomData.DESCRIPTION)
public class PermissionsExPlugin implements PermissionService, ImplementationInterface {

    private Optional<SqlService> sql;
    private Scheduler scheduler;
    @Inject private ServiceManager services;
    private final FormattedLogger logger;
    @Inject @ConfigDir(sharedRoot = false) private Path configDir;
    @Inject @DefaultConfig(sharedRoot = false) private ConfigurationLoader<CommentedConfigurationNode> configLoader;
    @Inject private Game game;
    private final Queue<Supplier<Set<CommandSpec>>> cachedCommands = new ConcurrentLinkedQueue<>();

    @Nullable
    private PermissionsEx<?> manager;

    private Executor spongeExecutor = runnable -> scheduler
            .createTaskBuilder()
            .async()
            .execute(runnable)
            .submit(PermissionsExPlugin.this);
    private final List<ContextCalculator<Subject>> contextCalculators = new CopyOnWriteArrayList<>();
    private final AsyncLoadingCache<String, PEXSubjectCollection> subjectCollections = Caffeine.newBuilder().executor(spongeExecutor).buildAsync((type, exec) -> PEXSubjectCollection.load(type, this));
    private PEXSubject defaults;
    private final Map<String, PEXPermissionDescription> descriptions = new ConcurrentHashMap<>();

    private Timings timings;

    @Inject
    PermissionsExPlugin(Logger logger) {
        this.logger = FormattedLogger.forLogger(logger, true);
    }

    @Listener
    public void onPreInit(GamePreInitializationEvent event) throws PEBKACException, InterruptedException, ExecutionException {
        this.timings = new Timings(this);
        logger.info(Messages.PLUGIN_INIT_BEGIN.toComponent(PomData.NAME, PomData.VERSION));
        sql = services.provide(SqlService.class);
        scheduler = game.getScheduler();

        try {
            convertFromBukkit();
            convertFromLegacySpongeName();
            Files.createDirectories(configDir);
            this.manager = new PermissionsEx<>(FilePermissionsExConfiguration.fromLoader(this.configLoader), this);
        } catch (Exception e) {
            throw new RuntimeException(new PermissionsException(Messages.PLUGIN_INIT_ERROR_GENERAL.toComponent(PomData.NAME), e));
        }

        defaults = (PEXSubject) loadCollection(PermissionsEx.SUBJECTS_DEFAULTS).thenCompose(coll -> coll.loadSubject(PermissionsEx.SUBJECTS_DEFAULTS)).get();

        getManager().getSubjects(SUBJECTS_SYSTEM).setTypeInfo(new FixedEntriesSubjectTypeDefinition<>(SUBJECTS_SYSTEM, ImmutableMap.of(
                "Server", () -> game.getServer().getConsole(),
                "RCON", () -> null)));
        getManager().getSubjects(SUBJECTS_USER).setTypeInfo(new UserSubjectTypeDescription(SUBJECTS_USER, this));


        registerFakeOpCommand("op", "minecraft.command.op");
        registerFakeOpCommand("deop", "minecraft.command.deop");


        // Registering the PEX service *must* occur after the plugin has been completely initialized
        if (!services.isRegistered(PermissionService.class)) {
            services.setProvider(this, PermissionService.class, this);
        } else {
            manager.close();
            throw new PEBKACException(Messages.PLUGIN_INIT_ERROR_OTHER_PROVIDER_INSTALLED.toComponent());
        }
    }

    private void registerFakeOpCommand(String alias, String permission) {
        registerCommands(() -> ImmutableSet.of(CommandSpecKt.command(new String[] {alias}, b -> {
            b.setPermission(new Permission(permission, null, 0));
            b.setDescription(Messages.COMMANDS_FAKE_OP_DESCRIPTION.toComponent());
            b.setArgs(string().key(Messages.COMMANDS_FAKE_OP_ARG_USER.toComponent()));
            b.executor((CommandExecutor) (src, ctx) -> {
                throw new CommandException(Messages.COMMANDS_FAKE_OP_ERROR.toComponent());
            });

            return Unit.INSTANCE;
        })));
    }

    @Listener
    public void cacheUserAsync(ClientConnectionEvent.Auth event) {
        try {
            getManager().getSubjects(PermissionsEx.SUBJECTS_USER).get(event.getProfile().getUniqueId().toString());
        } catch (Exception e) {
            logger.warn(Messages.EVENT_CLIENT_AUTH_ERROR.toComponent(event.getProfile().getName(), event.getProfile().getUniqueId(), e.getMessage()), e);
        }
    }

    @Listener
    public void disable(GameStoppedServerEvent event) {
        logger.debug(Messages.PLUGIN_SHUTDOWN_BEGIN.toComponent(PomData.NAME));
        PermissionsEx<?> manager = this.manager;
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
        getManager().getCallbackController().clearOwnedBy(event.getTargetEntity().getUniqueId());
        getUserSubjects().suggestUnload(event.getTargetEntity().getIdentifier());
    }

    Timings getTimings() {
        return timings;
    }

    private void convertFromBukkit() throws IOException {
        Path bukkitConfigPath = Paths.get("plugins/PermissionsEx");
        if (Files.isDirectory(bukkitConfigPath) && isDirectoryEmpty(configDir)) {
            logger.info(Messages.MIGRATION_BUKKIT_BEGIN.toComponent());
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
            logger.info(Messages.MIGRATION_LEGACY_SPONGE_SUCCESS.toComponent(configDir.toString()));
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


    @NonNull
    PermissionsEx<?> getManager() {
        if (this.manager == null) {
            throw new IllegalStateException("Manager is not yet initialized, or there was an error loading the plugin!");
        }
        return this.manager;
    }

    @Override
    public PEXSubjectCollection getUserSubjects() {
        try {
            return subjectCollections.get(SUBJECTS_USER).get();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PEXSubjectCollection getGroupSubjects() {
        try {
            return subjectCollections.get(SUBJECTS_GROUP).get();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PEXSubject getDefaults() {
        return defaults;
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<SubjectCollection> loadCollection(String identifier) {
        return (CompletableFuture) this.subjectCollections.get(identifier);
    }

    @Override
    public Optional<SubjectCollection> getCollection(String identifier) {
        return Optional.ofNullable(this.subjectCollections.getIfPresent(identifier)).map(CompletableFuture::join);
    }

    @Override
    public CompletableFuture<Boolean> hasCollection(String identifier) {
        return CompletableFuture.completedFuture(true); // we like to pretend
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, SubjectCollection> getLoadedCollections() {
        return (Map) this.subjectCollections.synchronous().asMap();
    }

    @Override
    public Predicate<String> getIdentifierValidityPredicate() {
        return x -> true; // we accept any string as a subject collection name
    }

    @Override
    public CompletableFuture<Set<String>> getAllIdentifiers() {
        return CompletableFuture.completedFuture(this.manager.getRegisteredSubjectTypes());
    }

    @Override
    public SubjectReference newSubjectReference(String collectionIdentifier, String subjectIdentifier) {
        return createSubjectIdentifier(collectionIdentifier, subjectIdentifier);
    }

    @Override
    public void registerContextCalculator(ContextCalculator<Subject> calculator) {
        contextCalculators.add(calculator);
    }

    @Override
    public PermissionDescription.Builder newDescriptionBuilder(Object instance) {
        Optional<PluginContainer> container = this.game.getPluginManager().fromInstance(instance);
        if (!container.isPresent()) {
            throw new IllegalArgumentException("Provided plugin did not have an associated plugin instance. Are you sure it's your plugin instance?");
        }
        return new PEXPermissionDescription.Builder(container.get(), this);
    }

    void registerDescription(final PEXPermissionDescription description, Map<String, Integer> ranks) {
        this.descriptions.put(description.getId(), description);
        final SubjectType coll = getManager().getSubjects(SUBJECTS_ROLE_TEMPLATE);
        for (final Map.Entry<String, Integer> rank : ranks.entrySet()) {
            try {
                coll.transientData().update(rank.getKey(),
                        input -> input.setPermission(PermissionsEx.GLOBAL_CONTEXT, description.getId(), rank.getValue())).get();
            } catch (InterruptedException | ExecutionException e) {
                Throwables.throwIfUnchecked(e);
                throw new RuntimeException(e);
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
    public Path getBaseDirectory(BaseDirectoryScope scope) {
        switch (scope) {
            case CONFIG:
                return configDir;
            case JAR:
                return game.getGameDirectory().resolve("mods");
            case SERVER:
                return game.getGameDirectory();
            case WORLDS:
                return game.getSavesDirectory();
            default:
                throw new IllegalArgumentException("Unknown directory scope" + scope);
        }
    }

    @Override
    public FormattedLogger getLogger() {
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
            logger.error(Messages.PLUGIN_DATA_SOURCE_ERROR.toComponent(url), e);
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

    private void tryRegisterCommands() {
        if (this.manager != null) {
            Supplier<Set<CommandSpec>> supply;

            while ((supply = cachedCommands.poll()) != null) {
                tryRegisterCommands(supply);
            }
        }
    }

    private void tryRegisterCommands(Supplier<Set<CommandSpec>> commandSupplier) {
        requireNonNull(this.manager);
        for (CommandSpec spec : commandSupplier.get()) {
            game.getCommandManager().register(this, new PEXSpongeCommand(spec, this), spec.getAliases());
        }
    }

    @Override
    public void registerCommands(Supplier<Set<CommandSpec>> specSupplier) {
        cachedCommands.add(specSupplier);
        tryRegisterCommands();
    }

    @Override
    public Set<CommandSpec> getImplementationCommands() {
        return ImmutableSet.of();
    }

    @Override
    public PEXSubjectReference createSubjectIdentifier(String collection, String ident) {
        return new PEXSubjectReference(collection, ident, this);
    }

    @Override
    public CompletableFuture<Integer> lookupMinecraftProfilesByName(Iterable<String> names, Function<MinecraftProfile, CompletableFuture<Void>> action) {
        return game.getServer().getGameProfileManager().getAllByName(names, true).thenComposeAsync(profiles -> CompletableFuture.allOf(profiles.stream()
                .map(profile -> action.apply(new SpongeMinecraftProfile(profile)))
                .toArray(CompletableFuture[]::new))
                .thenApply(it -> profiles.size()));
    }

    @Override
    public String getVersion() {
        return PomData.VERSION;
    }

    public Iterable<PEXSubject> getAllActiveSubjects() {
        return Iterables.concat(Iterables.transform(subjectCollections.synchronous().asMap().values(), PEXSubjectCollection::getActiveSubjects));
    }

    public Game getGame() {
        return game;
    }

    public <V> CachingValue<V> tickBasedCachingValue(long deltaTicks, Function0<V> update) {
        return new CachingValue<>(() -> (long) getGame().getServer().getRunningTimeTicks(), deltaTicks, update);
    }
}
