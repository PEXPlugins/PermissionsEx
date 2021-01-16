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

import ca.stellardrift.permissionsex.PermissionsEngine
import ca.stellardrift.permissionsex.exception.PEBKACException
import ca.stellardrift.permissionsex.exception.PermissionsException
import ca.stellardrift.permissionsex.impl.PermissionsEx
import ca.stellardrift.permissionsex.impl.logging.WrappingFormattedLogger
import ca.stellardrift.permissionsex.impl.util.CachingValue
import ca.stellardrift.permissionsex.minecraft.BaseDirectoryScope
import ca.stellardrift.permissionsex.minecraft.MinecraftPermissionsEx
import ca.stellardrift.permissionsex.minecraft.command.CommandRegistrationContext
import ca.stellardrift.permissionsex.minecraft.command.Commander
import ca.stellardrift.permissionsex.minecraft.command.Permission
import ca.stellardrift.permissionsex.sponge.command.SpongeApi7CommandManager
import ca.stellardrift.permissionsex.sponge.command.SpongeApi7MetaKeys
import ca.stellardrift.permissionsex.subject.SubjectType
import cloud.commandframework.arguments.standard.StringArgument
import cloud.commandframework.permission.CommandPermission
import com.github.benmanes.caffeine.cache.Caffeine
import com.google.inject.Inject
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.Optional
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.function.Predicate
import java.util.function.Supplier
import net.kyori.adventure.platform.spongeapi.SpongeAudiences
import org.slf4j.Logger
import org.spongepowered.api.Game
import org.spongepowered.api.config.ConfigDir
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.game.GameReloadEvent
import org.spongepowered.api.event.game.state.GamePreInitializationEvent
import org.spongepowered.api.event.game.state.GameStoppedServerEvent
import org.spongepowered.api.event.network.ClientConnectionEvent
import org.spongepowered.api.plugin.Plugin
import org.spongepowered.api.plugin.PluginContainer
import org.spongepowered.api.scheduler.Scheduler
import org.spongepowered.api.service.ServiceManager
import org.spongepowered.api.service.context.ContextCalculator
import org.spongepowered.api.service.permission.PermissionDescription
import org.spongepowered.api.service.permission.PermissionService
import org.spongepowered.api.service.permission.Subject
import org.spongepowered.api.service.permission.SubjectCollection
import org.spongepowered.api.service.permission.SubjectReference
import org.spongepowered.api.service.sql.SqlService
import org.spongepowered.api.util.annotation.NonnullByDefault
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.hocon.HoconConfigurationLoader
import org.spongepowered.configurate.loader.ConfigurationLoader
import org.spongepowered.configurate.util.UnmodifiableCollections.immutableMapEntry
import org.spongepowered.configurate.yaml.YamlConfigurationLoader

/**
 * PermissionsEx plugin
 */
