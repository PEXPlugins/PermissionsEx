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
 *
 */

package ca.stellardrift.permissionsex.fabric

import ca.stellardrift.permissionsex.ImplementationInterface
import ca.stellardrift.permissionsex.PermissionsEx
import ca.stellardrift.permissionsex.PermissionsEx.GLOBAL_CONTEXT
import ca.stellardrift.permissionsex.PermissionsEx.SUBJECTS_DEFAULTS
import ca.stellardrift.permissionsex.PermissionsEx.SUBJECTS_USER
import ca.stellardrift.permissionsex.config.FilePermissionsExConfiguration
import ca.stellardrift.permissionsex.hikariconfig.createHikariDataSource
import ca.stellardrift.permissionsex.logging.TranslatableLogger
import ca.stellardrift.permissionsex.util.Translations.t
import ca.stellardrift.permissionsex.util.Util
import ca.stellardrift.permissionsex.util.command.CommandSpec
import com.mojang.brigadier.CommandDispatcher
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.server.ServerStartCallback
import net.fabricmc.fabric.api.event.server.ServerStopCallback
import net.fabricmc.fabric.api.registry.CommandRegistry
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.ModContainer
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import ninja.leaping.configurate.hocon.HoconConfigurationLoader
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

private const val MOD_ID: String = "permissionsex"
object PermissionsExMod : ImplementationInterface, ModInitializer {

    val manager: PermissionsEx
    get() {
        val temp = _manager
        if (temp != null) {
            return temp
        } else {
            throw IllegalStateException("PermissionsEx has not yet been initialized!")
        }
    }
    private var _manager: PermissionsEx? = null
    private lateinit var container: ModContainer
    private lateinit var dataDir: Path
    lateinit var server: MinecraftServer private set

    private val _logger = TranslatableLogger.forLogger(LoggerFactory.getLogger(MOD_ID))
    private val exec = Executors.newCachedThreadPool()
    private val commands = mutableListOf<CommandSpec>()


    override fun onInitialize() {
        this.dataDir = FabricLoader.getInstance().configDirectory.toPath().resolve(MOD_ID)
        this.container = FabricLoader.getInstance().getModContainer(MOD_ID)
            .orElseThrow { IllegalStateException("Mod container for PermissionsEx was not available in init!") }

        logger.info(t("Loaded mod %s v%s", container.metadata.name, container.metadata.version.friendlyString))
        ServerStartCallback.EVENT.register(ServerStartCallback {init(it) })
        ServerStopCallback.EVENT.register(ServerStopCallback {  shutdown(it) })
        CommandRegistry.INSTANCE.register(true) {
            tryRegisterCommands(it)
        }
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
            logger.error(t("Unable to enable PEX"), e)
            server.stop(false)
            return
        }

        manager.registerContextDefinition(WorldContextDefinition)
        manager.registerContextDefinition(DimensionContextDefinition)
        manager.getSubjects(SUBJECTS_USER).typeInfo = UserSubjectTypeDefinition()
        manager.getSubjects(SUBJECTS_DEFAULTS).transientData().update(SUBJECTS_SYSTEM) {
            it.setDefaultValue(GLOBAL_CONTEXT, 1)
        }
        logger.info(t("%s v%s successfully enabled! Welcome!", container.metadata.name, container.metadata.version))
    }

    private fun shutdown(server: MinecraftServer) {
        val manager = _manager
        if (manager != null) {
            manager.close()
            _manager = null
        }
        this.exec.shutdown()
        try {
            this.exec.awaitTermination(10, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            logger.error(t("Unable to shut down PermissionsEx executor in 10 seconds, remaining tasks will be killed"))
            this.exec.shutdownNow()
        }
    }

    fun handlePlayerJoin(player: ServerPlayerEntity) {
        manager.getSubjects(SUBJECTS_USER).get(player.uuidAsString).thenAccept {
            // Update name option
            it.data().cache.isRegistered(it.identifier.value).thenAccept {isReg ->
                if (isReg) {
                    it.data().update {data ->
                        data.setOption(GLOBAL_CONTEXT, "name", player.name.asString())
                    }
                }
            }

            // Add listener to re-send command tree on a permission update
            it.registerListener { newSubj ->
                Util.castOptional(newSubj.associatedObject, ServerPlayerEntity::class.java).ifPresent { ply ->
                    ply.server.playerManager.sendCommandTree(ply)
                }
            }
        }
    }

    fun handlePlayerQuit(player: ServerPlayerEntity) {
            _manager?.getSubjects(SUBJECTS_USER)?.uncache(player.uuidAsString)
    }

    override fun getBaseDirectory(): Path {
        return dataDir
    }

    override fun getLogger(): TranslatableLogger {
        return _logger
    }

    override fun getDataSourceForURL(url: String): DataSource {
        return createHikariDataSource(url, dataDir)
    }

    override fun getAsyncExecutor(): Executor {
        return exec
    }

    override fun registerCommand(command: CommandSpec) {
        synchronized (commands) {
            if (this::server.isInitialized) {
                registerCommand(command, this.server.commandManager.dispatcher)
            } else {
                commands.add(command)
            }
        }
    }

    private fun tryRegisterCommands(possibleDispatch: CommandDispatcher<ServerCommandSource>? = null) {
        synchronized (commands) {
            val dispatcher = if (possibleDispatch == null && this::server.isInitialized) {
                server.commandManager.dispatcher
            } else {
                possibleDispatch
            }
            if (dispatcher != null) {
                commands.forEach {
                    registerCommand(it, dispatcher)
                }
                commands.clear()
            }
        }
    }

    override fun getImplementationCommands(): Set<CommandSpec> {
        return setOf()
    }

    override fun getVersion(): String {
        return container.metadata.version.friendlyString
    }

}