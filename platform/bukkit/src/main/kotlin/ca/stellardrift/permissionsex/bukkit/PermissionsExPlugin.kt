/*
 * PermissionsEx - a permissions plugin for your server ecosystem
 * Copyright © 2020 zml [at] stellardrift [dot] ca and PermissionsEx contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ca.stellardrift.permissionsex.bukkit

import ca.stellardrift.permissionsex.BaseDirectoryScope
import ca.stellardrift.permissionsex.ImplementationInterface
import ca.stellardrift.permissionsex.PermissionsEx
import ca.stellardrift.permissionsex.bukkit.PermissibleInjector.ClassNameRegexPermissibleInjector
import ca.stellardrift.permissionsex.bukkit.PermissibleInjector.ClassPresencePermissibleInjector
import ca.stellardrift.permissionsex.commands.parse.CommandSpec
import ca.stellardrift.permissionsex.config.FilePermissionsExConfiguration
import ca.stellardrift.permissionsex.data.ImmutableSubjectData
import ca.stellardrift.permissionsex.hikariconfig.createHikariDataSource
import ca.stellardrift.permissionsex.logging.FormattedLogger
import ca.stellardrift.permissionsex.subject.SubjectType
import ca.stellardrift.permissionsex.util.MinecraftProfile
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.nio.file.Path
import java.sql.SQLException
import java.util.Queue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier
import javax.sql.DataSource
import ninja.leaping.configurate.objectmapping.Setting
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import org.slf4j.Logger
import org.slf4j.impl.JDK14LoggerAdapter
import org.yaml.snakeyaml.DumperOptions

private val INJECTORS = arrayOf(
    ClassPresencePermissibleInjector("net.glowstone.entity.GlowHumanEntity", "permissions", true),
    ClassPresencePermissibleInjector("org.getspout.server.entity.SpoutHumanEntity", "permissions", true),
    ClassNameRegexPermissibleInjector(
        "org.getspout.spout.player.SpoutCraftPlayer",
        "perm",
        false,
        "org\\.getspout\\.spout\\.player\\.SpoutCraftPlayer"
    ),
    ClassPresencePermissibleInjector(
        getCraftClassName("entity.CraftHumanEntity"),
        "perm",
        true
    )
)

@ConfigSerializable
class BukkitConfiguration {
    @Setting(
        value = "fallback-op",
        comment = "Whether to fall back to checking op status when a permission is unset in PEX"
    )
    var fallbackOp = true
        private set
}

/**
 * PermissionsEx plugin
 */
class PermissionsExPlugin : JavaPlugin(), Listener {
    private var _manager: PermissionsEx<BukkitConfiguration>? = null

    /**
     * Access the PEX engine
     *
     * @return The engine
     */
    val manager: PermissionsEx<BukkitConfiguration>
        get() = requireNotNull(_manager) { "PermissionsEx is not currently initialized!" }

    private lateinit var logger: FormattedLogger

    // Injections into superperms
    var permissionList: PermissionList? = null
        private set

    // Permissions subscriptions handling
    var subscriptionHandler: PEXPermissionSubscriptionMap? = null
        private set

    // Location of plugin configuration data
    lateinit var dataPath: Path
    private val executorService = Executors.newCachedThreadPool()

    /**
     * Because of Bukkit's special logging fun, we have to get an slf4j wrapper using specifically the logger that Bukkit provides us...
     *
     * @return Our wrapper of Bukkit's logger
     */
    private fun createLogger(): FormattedLogger {
        return try {
            val adapter =
                JDK14LoggerAdapter::class.java.getDeclaredConstructor(java.util.logging.Logger::class.java)
            adapter.isAccessible = true
            FormattedLogger.forLogger(adapter.newInstance(getLogger()), true)
        } catch (e: NoSuchMethodException) {
            throw ExceptionInInitializerError(e)
        } catch (e: InstantiationException) {
            throw ExceptionInInitializerError(e)
        } catch (e: IllegalAccessException) {
            throw ExceptionInInitializerError(e)
        } catch (e: InvocationTargetException) {
            throw ExceptionInInitializerError(e)
        }
    }

