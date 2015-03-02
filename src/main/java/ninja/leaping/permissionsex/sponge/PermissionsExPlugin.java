/**
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
package ninja.leaping.permissionsex.sponge;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.inject.Inject;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.exception.PEBKACException;
import ninja.leaping.permissionsex.config.ConfigTransformations;
import ninja.leaping.permissionsex.config.PermissionsExConfiguration;
import ninja.leaping.permissionsex.config.DataStoreSerializer;
import org.slf4j.Logger;
import org.spongepowered.api.event.state.PreInitializationEvent;
import org.spongepowered.api.event.state.ServerStoppedEvent;
import org.spongepowered.api.event.state.ServerStoppingEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.service.ProviderExistsException;
import org.spongepowered.api.service.ServiceManager;
import org.spongepowered.api.service.ServiceReference;
import org.spongepowered.api.service.config.ConfigDir;
import org.spongepowered.api.service.config.DefaultConfig;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.context.ContextCalculator;
import org.spongepowered.api.service.scheduler.Scheduler;
import org.spongepowered.api.service.sql.SqlService;
import org.spongepowered.api.util.event.Subscribe;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * PermissionsEx plugin
 */
@Plugin(id = PomData.ARTIFACT_ID, name = PomData.NAME, version = PomData.VERSION)
public class PermissionsExPlugin implements PermissionService {
    static {
        TypeSerializers.registerSerializer(new DataStoreSerializer());
    }

    private ServiceReference<SqlService> sql;
    private ServiceReference<Scheduler> scheduler;
    @Inject private ServiceManager services;
    @Inject private Logger logger;
    @Inject @ConfigDir(sharedRoot = false) private File configDir;
    @Inject @DefaultConfig(sharedRoot = false) private ConfigurationLoader<CommentedConfigurationNode> configLoader;
    private PermissionsEx manager;
    private PermissionsExConfiguration config;
    private ConfigurationNode rawConfig;

    @Subscribe
    public void onPreInit(PreInitializationEvent event) throws PEBKACException {
        logger.info("Pre-init of " + PomData.NAME + " v" + PomData.VERSION);
        sql = services.potentiallyProvide(SqlService.class);
        scheduler = services.potentiallyProvide(Scheduler.class);

        try {
            convertFromBukkit();
            configDir.mkdirs();
            reloadSync();
        } catch (PEBKACException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while enabling " + PomData.NAME, e);
        }

        try {
            PermissionsExConfiguration.MAPPER.bind(config).serialize(rawConfig);
            configLoader.save(rawConfig);
        } catch (IOException | ObjectMappingException e) {
            throw new RuntimeException(e);
        }

        // Registering the PEX service *must* occur after the plugin has been completely initialized
        try {
            event.getGame().getServiceManager().setProvider(this, PermissionService.class, this);
        } catch (ProviderExistsException e) {
            manager.close();
            throw new PEBKACException("Your appear to already be using a different permissions plugin: " + e.getLocalizedMessage());
        }
    }

    @Subscribe
    public void disable(ServerStoppingEvent event) {
        logger.debug("Disabling PermissionsEx");
        if (manager != null) {
            manager.close();
            manager = null;
        }
    }

    static ConfigurationNode loadDefaultConfiguration() throws IOException {
        final URL defaultConfig = PermissionsExPlugin.class.getResource("default.conf");
        if (defaultConfig == null) {
            throw new Error("Default config file is not present in jar!");
        }
        HoconConfigurationLoader fallbackLoader = HoconConfigurationLoader.builder().setURL(defaultConfig).build();
        return fallbackLoader.load();

    }

    private void convertFromBukkit() throws IOException {
        File bukkitConfigDir = new File("plugins/PermissionsEx");
        if (bukkitConfigDir.isDirectory() && !configDir.isDirectory()) {
            logger.info("Migrating configuration data from Bukkit");
            if (!bukkitConfigDir.renameTo(configDir)) {
                throw new IOException("Unable to move Bukkit configuration directory to location for Sponge!");
            }
        }
        File bukkitConfigFile = new File(configDir, "config.yml");
        if (bukkitConfigFile.isFile()) {
            ConfigurationLoader<ConfigurationNode> yamlReader = YAMLConfigurationLoader.builder().setFile(bukkitConfigFile).build();
            ConfigurationNode bukkitConfig = yamlReader.load();
            ConfigTransformations.fromBukkit().apply(bukkitConfig);
            configLoader.save(bukkitConfig);
            if (!bukkitConfigFile.renameTo(new File(configDir, "config.yml.bukkit"))) {
                logger.warn("Could not rename old Bukkit configuration file to old name");
            }
        }
    }

    private void reloadSync() throws Exception {
        try {
            rawConfig = configLoader.load();
            ConfigurationNode fallbackConfig;
            try {
                fallbackConfig = loadDefaultConfiguration();
            } catch (IOException e) {
                throw new Error("PEX's default configuration could not be loaded!", e);
            }
            rawConfig.mergeValuesFrom(fallbackConfig);
            config = PermissionsExConfiguration.MAPPER.bindToNew().populate(rawConfig);
            config.validate();
            PermissionsEx oldManager = manager;
            manager = new PermissionsEx(config, configDir);
            if (oldManager != null) {
                oldManager.close();
            }

        } catch (IOException e) {
            throw new PEBKACException("Error while loading configuration: " + e.getLocalizedMessage());
        }
    }

    public ListenableFuture<Void> reload() {
        return ListenableFutureTask.create(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                reloadSync();
                return null;
            }
        });
    }


    @Override
    public SubjectCollection getUserSubjects() {
        return null;
    }

    @Override
    public SubjectCollection getGroupSubjects() {
        return null;
    }

    @Override
    public SubjectData getDefaultData() {
        return null;
    }

    @Override
    public Optional<SubjectCollection> getSubjects(String identifier) {
        return null;
    }

    @Override
    public Map<String, SubjectCollection> getKnownSubjects() {
        return null;
    }

    @Override
    public void registerContextCalculator(ContextCalculator calculator) {

    }
}
