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

import ca.stellardrift.permissionsex.PermissionsEx;
import ca.stellardrift.permissionsex.backend.DataStore;
import ca.stellardrift.permissionsex.data.SubjectCache;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Collection providing a view of subjects of a given type within the PEX engine
 */
public class SubjectType {
    private final PermissionsEx<?> pex;
    private SubjectTypeDefinition<?> type;
    private SubjectCache persistentData, transientData;
    private final AsyncLoadingCache<String, CalculatedSubject> cache;

    public SubjectType(PermissionsEx<?> pex, String type, SubjectCache persistentData, SubjectCache transientData) {
        this.pex = pex;
        this.type = SubjectTypeDefinitionKt.subjectType(type);
        this.persistentData = persistentData;
        this.transientData = transientData; 
        cache = Caffeine.newBuilder().executor(pex.getAsyncExecutor()).buildAsync(((key, executor) -> {
            CalculatedSubject subj = new CalculatedSubject(SubjectDataBaker.inheritance(), this.pex.createSubjectIdentifier(this.type.getTypeName(), key), SubjectType.this);
            return persistentData.getReference(key, false).thenCombine(transientData.getReference(key, false), (persistentRef, transientRef) -> {
                subj.initialize(persistentRef, transientRef);
                return subj;
            });
        }));
    }

    public void setTypeInfo(SubjectTypeDefinition<?> def) {
        this.type = def;
    }

    public SubjectTypeDefinition<?> getTypeInfo() {
        return this.type;
    }

    /**
     * Request that all subjects of this type be loaded into the cache.
     * Be aware that on larger collections this may be a very expensive operation.
     */
    public void cacheAll() {
        this.persistentData.cacheAll();
        this.transientData.cacheAll();
    }

    /**
     * Get an unmodifiable view of the calculated subjects currently active in this data store.
     * This view may update at any time .
     *
     * @return An unmodifiable view of the subjects
     */
    public Collection<CalculatedSubject> getActiveSubjects() {
        return Collections.unmodifiableCollection(this.cache.synchronous().asMap().values());
    }

    /**
     * Request that a given identifier be removed from the cached data stores.
     *
     * @param identifier The subject identifier to uncache.
     */
    public void uncache(String identifier) {
        if (!getTypeInfo().isNameValid(identifier)) {
            throw new IllegalArgumentException("Provided name " + identifier + " is not valid for subjects of type " + type.getTypeName());
        }

        persistentData.invalidate(identifier);
        transientData.invalidate(identifier);
        cache.synchronous().invalidate(identifier);
    }

    /**
     * Get the calculated subject data for a given subject. The returned data allows querying subject data taking into
     * account both persistent and transient data, inheritance, context priorities and inheritance, and any factors
     * that may be implemented in the future.
     *
     * @param identifier The identifier of the subject to get
     * @throws IllegalArgumentException if the given identifier is not valid for this subject type
     * @return A future providing the calculated subject
     */
    public CompletableFuture<CalculatedSubject> get(String identifier) {
        if (!getTypeInfo().isNameValid(identifier)) {
            throw new IllegalArgumentException("Provided name " + identifier + " is not valid for subjects of type " + type.getTypeName());
        }

        return cache.get(identifier);
    }

    /**
     * Access the transient data for subjects of this type. Operations on these data objects will only be stored in memory.
     * If {@link SubjectTypeDefinition#transientHasPriority()} is true, data set here will override the persistent data
     * for subjects of this type.
     *
     * @return The transient data cache
     */
    public SubjectCache transientData() {
        return transientData;
    }

    /**
     * Access the persistent data for subjects of this type
     *
     * @return The persistent data cache
     */
    public SubjectCache persistentData() {
        return persistentData;
    }

    PermissionsEx<?> getManager() {
        return pex;
    }

    /**
     * For internal use only. Update the data store associated with this cache, when for example the
     * active data store is changed in the engine.
     *
     * @param newDataStore The new data store to work off of
     */
    public void update(DataStore newDataStore) {
        this.persistentData.update(newDataStore);
    }

    /**
     * Request that a subject of a given identifier is loaded into the cache. This operation completes asynchronously.
     *
     * @param identifier The identifier of the subject being loaded.
     */
    public void load(String identifier) {
        this.persistentData.load(identifier);
        this.transientData.load(identifier);
    }

    /**
     * Query the backing data stores to see if a given subject is registered
     *
     * @param identifier The identifier of the subject to query
     * @return A future returning whether or not this subject has data registered
     */
    public CompletableFuture<Boolean> isRegistered(String identifier) {
        return this.persistentData.isRegistered(identifier)
                .thenCombine(this.transientData.isRegistered(identifier), Boolean::logicalAnd);
    }

    /**
     * Get a set of subject identifiers for every subject registered of this type.
     *
     * @return All subject identifiers
     */
    public Set<String> getAllIdentifiers() {
        Set<String> ret = new HashSet<>();
        ret.addAll(this.persistentData.getAllIdentifiers());
        ret.addAll(this.transientData.getAllIdentifiers());
        return ret;
    }
}
