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

import ca.stellardrift.permissionsex.logging.FormattedLogger;
import ca.stellardrift.permissionsex.subject.SubjectType;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * A PermissionsEx engine.
 *
 * @since 2.0.0
 */
public interface PermissionsEngine {
    String SUBJECTS_USER = "user";
    String SUBJECTS_GROUP = "group";
    String SUBJECTS_DEFAULTS = "default";
    String SUBJECTS_FALLBACK = "fallback";

    // -- Working with subject types -- //

    /**
     * Get a subject type by name.
     *
     * @param type the type identifier
     * @return a subject type instance, never null
     */
    SubjectType subjectType(final String type);

    /**
     * Get subject types with actively stored data.
     *
     * @return an unmodifiable view of the actively loaded subject types
     */
    Collection<? extends SubjectType> loadedSubjectTypes();

    /**
     * Get all subject types that have data stored.
     *
     * @return a stream producing all subject types
     * @since 2.0.0
     */
    Stream<String> knownSubjectTypes();

    // -- Engine state -- //

    /**
     * Get whether or not debug mode is enabled.
     *
     * <p>When debug mode is enabled, all permission queries will be logged to the engine's
     *    active logger.</p>
     *
     * @return the debug mode state
     * @since 2.0.0
     */
    boolean debugMode();

    /**
     * Set the active debug mode state.
     *
     * @param enabled whether debug mode is enabled
     * @since 2.0.0
     * @see #debugMode() for information on the consequences of debug mode
     */
    default void debugMode(final boolean enabled) {
        this.debugMode(enabled, null);
    }

    /**
     * Set the active debug mode state, and a filter.
     *
     * @param enabled whether debug mode is enabled
     * @param filter a filter for values (permissions, options, and subject names) that will be
     *               logged by debug mode.
     * @since 2.0.0
     * @see #debugMode() for information on the consequences of debug mode
     */
    void debugMode(final boolean enabled, final @Nullable Pattern filter);

    /**
     * Access the engine's async executor that can be used to schedule tasks
     *
     * @return the executor
     * @since 2.0.0
     */
    Executor asyncExecutor();

    /**
     * Get the logger used to log engine output.
     *
     * @return the engine logger
     * @since 2.0.0
     */
    FormattedLogger logger();

    /**
     * Get the base data directory where the engine will store data and configuration.
     *
     * @return the base data directory
     * @since 2.0.0
     */
    Path baseDirectory();

    /**
     * Temporary -- create a pooled SQL datasource for a certain URL.
     *
     * @param url the URL to query
     * @return a valid data source
     * @throws SQLException if the connection is invalid
     * @since 2.0.0
     * @deprecated need to find a better place to put this
     */
    @Deprecated
    DataSource dataSourceForUrl(String url) throws SQLException;
}
