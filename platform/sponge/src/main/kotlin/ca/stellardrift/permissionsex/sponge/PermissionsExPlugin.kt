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

package ca.stellardrift.permissionsex.sponge

import ca.stellardrift.permissionsex.BaseDirectoryScope
import ca.stellardrift.permissionsex.ImplementationInterface
import ca.stellardrift.permissionsex.PermissionsEx
import ca.stellardrift.permissionsex.commands.commander.Permission
import ca.stellardrift.permissionsex.commands.parse.CommandException
import ca.stellardrift.permissionsex.commands.parse.CommandSpec
import ca.stellardrift.permissionsex.commands.parse.command
import ca.stellardrift.permissionsex.commands.parse.string
import ca.stellardrift.permissionsex.config.FilePermissionsExConfiguration
import ca.stellardrift.permissionsex.exception.PEBKACException
import ca.stellardrift.permissionsex.exception.PermissionsException
import ca.stellardrift.permissionsex.logging.FormattedLogger
import ca.stellardrift.permissionsex.sponge.command.register
import ca.stellardrift.permissionsex.sponge.command.registerRegistrar
import ca.stellardrift.permissionsex.subject.FixedEntriesSubjectTypeDefinition
import ca.stellardrift.permissionsex.util.CachingValue
import ca.stellardrift.permissionsex.util.MinecraftProfile
import ca.stellardrift.permissionsex.util.SubjectIdentifier
import ca.stellardrift.permissionsex.util.optionally
import com.github.benmanes.caffeine.cache.Caffeine
import com.google.inject.Inject
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.sql.SQLException
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.function.Function
import java.util.function.Predicate
import java.util.function.Supplier
import javax.sql.DataSource
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.commented.CommentedConfigurationNode
import ninja.leaping.configurate.loader.ConfigurationLoader
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.spi.ExtendedLogger
import org.apache.logging.slf4j.Log4jLogger
import org.spongepowered.api.Game
import org.spongepowered.api.Server
import org.spongepowered.api.config.ConfigDir
import org.spongepowered.api.config.DefaultConfig
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.lifecycle.LifecycleEvent
import org.spongepowered.api.event.lifecycle.LoadedGameEvent
import org.spongepowered.api.event.lifecycle.ProvideServiceEvent
import org.spongepowered.api.event.lifecycle.RefreshGameEvent
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent
import org.spongepowered.api.event.network.ServerSideConnectionEvent
import org.spongepowered.api.service.context.ContextCalculator
import org.spongepowered.api.service.permission.PermissionDescription
import org.spongepowered.api.service.permission.PermissionService
import org.spongepowered.api.service.permission.Subject
import org.spongepowered.api.service.permission.SubjectCollection
import org.spongepowered.api.service.permission.SubjectReference
import org.spongepowered.api.sql.SqlManager
import org.spongepowered.plugin.PluginContainer
import org.spongepowered.plugin.jvm.Plugin

/**
 * PermissionsEx plugin
 */