    override fun onEnable() {
        this.dataPath = dataFolder.toPath()
        this.logger = createLogger()
        val configLoader = YAMLConfigurationLoader.builder()
                .setFile(File(dataFolder, "config.yml"))
                .setFlowStyle(DumperOptions.FlowStyle.BLOCK)
                .build()
        try {
            val impl = BukkitImplementationInterface()
            dataFolder.mkdirs()
            _manager = PermissionsEx(
                FilePermissionsExConfiguration.fromLoader(
                    configLoader,
                    BukkitConfiguration::class.java
                ), impl
            )
            impl.registerCommandsNow()
            /*} catch (PEBKACException e) {
            logger.warn(e.getTranslatableMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;*/
        } catch (e: Exception) {
            logger.error(Messages.ERROR_ON_ENABLE(description.name), e)
            server.pluginManager.disablePlugin(this)
            return
        }
        manager.registerContextDefinitions(
            WorldContextDefinition,
            DimensionContextDefinition,
            RemoteIpContextDefinition,
            LocalIpContextDefinition,
            LocalHostContextDefinition,
            LocalPortContextDefiniiton
        )
        manager.getSubjects(PermissionsEx.SUBJECTS_USER).typeInfo = UserSubjectTypeDefinition(
            PermissionsEx.SUBJECTS_USER,
            this
        )
        server.pluginManager.registerEvents(this, this)
        subscriptionHandler = PEXPermissionSubscriptionMap.inject(this, server.pluginManager)
        permissionList = PermissionList.inject(this)
        injectAllPermissibles()
        detectWorldGuard(this)
        detectVault(this)
    }