@NonnullByDefault
@Plugin(id = PomData.ARTIFACT_ID, name = PomData.NAME, version = PomData.VERSION, description = PomData.DESCRIPTION)
class PermissionsExPlugin @Inject internal constructor(
    logger: Logger,
    val game: Game,
    private val services: ServiceManager,
    private val container: PluginContainer,
    @ConfigDir(sharedRoot = false) private val configDir: Path,
    internal val adventure: SpongeAudiences
) : PermissionService {
    private var sql: Optional<SqlService> = Optional.empty()
    private var scheduler: Scheduler = game.scheduler
    private val logger = WrappingFormattedLogger.of(logger, true)
    private var _manager: MinecraftPermissionsEx<*>? = null
    val manager: PermissionsEx<*>
        get() {
        return _manager?.engine() ?: throw IllegalStateException("Manager is not yet initialized, or there was an error loading the plugin!")
    }

    internal val mcManager: MinecraftPermissionsEx<*> get() {
        return _manager ?: throw IllegalStateException("Manager is not yet initialized, or there was an error loading the plugin!")
    }

    private val spongeExecutor = Executor { runnable: Runnable ->
        scheduler
            .createTaskBuilder()
            .async()
            .execute(runnable)
            .submit(this@PermissionsExPlugin)
    }
    private val configFile = this.configDir.resolve("${PomData.ARTIFACT_ID}.conf")

    internal val contextCalculators: MutableList<ContextCalculator<Subject>> = CopyOnWriteArrayList()
    @Suppress("UNCHECKED_CAST") // whee
    private val subjectCollections = Caffeine.newBuilder().executor(spongeExecutor)
        .buildAsync<SubjectType<*>, PEXSubjectCollection<*>> { type, _ ->
            PEXSubjectCollection.load(type, this) as CompletableFuture<PEXSubjectCollection<*>>
        }

    private var defaults: PEXSubject? = null
    private val descriptions: MutableMap<String, PEXPermissionDescription> = ConcurrentHashMap()
    lateinit var timings: Timings
        private set

    // Subject types defined by Sponge
    val systemSubjectType = SubjectType.stringIdentBuilder(PermissionService.SUBJECTS_SYSTEM)
        .fixedEntries(
            immutableMapEntry("Server", Supplier { game.server.console }),
            immutableMapEntry("RCON", Supplier { null })
        )
        .undefinedValues { true }
        .build()

    val roleTemplateSubjectType = SubjectType.stringIdentBuilder(PermissionService.SUBJECTS_ROLE_TEMPLATE)
        .build()

    val commandBlockSubjectType = SubjectType.stringIdentBuilder(PermissionService.SUBJECTS_COMMAND_BLOCK)
        .undefinedValues { true }
        .build()

    @Listener
    @Throws(PEBKACException::class, InterruptedException::class, ExecutionException::class)
    fun onPreInit(event: GamePreInitializationEvent) {
        timings = Timings(this)
        logger.info(Messages.PLUGIN_INIT_BEGIN.tr(PomData.NAME, PomData.VERSION))
        sql = services.provide(SqlService::class.java)
        try {
            convertFromBukkit()
            convertFromLegacySpongeName()
            Files.createDirectories(configDir)
            _manager = MinecraftPermissionsEx.builder<Unit>()
                .configuration(this.configFile)
                .baseDirectory(this.configDir)
                .logger(this.logger)
                .asyncExecutor(this.spongeExecutor)
                .baseDirectory(BaseDirectoryScope.JAR, game.gameDirectory.resolve("mods"))
                .baseDirectory(BaseDirectoryScope.SERVER, game.gameDirectory)
                .baseDirectory(BaseDirectoryScope.WORLDS, game.savesDirectory)
                .playerProvider { id -> game.server.getPlayer(id).orElse(null) }
                .databaseProvider {
                    if (!sql.isPresent) {
                        null
                    } else {
                        sql.get().getDataSource(this, it)
                    }
                }.cachedUuidResolver resolved@{ name ->
                    val res = game.server.gameProfileManager.cache
                    for (profile in res.match(name)) {
                        if (profile.name.isPresent && profile.name.get().equals(name, ignoreCase = true)) {
                            return@resolved profile.uniqueId
                        }
                    }
                    null
                }.commands { coord ->
                    object : SpongeApi7CommandManager<Commander>(
                        this.container,
                        coord,
                        { SpongeCommander(this, it) },
                        { (it as SpongeCommander).commandSource }
                    ) {
                        override fun hasPermission(sender: Commander, permission: CommandPermission): Boolean {
                            if (permission is Permission) {
                                return sender.hasPermission(permission)
                            }
                            return super.hasPermission(sender, permission)
                        }
                    }
                }
                .messageFormatter(::SpongeMessageFormatter)
                .commandContributor(this::registerFakeOpCommand)
                .create()
        } catch (e: Exception) {
            throw RuntimeException(PermissionsException(Messages.PLUGIN_INIT_ERROR_GENERAL.tr(PomData.NAME), e))
        }
        defaults = loadCollection(manager.defaults().type())
            .thenCompose { coll -> coll.loadSubject(
                manager.defaults().type().name()
            ) }.get() as PEXSubject
        manager.subjects(systemSubjectType)
        manager.subjects(roleTemplateSubjectType)
        manager.subjects(commandBlockSubjectType)

        // Registering the PEX service *must* occur after the plugin has been completely initialized
        if (!services.isRegistered(PermissionService::class.java)) {
            services.setProvider(this, PermissionService::class.java, this)
        } else {
            _manager?.close()
            throw PEBKACException(Messages.PLUGIN_INIT_ERROR_OTHER_PROVIDER_INSTALLED.tr())
        }
    }

    private fun registerFakeOpCommand(ctx: CommandRegistrationContext) {
        fun register(name: String, permission: String) {
            ctx.register(ctx.absoluteBuilder(name)
                .argument(StringArgument.of("user"))
                .permission(permission)
                .meta(SpongeApi7MetaKeys.RICH_DESCRIPTION, Messages.COMMANDS_FAKE_OP_DESCRIPTION.tr().toSponge())
                .handler {
                    it.sender.error(Messages.COMMANDS_FAKE_OP_ERROR.tr())
                })
        }

        register("op", "minecraft.command.op")
        register("deop", "minecraft.command.deop")
    }

    @Listener
    fun cacheUserAsync(event: ClientConnectionEvent.Auth) {
        try {
            this.mcManager.users()[event.profile.uniqueId]
        } catch (e: Exception) {
            logger.warn(
                Messages.EVENT_CLIENT_AUTH_ERROR.tr(
                    event.profile.name,
                    event.profile.uniqueId,
                    e.message!!
                ), e
            )
        }
    }

    @Listener
    fun disable(event: GameStoppedServerEvent) {
        logger.debug(Messages.PLUGIN_SHUTDOWN_BEGIN.tr(PomData.NAME))
        this._manager?.close()
    }

    @Listener
    fun onReload(event: GameReloadEvent) {
        this._manager?.engine()?.reload()
    }

    @Listener
    fun onPlayerJoin(event: ClientConnectionEvent.Join) {
        val identifier = event.targetEntity.uniqueId
        val cache = this.mcManager.users()
        cache.isRegistered(identifier).thenAccept { registered: Boolean ->
            if (registered) {
                cache.persistentData().update(identifier) { input ->
                    input.withSegment(PermissionsEngine.GLOBAL_CONTEXT) {
                        if (event.targetEntity.name == it.options()["name"]) {
                            it
                        } else {
                            it.withOption("name", event.targetEntity.name)
                        }
                    }
                }
            }
        }
    }

    @Listener
    fun onPlayerQuit(event: ClientConnectionEvent.Disconnect) {
        mcManager.callbackController().clearOwnedBy(event.targetEntity.uniqueId)
        userSubjects.suggestUnload(event.targetEntity.identifier)
    }

    @Throws(IOException::class)
    private fun convertFromBukkit() {
        val bukkitConfigPath = Paths.get("plugins/PermissionsEx")
        if (Files.isDirectory(bukkitConfigPath) && isDirectoryEmpty(configDir)) {
            logger.info(Messages.MIGRATION_BUKKIT_BEGIN.tr())
            Files.move(bukkitConfigPath, configDir, StandardCopyOption.REPLACE_EXISTING)
        }
        val bukkitConfigFile = configDir.resolve("config.yml")
        if (Files.exists(bukkitConfigFile)) {
            val yamlReader: ConfigurationLoader<CommentedConfigurationNode> =
                YamlConfigurationLoader.builder().path(bukkitConfigFile).build()
            val bukkitConfig = yamlReader.load()

            HoconConfigurationLoader.builder()
                .path(this.configFile)
                .build()
                .save(bukkitConfig)
            Files.move(bukkitConfigFile, configDir.resolve("config.yml.bukkit"))
        }
    }

    @Throws(IOException::class)
    private fun convertFromLegacySpongeName() {
        val oldPath = configDir.resolveSibling("ninja.leaping.permissionsex") // Old plugin ID
        if (Files.exists(oldPath) && isDirectoryEmpty(configDir)) {
            Files.move(oldPath, configDir, StandardCopyOption.REPLACE_EXISTING)
            Files.move(
                configDir.resolve("ninja.leaping.permissionsex.conf"),
                configDir.resolve("${PomData.ARTIFACT_ID}.conf")
            )
            logger.info(Messages.MIGRATION_LEGACY_SPONGE_SUCCESS.tr(configDir.toString()))
        }
    }

    @Throws(IOException::class)
    private fun isDirectoryEmpty(dir: Path): Boolean {
        if (Files.exists(dir)) {
            return true
        }
        Files.newDirectoryStream(dir).use { dirStream -> return !dirStream.iterator().hasNext() }
    }

    internal fun subjectTypeFromIdentifier(identifier: String): SubjectType<*> {
        val existing = this.manager.knownSubjectTypes().firstOrNull { it.name() == identifier }
        if (existing != null) return existing

        // There's nothing registered, but Sponge doesn't have a concept of types, so we'll create a fallback string type
        return SubjectType.stringIdentBuilder(identifier).build()
    }

    internal fun <I> getSubjects(type: SubjectType<I>): PEXSubjectCollection<I> {
        @Suppress("UNCHECKED_CAST")
        return subjectCollections[type].join() as PEXSubjectCollection<I>
    }

    override fun getUserSubjects(): PEXSubjectCollection<UUID> {
        return this.getSubjects(this.mcManager.users().type())
    }

    override fun getGroupSubjects(): PEXSubjectCollection<String> {
        return this.getSubjects(this.mcManager.groups().type())
    }

    override fun getDefaults(): PEXSubject {
        return defaults!!
    }

    override fun loadCollection(identifier: String): CompletableFuture<SubjectCollection> {
        @Suppress("UNCHECKED_CAST") // interface generics are unnecessarily strict
        return subjectCollections[this.subjectTypeFromIdentifier(identifier)] as CompletableFuture<SubjectCollection>
    }

    fun loadCollection(identifier: SubjectType<*>): CompletableFuture<out SubjectCollection> {
        return subjectCollections[identifier]
    }

    override fun getCollection(identifier: String): Optional<SubjectCollection> {
        return subjectCollections.getIfPresent(this.subjectTypeFromIdentifier(identifier))?.join().optionally()
    }

    override fun hasCollection(identifier: String): CompletableFuture<Boolean> {
        return CompletableFuture.completedFuture(true) // we like to pretend
    }

    override fun getLoadedCollections(): Map<String, SubjectCollection> {
        return subjectCollections.synchronous().asMap().mapKeys { it.key.name() }
    }

    override fun getIdentifierValidityPredicate(): Predicate<String> {
        return Predicate { true } // we accept any string as a subject collection name
    }

    override fun getAllIdentifiers(): CompletableFuture<Set<String>> {
        return CompletableFuture.completedFuture(manager.knownSubjectTypes().map { it.name() }.toSet())
    }

    override fun newSubjectReference(collectionIdentifier: String, subjectIdentifier: String): SubjectReference {

        return PEXSubjectReference.of(this.subjectTypeFromIdentifier(collectionIdentifier), subjectIdentifier, this)
    }

    override fun registerContextCalculator(calculator: ContextCalculator<Subject>) {
        contextCalculators.add(calculator)
    }

    override fun newDescriptionBuilder(instance: Any): PermissionDescription.Builder {
        val container = game.pluginManager.fromInstance(instance)
        require(container.isPresent) { "Provided plugin did not have an associated plugin instance. Are you sure it's your plugin instance?" }
        return PEXPermissionDescription.Builder(container.get(), this)
    }

    fun registerDescription(description: PEXPermissionDescription, ranks: Map<String, Int>) {
        descriptions[description.id] = description
        val coll = manager.subjects(roleTemplateSubjectType)
        for ((key, value) in ranks) {
            coll.transientData().update(key) { input ->
                input.withSegment(PermissionsEx.GLOBAL_CONTEXT) { it.withPermission(description.id, value) }
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

    fun <V> tickBasedCachingValue(deltaTicks: Long, update: () -> V): CachingValue<V> {
        return CachingValue(
            { game.server.runningTimeTicks.toLong() },
            deltaTicks,
            update
        )
    }
}
