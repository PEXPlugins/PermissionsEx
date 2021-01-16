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
package ca.stellardrift.permissionsex;

import ca.stellardrift.permissionsex.exception.PermissionsLoadingException;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.spongepowered.configurate.util.CheckedFunction;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * A builder for creating permissions engines.
 *
 * @since 2.0.0
 */
public interface PermissionsEngineBuilder {

    /**
     * Set the file that this engine will load its configuration from.
     *
     * @param configFile The configuration file.
     * @return this builder
     * @since 2.0.0
     */
    PermissionsEngineBuilder configuration(final Path configFile);

    /**
     * Set the base directory for this engine instance.
     *
     * @param baseDir the base directory
     * @return this builder
     * @since 2.0.0
     */
    PermissionsEngineBuilder baseDirectory(Path baseDir);

    /**
     * Set a logger that will receive messages logged by the engine.
     *
     * <p>By default, this will be a logger named {@code PermissionsEx}</p>
     *
     * @param logger the logger to log to
     * @return this builder
     * @since 2.0.0
     */
    PermissionsEngineBuilder logger(final Logger logger);

    /**
     * Set an executor to use to execute asynchronous tasks.
     *
     * <p>By default, the {@link ForkJoinPool#commonPool()} will be used.</p>
     *
     * @param executor The executor
     * @return this builder
     * @since 2.0.0
     */
    PermissionsEngineBuilder asyncExecutor(final Executor executor);

    /**
     * Set a callback function that will be queried to provide database for a url.
     *
     * <p>Implementations may allow this url to be an alias to an existing connection definition,
     * rather than an actual URL.</p>
     *
     * @param databaseProvider The database provider
     * @return this builder
     * @since 2.0.0
     */
    PermissionsEngineBuilder databaseProvider(final CheckedFunction<String, @Nullable DataSource, SQLException> databaseProvider);

    /**
     * Create a full engine..
     *
     * @return the built engine
     * @throws PermissionsLoadingException if any errors occur while loading the engine or its configuration
     * @since 2.0.0
     */
    PermissionsEngine build() throws PermissionsLoadingException;

    /**
     * A service interface for creating new engine builders.
     *
     * <p>Implementations of the PermissionsEx engine must register a {@link java.util.ServiceLoader}-compatible service
     * implementing this interface.</p>
     *
     * @see PermissionsEngine#builder() to create a new builder
     * @since 2.0.0
     */
    interface Factory {

        /**
         * Create a new empty builder.
         *
         * @return a new builder
         * @since 2.0.0
         */
        PermissionsEngineBuilder newBuilder();
    }
}
