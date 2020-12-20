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
import ca.stellardrift.permissionsex.config.FilePermissionsExConfiguration
import ca.stellardrift.permissionsex.logging.FormattedLogger
import ca.stellardrift.permissionsex.logging.WrappingFormattedLogger
import ca.stellardrift.permissionsex.minecraft.MinecraftPermissionsEx
import ca.stellardrift.permissionsex.sql.hikari.Hikari
import ca.stellardrift.permissionsex.subject.ImmutableSubjectData
import ca.stellardrift.permissionsex.subject.SubjectTypeCollection
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.nio.file.Path
import java.sql.SQLException
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.sql.DataSource
import net.kyori.adventure.platform.bukkit.BukkitAudiences
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
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader

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
data class BukkitConfiguration(
    @Comment("Whether to fall back to checking op status when a permission is unset in PEX") val fallbackOp: Boolean = true
)

/**
 * PermissionsEx plugin
 */
class PermissionsExPlugin : JavaPlugin(), Listener {
    private var _manager: MinecraftPermissionsEx<BukkitConfiguration>? = null

    /**
     * Access the PEX engine
     *
     * @return The engine
     */
    val manager: PermissionsEx<BukkitConfiguration>
        get() = (requireNotNull(_manager) { "PermissionsEx is not currently initialized!" }).engine()

    private lateinit var logger: FormattedLogger

    // Injections into superperms
    var permissionList: PermissionList? = null
        private set

    // Permissions subscriptions handling
    var subscriptionHandler: PEXPermissionSubscriptionMap? = null
        private set

    // Location of plugin configuration data
    lateinit var dataPath: Path
    internal lateinit var adventure: BukkitAudiences private set
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
            WrappingFormattedLogger.of(adapter.newInstance(getLogger()), true)
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
        this.adventure = BukkitAudiences.create(this)
        val configLoader = YamlConfigurationLoader.builder()
            .file(File(dataFolder, "config.yml"))
            .nodeStyle(NodeStyle.BLOCK)
            .defaultOptions { FilePermissionsExConfiguration.decorateOptions(it) }
            .build()

        try {
            val impl = BukkitImplementationInterface()
            dataFolder.mkdirs()
            _manager = MinecraftPermissionsEx.builder(FilePermissionsExConfiguration.fromLoader(
                    configLoader,
                    BukkitConfiguration::class.java
                ))
                .implementationInterface(impl)
                .opProvider { server.operators.contains(server.getOfflinePlayer(it)) }
                .cachedUuidResolver { server.getOfflinePlayer(it)?.uniqueId } // TODO: is this correct?
                .playerProvider(server::getPlayer)
                .build()
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
            LocalPortContextDefinition
        )

        server.pluginManager.registerEvents(this, this)
        subscriptionHandler = PEXPermissionSubscriptionMap.inject(this, server.pluginManager)
        permissionList = PermissionList.inject(this)
        injectAllPermissibles()
        detectWorldGuard(this)
        detectVault(this)

        manager.registerCommandsTo {
            val cmd = getCommand(it.aliases[0])
            if (cmd != null) {
                val bukkitCommand = PEXBukkitCommand(it, this@PermissionsExPlugin)
                cmd.setExecutor(bukkitCommand)
                cmd.tabCompleter = bukkitCommand
            }
        }
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
        this.adventure.close()
    }

    @EventHandler
    private fun onPlayerPreLogin(event: AsyncPlayerPreLoginEvent) {
        userSubjects[event.uniqueId]
            .exceptionally { e ->
                logger.warn(Messages.ERROR_LOAD_PRELOGIN(event.name, event.uniqueId.toString(), e.message!!), e)
                null
            }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onPlayerLogin(event: PlayerLoginEvent) {
        val identifier = event.player.uniqueId

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
            userSubjects.uncache(event.player.uniqueId)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR) // Happen last
    private fun onPlayerQuit(event: PlayerQuitEvent) {
        uninjectPermissible(event.player)
        this._manager?.engine()?.callbackController?.clearOwnedBy(event.player.uniqueId)
        userSubjects.uncache(event.player.uniqueId)
    }

    /**
     * Access user subjects
     *
     * @return The user subject collection
     */
    val userSubjects: SubjectTypeCollection<UUID>
        get() = this._manager!!.users()

    /**
     * Access group subjects
     *
     * @return The group subject collection
     */
    val groupSubjects: SubjectTypeCollection<String>
        get() = this._manager!!.groups()

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
            if (success && manager.debugMode()) {
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
            } else if (_manager?.engine()?.debugMode() == true) {
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
        override fun baseDirectory(scope: BaseDirectoryScope): Path {
            return when (scope) {
                BaseDirectoryScope.CONFIG -> dataPath
                BaseDirectoryScope.JAR -> Bukkit.getUpdateFolderFile().toPath()
                BaseDirectoryScope.SERVER -> dataPath.toAbsolutePath().parent.parent
                BaseDirectoryScope.WORLDS -> Bukkit.getWorldContainer().toPath()
                else -> throw IllegalArgumentException("Unknown directory scope$scope")
            }
        }

        @Throws(SQLException::class)
        override fun dataSourceForUrl(url: String): DataSource {
            return Hikari.createDataSource(url, this.baseDirectory())
        }

        /**
         * Get an executor to run tasks asynchronously on.
         *
         * @return The async executor
         */
        override fun asyncExecutor(): Executor {
            return executorService
        }

        override fun getVersion(): String {
            return description.version
        }

        override fun logger(): Logger {
            return this@PermissionsExPlugin.logger
        }
    }
}
