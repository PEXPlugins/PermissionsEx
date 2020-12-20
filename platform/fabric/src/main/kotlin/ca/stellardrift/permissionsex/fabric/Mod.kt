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
package ca.stellardrift.permissionsex.fabric

import ca.stellardrift.permissionsex.BaseDirectoryScope
import ca.stellardrift.permissionsex.ImplementationInterface
import ca.stellardrift.permissionsex.PermissionsEngine.SUBJECTS_DEFAULTS
import ca.stellardrift.permissionsex.PermissionsEx
import ca.stellardrift.permissionsex.PermissionsEx.GLOBAL_CONTEXT
import ca.stellardrift.permissionsex.commands.parse.CommandSpec
import ca.stellardrift.permissionsex.config.FilePermissionsExConfiguration
import ca.stellardrift.permissionsex.logging.FormattedLogger
import ca.stellardrift.permissionsex.logging.WrappingFormattedLogger
import ca.stellardrift.permissionsex.minecraft.MinecraftPermissionsEx
import ca.stellardrift.permissionsex.sql.hikari.Hikari
import ca.stellardrift.permissionsex.subject.SubjectType
import com.google.common.collect.Maps.immutableEntry
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.function.Supplier
import javax.sql.DataSource
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.ModContainer
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.WorldSavePath
import org.slf4j.LoggerFactory
import org.spongepowered.configurate.hocon.HoconConfigurationLoader

class PreLaunchInjector : PreLaunchEntrypoint {
    override fun onPreLaunch() {
        PreLaunchHacks.hackilyLoadForMixin("com.mojang.brigadier.Message")
        // PreLaunchHacks.hackilyLoadForMixin("ca.stellardrift.permissionsex.PermissionsEx")
        // TODO: Why is Adventure loading in app ClassLoader and not Knot, can we fix this in Loom?
    }
}

private const val MOD_ID: String = "permissionsex"
object PermissionsExMod : ImplementationInterface, ModInitializer {

    val manager: PermissionsEx<*>
        get() = requireNotNull(_manager?.engine()) { "PermissionsEx has not yet been initialized!" }
    val mcManager: MinecraftPermissionsEx<*>
        get() = requireNotNull(_manager) { "PermissionsEx has not yet been initialized!" }

    private var _manager: MinecraftPermissionsEx<*>? = null
    private lateinit var container: ModContainer
    private lateinit var dataDir: Path
    lateinit var server: MinecraftServer private set

    val available get() = _manager != null

    private lateinit var _logger: FormattedLogger
    private val exec = Executors.newCachedThreadPool()
    private val commands = mutableSetOf<Supplier<Set<CommandSpec>>>()

    val systemSubjectType = SubjectType.stringIdentBuilder("system")
        .fixedEntries(
            immutableEntry("console", Supplier { this.server }),
            immutableEntry(IDENTIFIER_RCON, Supplier { null })
        )
        .undefinedValues { true }
        .build()

    val commandBlockSubjectType = SubjectType.stringIdentBuilder("command-block")
        .undefinedValues { this.server.opPermissionLevel <= 2 }
        .build()

    override fun onInitialize() {
        this._logger = WrappingFormattedLogger.of(LoggerFactory.getLogger(MOD_ID), false)
        this.dataDir = FabricLoader.getInstance().configDir.resolve(MOD_ID)
        this.container = FabricLoader.getInstance().getModContainer(MOD_ID)
            .orElseThrow { IllegalStateException("Mod container for PermissionsEx was not available in init!") }
        logger().prefix("[${container.metadata.name}] ")

        logger().info(Messages.MOD_LOAD_SUCCESS.tr(container.metadata.version.friendlyString))
        ServerLifecycleEvents.SERVER_STARTING.register(ServerLifecycleEvents.ServerStarting { init(it) })
        ServerLifecycleEvents.SERVER_STOPPED.register(ServerLifecycleEvents.ServerStopped { shutdown() })

        // TODO: expose these commands earlier?
        CommandRegistrationCallback.EVENT.register(CommandRegistrationCallback { dispatcher, _ ->
            if (this._manager != null) {
                this._manager?.engine()?.registerCommandsTo {
                    registerCommand(it, dispatcher)
                }
            }
        })
        registerWorldEdit()
    }