@Plugin(ProjectData.ARTIFACT_ID)
class PermissionsExPlugin @Inject internal constructor(
    logger: Logger,
    internal val container: PluginContainer,
    internal val game: Game,
    private val sql: SqlManager,
    @ConfigDir(sharedRoot = false) private val configDir: Path,
    @DefaultConfig(sharedRoot = false) private val configLoader: ConfigurationLoader<CommentedConfigurationNode>
) : ImplementationInterface {
    internal val scheduler = game.asyncScheduler.createExecutor(container)
    private val logger = FormattedLogger.forLogger(Log4jLogger(logger as ExtendedLogger, logger.name), true)
    private var _manager: PermissionsEx<*>? = null
    val manager: PermissionsEx<*> get() {
        return _manager ?: throw IllegalStateException("Manager is not yet initialized, or there was an error loading the plugin!")
    }
    private var service: PermissionsExService? = null
    private val lifecycleLogger = LogManager.getLogger("Lifecycle")

    init {
        registerRegistrar(this)
    }

    @Listener
    fun onAnyLifecycle(event: LifecycleEvent) {
        lifecycleLogger.info("Lifecycle :: ${event.javaClass.simpleName}")
    }

    @Listener
    @Throws(PEBKACException::class, InterruptedException::class, ExecutionException::class)
    fun onPreInit(event: LoadedGameEvent) {
        logger.debug("state :: LoadedGameEvent")
        logger.info(Messages.PLUGIN_INIT_BEGIN(ProjectData.NAME, ProjectData.VERSION))
        try {
            convertFromBukkit()
            convertFromLegacySpongeName()
            Files.createDirectories(configDir)
            _manager = PermissionsEx(FilePermissionsExConfiguration.fromLoader(configLoader), this)
        } catch (e: Exception) {
            throw RuntimeException(PermissionsException(Messages.PLUGIN_INIT_ERROR_GENERAL(ProjectData.NAME), e))
        }
        manager.getSubjects(PermissionService.SUBJECTS_SYSTEM).typeInfo = FixedEntriesSubjectTypeDefinition<Any?>(
            PermissionService.SUBJECTS_SYSTEM, mapOf(
                "Server" to { event.game.systemSubject },
                "RCON" to { null })
        )
        manager.getSubjects(PermissionService.SUBJECTS_USER).typeInfo =
            UserSubjectTypeDefinition(PermissionService.SUBJECTS_USER, event.game)
    }

    @Listener
    fun registerPermissionService(event: ProvideServiceEvent<PermissionService>) {
        logger.debug("state :: ProvideServiceEvent<PermissionService>")
        event.suggest {
            service = PermissionsExService(event.game, this)
            service
        }
    }

    @Listener
    fun registerCommands(event: RegisterCommandEvent<CommandSpec>) {
        logger.debug("state :: RegisterCommandEvent<CommandSpec>")
        // fake op commands
        mapOf("op" to "minecraft.command.op",
            "deop" to "minecraft.command.deop").forEach { (alias, perm) ->
            event.register(container, command(alias) {
                permission = Permission(perm, null, 0)
                description = Messages.COMMANDS_FAKE_OP_DESCRIPTION()
                args = string().key(Messages.COMMANDS_FAKE_OP_ARG_USER())
                executor { _, _ -> throw CommandException(Messages.COMMANDS_FAKE_OP_ERROR()) }
            })
        }

        // TODO: register events
    }

    @Listener
    fun cacheUserAsync(event: ServerSideConnectionEvent.Auth) {
        logger.debug("state :: ServerSideConnectionEvent.Auth")
        try {
            manager.getSubjects(PermissionsEx.SUBJECTS_USER)[event.profile.uniqueId.toString()]
        } catch (e: Exception) {
            logger.warn(Messages.EVENT_CLIENT_AUTH_ERROR(event.profile.name, event.profile.uniqueId, e.message!!), e)
        }
    }

    // TODO: This is not the right event for integrated server. is there another event for game end?
    @Listener
    fun disable(event: StoppingEngineEvent<Server>) {
        logger.debug("state :: StoppingEngineEvent<Server>")
        logger.debug(Messages.PLUGIN_SHUTDOWN_BEGIN(ProjectData.NAME))
        this._manager?.close()
    }

    @Listener
    fun onReload(event: RefreshGameEvent) {
        logger.debug("state :: RefreshGameEvent")
        this._manager?.reload()
    }

    @Listener
    fun onPlayerJoin(event: ServerSideConnectionEvent.Join) {
        val identifier = event.player.identifier
        val cache = this.manager.getSubjects(PermissionsEx.SUBJECTS_USER)
        cache.isRegistered(identifier).thenAccept { registered: Boolean ->
            if (registered) {
                cache.persistentData().update(identifier) { input ->
                    if (event.player.name == input.getOptions(PermissionsEx.GLOBAL_CONTEXT)["name"]) {
                        return@update input
                    } else {
                        return@update input.setOption(PermissionsEx.GLOBAL_CONTEXT, "name", event.player.name)
                    }
                }
            }
        }
    }

    @Listener
    fun onPlayerQuit(event: ServerSideConnectionEvent.Disconnect) {
        manager.callbackController.clearOwnedBy(event.player.uniqueId)
        service?.userSubjects?.suggestUnload(event.player.identifier)
    }

    @Throws(IOException::class)
    private fun convertFromBukkit() {
        val bukkitConfigPath = Paths.get("plugins/PermissionsEx")
        if (Files.isDirectory(bukkitConfigPath) && configDir.isEmptyDirectory()) {
            logger.info(Messages.MIGRATION_BUKKIT_BEGIN())
            Files.move(bukkitConfigPath, configDir, StandardCopyOption.REPLACE_EXISTING)
        }
        val bukkitConfigFile = configDir.resolve("config.yml")
        if (Files.exists(bukkitConfigFile)) {
            val yamlReader: ConfigurationLoader<ConfigurationNode> =
                YAMLConfigurationLoader.builder().setPath(bukkitConfigFile).build()
            val bukkitConfig = yamlReader.load()
            configLoader.save(bukkitConfig)
            Files.move(bukkitConfigFile, configDir.resolve("config.yml.bukkit"))
        }
    }

    @Throws(IOException::class)
    private fun convertFromLegacySpongeName() {
        val oldPath = configDir.resolveSibling("ninja.leaping.permissionsex") // Old plugin ID
        if (Files.exists(oldPath) && configDir.isEmptyDirectory()) {
            Files.move(oldPath, configDir, StandardCopyOption.REPLACE_EXISTING)
            Files.move(
                configDir.resolve("ninja.leaping.permissionsex.conf"),
                configDir.resolve("${ProjectData.ARTIFACT_ID}.conf")
            )
            logger.info(Messages.MIGRATION_LEGACY_SPONGE_SUCCESS(configDir.toString()))
        }
    }

    @Throws(IOException::class)
    private fun Path.isEmptyDirectory(): Boolean {
        if (!Files.exists(this)) {
            return true
        }
        Files.newDirectoryStream(this).use { dirStream -> return !dirStream.iterator().hasNext() }
    }

    // ImplementationInterface

    override fun getBaseDirectory(scope: BaseDirectoryScope): Path {
        return when (scope) {
            BaseDirectoryScope.CONFIG -> configDir
            BaseDirectoryScope.JAR -> game.gameDirectory.resolve("mods")
            BaseDirectoryScope.SERVER -> game.gameDirectory
            BaseDirectoryScope.WORLDS -> TODO("level container")
        }
    }

    override fun getLogger(): FormattedLogger {
        return logger
    }

    override fun getDataSourceForURL(url: String): DataSource? {
        return try {
            sql.getDataSource(container, url)
        } catch (e: SQLException) {
            logger.error(Messages.PLUGIN_DATA_SOURCE_ERROR(url), e)
            null
        }
    }

    /**
     * Get an executor to run tasks asynchronously on.
     *
     * @return The async executor
     */
    override fun getAsyncExecutor(): Executor {
        return this.scheduler
    }

    override fun registerCommands(specSupplier: Supplier<Set<CommandSpec>>) {
        TODO("redesign")
    }

    override fun getImplementationCommands(): Set<CommandSpec> {
        return emptySet()
    }

    override fun createSubjectIdentifier(collection: String, ident: String): SubjectIdentifier {
        val service = this.service
        return if (service != null) {
            PEXSubjectReference(collection, ident, service)
        } else {
            super.createSubjectIdentifier(collection, ident)
        }
    }

    override fun lookupMinecraftProfilesByName(
        names: Iterable<String>,
        action: Function<MinecraftProfile, CompletableFuture<Void>>
    ): CompletableFuture<Int> {
        return game.server.gameProfileManager.getAllByName(names, true)
            .thenComposeAsync { profiles ->
                CompletableFuture.allOf(*profiles.filterValues { it.isPresent }
                    .map { action.apply(SpongeMinecraftProfile(it.value.get())) }
                    .toTypedArray())
                    .thenApply { profiles.size }
            }
    }

    override fun getVersion(): String {
        return ProjectData.VERSION
    }
}

