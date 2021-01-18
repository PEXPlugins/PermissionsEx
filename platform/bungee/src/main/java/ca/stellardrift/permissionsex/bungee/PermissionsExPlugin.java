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
package ca.stellardrift.permissionsex.bungee;

import ca.stellardrift.permissionsex.PermissionsEngine;
import ca.stellardrift.permissionsex.context.ContextDefinitionProvider;
import ca.stellardrift.permissionsex.minecraft.BaseDirectoryScope;
import ca.stellardrift.permissionsex.impl.logging.WrappingFormattedLogger;
import ca.stellardrift.permissionsex.logging.FormattedLogger;
import ca.stellardrift.permissionsex.minecraft.MinecraftPermissionsEx;
import ca.stellardrift.permissionsex.minecraft.command.Commander;
import ca.stellardrift.permissionsex.proxycommon.ProxyCommon;
import ca.stellardrift.permissionsex.proxycommon.ProxyContextDefinition;
import ca.stellardrift.permissionsex.sql.hikari.Hikari;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import ca.stellardrift.permissionsex.subject.SubjectTypeCollection;
import cloud.commandframework.CommandTree;
import cloud.commandframework.bungee.BungeeCommandManager;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PermissionCheckEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.impl.JDK14LoggerAdapter;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.logging.Logger;

import static ca.stellardrift.permissionsex.proxycommon.ProxyCommon.IDENT_SERVER_CONSOLE;
import static ca.stellardrift.permissionsex.proxycommon.ProxyCommon.SUBJECTS_SYSTEM;

public class PermissionsExPlugin extends Plugin implements Listener {
    private @MonotonicNonNull FormattedLogger logger;
    private @MonotonicNonNull Path dataPath;
    private @MonotonicNonNull BungeeAudiences adventure;

    private @Nullable MinecraftPermissionsEx<?> manager;
    private final Executor exec = task -> getProxy().getScheduler().runAsync(PermissionsExPlugin.this, task);

    /**
     * Get the permissions engine active for this proxy.
     *
     * @return the permissions engine
     * @since 2.0.0
     */
    public PermissionsEngine engine() {
        return this.manager().engine();
    }

    MinecraftPermissionsEx<?> manager() {
        final @Nullable MinecraftPermissionsEx<?> manager = this.manager;
        if (manager == null) {
            throw new IllegalStateException("PermissionsEx is not currently initialized! Check for an earlier error, " +
                "or whether permissions may be checked too early.");
        }
        return manager;
    }

    /**
     * Get a collection of users stored on this server.
     *
     * <p>Not all elements of this collection are cached, so some requests may involve network lookups.</p>
     *
     * @return the user subjects
     * @since 2.0.0
     */
    public SubjectTypeCollection<UUID> users() {
        return this.manager().users();
    }

    /**
     * Get a collection of groups stored on this server.
     *
     * <p>Elements of this collection will be cached on load.</p>
     *
     * @return the group subjects
     * @since 2.0.0
     */
    public SubjectTypeCollection<String> groups() {
        return this.manager().groups();
    }

    private BungeeCommandManager<Commander> createCommandManager(
        final Function<CommandTree<Commander>, CommandExecutionCoordinator<Commander>> execCoord
    ) {
        return new BungeeCommandManager<>(this,
            execCoord,
            sender -> new BungeeCommander(this, sender),
            commander -> ((BungeeCommander) commander).source()
        );
    }

    @Override
    public void onEnable() {
        this.logger = createLogger();
        this.dataPath = getDataFolder().toPath();
        this.adventure = BungeeAudiences.create(this);

        try {
            Files.createDirectories(this.dataPath);
        } catch (final IOException ex) {
            this.logger.error("Unable to load PermissionsEx!", ex);
        }

        try {
            this.manager = MinecraftPermissionsEx.builder()
                .configuration(this.dataPath.resolve("config.yml"))
                .baseDirectory(this.dataPath)
                .databaseProvider(url -> Hikari.createDataSource(url, this.dataPath))
                .logger(this.logger)
                .asyncExecutor(this.exec)
                .baseDirectory(BaseDirectoryScope.JAR, this.getProxy().getPluginsFolder().toPath())
                .commands(this::createCommandManager, ProxyCommon.PROXY_COMMAND_PREFIX)
                .messageFormatter(BungeeMessageFormatter::new)
                .playerProvider(it -> getProxy().getPlayer(it))
                .cachedUuidResolver(it -> {
                    final @Nullable ProxiedPlayer player = getProxy().getPlayer(it);
                    // bungee only supports online players
                    return player == null ? null: player.getUniqueId();
                })
                .create();
        } catch (final Exception ex) {
            this.logger.error("Unable to load PermissionsEx!", ex);
            return;
        }

        this.engine().subjects(SUBJECTS_SYSTEM).transientData().update(IDENT_SERVER_CONSOLE.identifier(), it ->
            it.withSegment(ContextDefinitionProvider.GLOBAL_CONTEXT, seg -> seg.withFallbackPermission(1))
        );

        this.engine().registerContextDefinitions(
            ProxyContextDefinition.INSTANCE,
            BungeeContexts.remoteIp(),
            BungeeContexts.localIp(),
            BungeeContexts.localHost(),
            BungeeContexts.localPort()
        );

        this.getProxy().getPluginManager().registerListener(this, this);
    }

    @Override
    public void onDisable() {
        if (this.manager != null) {
            this.manager.close();
        }
        this.adventure.close();
        super.onDisable();
    }

    @EventHandler
    public void onPermissionCheck(final PermissionCheckEvent event) {
        event.setHasPermission(asCalculatedSubject(event.getSender()).hasPermission(event.getPermission()));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void cachePlayer(final LoginEvent event) {
        final PendingConnection connection = event.getConnection();
        try {
            this.users().load(connection.getUniqueId());
        } catch (final Exception ex) {
            this.logger.warn(Messages.ERROR_LOAD_LOGIN.tr(connection.getName(), connection.getUniqueId()), ex);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void unloadPlayer(final PlayerDisconnectEvent event) {
        final ProxiedPlayer player = event.getPlayer();
        try {
            this.manager().callbackController().clearOwnedBy(player.getUniqueId());
            this.users().uncache(player.getUniqueId());
        } catch (final Exception ex) {
            logger.warn(Messages.ERROR_LOAD_LOGOUT.tr(player.getName(), player.getUniqueId()));
        }
    }

    BungeeAudiences adventure() {
        return this.adventure;
    }

    /**
     * Because of Bukkit's special logging fun, we have to get an slf4j wrapper using specifically the logger that Bukkit provides us...
     *
     * @return Our wrapper of Bukkit's logger
     */
    private FormattedLogger createLogger() {
        try {
            final Constructor<JDK14LoggerAdapter> adapter = JDK14LoggerAdapter.class.getDeclaredConstructor(Logger.class);
            adapter.setAccessible(true);
            return WrappingFormattedLogger.of(adapter.newInstance(super.getLogger()), false);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            throw new RuntimeException("Failed to initialize PermissionsEx logger", ex);
        }
    }

    private static CalculatedSubject asCalculatedSubject(final CommandSender sender) {
        final PermissionsExPlugin plugin = (PermissionsExPlugin) ProxyServer.getInstance()
            .getPluginManager().getPlugin("PermissionsEx");

        if (sender instanceof ProxiedPlayer) {
            return plugin.users().get(((ProxiedPlayer) sender).getUniqueId()).join();
        } else {
            return plugin.engine().subjects(SUBJECTS_SYSTEM).get(IDENT_SERVER_CONSOLE.identifier()).join();
        }
    }

}
