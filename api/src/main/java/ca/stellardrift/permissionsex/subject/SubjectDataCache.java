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
package ca.stellardrift.permissionsex.subject;

import ca.stellardrift.permissionsex.datastore.DataStore;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * A cached view of the subject data within a particular {@link DataStore}.
 *
 * <p>Provides operations to manage querying, writing, and updating
 * {@link ImmutableSubjectData} objects.</p>
 *
 * @since 2.0.0
 */
public interface SubjectDataCache<I> {
    /**
     * Get the subject type for this cache.
     *
     * @return the subject type this cache contains data for
     * @since 2.0.0
     */
    SubjectType<I> type();

    /**
     * Get data for a given subject.
     *
     * <p>This will return a data object even if the subject is not registered; the data object will
     * just be empty.</p>
     *
     * <p>For most longer-lifetime use cases, {@link #referenceTo(Object)} will be the preferred
     * method to get a reference to the latest subject data.</p>
     *
     * @param identifier The identifier of the subject to query
     * @param listener A callback that will be notified whenever a change is made to the data object
     * @return A future returning when the data is available
     */
    CompletableFuture<ImmutableSubjectData> data(final I identifier, final @Nullable Consumer<ImmutableSubjectData> listener);

    /**
     * Get a reference to subject data for a given subject. The reference will update as changes are made to the backing
     * data store, and can always be used to query a specific subject's raw data.
     *
     * @param identifier The identifier of the subject to get data for
     * @return A future returning with a full reference to the given subject's data.
     */
    CompletableFuture<? extends SubjectRef.ToData<I>> referenceTo(final I identifier);

    /**
     * Get a reference to subject data for a given subject
     *
     * @param identifier The identifier of the subject to get data for
     * @param strongListeners Whether to hold listeners to this subject data even after they would be otherwise GC'd
     * @return A future completing with the subject data reference
     */
    CompletableFuture<? extends SubjectRef.ToData<I>> referenceTo(final I identifier, boolean strongListeners);

    /**
     * Update data for a given subject, acting on the latest data available.
     * The {@code action} callback may be called within an asynchronous task.
     *
     * @param identifier The identifier of the subject to be updated
     * @param action     A function taking an old subject data instance and returning an updated one
     * @return A future completing with the latest subject data after modifications are made
     */
    CompletableFuture<ImmutableSubjectData> update(final I identifier, final UnaryOperator<ImmutableSubjectData> action);

    /**
     * Load data (if any) known for the given identifier
     *
     * @param identifier The subject identifier
     */
    void load(final I identifier);

    /**
     * Remove a given subject identifier from the cache
     *
     * @param identifier The identifier of the subject to be removed
     */
    void invalidate(final I identifier);

    /**
     * Enter all subjects of this type into cache
     */
    void cacheAll();

    /**
     * Check if a given subject is registered. This operation occurs asynchronously
     * Registered means that a subject has any sort of data stored.
     *
     * @param identifier The identifier of the subject to check
     * @return A future returning whether the subject has data stored
     */
    CompletableFuture<Boolean> isRegistered(final I identifier);

    /**
     * Remove a subject from the backing data store
     *
     * @param identifier The identifier of the subject to remove
     * @return A future returning the previous subject data.
     */
    CompletableFuture<ImmutableSubjectData> remove(final I identifier);

    /**
     * Set the data for {@code identifier}.
     *
     * @param identifier identifier of the target subject
     * @param newData data to set for the subject, {@code null} to clear all data.
     * @return a future completing with the newly set subject data.
     */
    CompletableFuture<ImmutableSubjectData> set(final I identifier, final @Nullable ImmutableSubjectData newData);

    /**
     * Add a listener to be notified on updates to the given subject
     *
     * @param identifier The identifier of the subject to receive notifications about
     * @param listener The callback function to notify
     */
    void addListener(final I identifier, final Consumer<ImmutableSubjectData> listener);

    /**
     * Get a set of identifiers for all registered subjects of this type
     *
     * @return The set of identifiers
     */
    Stream<I> getAllIdentifiers();

    /**
     * Get the identifier for the subject holding default data for subjects of this type
     *
     * @return The id for the default subject of this type
     */
    SubjectRef<SubjectType<?>> getDefaultIdentifier();
}