    override fun onDisable() {
        if (_manager != null) {
            this._manager?.close()
            this._manager = null
        }
        if (subscriptionHandler != null) {
            subscriptionHandler!!.uninject()
            subscriptionHandler = null
        }
        if (permissionList != null) {
            permissionList!!.uninject()
            permissionList = null
        }
        uninjectAllPermissibles()
        executorService.shutdown()
        try {
            executorService.awaitTermination(20, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            logger.error(Messages.ERROR_DISABLE_TASK_TIMEOUT())
            executorService.shutdownNow()
        }
    }

    @EventHandler
    private fun onPlayerPreLogin(event: AsyncPlayerPreLoginEvent) {
        userSubjects[event.uniqueId.toString()]
            .exceptionally { e ->
                logger.warn(Messages.ERROR_LOAD_PRELOGIN(event.name, event.uniqueId.toString(), e.message!!), e)
                null
            }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onPlayerLogin(event: PlayerLoginEvent) {
        val identifier = event.player.uniqueId.toString()

        // Spigot doesn't seem to store virtual host names, so we have to do it ourselves.
        // Hostnames are provided as <host>:<port>, and we don't need the port.
        userSubjects.transientData().update(identifier) {
            it.setOption(PermissionsEx.GLOBAL_CONTEXT, "hostname", event.hostname.substringBeforeLast(":"))
        }
        userSubjects.isRegistered(identifier)
            .thenAccept { registered ->
                if (registered) {
                    userSubjects.persistentData().update(identifier) { input: ImmutableSubjectData ->
                        if (event.player.name != input.getOptions(PermissionsEx.GLOBAL_CONTEXT)["name"]) {
                            input.setOption(PermissionsEx.GLOBAL_CONTEXT, "name", event.player.name)
                        } else {
                            input
                        }
                    }
                }
            }
        injectPermissible(event.player)
    }

    /**
     * If the login event is cancelled, we want to make sure we properly uninject the permissible.
     * Because this listener is on priority MONITOR, any plugin that might cancel the event has had a chance to have its say.
     *
     * @param event The login event that may be cancelled.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    private fun onPlayerLoginDeny(event: PlayerLoginEvent) {
        if (event.result != PlayerLoginEvent.Result.ALLOWED) {
            uninjectPermissible(event.player)
            userSubjects.uncache(event.player.uniqueId.toString())
        }
    }

    @EventHandler(priority = EventPriority.MONITOR) // Happen last
    private fun onPlayerQuit(event: PlayerQuitEvent) {
        uninjectPermissible(event.player)
        this._manager?.callbackController?.clearOwnedBy(event.player.uniqueId)
        userSubjects.uncache(event.player.uniqueId.toString())
    }

    /**
     * Access user subjects
     *
     * @return The user subject collection
     */
    val userSubjects: SubjectType
        get() = manager.getSubjects(PermissionsEx.SUBJECTS_USER)

    /**
     * Access group subjects
     *
     * @return The group subject collection
     */
    val groupSubjects: SubjectType
        get() = manager.getSubjects(PermissionsEx.SUBJECTS_GROUP)

    private fun injectPermissible(player: Player) {
        try {
            val permissible = PEXPermissible(player, this)
            var success = false
            var found = false
            for (injector in INJECTORS) {
                if (injector.isApplicable(player)) {
                    found = true
                    val oldPerm = injector.inject(player, permissible)
                    if (oldPerm != null) {
                        permissible.previousPermissible = oldPerm
                        success = true
                        break
                    }
                }
            }
            if (!found) {
                logger.warn(Messages.SUPERPERMS_INJECT_NO_INJECTOR())
            } else if (!success) {
                logger.warn(Messages.SUPERPERMS_INJECT_ERROR_GENERIC(player.name))
            }
            permissible.recalculatePermissions()
            if (success && manager.hasDebugMode()) {
                logger.info(Messages.SUPERPERMS_INJECT_SUCCESS())
            }
        } catch (e: Throwable) {
            logger.error(
                Messages.SUPERPERMS_INJECT_ERROR_GENERIC(player.name),
                e
            )
        }
    }

    private fun injectAllPermissibles() {
        server.onlinePlayers.forEach { injectPermissible(it) }
    }

    private fun uninjectPermissible(player: Player) {
        try {
            var success = false
            for (injector in INJECTORS) {
                if (injector.isApplicable(player)) {
                    val pexPerm = injector.getPermissible(player)
                    if (pexPerm is PEXPermissible) {
                        if (injector.inject(player, pexPerm.previousPermissible) != null) {
                            success = true
                            break
                        }
                    } else {
                        success = true
                        break
                    }
                }
            }
            if (!success) {
                logger.warn(Messages.SUPERPERMS_UNINJECT_NO_INJECTOR(player.name))
            } else if (_manager?.hasDebugMode() == true) {
                logger.info(Messages.SUPERPERMS_UNINJECT_SUCCESS(player.name))
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun uninjectAllPermissibles() {
        server.onlinePlayers.forEach { player: Player -> uninjectPermissible(player) }
    }

    private inner class BukkitImplementationInterface : ImplementationInterface {
        private val stagedCommands: Queue<Supplier<Set<CommandSpec>>> =
            ConcurrentLinkedQueue()

        override fun getBaseDirectory(scope: BaseDirectoryScope): Path {
            return when (scope) {
                BaseDirectoryScope.CONFIG -> dataPath
                BaseDirectoryScope.JAR -> Bukkit.getUpdateFolderFile().toPath()
                BaseDirectoryScope.SERVER -> dataPath.toAbsolutePath().parent.parent
                BaseDirectoryScope.WORLDS -> Bukkit.getWorldContainer().toPath()
                else -> throw IllegalArgumentException("Unknown directory scope$scope")
            }
        }

        @Throws(SQLException::class)
        override fun getDataSourceForURL(url: String): DataSource {
            return createHikariDataSource(url, this.baseDirectory)
        }

        /**
         * Get an executor to run tasks asynchronously on.
         *
         * @return The async executor
         */
        override fun getAsyncExecutor(): Executor {
            return executorService
        }

        override fun registerCommands(commands: Supplier<Set<CommandSpec>>) {
            stagedCommands.add(commands)
            registerCommandsNow()
        }

        fun registerCommandsNow(): Boolean {
            if (_manager == null) {
                return false
            }
            var supply: Supplier<Set<CommandSpec>>
            while (stagedCommands.poll().also { supply = it } != null) {
                registerCommandsNow(supply)
            }
            return true
        }

        fun registerCommandsNow(commandSupplier: Supplier<Set<CommandSpec>>) {
            requireNotNull(_manager) { "Manager must be initialized to register commands!" }
            for (command in commandSupplier.get()) {
                val cmd = getCommand(command.aliases[0])
                if (cmd != null) {
                    val bukkitCommand = PEXBukkitCommand(command, this@PermissionsExPlugin)
                    cmd.setExecutor(bukkitCommand)
                    cmd.tabCompleter = bukkitCommand
                }
            }
        }

        override fun getImplementationCommands(): Set<CommandSpec> {
            return emptySet()
        }

        override fun getVersion(): String {
            return description.version
        }

        override fun getLogger(): Logger {
            return this@PermissionsExPlugin.logger
        }

        override fun lookupMinecraftProfilesByName(
            names: Iterable<String>,
            action: Function<MinecraftProfile, CompletableFuture<Void>>
        ): CompletableFuture<Int> {
            return lookupMinecraftProfilesByName(names, Consumer { action.apply(it) })
        }
    }
}
