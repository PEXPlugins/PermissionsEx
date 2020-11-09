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

import ca.stellardrift.permissionsex.BaseDirectoryScope
import ca.stellardrift.permissionsex.ImplementationInterface
import ca.stellardrift.permissionsex.PermissionsEx
import ca.stellardrift.permissionsex.PermissionsEx.SUBJECTS_USER
import ca.stellardrift.permissionsex.commands.parse.CommandSpec
import ca.stellardrift.permissionsex.config.FilePermissionsExConfiguration
import ca.stellardrift.permissionsex.hikariconfig.createHikariDataSource
import ca.stellardrift.permissionsex.logging.FormattedLogger
import ca.stellardrift.permissionsex.proxycommon.IDENT_SERVER_CONSOLE
import ca.stellardrift.permissionsex.proxycommon.ProxyContextDefinition
import ca.stellardrift.permissionsex.proxycommon.SUBJECTS_SYSTEM
import ca.stellardrift.permissionsex.subject.CalculatedSubject
import ca.stellardrift.permissionsex.util.MinecraftProfile
import java.lang.reflect.Constructor
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executor
import java.util.function.Function
import java.util.function.Supplier
import java.util.logging.Logger
import javax.sql.DataSource
import net.kyori.adventure.platform.bungeecord.BungeeAudiences
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.event.LoginEvent
import net.md_5.bungee.api.event.PermissionCheckEvent
import net.md_5.bungee.api.event.PlayerDisconnectEvent
import net.md_5.bungee.api.plugin.Command
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.api.plugin.TabExecutor
import net.md_5.bungee.event.EventHandler
import net.md_5.bungee.event.EventPriority
import org.slf4j.impl.JDK14LoggerAdapter
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader

class PermissionsExPlugin : Plugin(), Listener {
    private val cachedCommands = ConcurrentLinkedQueue<Supplier<Set<CommandSpec>>>()
    internal lateinit var logger: FormattedLogger private set
    internal lateinit var dataPath: Path private set
    internal lateinit var adventure: BungeeAudiences

    lateinit var manager: PermissionsEx<*> internal set

    /**
     * Because of Bukkit's special logging fun, we have to get an slf4j wrapper using specifically the logger that Bukkit provides us...
     *
     * @return Our wrapper of Bukkit's logger
     */
    private fun createLogger(): FormattedLogger {
        val adapter: Constructor<JDK14LoggerAdapter> =
            JDK14LoggerAdapter::class.java.getDeclaredConstructor(Logger::class.java)
        adapter.isAccessible = true
        return FormattedLogger.forLogger(adapter.newInstance(super.getLogger()), false)
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
            this.manager = PermissionsEx(FilePermissionsExConfiguration.fromLoader(configLoader), BungeeImplementationInterface(this))
        } catch (e: Exception) {
            logger.error("Unable to load PermissionsEx!", e)
        }
        this.manager.getSubjects(SUBJECTS_USER).typeInfo = UserSubjectTypeDefinition(this)
        manager.getSubjects(SUBJECTS_SYSTEM).transientData().update(IDENT_SERVER_CONSOLE.value) {
            it.setDefaultValue(PermissionsEx.GLOBAL_CONTEXT, 1)
        }
        this.manager.registerContextDefinitions(ProxyContextDefinition, RemoteIpContextDefinition,
            LocalIpContextDefinition, LocalHostContextDefinition, LocalPortContextDefiniiton)

        this.manager.registerCommandsTo {
            proxy.pluginManager.registerCommand(this, PEXBungeeCommand(this, it))
        }
        this.proxy.pluginManager.registerListener(this, this)
    }

    override fun onDisable() {
        if (this::manager.isInitialized) {
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
            manager.getSubjects(SUBJECTS_USER).load(event.connection.uniqueId.toString())
        } catch (e: Exception) {
            logger.warn(Messages.ERROR_LOAD_LOGIN(event.connection.name, event.connection.uniqueId), e)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun unloadPlayer(event: PlayerDisconnectEvent) {
        try {
            manager.callbackController.clearOwnedBy(event.player.uniqueId)
            manager.getSubjects(SUBJECTS_USER).uncache(event.player.uniqueId.toString())
        } catch (e: Exception) {
            logger.warn(Messages.ERROR_LOAD_LOGOUT(event.player.name, event.player.uniqueId))
        }
    }
}

fun CommandSender.toCalculatedSubject(): CalculatedSubject {
    return (ProxyServer.getInstance().pluginManager.getPlugin("PermissionsEx") as PermissionsExPlugin).manager.getSubjects(when (this) {
        is ProxiedPlayer -> SUBJECTS_USER
        else -> IDENT_SERVER_CONSOLE.key
    })[when (this) {
        is ProxiedPlayer -> uniqueId.toString()
        else -> IDENT_SERVER_CONSOLE.value
    }].join()
}

class BungeeImplementationInterface(private val plugin: PermissionsExPlugin) : ImplementationInterface {
    override fun lookupMinecraftProfilesByName(
        names: Iterable<String>,
        action: Function<MinecraftProfile, CompletableFuture<Void>>
    ): CompletableFuture<Int> {
        return ca.stellardrift.permissionsex.profile.lookupMinecraftProfilesByName(names, action::apply)
    }

    private val exec = Executor { task ->
        plugin.proxy.scheduler.runAsync(plugin, task)
    }
    override fun getBaseDirectory(scope: BaseDirectoryScope): Path {
        return when (scope) {
            BaseDirectoryScope.CONFIG -> plugin.dataPath
            BaseDirectoryScope.JAR -> plugin.proxy.pluginsFolder.toPath()
            BaseDirectoryScope.SERVER -> plugin.proxy.pluginsFolder.parentFile.toPath()
            BaseDirectoryScope.WORLDS -> plugin.proxy.pluginsFolder.parentFile.toPath() // proxies don't have worlds... so this will just be the server dir
        }
    }

    override fun getLogger(): org.slf4j.Logger {
        return plugin.logger
    }

    override fun getDataSourceForURL(url: String): DataSource {
        return createHikariDataSource(url, plugin.dataPath)
    }

    override fun getAsyncExecutor(): Executor {
        return exec
    }

    override fun getVersion(): String {
        return plugin.description.version
    }
}

class PEXBungeeCommand(private val pex: PermissionsExPlugin, private val wrapped: CommandSpec) : Command("/${wrapped.aliases.first()}", wrapped.permission?.value, *wrapped.aliases.drop(1).map { "/$it" }.toTypedArray()), TabExecutor {
    override fun onTabComplete(sender: CommandSender, args: Array<out String>): Iterable<String> {
        return wrapped.tabComplete(BungeeCommander(pex, sender), args.joinToString(" "))
    }

    override fun execute(sender: CommandSender, args: Array<out String>) {
        wrapped.process(BungeeCommander(pex, sender), args.joinToString(" "))
    }
}
