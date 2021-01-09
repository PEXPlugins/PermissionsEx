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
package ca.stellardrift.permissionsex.bungee

import ca.stellardrift.permissionsex.context.ContextDefinitionProvider
import ca.stellardrift.permissionsex.impl.BaseDirectoryScope
import ca.stellardrift.permissionsex.impl.ImplementationInterface
import ca.stellardrift.permissionsex.impl.PermissionsEx
import ca.stellardrift.permissionsex.impl.config.FilePermissionsExConfiguration
import ca.stellardrift.permissionsex.impl.logging.WrappingFormattedLogger
import ca.stellardrift.permissionsex.logging.FormattedLogger
import ca.stellardrift.permissionsex.minecraft.MinecraftPermissionsEx
import ca.stellardrift.permissionsex.minecraft.command.Commander
import ca.stellardrift.permissionsex.minecraft.command.Permission
import ca.stellardrift.permissionsex.proxycommon.ProxyCommon
import ca.stellardrift.permissionsex.proxycommon.ProxyCommon.IDENT_SERVER_CONSOLE
import ca.stellardrift.permissionsex.proxycommon.ProxyCommon.SUBJECTS_SYSTEM
import ca.stellardrift.permissionsex.proxycommon.ProxyContextDefinition
import ca.stellardrift.permissionsex.sql.hikari.Hikari
import ca.stellardrift.permissionsex.subject.CalculatedSubject
import ca.stellardrift.permissionsex.subject.SubjectTypeCollection
import cloud.commandframework.CommandTree
import cloud.commandframework.bungee.BungeeCommandManager
import cloud.commandframework.execution.CommandExecutionCoordinator
import cloud.commandframework.permission.CommandPermission
import java.lang.reflect.Constructor
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.Executor
import java.util.function.Function
import java.util.logging.Logger
import javax.sql.DataSource
import net.kyori.adventure.platform.bungeecord.BungeeAudiences
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.event.LoginEvent
import net.md_5.bungee.api.event.PermissionCheckEvent
import net.md_5.bungee.api.event.PlayerDisconnectEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.event.EventHandler
import net.md_5.bungee.event.EventPriority
import org.slf4j.impl.JDK14LoggerAdapter
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader

class PermissionsExPlugin : Plugin(), Listener {
    internal lateinit var logger: FormattedLogger private set
    internal lateinit var dataPath: Path private set
    internal lateinit var adventure: BungeeAudiences

    private var _manager: MinecraftPermissionsEx<*>? = null
    val manager: PermissionsEx<*> get() = requireNotNull(_manager) { "PermissionsEx has not yet been initialized" }.engine()
    val mcManager: MinecraftPermissionsEx<*> get() = requireNotNull(_manager) { "PermissionsEx has not yet been initialized" }

    /**
     * Because of Bukkit's special logging fun, we have to get an slf4j wrapper using specifically the logger that Bukkit provides us...
     *
     * @return Our wrapper of Bukkit's logger
     */
    private fun createLogger(): FormattedLogger {
        val adapter: Constructor<JDK14LoggerAdapter> =
            JDK14LoggerAdapter::class.java.getDeclaredConstructor(Logger::class.java)
        adapter.isAccessible = true
        return WrappingFormattedLogger.of(adapter.newInstance(super.getLogger()), false)
    }

    val users: SubjectTypeCollection<UUID>
        get() = this._manager!!.users()

    val groups: SubjectTypeCollection<String>
        get() = this._manager!!.groups()

    private fun createCommandManager(
        execCoord: Function<CommandTree<Commander>, CommandExecutionCoordinator<Commander>>
    ): BungeeCommandManager<Commander> {
        return object : BungeeCommandManager<Commander>(this,
            execCoord,
            { sender -> BungeeCommander(this, sender) },
            { commander -> (commander as BungeeCommander).src }
        ) {
            override fun hasPermission(sender: Commander, permission: CommandPermission): Boolean {
                if (permission is Permission) {
                    return sender.hasPermission(permission)
                }
                return super.hasPermission(sender, permission)
            }
        }
    }

