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
package ca.stellardrift.permissionsex.velocity;

import ca.stellardrift.permissionsex.PermissionsEngine;
import ca.stellardrift.permissionsex.impl.BaseDirectoryScope;
import ca.stellardrift.permissionsex.impl.ImplementationInterface;
import ca.stellardrift.permissionsex.impl.config.FilePermissionsExConfiguration;
import ca.stellardrift.permissionsex.impl.logging.WrappingFormattedLogger;
import ca.stellardrift.permissionsex.logging.FormattedLogger;
import ca.stellardrift.permissionsex.minecraft.MinecraftPermissionsEx;
import ca.stellardrift.permissionsex.minecraft.command.Commander;
import ca.stellardrift.permissionsex.minecraft.command.Permission;
import ca.stellardrift.permissionsex.proxycommon.ProxyCommon;
import ca.stellardrift.permissionsex.proxycommon.ProxyContextDefinition;
import ca.stellardrift.permissionsex.sql.hikari.Hikari;
import ca.stellardrift.permissionsex.subject.SubjectTypeCollection;
import cloud.commandframework.CommandManager;
import cloud.commandframework.CommandTree;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.permission.CommandPermission;
import cloud.commandframework.velocity.VelocityCommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyReloadEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static ca.stellardrift.permissionsex.context.ContextDefinitionProvider.GLOBAL_CONTEXT;

@Plugin(id = ProjectData.ARTIFACT_ID, name = ProjectData.NAME, version = ProjectData.VERSION, description = ProjectData.DESCRIPTION)
public class PermissionsExPlugin {
    // Base directories since these aren't really exposed
    private static final Path SERVER_PATH = Paths.get(".");
    private static final Path PLUGINS_PATH = SERVER_PATH.resolve("plugins");

    private final PluginContainer container;
    private final ProxyServer server;
    private final Path dataPath;
    private final FormattedLogger logger;
    private final ExecutorService exec = Executors.newCachedThreadPool();

    private @Nullable MinecraftPermissionsEx<?> manager;

    @Inject
    PermissionsExPlugin(
        final Logger rawLogger,
        final PluginContainer container,
        final ProxyServer server,
        final @DataDirectory Path dataPath
    ) {
        this.container = container;
        this.server = server;
        this.dataPath = dataPath;
        this.logger = WrappingFormattedLogger.of(rawLogger, true);
    }

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

    // Internal management

    private CommandManager<Commander> createCommandManager(final Function<CommandTree<Commander>, CommandExecutionCoordinator<Commander>> execCoord) {
        return new VelocityCommandManager<Commander>(
            this.container,
            this.server,
            execCoord,
            source -> new VelocityCommander(this, source),
            cmd -> ((VelocityCommander) cmd).audience()
        ) {
            @Override
            public boolean hasPermission(final Commander sender, final CommandPermission permission) {
                if (permission instanceof Permission) {
                    return sender.hasPermission((Permission) permission);
                }
                return super.hasPermission(sender, permission);
            }
        };
    }

    @Subscribe
    public void onProxyInit(final ProxyInitializeEvent event) {
        try {
            Files.createDirectories(dataPath);
        } catch (final IOException ex) {
            logger.error(Messages.PLUGIN_INIT_ERROR.tr(), ex);
        }

        final HoconConfigurationLoader configLoader = HoconConfigurationLoader.builder()
            .path(dataPath.resolve("permissionsex.conf"))
            .defaultOptions(FilePermissionsExConfiguration::decorateOptions)
            .build();

        try {
            this.manager = MinecraftPermissionsEx.builder(FilePermissionsExConfiguration.fromLoader(configLoader))
                .implementationInterface(new VelocityImplementationInterface())
                .cachedUuidResolver(name -> server.getPlayer(name).map(Player::getUniqueId).orElse(null))
                .playerProvider(id -> server.getPlayer(id).orElse(null))
                .commands(this::createCommandManager, ProxyCommon.PROXY_COMMAND_PREFIX)
                .messageFormatter(VelocityMessageFormatter::new)
                .build();

        } catch (final Exception ex) {
            logger.error(Messages.PLUGIN_INIT_ERROR.tr(), ex);
            return;
        }

        this.engine().subjects(ProxyCommon.SUBJECTS_SYSTEM).transientData().update(ProxyCommon.IDENT_SERVER_CONSOLE.identifier(), it ->
            it.withSegment(GLOBAL_CONTEXT,  s -> s.withFallbackPermission(1))
        );

        this.engine().registerContextDefinitions(
            ProxyContextDefinition.INSTANCE,
            VelocityContexts.remoteIp(),
            VelocityContexts.localIp(),
            VelocityContexts.localHost(),
            VelocityContexts.localPort()
        );

        logger.info(Messages.PLUGIN_INIT_SUCCESS.tr(ProjectData.NAME, ProjectData.VERSION));
    }

    @Subscribe
    public void onDisable(final ProxyShutdownEvent event) {
        if (this.manager != null) {
            this.manager.close();
            this.manager = null;
        }
        this.exec.shutdown();
        boolean successful;
        try {
            successful = this.exec.awaitTermination(10, TimeUnit.SECONDS);
        } catch (final Exception ex) {
            successful = false;
        }

        if (!successful) {
            logger.error(Messages.PLUGIN_DISABLE_TIMEOUT.tr());
            exec.shutdownNow();
        }

        logger.info(Messages.PLUGIN_DISABLE_SUCCESS.tr(ProjectData.NAME, ProjectData.VERSION));
    }

    @Subscribe
    public void onReload(final ProxyReloadEvent event) {
        this.manager().engine().reload()
        .exceptionally(err -> {
            this.logger.error(Messages.PLUGIN_RELOAD_ERROR.tr(), err);
            return null;
        });
    }

    @Subscribe
    public void onPermissionSetup(final PermissionsSetupEvent event) {
        event.setProvider(it -> {
            final PEXPermissionFunction func = new PEXPermissionFunction(this, it);
            if (this.manager != null) {
                // Trigger loading
                func.subject().identifier();
            }
            return func;
        });
    }

    @Subscribe
    public void uncachePlayer(final DisconnectEvent event) {
        this.manager().callbackController().clearOwnedBy(event.getPlayer().getUniqueId());
        this.users().uncache(event.getPlayer().getUniqueId());
    }

    class VelocityImplementationInterface implements ImplementationInterface {

        @Override
        public Path baseDirectory(final BaseDirectoryScope scope) {
            switch (scope) {
                case CONFIG: return dataPath;
                case JAR: return PLUGINS_PATH;
                case SERVER:
                case WORLDS: return SERVER_PATH;
                default: throw new AssertionError("Unknown directory scope " + scope);
            }
        }

        @Override
        public DataSource dataSourceForUrl(final String url) throws SQLException {
            return Hikari.createDataSource(url, dataPath);
        }

        @Override
        public FormattedLogger logger() {
            return logger;
        }

        @Override
        public Executor asyncExecutor() {
            return exec;
        }

        @Override
        public String version() {
            return ProjectData.VERSION;
        }
    }
}