    private fun init(gameInstance: MinecraftServer) {
        this.server = gameInstance
        Files.createDirectories(dataDir)

        val loader = HoconConfigurationLoader.builder()
            .path(dataDir.resolve("$MOD_ID.conf"))
            .defaultOptions { FilePermissionsExConfiguration.decorateOptions(it) }
            .build()

        try {
            _manager = MinecraftPermissionsEx.builder(FilePermissionsExConfiguration.fromLoader(loader))
                .implementationInterface(this)
                .playerProvider { gameInstance.playerManager.getPlayer(it) }
                .cachedUuidResolver { gameInstance.userCache.findByName(it)?.id }
                .opProvider { gameInstance.userCache.getByUuid(it)?.let(gameInstance.playerManager::isOperator) ?: false }
                .build()
        } catch (e: Exception) {
            logger().error(Messages.MOD_ENABLE_ERROR.tr(), e)
            server.stop(false)
            return
        }

        manager.registerContextDefinitions(WorldContextDefinition,
            DimensionContextDefinition,
            RemoteIpContextDefinition,
            LocalIpContextDefinition,
            LocalHostContextDefinition,
            LocalPortContextDefinition)
        manager.subjects(SUBJECTS_DEFAULTS).transientData().update(SUBJECTS_SYSTEM) {
            it.setDefaultValue(GLOBAL_CONTEXT, 1)
        }

        manager.subjects(systemSubjectType)
        manager.subjects(commandBlockSubjectType)

        gameInstance.commandManager?.also {
            manager.registerCommandsTo { cmd -> registerCommand(cmd, it.dispatcher) }
        }

        logger().info(Messages.MOD_ENABLE_SUCCESS.tr(container.metadata.version))
    }

    private fun shutdown() {
        val manager = _manager
        if (manager != null) {
            manager.close()
            _manager = null
        }
        this.exec.shutdown()
        try {
            this.exec.awaitTermination(10, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            logger().error(Messages.MOD_ERROR_SHUTDOWN_TIMEOUT.tr())
            this.exec.shutdownNow()
        }
    }

    fun handlePlayerJoin(player: ServerPlayerEntity) {
        mcManager.users()[player.uuid].thenAccept {
            // Update name option
            it.data().isRegistered.thenAccept { isReg ->
                if (isReg) {
                    it.data().update { data ->
                        data.setOption(GLOBAL_CONTEXT, "name", player.name.asString())
                    }
                }
            }

            // Add listener to re-send command tree on a permission update
            it.registerListener { newSubj ->
                (newSubj.associatedObject() as? ServerPlayerEntity)?.apply {
                    server.execute {
                        server.playerManager.sendCommandTree(this)
                    }
                }
            }
        }
    }

    fun handlePlayerQuit(player: ServerPlayerEntity) {
        _manager?.engine()?.callbackController?.clearOwnedBy(player.uuidAsString)
        _manager?.users()?.uncache(player.uuid)
    }

    override fun baseDirectory(scope: BaseDirectoryScope): Path {
        return when (scope) {
            BaseDirectoryScope.CONFIG -> dataDir
            BaseDirectoryScope.JAR -> FabricLoader.getInstance().gameDir.resolve("mods")
            BaseDirectoryScope.SERVER -> FabricLoader.getInstance().gameDir
            BaseDirectoryScope.WORLDS -> server.getSavePath(WorldSavePath.ROOT)
        }
    }

    override fun logger(): FormattedLogger {
        return _logger
    }

    override fun dataSourceForUrl(url: String): DataSource {
        return Hikari.createDataSource(url, this.dataDir)
    }

    override fun asyncExecutor(): Executor {
        return exec
    }

    override fun getImplementationSubcommands(): Set<CommandSpec> {
        return emptySet()
    }

    override fun getVersion(): String {
        return container.metadata.version.friendlyString
    }

    fun logUnredirectedPermissionsCheck(method: String) {
        logger().warn(Messages.MOD_ERROR_UNREDIRECTED_CHECK.tr(method))
        logger().debug(Messages.MOD_ERROR_UNREDIRECTED_CHECK.tr(method), Exception("call chain"))
    }
}

/** Old profile lookup -- would be good to bring back
override fun lookupMinecraftProfilesByName(
    namesIter: Iterable<String>,
    action: Function<MinecraftProfile, CompletableFuture<Void>>
): CompletableFuture<Int> {
    val futures = mutableListOf<CompletableFuture<Void>>()
    return CompletableFuture.supplyAsync({
        val names = Iterables.toArray(namesIter, String::class.java)
        val state = CountDownLatch(names.size)
        val callback = PEXProfileLookupCallback(state, action, futures)
        this.server.gameProfileRepo.findProfilesByNames(names, Agent.MINECRAFT, callback)
        state.await()
        futures.size
    }, asyncExecutor).thenCombine(CompletableFuture.allOf(*futures.toTypedArray())) { count, _ -> count }
}

internal class PEXProfileLookupCallback(private val state: CountDownLatch, private val action: Function<MinecraftProfile, CompletableFuture<Void>>, val futures: MutableList<CompletableFuture<Void>>) : ProfileLookupCallback {
    override fun onProfileLookupSucceeded(profile: GameProfile) {
        try {
            futures.add(action.apply(profile as MinecraftProfile))
        } finally {
            state.countDown()
        }
    }

    override fun onProfileLookupFailed(profile: GameProfile, exception: java.lang.Exception) {
        state.countDown()
        PermissionsExMod.logger.error(Messages.GAMEPROFILE_ERROR_LOOKUP(profile, exception.message.toString()), exception)
    }
} */