    override fun onEnable() {
        this.logger = createLogger()
        this.dataPath = dataFolder.toPath()
        this.adventure = BungeeAudiences.create(this)

        Files.createDirectories(this.dataPath)
        val configLoader = YamlConfigurationLoader.builder().apply {
            path(dataPath.resolve("config.yml"))
            defaultOptions { FilePermissionsExConfiguration.decorateOptions(it) }
            nodeStyle(NodeStyle.BLOCK)
        }.build()

        try {
            this._manager = MinecraftPermissionsEx.builder(FilePermissionsExConfiguration.fromLoader(configLoader))
                .implementationInterface(BungeeImplementationInterface(this))
                .commands(this::createCommandManager, ProxyCommon.PROXY_COMMAND_PREFIX)
                .messageFormatter(::BungeePluginMessageFormatter)
                .playerProvider { proxy.getPlayer(it) }
                .cachedUuidResolver { proxy.getPlayer(it)?.uniqueId } // bungee only supports online players
                .build()
        } catch (e: Exception) {
            logger.error("Unable to load PermissionsEx!", e)
            return
        }

        manager.subjects(SUBJECTS_SYSTEM).transientData().update(IDENT_SERVER_CONSOLE.identifier()) {
            it.withSegment(ContextDefinitionProvider.GLOBAL_CONTEXT) { it.withFallbackPermission(1) }
        }

        this.manager.registerContextDefinitions(
            ProxyContextDefinition.INSTANCE,
            RemoteIpContextDefinition,
            LocalIpContextDefinition,
            LocalHostContextDefinition,
            LocalPortContextDefiniiton
        )

        this.proxy.pluginManager.registerListener(this, this)
    }

    override fun onDisable() {
        if (this._manager != null) {
            this.manager.close()
        }
        this.adventure.close()
        super.onDisable()
    }

    @EventHandler
    fun onPermissionCheck(event: PermissionCheckEvent) {
        event.setHasPermission(event.sender.toCalculatedSubject().hasPermission(event.permission))
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun cachePlayer(event: LoginEvent) {
        try {
            this.users.load(event.connection.uniqueId)
        } catch (e: Exception) {
            logger.warn(Messages.ERROR_LOAD_LOGIN.tr(event.connection.name, event.connection.uniqueId), e)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun unloadPlayer(event: PlayerDisconnectEvent) {
        try {
            mcManager.callbackController().clearOwnedBy(event.player.uniqueId)
            this.users.uncache(event.player.uniqueId)
        } catch (e: Exception) {
            logger.warn(Messages.ERROR_LOAD_LOGOUT.tr(event.player.name, event.player.uniqueId))
        }
    }
}

fun CommandSender.toCalculatedSubject(): CalculatedSubject {
    val plugin = (ProxyServer.getInstance().pluginManager.getPlugin("PermissionsEx") as PermissionsExPlugin)

    return if (this is ProxiedPlayer) {
        plugin.users[uniqueId]
    } else {
        plugin.manager.subjects(SUBJECTS_SYSTEM)[IDENT_SERVER_CONSOLE.identifier()]
    }.join()
}

internal class BungeeImplementationInterface(private val plugin: PermissionsExPlugin) :
    ImplementationInterface {

    private val exec = Executor { task ->
        plugin.proxy.scheduler.runAsync(plugin, task)
    }
    override fun baseDirectory(scope: BaseDirectoryScope): Path {
        return when (scope) {
            BaseDirectoryScope.CONFIG -> plugin.dataPath
            BaseDirectoryScope.JAR -> plugin.proxy.pluginsFolder.toPath()
            BaseDirectoryScope.SERVER -> plugin.proxy.pluginsFolder.parentFile.toPath()
            // proxies don't have worlds... so this will just be the server dir
            BaseDirectoryScope.WORLDS -> plugin.proxy.pluginsFolder.parentFile.toPath()
        }
    }

    override fun logger(): org.slf4j.Logger {
        return plugin.logger
    }

    override fun dataSourceForUrl(url: String): DataSource {
        return Hikari.createDataSource(url, plugin.dataPath)
    }

    override fun asyncExecutor(): Executor {
        return exec
    }

    override fun version(): String {
        return plugin.description.version
    }
}