class PermissionsExService internal constructor(private val game: Game, private val plugin: PermissionsExPlugin) : PermissionService {
    val manager get() = plugin.manager

    internal val contextCalculators: MutableList<ContextCalculator<Subject>> = CopyOnWriteArrayList()
    private val subjectCollections = Caffeine.newBuilder().executor(plugin.scheduler)
        .buildAsync<String, PEXSubjectCollection> { type, _ ->
            PEXSubjectCollection.load(type, this)
        }

    private var defaults: PEXSubject
    private val descriptions: MutableMap<String, PEXPermissionDescription> = ConcurrentHashMap()
    internal val timings = Timings(plugin.container)

    init {
        defaults = loadCollection(PermissionsEx.SUBJECTS_DEFAULTS)
            .thenCompose { coll -> coll.loadSubject(PermissionsEx.SUBJECTS_DEFAULTS) }
            .get() as PEXSubject
    }

    override fun getUserSubjects(): PEXSubjectCollection {
        // TODO: error handling
        return subjectCollections[PermissionService.SUBJECTS_USER].join()
    }

    override fun getGroupSubjects(): PEXSubjectCollection {
        // TODO: error handling
        return subjectCollections[PermissionService.SUBJECTS_GROUP].join()
    }

    override fun getDefaults(): PEXSubject {
        return defaults
    }

