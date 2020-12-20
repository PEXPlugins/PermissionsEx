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

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Collection providing a view of subjects of a given type within the PEX engine.
 *
 * @param <I> subject identifier type
 * @since 2.0.0
 */
public interface SubjectTypeCollection<I> {

    /**
     * Set information configuring resolution for this subject type.
     *
     * @return the active type information
     * @since 2.0.0
     */
    SubjectType<I> getType();

    /**
     * Request that all subjects of this type be loaded into the cache.
     *
     * <p>Be aware that on larger collections this may be a very expensive operation.</p>
     *
     * @since 2.0.0
     */
    void cacheAll();

    /**
     * Get an unmodifiable view of the calculated subjects currently active in this data store.
     *
     * <p>This view may update at any time.</p>
     *
     * @return An unmodifiable view of the subjects
     * @since 2.0.0
     */
    Collection<CalculatedSubject> getActiveSubjects();

    /**
     * Request that a given identifier be removed from the cached data stores.
     *
     * @param identifier The subject identifier to uncache.
     * @since 2.0.0
     */
    void uncache(I identifier);

    /**
     * Get the calculated subject data for a given subject.
     *
     * <p>The returned data allows querying subject data taking into account both persistent and
     * transient data, inheritance, context priorities and inheritance, and any factors that may be
     * implemented in the future.</p>
     *
     * @param identifier The identifier of the subject to get
     * @return A future providing the calculated subject
     * @throws IllegalArgumentException if the given identifier is not valid for this subject type
     * @since 2.0.0
     */
    CompletableFuture<CalculatedSubject> get(I identifier);

    /**
     * Access the transient data for subjects of this type.
     *
     * <p>Operations on these data objects will only be stored in memory.
     * If {@link SubjectType#transientHasPriority()} is true, data set here will override the
     * persistent data for subjects in this collection.</p>
     *
     * @return The transient data cache
     * @since 2.0.0
     */
    SubjectDataCache<I> transientData();

    /**
     * Access the persistent data for subjects of this type
     *
     * @return The persistent data cache
     * @since 2.0.0
     */
    SubjectDataCache<I> persistentData();

    /**
     * Request that a subject of a given identifier is loaded into the cache. This operation completes asynchronously.
     *
     * @param identifier The identifier of the subject being loaded.
     * @since 2.0.0
     */
    void load(I identifier);

    /**
     * Query the backing data stores to see if a given subject is registered
     *
     * @param identifier The identifier of the subject to query
     * @return A future returning whether or not this subject has data registered
     * @since 2.0.0
     */
    CompletableFuture<Boolean> isRegistered(I identifier);

    /**
     * Get a set of subject identifiers for every subject registered of this type.
     *
     * @return All subject identifiers
     * @since 2.0.0
     */
    Stream<I> getAllIdentifiers();
}
