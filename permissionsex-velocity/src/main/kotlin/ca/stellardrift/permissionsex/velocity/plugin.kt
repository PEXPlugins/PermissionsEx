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

package ca.stellardrift.permissionsex.velocity

import ca.stellardrift.permissionsex.ImplementationInterface
import ca.stellardrift.permissionsex.PermissionsEx
import ca.stellardrift.permissionsex.PermissionsEx.GLOBAL_CONTEXT
import ca.stellardrift.permissionsex.PermissionsEx.SUBJECTS_USER
import ca.stellardrift.permissionsex.config.FilePermissionsExConfiguration
import ca.stellardrift.permissionsex.hikariconfig.createHikariDataSource
import ca.stellardrift.permissionsex.logging.TranslatableLogger
import ca.stellardrift.permissionsex.proxycommon.IDENT_SERVER_CONSOLE
import ca.stellardrift.permissionsex.proxycommon.ProxyContextDefinition
import ca.stellardrift.permissionsex.proxycommon.SUBJECTS_SYSTEM
import ca.stellardrift.permissionsex.util.MinecraftProfile
import ca.stellardrift.permissionsex.util.MinecraftProfileImpl
import ca.stellardrift.permissionsex.util.Translations.t
import ca.stellardrift.permissionsex.util.command.CommandSpec
import ca.stellardrift.permissionsex.util.resolveMinecraftProfile
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.permission.PermissionsSetupEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyReloadEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.permission.PermissionProvider
import com.velocitypowered.api.permission.Tristate
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import ninja.leaping.configurate.hocon.HoconConfigurationLoader
import org.slf4j.Logger
import reactor.core.publisher.Flux
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.sql.DataSource

@Plugin(
    id = ProjectData.ARTIFACT_ID,
    name = ProjectData.NAME,
    version = ProjectData.VERSION,
    description = ProjectData.DESCRIPTION
)
class PermissionsExPlugin @Inject constructor(
    rawLogger: Logger,
    internal val server: ProxyServer, @DataDirectory private val dataPath: Path
) : ImplementationInterface {

    private val exec = Executors.newCachedThreadPool()
    val _scheduler: Scheduler = Schedulers.fromExecutor(exec)

    override fun getBaseDirectory(): Path {
        return dataPath
    }

    override fun getScheduler(): Scheduler {
        return _scheduler
    }

    override fun getDataSourceForURL(url: String): DataSource {
        return createHikariDataSource(url, dataPath)
    }

    override fun getLogger(): TranslatableLogger = logger

    override fun getAsyncExecutor(): Executor {
        return exec
    }

    override fun registerCommand(command: CommandSpec) {
        server.commandManager.register(VelocityCommand(this, command), *command.aliases.map { "/$it" }.toTypedArray())
    }

    override fun getImplementationCommands(): Set<CommandSpec> {
        return setOf()
    }

    override fun getVersion(): String {
        return ProjectData.VERSION
    }

    @ExperimentalCoroutinesApi
    override fun lookupMinecraftProfilesByName(
        names: Flux<String>
    ): Flux<MinecraftProfile> {
        return resolveMinecraftProfile(names) {
            server.getPlayer(it).orElse(null)?.run {
                MinecraftProfileImpl(gameProfile.name, gameProfile.id)
            }
        }
    }

    private val logger = TranslatableLogger.forLogger(rawLogger)

    lateinit var manager: PermissionsEx

    @Subscribe
    fun onProxyInit(event: ProxyInitializeEvent) {
        Files.createDirectories(dataPath)

        val configLoader = HoconConfigurationLoader.builder()
            .setPath(dataPath.resolve("permissionsex.conf"))
            .build()

        try {
            manager = PermissionsEx(FilePermissionsExConfiguration.fromLoader(configLoader), this)
        } catch (e: Exception) {
            logger.error(t("Unable to load PermissionsEx engine"), e)
            return
        }
        manager.getSubjects(SUBJECTS_USER).typeInfo = UserSubjectTypeDefinition(this)
        manager.getSubjects(SUBJECTS_SYSTEM).transientData().update(IDENT_SERVER_CONSOLE.value) {
            it.setDefaultValue(GLOBAL_CONTEXT, 1)
        }.subscribe()

        this.manager.registerContextDefinitions(
            ProxyContextDefinition, RemoteIpContextDefinition,
            LocalIpContextDefinition, LocalHostContextDefinition, LocalPortContextDefinition
        )
        logger.info(t("Successfully enabled %s v%s", ProjectData.NAME, ProjectData.VERSION))
    }

    @Subscribe
    fun onDisable(event: ProxyShutdownEvent) {
        if (this::manager.isInitialized) {
            this.manager.close()
        }
        this.exec.shutdown()
        try {
            this.exec.awaitTermination(10, TimeUnit.SECONDS)
        } catch (e: Exception) {
            logger.error(t("Unable to close executor nicely, tasks did not finish in time!"))
            exec.shutdownNow()
        }
        logger.info(t("Successfully disabled %s v%s -- see you next time!", ProjectData.NAME, ProjectData.VERSION))
    }

    @Subscribe
    fun onReload(event: ProxyReloadEvent) {
        this.manager.reload().subscribe()
    }

    @Subscribe
    fun onPermissionSetup(event: PermissionsSetupEvent) {
        event.provider = PermissionProvider {
            val func = PEXPermissionFunction(this, it)
            if (this::manager.isInitialized) {
                func.subject.subscribe()
            }
            func
        }
    }

    @Subscribe
    fun uncachePlayer(event: DisconnectEvent) {
        manager.getSubjects(SUBJECTS_USER).uncache(event.player.uniqueId.toString())
    }
}

fun Int.asTristate(): Tristate {
    return when {
        this < 0 -> Tristate.FALSE
        this > 0 -> Tristate.TRUE
        else -> Tristate.UNDEFINED
    }
}

