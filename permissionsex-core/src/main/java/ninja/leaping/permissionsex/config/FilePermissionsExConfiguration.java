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
package ninja.leaping.permissionsex.config;

import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMapper;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;
import ninja.leaping.permissionsex.backend.DataStore;
import ninja.leaping.permissionsex.exception.PEBKACException;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static ninja.leaping.permissionsex.util.Translations.t;

/**
 * Configuration for PermissionsEx. This is designed to be serialized with a Configurate {@link ObjectMapper}
 */
@ConfigSerializable
public class FilePermissionsExConfiguration implements PermissionsExConfiguration {
    private static final TypeToken<FilePermissionsExConfiguration> TYPE = TypeToken.of(FilePermissionsExConfiguration.class);
    static {
        TypeSerializers.getDefaultSerializers().registerType(TypeToken.of(DataStore.class), new DataStoreSerializer());
    }

    private final ConfigurationLoader<?> loader;
    private final ConfigurationNode node;
    @Setting private Map<String, DataStore> backends;
    @Setting("default-backend") private String defaultBackend;
    @Setting private boolean debug;
    @Setting("server-tags") private List<String> serverTags;

    protected FilePermissionsExConfiguration(ConfigurationLoader<?> loader, ConfigurationNode node) {
        this.loader = loader;
        this.node = node;
    }

    public static FilePermissionsExConfiguration fromLoader(ConfigurationLoader<?> loader) throws IOException {
        ConfigurationNode node = loader.load();
        ConfigurationNode fallbackConfig;
        try {
            fallbackConfig = FilePermissionsExConfiguration.loadDefaultConfiguration();
        } catch (IOException e) {
            throw new Error("PEX's default configuration could not be loaded!", e);
        }
        ConfigTransformations.versions().apply(node);
        node.mergeValuesFrom(fallbackConfig);
        ConfigurationNode defBackendNode = node.getNode("default-backend");
        if (defBackendNode.isVirtual() || defBackendNode.getValue() == null) { // Set based on whether or not the H2 backend is available
            try {
                Class.forName("org.h2.Driver");
                defBackendNode.setValue("default");
            } catch (ClassNotFoundException e) {
                defBackendNode.setValue("default-file");
            }
        }

        FilePermissionsExConfiguration config = new FilePermissionsExConfiguration(loader, node);
        config.load();
        return config;
    }

    private void load() throws IOException {
        try {
            ObjectMapper.forObject(this).populate(this.node);
        } catch (ObjectMappingException e) {
            throw new IOException(e);
        }
        this.loader.save(node);
    }

    @Override
    public DataStore getDataStore(String name) {
        return backends.get(name);
    }

    @Override
    public DataStore getDefaultDataStore() {
        return backends.get(defaultBackend);
    }

    @Override
    public boolean isDebugEnabled() {
        return debug;
    }

    @Override
    public List<String> getServerTags() {
        return Collections.unmodifiableList(serverTags);
    }

    @Override
    public void validate() throws PEBKACException {
        if (backends.isEmpty()) {
            throw new PEBKACException(t("No backends defined!"));
        }
        if (defaultBackend == null) {
            throw new PEBKACException(t("Default backend is not set!"));
        }

        if (!backends.containsKey(defaultBackend)) {
            throw new PEBKACException(t("Default backend % is not an available backend! Choices are: %s", defaultBackend, backends.keySet()));
        }
    }

    @Override
    public PermissionsExConfiguration reload() throws IOException {
        ConfigurationNode node = this.loader.load();
        FilePermissionsExConfiguration ret = new FilePermissionsExConfiguration(this.loader, node);
        ret.load();
        return ret;
    }

    public static ConfigurationNode loadDefaultConfiguration() throws IOException {
        final URL defaultConfig = FilePermissionsExConfiguration.class.getResource("default.conf");
        if (defaultConfig == null) {
            throw new Error(t("Default config file is not present in jar!").translate(Locale.getDefault()));
        }
        HoconConfigurationLoader fallbackLoader = HoconConfigurationLoader.builder().setURL(defaultConfig).build();
        return fallbackLoader.load();

    }
}
