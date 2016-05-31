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
package ninja.leaping.permissionsex.subject;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.backend.DataStore;
import ninja.leaping.permissionsex.data.SubjectCache;
import ninja.leaping.permissionsex.data.SubjectRef;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Collection holding data defining a type of subject.
 */
public class SubjectType {
    private final PermissionsEx pex;
    private SubjectTypeDefinition type;
    private SubjectCache persistentData, transientData;
    private final AsyncLoadingCache<String, CalculatedSubject> cache = Caffeine.newBuilder().buildAsync(((key, executor) -> {
        CalculatedSubject subj = new CalculatedSubject(SubjectDataBaker.inheritance(), SubjectRef.of(type.getTypeName(), key), SubjectType.this);
        return persistentData.getReference(key).thenCombine(transientData.getReference(key), (persistentRef, transientRef) -> {
            subj.initialize(persistentRef, transientRef);
            return subj;
        });
    }));

    public SubjectType(PermissionsEx pex, String type, SubjectCache persistentData, SubjectCache transientData) {
        this.pex = pex;
        this.type = SubjectTypeDefinition.defaultFor(type);
        this.persistentData = persistentData;
        this.transientData = transientData;
    }

    public void setTypeInfo(SubjectTypeDefinition def) {
        this.type = def;
    }

    public SubjectTypeDefinition getTypeInfo() {
        return this.type;
    }

    public void cacheAll() {
        this.persistentData.cacheAll();
        this.transientData.cacheAll();
    }

    public Collection<CalculatedSubject> getActiveSubjects() {
        return Collections.unmodifiableCollection(this.cache.synchronous().asMap().values());
    }

    public void uncache(String identifier) {
        if (!getTypeInfo().isNameValid(identifier)) {
            throw new IllegalArgumentException("Provided name " + identifier + " is not valid for subjects of type " + type.getTypeName());
        }

        persistentData.invalidate(identifier);
        transientData.invalidate(identifier);
        cache.synchronous().invalidate(identifier);
    }

    public CompletableFuture<CalculatedSubject> get(String identifier) {
        if (!getTypeInfo().isNameValid(identifier)) {
            throw new IllegalArgumentException("Provided name " + identifier + " is not valid for subjects of type " + type.getTypeName());
        }

        return cache.get(identifier);
    }

    public SubjectCache transientData() {
        return transientData;
    }

    public SubjectCache persistentData() {
        return persistentData;
    }

    PermissionsEx getManager() {
        return pex;
    }

    public void update(DataStore newDataStore) {
        this.persistentData.update(newDataStore);
    }

    public void load(String identifier) throws ExecutionException {
        this.persistentData.load(identifier);
        this.transientData.load(identifier);
    }

    public CompletableFuture<Boolean> isRegistered(String identifier) {
        return this.persistentData.isRegistered(identifier);
    }

    public Set<String> getAllIdentifiers() {
        Set<String> ret = new HashSet<>();
        ret.addAll(this.persistentData.getAllIdentifiers());
        ret.addAll(this.transientData.getAllIdentifiers());
        return ret;
    }
}
