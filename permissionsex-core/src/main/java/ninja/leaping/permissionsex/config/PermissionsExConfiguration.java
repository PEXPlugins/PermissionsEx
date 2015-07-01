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

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMapper;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;
import ninja.leaping.permissionsex.backend.DataStore;
import ninja.leaping.permissionsex.exception.PEBKACException;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static ninja.leaping.permissionsex.util.Translations._;

/**
 * Configuration for PermissionsEx. This is designed to be serialized with a Configurate {@link ObjectMapper}
 */
public class PermissionsExConfiguration {
    public static final ObjectMapper<PermissionsExConfiguration> MAPPER;

    static {
        try {
            MAPPER = ObjectMapper.forClass(PermissionsExConfiguration.class);
        } catch (ObjectMappingException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    static {
        TypeSerializers.registerSerializer(new DataStoreSerializer());
    }

    @Setting private Map<String, DataStore> backends;
    @Setting("default-backend") private String defaultBackend;
    @Setting private boolean debug;
    @Setting("server-tags") private List<String> serverTags;

    protected PermissionsExConfiguration() {}

    public DataStore getDataStore(String name) {
        return backends.get(name);
    }

    public DataStore getDefaultDataStore() {
        return backends.get(defaultBackend);
    }

    public boolean isDebugEnabled() {
        return debug;
    }

    public List<String> getServerTags() {
        return Collections.unmodifiableList(serverTags);
    }

    public void validate() throws PEBKACException {
        if (backends.isEmpty()) {
            throw new PEBKACException(_("No backends defined!"));
        }
        if (defaultBackend == null) {
            throw new PEBKACException(_("Default backend is not set!"));
        }

        if (!backends.containsKey(defaultBackend)) {
            throw new PEBKACException(_("Default backend % is not an available backend! Choices are: %s", defaultBackend, backends.keySet()));
        }
    }

    public static ConfigurationNode loadDefaultConfiguration() throws IOException {
        final URL defaultConfig = PermissionsExConfiguration.class.getResource("default.conf");
        if (defaultConfig == null) {
            throw new Error(_("Default config file is not present in jar!").translate(Locale.getDefault()));
        }
        HoconConfigurationLoader fallbackLoader = HoconConfigurationLoader.builder().setURL(defaultConfig).build();
        return fallbackLoader.load();

    }
}
