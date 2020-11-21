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
import ca.stellardrift.permissionsex.datastore.DataStore;
import ca.stellardrift.permissionsex.data.SubjectDataCacheImpl;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class SubjectTypeImpl implements SubjectType {
    private final PermissionsEx<?> pex;
    private SubjectTypeDefinition<?> type;
    private SubjectDataCacheImpl persistentData;
    private SubjectDataCacheImpl transientData;
    private final AsyncLoadingCache<String, CalculatedSubject> cache;

    public SubjectTypeImpl(PermissionsEx<?> pex, String type, SubjectDataCacheImpl persistentData, SubjectDataCacheImpl transientData) {
        this.pex = pex;
        this.type = SubjectTypeDefinition.of(type);
        this.persistentData = persistentData;
        this.transientData = transientData; 
        this.cache = Caffeine.newBuilder().executor(pex.getAsyncExecutor()).buildAsync((key, executor) -> {
            CalculatedSubjectImpl subj = new CalculatedSubjectImpl(SubjectDataBaker.inheritance(), this.pex.createSubjectIdentifier(this.type.typeName(), key), SubjectTypeImpl.this);
            return persistentData.getReference(key, false).thenCombine(transientData.getReference(key, false), (persistentRef, transientRef) -> {
                subj.initialize(persistentRef, transientRef);
                return subj;
            });
        });
    }

    @Override
    public void setTypeInfo(SubjectTypeDefinition<?> def) {
        this.type = def;
    }

    @Override
    public SubjectTypeDefinition<?> getTypeInfo() {
        return this.type;
    }

    @Override
    public void cacheAll() {
        this.persistentData.cacheAll();
        this.transientData.cacheAll();
    }

    @Override
    public Collection<CalculatedSubject> getActiveSubjects() {
        return Collections.unmodifiableCollection(this.cache.synchronous().asMap().values());
    }

    @Override
    public void uncache(final String identifier) {
        if (!getTypeInfo().isNameValid(identifier)) {
            throw new IllegalArgumentException("Provided name " + identifier + " is not valid for subjects of type " + this.type.typeName());
        }

        this.persistentData.invalidate(identifier);
        this.transientData.invalidate(identifier);
        this.cache.synchronous().invalidate(identifier);
    }

    @Override
    public CompletableFuture<CalculatedSubject> get(final String identifier) {
        if (!getTypeInfo().isNameValid(identifier)) {
            throw new IllegalArgumentException("Provided name " + identifier + " is not valid for subjects of type " + type.typeName());
        }

        return this.cache.get(identifier);
    }

    @Override
    public SubjectDataCacheImpl transientData() {
        return this.transientData;
    }

    @Override
    public SubjectDataCacheImpl persistentData() {
        return this.persistentData;
    }

    PermissionsEx<?> getManager() {
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
    public void load(String identifier) {
        this.persistentData.load(identifier);
        this.transientData.load(identifier);
    }

    @Override
    public CompletableFuture<Boolean> isRegistered(final String identifier) {
        return this.persistentData.isRegistered(identifier)
                .thenCombine(this.transientData.isRegistered(identifier), Boolean::logicalAnd);
    }

    @Override
    public Set<String> getAllIdentifiers() {
        Set<String> ret = new HashSet<>();
        ret.addAll(this.persistentData.getAllIdentifiers());
        ret.addAll(this.transientData.getAllIdentifiers());
        return ret;
    }
}