    override fun loadCollection(identifier: String): CompletableFuture<SubjectCollection> {
        @Suppress("UNCHECKED_CAST") // interface generics are unnecessarily strict
        return subjectCollections[identifier] as CompletableFuture<SubjectCollection>
    }

    override fun getCollection(identifier: String): Optional<SubjectCollection> {
        return subjectCollections.getIfPresent(identifier)?.join().optionally()
    }

    override fun hasCollection(identifier: String): CompletableFuture<Boolean> {
        return CompletableFuture.completedFuture(true) // we like to pretend
    }

    override fun getLoadedCollections(): Map<String, SubjectCollection> {
        return subjectCollections.synchronous().asMap()
    }

    override fun getIdentifierValidityPredicate(): Predicate<String> {
        return Predicate { true } // we accept any string as a subject collection name
    }

    override fun getAllIdentifiers(): CompletableFuture<Set<String>> {
        return CompletableFuture.completedFuture(manager.registeredSubjectTypes)
    }

    override fun newSubjectReference(collectionIdentifier: String, subjectIdentifier: String): SubjectReference {
        return manager.createSubjectIdentifier(collectionIdentifier, subjectIdentifier).asSponge(this)
    }

    override fun registerContextCalculator(calculator: ContextCalculator<Subject>) {
        contextCalculators.add(calculator)
    }

    override fun newDescriptionBuilder(container: PluginContainer): PermissionDescription.Builder {
        return PEXPermissionDescription.Builder(container, this)
    }

    fun registerDescription(description: PEXPermissionDescription, ranks: Map<String, Int>) {
        descriptions[description.id] = description
        val coll = manager.getSubjects(PermissionService.SUBJECTS_ROLE_TEMPLATE)
        for ((key, value) in ranks) {
            coll.transientData().update(key) { input ->
                input.setPermission(PermissionsEx.GLOBAL_CONTEXT, description.id, value)
            }.get()
        }
    }

    override fun getDescription(permission: String): Optional<PermissionDescription> {
        return descriptions[permission].optionally()
    }

    override fun getDescriptions(): Collection<PermissionDescription> {
        return descriptions.values.toSet()
    }

    val allActiveSubjects: Iterable<PEXSubject>
        get() = subjectCollections.synchronous().asMap().values.flatMap { it.activeSubjects }

    internal fun <V> tickBasedCachingValue(deltaTicks: Long, update: () -> V): CachingValue<V> {
        return CachingValue({
            if (game.isServerAvailable) {
                game.server.runningTimeTicks.toLong()
            } else {
                -1L
            }
        }, deltaTicks, update)
    }
}
