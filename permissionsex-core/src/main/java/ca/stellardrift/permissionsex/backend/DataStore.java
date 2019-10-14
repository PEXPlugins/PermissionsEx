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

package ca.stellardrift.permissionsex.backend;

import ca.stellardrift.permissionsex.PermissionsEx;
import ca.stellardrift.permissionsex.data.ContextInheritance;
import ca.stellardrift.permissionsex.data.ImmutableSubjectData;
import ca.stellardrift.permissionsex.exception.PermissionsException;
import ca.stellardrift.permissionsex.exception.PermissionsLoadingException;
import ca.stellardrift.permissionsex.rank.RankLadder;
import kotlin.Pair;
import ninja.leaping.configurate.ConfigurationNode;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static ca.stellardrift.permissionsex.util.Translations.t;

/**
 * Data type abstraction for permissions data
 */
@NonNull
public interface DataStore {
    /**
     * Activate this data store from the required data
     * @throws PermissionsLoadingException If the backing data cannot be loaded
     * @return true if there was pre-existing data to load, false if this is first run
     */
    boolean initialize(PermissionsEx core) throws PermissionsLoadingException;

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
    Mono<ImmutableSubjectData> getData(String type, String identifier, @Nullable Consumer<ImmutableSubjectData> listener);

    /**
     * Sets the data at the specified type and identifier.
     *
     * @param type The type of subject data is being fetched for
     * @param identifier The identifier of the subject data is being fetched for
     * @param data The data to commit to this backend. This being null deletes any data for the given identifier
     * @return A future that can be used to listen for completion of writing the changed data
     */
    Mono<ImmutableSubjectData> setData(String type, String identifier, @Nullable ImmutableSubjectData data);

    /**
     * Move data from one subject to another
     *
     * @param oldType The old subject's type
     * @param oldIdentifier The old subject's identifier
     * @param newType The new subject's type
     * @param newIdentifier The new subject's identifier
     * @return A future that will complete when the move is complete
     */
    default Mono<?> moveData(String oldType, String oldIdentifier, String newType, String newIdentifier) {
        // oldRegistered && !newRegistered
        return isRegistered(oldType, oldIdentifier).concatWith(isRegistered(newType, newIdentifier).map(x -> !x)).all(x ->x).flatMap(shouldMove -> {
            if (shouldMove) {
                return getData(oldType, oldIdentifier, null)
                        .flatMap(oldData -> setData(newType, newIdentifier, oldData))
                        .flatMap(newData -> setData(oldType, oldIdentifier, null))
                        .ignoreElement();
            } else {
                return Mono.error(new PermissionsException(t("Destination subject already existed or source subject did not!")));
            }
        });
    }

    /**
     * Return if the given subject has any data stored in this backend.
     *
     * @param type The subject's type
     * @param identifier The subject's identifier
     * @return whether any data is stored
     */
    Mono<Boolean> isRegistered(String type, String identifier);

    /**
     * Get all data for subjects of the specified type. This {@link Iterable} may be filled asynchronously
     * @param type The type to get all data for
     * @return An iterable providing data
     */
    Flux<Pair<String, ImmutableSubjectData>> getAll(String type);

    /**
     * Get all subject identifiers for subjects of the given type.
     *
     * @param type The type of subject to get identifiers for
     * @return The registered identifiers of subjects of type {@code type}
     */
    Flux<String> getAllIdentifiers(String type);

    /**
     * Return all subject types that contain data
     *
     * @return The registered subject types
     */
    Flux<String> getRegisteredTypes();

    /**
     * Enumerate all contexts defined within this data store
     *
     * @return The contexts available within this data store
     */
    Flux<String> getDefinedContextKeys();

    /**
     * Serialize this data store's options
     *
     * @return An iterable containing all subjects
     */
     String serialize(ConfigurationNode node) throws PermissionsLoadingException;

    /**
     * Returns all subjects present in this data store
     *
     * @return An iterable containing all subjects
     */
    Flux<Pair<Map.Entry<String,String>,ImmutableSubjectData>> getAll();

    /**
     * Perform a bulk operation on this data store. While this operation is in progress, all writes must be suppressed
     * (meaning changes must be cached in memory until the operation is complete).
     *
     * Bulk operations may be executed asynchronously.
     *
     * @param function The function to call containing the operation.
     */
    <T, F extends Publisher<T>> F performBulkOperation(Function<DataStore, F> function);

    /**
     * Get all rank ladders.
     *
     * @return The names of all rank ladders
     */
    Flux<String> getAllRankLadders();

    /**
     * Get a specific rank ladder, with a possible update listener.
     *
     * @param ladder The ladder to get. Case-insensitive
     * @param listener The listener to track possible updates
     * @return the ladder
     */
    Mono<RankLadder> getRankLadder(String ladder, @Nullable Consumer<RankLadder> listener);

    /**
     * Whether a rank ladder by the given name is present.
     *
     * @param ladder The ladder to check. Case-insensitive
     * @return Whether a ladder by the provided name exists
     */
    Mono<Boolean> hasRankLadder(String ladder);

    /**
     * Set the rank ladder at the given identifier.
     *
     * @param identifier The name of the ladder. Case-insensitive for overwriting existing ladders
     * @param ladder The ladder to update
     * @return a future tracking the status of this operation
     */
    Mono<RankLadder> setRankLadder(String identifier, @Nullable RankLadder ladder);

    /**
     * Get context inheritance information
     * @param inheritance The listener to notify about changes
     * @return A future that will supply context inheritance
     */
    Mono<ContextInheritance> getContextInheritance(Consumer<ContextInheritance> inheritance);

    Mono<ContextInheritance> setContextInheritance(ContextInheritance inheritance);

}

