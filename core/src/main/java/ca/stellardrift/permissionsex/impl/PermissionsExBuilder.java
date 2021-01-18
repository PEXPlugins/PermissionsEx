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
package ca.stellardrift.permissionsex.impl;

import ca.stellardrift.permissionsex.PermissionsEngine;
import ca.stellardrift.permissionsex.PermissionsEngineBuilder;
import ca.stellardrift.permissionsex.exception.PermissionsLoadingException;
import ca.stellardrift.permissionsex.impl.config.FilePermissionsExConfiguration;
import com.google.auto.service.AutoService;
import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.util.CheckedFunction;
import org.spongepowered.configurate.util.UnmodifiableCollections;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static net.kyori.adventure.text.Component.text;

public final class PermissionsExBuilder<C> implements PermissionsEngineBuilder<C> {
    final Class<C> configType;
    @Nullable Path configFile;
    @Nullable Path baseDirectory;
    @Nullable Logger logger;
    @Nullable Executor asyncExecutor;
    CheckedFunction<String, @Nullable DataSource, SQLException> databaseProvider = $ -> null;

    PermissionsExBuilder(final Class<C> configType) {
        this.configType = configType;
    }

    @Override
    public PermissionsEngineBuilder<C> configuration(final Path configFile) {
        this.configFile = configFile;
        return this;
    }

    @Override
    public PermissionsEngineBuilder<C> baseDirectory(final Path baseDir) {
        this.baseDirectory = baseDir;
        return this;
    }

    @Override
    public PermissionsEngineBuilder<C> logger(final Logger logger) {
        this.logger = requireNonNull(logger, "logger");
        return this;
    }

    @Override
    public PermissionsEngineBuilder<C> asyncExecutor(final Executor executor) {
        this.asyncExecutor = requireNonNull(executor, "executor");
        return this;
    }

    @Override
    public PermissionsEngineBuilder<C> databaseProvider(final CheckedFunction<String, @Nullable DataSource, SQLException> databaseProvider) {
        this.databaseProvider = requireNonNull(databaseProvider, "databaseProvider");
        return this;
    }

    @Override
    public PermissionsEx<?> build() throws PermissionsLoadingException {
        return (PermissionsEx<?>) this.buildWithConfig().getKey();
    }

    @Override
    public Map.Entry<PermissionsEngine, Supplier<C>> buildWithConfig() throws PermissionsLoadingException {
        if (this.logger == null) {
            this.logger = LoggerFactory.getLogger("PermissionsEx");
        }

        if (this.asyncExecutor == null) {
            this.asyncExecutor = ForkJoinPool.commonPool();
        }

        if (this.configFile == null) {
            throw new PermissionsLoadingException(text("Configuration file has not been set"));
        }

        if (this.baseDirectory == null) {
            this.baseDirectory = Paths.get(".");
        }

        final FilePermissionsExConfiguration<C> config = makeConfiguration(this.configFile);
        final PermissionsEx<C> engine = new PermissionsEx<>(
            this.logger,
            this.baseDirectory,
            this.asyncExecutor,
            this.databaseProvider
        );

        engine.initialize(config);
        return UnmodifiableCollections.immutableMapEntry(engine, engine.config()::getPlatformConfig);
    }

    private FilePermissionsExConfiguration<C> makeConfiguration(final Path configFile) throws PermissionsLoadingException {
        final String fileName = configFile.getFileName().toString();
        final ConfigurationLoader<?> loader;
        if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) {
            loader = YamlConfigurationLoader.builder()
                .path(configFile)
                .nodeStyle(NodeStyle.BLOCK)
                .defaultOptions(FilePermissionsExConfiguration.PEX_OPTIONS)
                .build();
        } else if (fileName.endsWith(".conf") || fileName.endsWith(".hocon")) {
            loader = HoconConfigurationLoader.builder()
                .path(configFile)
                .defaultOptions(FilePermissionsExConfiguration.PEX_OPTIONS)
                .build();
        } else if (fileName.endsWith(".json")) {
            loader = GsonConfigurationLoader.builder()
                .indent(2)
                .defaultOptions(FilePermissionsExConfiguration.PEX_OPTIONS)
                .build();
        } else {
            throw new PermissionsLoadingException(Messages.CONFIG_ERROR_UNKNOWN_FORMAT.tr(fileName));
        }

        try {
            return FilePermissionsExConfiguration.fromLoader(loader, this.configType);
        } catch (final ConfigurateException ex) {
            throw new PermissionsLoadingException(Messages.CONFIG_ERROR_LOAD.tr(ex));
        }
    }

    @AutoService(PermissionsEngineBuilder.Factory.class)
    public static class Factory implements PermissionsEngineBuilder.Factory {

        @Override
        public <C> PermissionsEngineBuilder<C> newBuilder(final Class<C> configType) {
            return new PermissionsExBuilder<>(configType);
        }

    }

}
