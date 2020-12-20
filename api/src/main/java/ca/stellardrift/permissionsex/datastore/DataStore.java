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
package ca.stellardrift.permissionsex.datastore;

import ca.stellardrift.permissionsex.PermissionsEngine;
import ca.stellardrift.permissionsex.context.ContextInheritance;
import ca.stellardrift.permissionsex.subject.ImmutableSubjectData;
import ca.stellardrift.permissionsex.exception.PermissionsLoadingException;
import ca.stellardrift.permissionsex.rank.RankLadder;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;


/**
 * Data type abstraction for permissions data.
 *
 * <p>This is the low-level interface used by the engine for querying data. It should perform
 * minimal caching, but is responsible for update notifications.</p>
 *
 * @since 2.0.0
 */
public interface DataStore {

    /**
     * Get the identifier that refers to this instance of the data store uniquely.
     *
     * <p>This is generally user-defined in the configuration.</p>
     *
     * @return The name of the current data store instance.
     */
    String getName();

    /**
     * Activate this data store from the required data.
     *
     * @param core manager the data store is attached to
     * @throws PermissionsLoadingException If the backing data cannot be loaded
     * @return true if there was pre-existing data to load, false if this is first run
     * @since 2.0.0
     */
    boolean initialize(PermissionsEngine core) throws PermissionsLoadingException;

    /**
     * Free any resources this data store may be using.
     *
     * @since 2.0.0
     */
    void close();

    /**
     * Loads the data at the specified type and identifier.
     *
     * <p>Implementations of this method do not need to perform any caching.</p>
     *
     * @param type The type of subject to get
     * @param identifier The subject's identifier
     * @param listener The update listener for this subject
     * @return The relevant subject data
     * @since 2.0.0
     */
    CompletableFuture<ImmutableSubjectData> getData(String type, String identifier, @Nullable Consumer<ImmutableSubjectData> listener);

    /**
     * Sets the data at the specified type and identifier.
     *
     * @param type The type of subject data is being fetched for
     * @param identifier The identifier of the subject data is being fetched for
     * @param data The data to commit to this backend. This being null deletes any data for the given identifier
     * @return A future that can be used to listen for completion of writing the changed data
     * @since 2.0.0
     */
    CompletableFuture<ImmutableSubjectData> setData(String type, String identifier, @Nullable ImmutableSubjectData data);

    /**
     * Move data from one subject to another
     *
     * @param oldType The old subject's type
     * @param oldIdentifier The old subject's identifier
     * @param newType The new subject's type
     * @param newIdentifier The new subject's identifier
     * @return A future that will complete when the move is complete
     * @since 2.0.0
     */
    CompletableFuture<Void> moveData(String oldType, String oldIdentifier, String newType, String newIdentifier);

    /**
     * Return if the given subject has any data stored in this backend.
     *
     * @param type The subject's type
     * @param identifier The subject's identifier
     * @return whether any data is stored
     * @since 2.0.0
     */
    CompletableFuture<Boolean> isRegistered(String type, String identifier);

    /**
     * Get all data for subjects of the specified type. This {@link Iterable} may be filled asynchronously
     * @param type The type to get all data for
     * @return An iterable providing data
     * @since 2.0.0
     */
    Iterable<Map.Entry<String, ImmutableSubjectData>> getAll(String type);

    /**
     * Get all subject identifiers for subjects of the given type.
     *
     * @param type The type of subject to get identifiers for
     * @return The registered identifiers of subjects of type {@code type}
     * @since 2.0.0
     */
    Set<String> getAllIdentifiers(String type);

    /**
     * Return all subject types that contain data
     *
     * @return The registered subject types
     * @since 2.0.0
     */
    Set<String> getRegisteredTypes();

    /**
     * Enumerate all contexts defined within this data store
     *
     * @return The contexts available within this data store
     * @since 2.0.0
     */
    CompletableFuture<Set<String>> getDefinedContextKeys();

    /**
     * Write this data store's configuration to the provided configuration node.
     *
     * @param node rode to write to
     * @return data store's type name
     * @throws PermissionsLoadingException if unable to do so
     * @since 2.0.0
     */
     String serialize(ConfigurationNode node) throws PermissionsLoadingException;

    /**
     * Returns all subjects present in this data store
     *
     * @return An iterable containing all subjects
     * @since 2.0.0
     */
    Iterable<Map.Entry<Map.Entry<String,String>,ImmutableSubjectData>> getAll();

    /**
     * Perform a bulk operation on this data store. While this operation is in progress, all writes must be suppressed
     * (meaning changes must be cached in memory until the operation is complete).
     *
     * Bulk operations may be executed asynchronously.
     *
     * @param <T> return value type
     * @param function The function to call containing the operation.
     * @return a future completing with the result of the operation or an error
     * @since 2.0.0
     */
    <T> CompletableFuture<T> performBulkOperation(Function<DataStore, T> function);

    /**
     * Get all rank ladders.
     *
     * @return The names of all rank ladders
     * @since 2.0.0
     */
    Iterable<String> getAllRankLadders();

    /**
     * Get a specific rank ladder, with a possible update listener.
     *
     * @param ladder The ladder to get. Case-insensitive
     * @param listener The listener to track possible updates
     * @return the ladder
     * @since 2.0.0
     */
    CompletableFuture<RankLadder> getRankLadder(String ladder, @Nullable Consumer<RankLadder> listener);

    /**
     * Whether a rank ladder by the given name is present.
     *
     * @param ladder The ladder to check. Case-insensitive
     * @return Whether a ladder by the provided name exists
     * @since 2.0.0
     */
    CompletableFuture<Boolean> hasRankLadder(String ladder);

    /**
     * Set the rank ladder at the given identifier.
     *
     * @param identifier The name of the ladder. Case-insensitive for overwriting existing ladders
     * @param ladder The ladder to update
     * @return a future tracking the status of this operation
     * @since 2.0.0
     */
    CompletableFuture<RankLadder> setRankLadder(String identifier, @Nullable RankLadder ladder);

    /**
     * Get context inheritance information.
     *
     * @param inheritance The listener to notify about changes
     * @return A future that will supply context inheritance
     * @since 2.0.0
     */
    CompletableFuture<ContextInheritance> getContextInheritance(@Nullable Consumer<ContextInheritance> inheritance);

    /**
     * Uppdate the stored context inheritance information.
     *
     * @param inheritance inheritance to apply
     * @return a future completing with the previous inheritance data
     * @since 2.0.0
     */
    CompletableFuture<ContextInheritance> setContextInheritance(ContextInheritance inheritance);
}

