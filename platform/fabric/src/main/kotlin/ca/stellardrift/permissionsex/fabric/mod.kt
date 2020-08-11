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
import ca.stellardrift.permissionsex.PermissionsEx
import ca.stellardrift.permissionsex.PermissionsEx.GLOBAL_CONTEXT
import ca.stellardrift.permissionsex.PermissionsEx.SUBJECTS_DEFAULTS
import ca.stellardrift.permissionsex.PermissionsEx.SUBJECTS_USER
import ca.stellardrift.permissionsex.commands.parse.CommandSpec
import ca.stellardrift.permissionsex.config.FilePermissionsExConfiguration
import ca.stellardrift.permissionsex.hikariconfig.createHikariDataSource
import ca.stellardrift.permissionsex.logging.FormattedLogger
import ca.stellardrift.permissionsex.util.MinecraftProfile
import com.google.common.collect.Iterables
import com.mojang.authlib.Agent
import com.mojang.authlib.GameProfile
import com.mojang.authlib.ProfileLookupCallback
import com.mojang.brigadier.CommandDispatcher
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.function.Function
import java.util.function.Supplier
import javax.sql.DataSource
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.ModContainer
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.WorldSavePath
import ninja.leaping.configurate.hocon.HoconConfigurationLoader
import org.slf4j.LoggerFactory

class PreLaunchInjector : PreLaunchEntrypoint {
    override fun onPreLaunch() {
        PreLaunchHacks.hackilyLoadForMixin("com.mojang.brigadier.Message")
        // PreLaunchHacks.hackilyLoadForMixin("ca.stellardrift.permissionsex.PermissionsEx")
        // TODO: Why is Kyori-Text loading in app ClassLoader and not Knot
    }
}

private const val MOD_ID: String = "permissionsex"
object PermissionsExMod : ImplementationInterface, ModInitializer {

    val manager: PermissionsEx<*>
    get() {
        val temp = _manager
        if (temp != null) {
            return temp
        } else {
            throw IllegalStateException("PermissionsEx has not yet been initialized!")
        }
    }
    private var _manager: PermissionsEx<*>? = null
    private lateinit var container: ModContainer
    private lateinit var dataDir: Path
    lateinit var server: MinecraftServer private set

    val available get() = _manager != null

    private lateinit var _logger: FormattedLogger
    private val exec = Executors.newCachedThreadPool()
    private val commands = mutableSetOf<Supplier<Set<CommandSpec>>>()

    override fun onInitialize() {
        this._logger = FormattedLogger.forLogger(LoggerFactory.getLogger(MOD_ID), false)
        this.dataDir = FabricLoader.getInstance().configDir.resolve(MOD_ID)
        this.container = FabricLoader.getInstance().getModContainer(MOD_ID)
            .orElseThrow { IllegalStateException("Mod container for PermissionsEx was not available in init!") }
        logger.prefix = "[${container.metadata.name}] "

        logger.info(Messages.MOD_LOAD_SUCCESS(container.metadata.version.friendlyString))
        ServerLifecycleEvents.SERVER_STARTED.register(ServerLifecycleEvents.ServerStarted { init(it) })
        ServerLifecycleEvents.SERVER_STOPPED.register(ServerLifecycleEvents.ServerStopped { shutdown() })
        CommandRegistrationCallback.EVENT.register(CommandRegistrationCallback { dispatcher, _ ->
            tryRegisterCommands(dispatcher)
        })
        registerWorldEdit()
    }

