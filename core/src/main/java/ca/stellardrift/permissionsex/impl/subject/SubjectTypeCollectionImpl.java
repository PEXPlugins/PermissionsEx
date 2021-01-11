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
package ca.stellardrift.permissionsex.impl.subject;

import ca.stellardrift.permissionsex.impl.PermissionsEx;
import ca.stellardrift.permissionsex.datastore.DataStore;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import ca.stellardrift.permissionsex.subject.SubjectRef;
import ca.stellardrift.permissionsex.subject.SubjectType;
import ca.stellardrift.permissionsex.subject.SubjectTypeCollection;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class SubjectTypeCollectionImpl<I> implements SubjectTypeCollection<I> {
    private final PermissionsEx<?> pex;
    private final SubjectType<I> type;
    private final SubjectDataCacheImpl<I> persistentData;
    private final SubjectDataCacheImpl<I> transientData;
    private final AsyncLoadingCache<I, CalculatedSubject> cache;

    public SubjectTypeCollectionImpl(PermissionsEx<?> pex, final SubjectType<I> type, SubjectDataCacheImpl<I> persistentData, SubjectDataCacheImpl<I> transientData) {
        this.pex = pex;
        this.type = type;
        this.persistentData = persistentData;
        this.transientData = transientData;
        this.cache = Caffeine.newBuilder()
                .executor(pex.asyncExecutor())
                .buildAsync((key, executor) -> {
                    CalculatedSubjectImpl<I> subj = new CalculatedSubjectImpl<>(
                            SubjectDataBaker.inheritance(),
                            SubjectRef.subject(this.type, key),
                            this);

                    return persistentData.referenceTo(key, false)
                            .thenCombine(
                                    transientData.referenceTo(key, false),
                                    (persistentRef, transientRef) -> {
                                        subj.initialize(persistentRef, transientRef);
                                        return subj;
                                    });
                });
    }

    @Override
    public SubjectType<I> type() {
        return this.type;
    }

    @Override
    public void cacheAll() {
        this.persistentData.cacheAll();
        this.transientData.cacheAll();
    }

    @Override
    public Collection<CalculatedSubject> activeSubjects() {
        return Collections.unmodifiableCollection(this.cache.synchronous().asMap().values());
    }

    @Override
    public void uncache(final I identifier) {
        this.persistentData.invalidate(identifier);
        this.transientData.invalidate(identifier);
        this.cache.synchronous().invalidate(identifier);
    }

    @Override
    public CompletableFuture<CalculatedSubject> get(final I identifier) {
        return this.cache.get(identifier);
    }

    @Override
    public SubjectDataCacheImpl<I> transientData() {
        return this.transientData;
    }

    @Override
    public SubjectDataCacheImpl<I> persistentData() {
        return this.persistentData;
    }

    PermissionsEx<?> engine() {
        return this.pex;
    }

    /**
     * For internal use only. Update the data store associated with this cache, when for example the
     * active data store is changed in the engine.
     *
     * @param newDataStore The new data store to work off of
     */
    public void update(final DataStore newDataStore) {
        this.persistentData.update(newDataStore);
    }

    @Override
    public void load(final I identifier) {
        this.persistentData.load(identifier);
        this.transientData.load(identifier);
    }

    @Override
    public CompletableFuture<Boolean> isRegistered(final I identifier) {
        return this.persistentData.isRegistered(identifier)
                .thenCombine(this.transientData.isRegistered(identifier), Boolean::logicalAnd);
    }

    @Override
    public Stream<I> allIdentifiers() {
        return Stream.concat(
                this.persistentData.getAllIdentifiers(),
                this.transientData.getAllIdentifiers())
            .distinct();
    }
}
