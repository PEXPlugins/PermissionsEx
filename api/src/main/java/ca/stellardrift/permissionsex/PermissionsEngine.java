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

import ca.stellardrift.permissionsex.datastore.DataStore;
import ca.stellardrift.permissionsex.logging.FormattedLogger;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import ca.stellardrift.permissionsex.subject.SubjectRef;
import ca.stellardrift.permissionsex.subject.SubjectType;
import ca.stellardrift.permissionsex.subject.SubjectTypeCollection;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
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

    /**
     * A subject type where subjects are
     */
    SubjectType<String> SUBJECTS_DEFAULTS = SubjectType.stringIdentBuilder("default")
                        .transientHasPriority(false)
                        .build();
    SubjectType<String> SUBJECTS_FALLBACK = SubjectType.stringIdentBuilder("fallback").build();

    // -- Working with subject types -- //

    /**
     * Get a subject type by name.
     *
     * <p>If this subject type has not been seen before, it will be registered.</p>
     *
     * @param type the type identifier
     * @return a subject type instance, never null
     * @since 2.0.0
     */
    <I> SubjectTypeCollection<I> subjects(final SubjectType<I> type);

    /**
     * Resolve a subject from a reference.
     *
     * @param reference the subject reference to resolve
     * @param <I> identifier type
     * @return a future providing the resolved subject
     * @since 2.0.0
     */
    default <I> CompletableFuture<CalculatedSubject> subject(final SubjectRef<I> reference) {
        return this.subjects(reference.type()).get(reference.identifier());
    }

    /**
     * Get subject types with actively stored data.
     *
     * @return an unmodifiable view of the actively loaded subject types
     */
    Collection<? extends SubjectTypeCollection<?>> loadedSubjectTypes();

    /**
     * Get all registered subject types
     *
     * @return a stream producing all subject types
     * @since 2.0.0
     */
    Set<SubjectType<?>> knownSubjectTypes();

    /**
     * Perform a low-level bulk operation.
     *
     * <p>This can be used for transforming subjects if the subject type definition changes, and
     * any large data changes that require information that may no longer be valid with current
     * subject type options.</p>
     *
     * <p>When possible, higher-level bulk query API (not yet written) should be used instead.</p>
     *
     * @param actor the action to perform
     * @param <V> the result type
     * @return a future completing with the result of the action
     */
    <V> CompletableFuture<V> doBulkOperation(final Function<DataStore, CompletableFuture<V>> actor);

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
