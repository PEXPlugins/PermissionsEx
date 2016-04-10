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
package ninja.leaping.permissionsex.backend;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.data.Caching;
import ninja.leaping.permissionsex.data.ContextInheritance;
import ninja.leaping.permissionsex.data.ImmutableSubjectData;
import ninja.leaping.permissionsex.exception.PermissionsException;
import ninja.leaping.permissionsex.exception.PermissionsLoadingException;
import ninja.leaping.permissionsex.rank.RankLadder;
import ninja.leaping.permissionsex.util.Util;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static ninja.leaping.permissionsex.util.Translations.t;

/**
 * Data type abstraction for permissions data
 */
public interface DataStore {
    /**
     * Activate this data store from the required data
     * @throws PermissionsLoadingException If the backing data cannot be loaded
     */
    void initialize(PermissionsEx core) throws PermissionsLoadingException;

    /**
     * Free any resources this backend may be using
     */
    void close();

    /**
     * Loads the data at the specified type and identifier. Implementations of this method do not need to perform any caching.
     *
     * @param type The type of subject to get
     * @param identifier The subject's identifier
     * @param listener The update listener for this subject
     * @return The relevant subject data
     */
    CompletableFuture<ImmutableSubjectData> getData(String type, String identifier, @Nullable Caching<ImmutableSubjectData> listener);

    /**
     * Sets the data at the specified type and identifier.
     *
     * @param type The type of subject data is being fetched for
     * @param identifier The identifier of the subject data is being fetched for
     * @param data The data to commit to this backend. This being null deletes any data for the given identifier
     * @return A future that can be used to listen for completion of writing the changed data
     */
    CompletableFuture<ImmutableSubjectData> setData(String type, String identifier, @Nullable ImmutableSubjectData data);

    default CompletableFuture<Void> moveData(String oldType, String oldIdentifier, String newType, String newIdentifier) {
        return isRegistered(oldType, oldIdentifier).thenCombine(isRegistered(newType, newIdentifier), (oldRegistered, newRegistered) -> {
            if (oldRegistered && !newRegistered) {
                return getData(oldType, oldIdentifier, null)
                        .thenCompose(oldData -> setData(newType, newIdentifier, oldData))
                        .thenCompose(newData -> setData(oldType, oldIdentifier, null))
                        .thenApply(inp -> (Void) null);
            } else {
                return Util.<Void>failedFuture(new PermissionsException(t("Destination subject already existed or target subject did not!")));
            }

        }).thenCompose(future -> future);
    }

    /**
     * Return if the given subject has any data stored in this backend.
     *
     * @param type The subject's type
     * @param identifier The subject's identifier
     * @return whether any data is stored
     */
    CompletableFuture<Boolean> isRegistered(String type, String identifier);

    /**
     * Get all data for subjects of the specified type. This {@link Iterable} may be filled asynchronously
     * @param type The type to get all data for
     * @return An iterable providing data
     */
    Iterable<Map.Entry<String, ImmutableSubjectData>> getAll(String type);

    /**
     * Get all subject identifiers for subjects of the given type.
     *
     * @param type The type of subject to get identifiers for
     * @return The registered identifiers of subjects of type {@code type}
     */
    Set<String> getAllIdentifiers(String type);

    /**
     * Return all subject types that contain data
     *
     * @return The registered subject types
     */
    Set<String> getRegisteredTypes();

    /**
     * Returns all subjects present in this data store
     *
     * @return An iterable containing all subjects
     */
     String serialize(ConfigurationNode node) throws PermissionsLoadingException;

    /**
     * Returns all subjects present in this data store
     *
     * @return An iterable containing all subjects
     */
    Iterable<Map.Entry<Map.Entry<String,String>,ImmutableSubjectData>> getAll();

    /**
     * Perform a bulk operation on this data store. While this operation is in progress, all writes must be suppressed
     * (meaning changes must be cached in memory until the operation is complete).
     *
     * Bulk operations may be executed asynchronously.
     *
     * @param function The function to call containing the operation.
     */
    <T> CompletableFuture<T> performBulkOperation(Function<DataStore, T> function);

    /**
     * Get all rank ladders.
     *
     * @return The names of all rank ladders
     */
    Iterable<String> getAllRankLadders();

    /**
     * Get a specific rank ladder, with a possible update listener.
     *
     * @param ladder The ladder to get. Case-insensitive
     * @param listener The listener to track possible updates
     * @return the ladder
     */
    CompletableFuture<RankLadder> getRankLadder(String ladder, @Nullable Caching<RankLadder> listener);

    /**
     * Whether a rank ladder by the given name is present.
     *
     * @param ladder The ladder to check. Case-insensitive
     * @return Whether a ladder by the provided name exists
     */
    CompletableFuture<Boolean> hasRankLadder(String ladder);

    /**
     * Set the rank ladder at the given identifier.
     *
     * @param identifier The name of the ladder. Case-insensitive for overwriting existing ladders
     * @param ladder The ladder to update
     * @return a future tracking the status of this operation
     */
    CompletableFuture<RankLadder> setRankLadder(String identifier, @Nullable RankLadder ladder);

    /**
     * Get context inheritance information
     * @param inheritance The listener to notify about changes
     * @return A future that will supply context inheritance
     */
    CompletableFuture<ContextInheritance> getContextInheritance(Caching<ContextInheritance> inheritance);

    CompletableFuture<ContextInheritance> setContextInheritance(ContextInheritance inheritance);
}

