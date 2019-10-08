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

import ca.stellardrift.permissionsex.ImplementationInterface
import ca.stellardrift.permissionsex.PermissionsEx
import ca.stellardrift.permissionsex.PermissionsEx.SUBJECTS_USER
import ca.stellardrift.permissionsex.config.FilePermissionsExConfiguration
import ca.stellardrift.permissionsex.hikariconfig.createHikariDataSource
import ca.stellardrift.permissionsex.logging.TranslatableLogger
import ca.stellardrift.permissionsex.proxycommon.IDENT_SERVER_CONSOLE
import ca.stellardrift.permissionsex.proxycommon.ProxyContextDefinition
import ca.stellardrift.permissionsex.proxycommon.SUBJECTS_SYSTEM
import ca.stellardrift.permissionsex.subject.CalculatedSubject
import ca.stellardrift.permissionsex.util.MinecraftProfile
import ca.stellardrift.permissionsex.util.Translations.t
import ca.stellardrift.permissionsex.util.command.CommandSpec
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.event.PermissionCheckEvent
import net.md_5.bungee.api.event.PlayerDisconnectEvent
import net.md_5.bungee.api.event.PlayerHandshakeEvent
import net.md_5.bungee.api.plugin.Command
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.api.plugin.TabExecutor
import net.md_5.bungee.event.EventHandler
import net.md_5.bungee.event.EventPriority
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader
import org.slf4j.impl.JDK14LoggerAdapter
import org.yaml.snakeyaml.DumperOptions
import java.lang.reflect.Constructor
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.function.Function
import java.util.logging.Logger
import javax.sql.DataSource

class PermissionsExPlugin : Plugin() {
    internal lateinit var logger: TranslatableLogger private set
    internal lateinit var dataPath: Path private set

    lateinit var manager: PermissionsEx private set

    /**
     * Because of Bukkit's special logging fun, we have to get an slf4j wrapper using specifically the logger that Bukkit provides us...
     *
     * @return Our wrapper of Bukkit's logger
     */
    private fun createLogger(): TranslatableLogger {
        val adapter: Constructor<JDK14LoggerAdapter> =
            JDK14LoggerAdapter::class.java.getDeclaredConstructor(Logger::class.java)
        adapter.isAccessible = true
        return TranslatableLogger.forLogger(adapter.newInstance(super.getLogger()))
    }

    override fun onEnable() {
        this.logger = createLogger()
        this.dataPath = dataFolder.toPath()

        Files.createDirectories(this.dataPath)
        val configLoader = YAMLConfigurationLoader.builder().apply {
            setPath(dataPath.resolve("config.yml"))
            setFlowStyle(DumperOptions.FlowStyle.BLOCK)
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
        this.manager.registerContextDefinition(ProxyContextDefinition)
        super.onEnable()
    }

    override fun onDisable() {
        if (this::manager.isInitialized) {
            this.manager.close()
        }
        super.onDisable()
    }

    @EventHandler
    fun onPermissionCheck(event: PermissionCheckEvent) {
        event.setHasPermission(event.sender.toCalculatedSubject().hasPermission(event.permission))
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun cachePlayer(event: PlayerHandshakeEvent) {
        try {
            manager.getSubjects(SUBJECTS_USER).load(event.connection.uniqueId.toString())
        } catch (e: Exception) {
            logger.warn(t("Error loading information for user %s/%s during handshake", event.connection.name, event.connection.uniqueId), e)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun unloadPlayer(event: PlayerDisconnectEvent) {
        try {
            manager.getSubjects(SUBJECTS_USER).uncache(event.player.uniqueId.toString())
        } catch (e: Exception)  {
            logger.warn(t("Error unloading user %s/%s during disconnect", event.player.name, event.player.uniqueId))
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
    override fun getBaseDirectory(): Path {
        return plugin.dataPath
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

    override fun registerCommand(command: CommandSpec) {
        plugin.proxy.pluginManager.registerCommand(plugin, PEXBungeeCommand(plugin, command))
    }

    override fun getImplementationCommands(): Set<CommandSpec> {
        return setOf()
    }

    override fun getVersion(): String {
        return plugin.description.version
    }
}

class PEXBungeeCommand(private val pex: PermissionsExPlugin, private val wrapped: CommandSpec) : Command("/${wrapped.aliases.first()}", wrapped.permission, *wrapped.aliases.drop(1).map {"/$it"}.toTypedArray()), TabExecutor {
    override fun onTabComplete(sender: CommandSender, args: Array<out String>): MutableIterable<String> {
        return wrapped.tabComplete(BungeeCommander(pex.manager, sender), args.joinToString(" "))
    }

    override fun execute(sender: CommandSender, args: Array<out String>) {
        wrapped.process(BungeeCommander(pex.manager, sender), args.joinToString(" "))
    }
}