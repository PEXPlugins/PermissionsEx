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

import ca.stellardrift.permissionsex.util.MinecraftProfile;
import ca.stellardrift.permissionsex.commands.parse.CommandSpec;
import com.google.common.collect.Maps;
import org.slf4j.Logger;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Methods that are specific to a certain implementation of PermissionsEx (Sponge, Forge, etc)
 */
public interface ImplementationInterface {

    /**
     * Return the base directory to store any additional configuration files in.
     *
     * @return The base directory
     */
    default Path getBaseDirectory() {
        return getBaseDirectory(BaseDirectoryScope.CONFIG);
    }

    /**
     * Return the base directory for storing various types of files, depending on the scope
     *
     * @param scope The scope to find the base directory for
     * @return An appropriate path
     */
    Path getBaseDirectory(BaseDirectoryScope scope);

    /**
     * Gets the appropriate logger
     * @return The base logger
     */
    Logger getLogger();

    /**
     * Returns an appropriate data source for the implementation-dependent specificer {@code url}.
     *
     * @param url The specifier to get a data source for
     * @return The appropriate data source, or null if not supported
     * @throws SQLException If a connection to the provided database cannot be established
     */
    DataSource getDataSourceForURL(String url) throws SQLException;

    /**
     * Get an executor to run tasks asynchronously on.
     *
     * @return The async executor
     */
    Executor getAsyncExecutor();

    /**
     * Provide a supplier for commands, to be called once the implementation's backend
     * is ready to accept command registrations.
     *
     * This method may be called multiple times. If the commands are not registered immediately,
     * the implementation must cache all values provided in each call.
     *
     * @param commandSupplier The function that will provide commands to register.
     */
    void registerCommands(Supplier<Set<CommandSpec>> commandSupplier);

    /**
     * Get commands that the implementation wants to register as a child of the {@code /pex} command
     *
     * @return The desired subcommands, or an empty set
     */
    Set<CommandSpec> getImplementationCommands();

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

    default CompletableFuture<Integer> lookupMinecraftProfilesByName(Iterable<String> names, Consumer<MinecraftProfile> action) {
         return lookupMinecraftProfilesByName(names, profile -> {
             action.accept(profile);
             return CompletableFuture.completedFuture(null);
         });
    }

    CompletableFuture<Integer> lookupMinecraftProfilesByName(Iterable<String> names, Function<MinecraftProfile, CompletableFuture<Void>> action);
}
