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
package ca.stellardrift.permissionsex.config;

import ca.stellardrift.permissionsex.datastore.DataStore;
import ca.stellardrift.permissionsex.exception.PEBKACException;
import ca.stellardrift.permissionsex.exception.PermissionsException;
import io.leangen.geantyref.TypeFactory;
import io.leangen.geantyref.TypeToken;
import kotlin.Unit;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.objectmapping.meta.NodeResolver;
import org.spongepowered.configurate.objectmapping.meta.Setting;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;
import org.spongepowered.configurate.util.CheckedSupplier;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * Configuration for PermissionsEx.
 *
 * <p>This is designed to be serialized with a Configurate {@link ObjectMapper}
 */
@ConfigSerializable
public class FilePermissionsExConfiguration<T> implements PermissionsExConfiguration<T> {

    private static final TypeSerializerCollection PEX_SERIALIZERS = populateSerializers(TypeSerializerCollection.defaults().childBuilder()).build();
    public static final ConfigurationOptions PEX_OPTIONS = ConfigurationOptions.defaults()
            .implicitInitialization(true)
            .shouldCopyDefaults(true)
            .serializers(PEX_SERIALIZERS);

    public static ConfigurationOptions decorateOptions(final ConfigurationOptions input) {
        return input
                .implicitInitialization(true)
                .shouldCopyDefaults(true)
                .serializers(PEX_SERIALIZERS);
    }

    private final ConfigurationLoader<?> loader;
    private final ConfigurationNode node;
    private final Class<T> platformConfigClass;
    private @MonotonicNonNull Instance<T> instance;

    @ConfigSerializable
    static class Instance<T> {
        @Setting
        private Map<String, DataStore> backends;
        @Setting
        private String defaultBackend;
        @Setting
        private boolean debug;
        @Setting
        private List<String> serverTags;

        T platform;

        void validate() throws PEBKACException {
            if (this.backends.isEmpty()) {
                throw new PEBKACException(Messages.CONFIG_ERROR_NO_BACKENDS.tr());
            }
            if (this.defaultBackend == null) {
                throw new PEBKACException(Messages.CONFIG_ERROR_NO_DEFAULT.tr());
            }

            if (!this.backends.containsKey(this.defaultBackend)) {
                throw new PEBKACException(Messages.CONFIG_ERROR_INVALID_DEFAULT.tr(defaultBackend, backends.keySet()));
            }
        }

    }


    protected FilePermissionsExConfiguration(ConfigurationLoader<?> loader, ConfigurationNode node, Class<T> platformConfigClass) {
        this.loader = loader;
        this.node = node;
        this.platformConfigClass = platformConfigClass;
    }

    public static FilePermissionsExConfiguration<Unit> fromLoader(ConfigurationLoader<?> loader) throws ConfigurateException {
        return fromLoader(loader, Unit.class);
    }

    /**
     * Register PEX's type serializers with the provided collection
     *
     * @param coll The collection to add to
     * @return provided collection
     */
    public static TypeSerializerCollection.Builder populateSerializers(TypeSerializerCollection.Builder coll) {
        return coll
                .register(DataStore.class, new DataStoreSerializer())
                .register(new TypeToken<CheckedSupplier<?, SerializationException>>() {}, SupplierSerializer.INSTANCE)
                .registerAnnotatedObjects(ObjectMapper.factoryBuilder()
                        .addNodeResolver(NodeResolver.onlyWithSetting())
                        .build());
    }

    public static <T> FilePermissionsExConfiguration<T> fromLoader(ConfigurationLoader<?> loader, Class<T> platformConfigClass) throws ConfigurateException {
        ConfigurationNode node = loader.load(loader.defaultOptions().serializers(PEX_SERIALIZERS).implicitInitialization(true).shouldCopyDefaults(true));
        ConfigurationNode fallbackConfig;
        try {
            fallbackConfig = FilePermissionsExConfiguration.loadDefaultConfiguration();
        } catch (final ConfigurateException e) {
            throw new Error("PEX's default configuration could not be loaded!", e);
        }
        ConfigTransformations.versions().apply(node);
        node.mergeFrom(fallbackConfig);
        ConfigurationNode defBackendNode = node.node("default-backend");
        if (defBackendNode.empty()) { // Set based on whether or not the H2 backend is available
            defBackendNode.set("default-file");
            /*try {
                Class.forName("org.h2.Driver");
                defBackendNode.setValue("default");
            } catch (ClassNotFoundException e) {
                defBackendNode.setValue("default-file");
            }*/
        }

        FilePermissionsExConfiguration<T> config = new FilePermissionsExConfiguration<>(loader, node, platformConfigClass);
        config.load();
        return config;
    }

    @SuppressWarnings("unchecked") // manual type checking
    private void load() throws ConfigurateException {
        this.instance = (Instance<T>) this.node.get(TypeFactory.parameterizedClass(Instance.class, this.platformConfigClass));
        if (this.platformConfigClass == Unit.class) {
            this.instance.platform = (T) Unit.INSTANCE;
        } else {
            this.instance.platform = this.platformConfigNode().get(this.platformConfigClass);
        }
        this.loader.save(node);
    }

    @Override
    public void save() throws IOException {
        this.node.set(TypeFactory.parameterizedClass(Instance.class, this.platformConfigClass), this.instance);
        if (this.platformConfigClass != Unit.class) {
            platformConfigNode().set(this.platformConfigClass, this.instance.platform);
        }

        this.loader.save(node);
    }

    private ConfigurationNode platformConfigNode() {
        return this.node.node("platform");
    }

    @Override
    public DataStore getDataStore(String name) {
        return this.instance.backends.get(name);
    }

    @Override
    public DataStore getDefaultDataStore() {
        return this.instance.backends.get(this.instance.defaultBackend);
    }

    @Override
    public boolean isDebugEnabled() {
        return this.instance.debug;
    }

    @Override
    public List<String> getServerTags() {
        return Collections.unmodifiableList(this.instance.serverTags);
    }

    @Override
    public void validate() throws PEBKACException {
        this.instance.validate();
    }

    @Override
    public T getPlatformConfig() {
        return this.instance.platform;
    }

    @Override
    public FilePermissionsExConfiguration<T> reload() throws IOException {
        try {
            ConfigurationNode node = this.loader.load();
            FilePermissionsExConfiguration<T> ret = new FilePermissionsExConfiguration<>(this.loader, node, this.platformConfigClass);
            ret.load();
            return ret;
        } catch (final ConfigurateException ex) {
            throw new IOException(ex);
        }
    }

    public static ConfigurationNode loadDefaultConfiguration() throws ConfigurateException {
        final URL defaultConfig = FilePermissionsExConfiguration.class.getResource("default.conf");
        if (defaultConfig == null) {
            throw new Error(new PermissionsException(Messages.CONFIG_ERROR_DEFAULT_CONFIG.tr()));
        }
        HoconConfigurationLoader fallbackLoader = HoconConfigurationLoader.builder()
                .defaultOptions(FilePermissionsExConfiguration::decorateOptions)
                .url(defaultConfig).build();
        return fallbackLoader.load();
    }
}
