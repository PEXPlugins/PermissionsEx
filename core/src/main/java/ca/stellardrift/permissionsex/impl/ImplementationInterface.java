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

import ca.stellardrift.permissionsex.impl.commands.parse.CommandSpec;
import com.google.common.collect.Maps;
import org.slf4j.Logger;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Methods that are specific to a certain implementation of PermissionsEx (Sponge, Forge, etc)
 */
public interface ImplementationInterface {

    /**
     * Return the base directory to store any additional configuration files in.
     *
     * @return The base directory
     */
    default Path baseDirectory() {
        return baseDirectory(BaseDirectoryScope.CONFIG);
    }

    /**
     * Return the base directory for storing various types of files, depending on the scope
     *
     * @param scope The scope to find the base directory for
     * @return An appropriate path
     */
    Path baseDirectory(BaseDirectoryScope scope);

    /**
     * Gets the appropriate logger
     * @return The base logger
     */
    Logger logger();

    /**
     * Returns an appropriate data source for the implementation-dependent specificer {@code url}.
     *
     * @param url The specifier to get a data source for
     * @return The appropriate data source, or null if not supported
     * @throws SQLException If a connection to the provided database cannot be established
     */
    DataSource dataSourceForUrl(String url) throws SQLException;

    /**
     * Get an executor to run tasks asynchronously on.
     *
     * @return The async executor
     */
    Executor asyncExecutor();

    /**
     * Get commands that the implementation wants to register as a child of the {@code /pex} command
     *
     * @return The desired subcommands, or an empty set
     */
    default Set<CommandSpec> getImplementationSubcommands() {
        return Collections.emptySet();
    }

    /**
     * Return the version number attached to this implementation of PEX
     *
     * @return The currently running version
     */
    String getVersion();

    /**
     * Return a function that supplies an implementation-dependent variant of a subject reference
     *
     * @param collection subject collection
     * @param ident specific identifier for a subject
     * @return The identifier for a certain subject
     */
     default Map.Entry<String, String> createSubjectIdentifier(String collection, String ident) {
        return Maps.immutableEntry(collection, ident);
     }
}