    private fun init(gameInstance: MinecraftServer) {
        this.server = gameInstance
        Files.createDirectories(dataDir)

        val loader = HoconConfigurationLoader.builder()
            .setPath(dataDir.resolve("$MOD_ID.conf"))
            .build()

        try {
            _manager = PermissionsEx(FilePermissionsExConfiguration.fromLoader(loader), this)
        } catch (e: Exception) {
            logger.error(Messages.MOD_ENABLE_ERROR(), e)
            server.stop(false)
            return
        }
        tryRegisterCommands()

        manager.registerContextDefinitions(WorldContextDefinition,
            DimensionContextDefinition,
            RemoteIpContextDefinition,
            LocalIpContextDefinition,
            LocalHostContextDefinition,
            LocalPortContextDefinition)
        manager.getSubjects(SUBJECTS_USER).typeInfo = UserSubjectTypeDefinition()
        manager.getSubjects(SUBJECTS_DEFAULTS).transientData().update(SUBJECTS_SYSTEM) {
            it.setDefaultValue(GLOBAL_CONTEXT, 1)
        }
        tryRegisterCommands()
        logger.info(Messages.MOD_ENABLE_SUCCESS(container.metadata.version))
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
            logger.error(Messages.MOD_ERROR_SHUTDOWN_TIMEOUT())
            this.exec.shutdownNow()
        }
    }

    fun handlePlayerJoin(player: ServerPlayerEntity) {
        manager.getSubjects(SUBJECTS_USER).get(player.uuidAsString).thenAccept {
            // Update name option
            it.data().cache.isRegistered(it.identifier.value).thenAccept { isReg ->
                if (isReg) {
                    it.data().update { data ->
                        data.setOption(GLOBAL_CONTEXT, "name", player.name.asString())
                    }
                }
            }

            // Add listener to re-send command tree on a permission update
            it.registerListener { newSubj ->
                (newSubj.associatedObject as? ServerPlayerEntity)?.apply {
                    server.playerManager.sendCommandTree(this)
                }
            }
        }
    }

    fun handlePlayerQuit(player: ServerPlayerEntity) {
        _manager?.callbackController?.clearOwnedBy(player.uuidAsString)
        _manager?.getSubjects(SUBJECTS_USER)?.uncache(player.uuidAsString)
    }

    override fun getBaseDirectory(scope: BaseDirectoryScope): Path {
        return when (scope) {
            BaseDirectoryScope.CONFIG -> dataDir
            BaseDirectoryScope.JAR -> FabricLoader.getInstance().gameDir.resolve("mods")
            BaseDirectoryScope.SERVER -> FabricLoader.getInstance().gameDir
            BaseDirectoryScope.WORLDS -> server.getSavePath(WorldSavePath.ROOT)
        }
    }

    override fun getLogger(): FormattedLogger {
        return _logger
    }

    override fun getDataSourceForURL(url: String): DataSource {
        return createHikariDataSource(url, dataDir)
    }

    override fun getAsyncExecutor(): Executor {
        return exec
    }

    override fun registerCommands(commandSupplier: Supplier<Set<CommandSpec>>) {
        synchronized(commands) {
                commands.add(commandSupplier)
                tryRegisterCommands()
        }
    }

    private fun tryRegisterCommands(possibleDispatch: CommandDispatcher<ServerCommandSource>? = null) {
        val dispatcher = if (possibleDispatch == null && this::server.isInitialized) {
            server.commandManager.dispatcher
        } else {
            possibleDispatch
        }
        if (dispatcher != null && _manager != null) {
            synchronized(commands) {
                commands.forEach {
                    it.get().forEach { cmd ->
                        registerCommand(cmd, dispatcher)
                    }
                }
                commands.clear() // TODO: Remove if we stop re-creating the PEX instance
            }
        }
    }

    override fun getImplementationCommands(): Set<CommandSpec> {
        return emptySet()
    }

    override fun getVersion(): String {
        return container.metadata.version.friendlyString
    }

    override fun lookupMinecraftProfilesByName(
        namesIter: Iterable<String>,
        action: Function<MinecraftProfile, CompletableFuture<Void>>
    ): CompletableFuture<Int> {
        val futures = mutableListOf<CompletableFuture<Void>>()
        return CompletableFuture.supplyAsync(Supplier {
            val names = Iterables.toArray(namesIter, String::class.java)
            val state = CountDownLatch(names.size)
            val callback = PEXProfileLookupCallback(state, action, futures)
            this.server.gameProfileRepo.findProfilesByNames(names, Agent.MINECRAFT, callback)
            state.await()
            futures.size
        }, asyncExecutor).thenCombine(CompletableFuture.allOf(*futures.toTypedArray())) { count, _ -> count }
    }

    fun logUnredirectedPermissionsCheck(method: String) {
        logger.warn(Messages.MOD_ERROR_UNREDIRECTED_CHECK(method))
        logger.debug(Messages.MOD_ERROR_UNREDIRECTED_CHECK(method), Exception("call chain"))
    }
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
}
