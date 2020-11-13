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

package ca.stellardrift.permissionsex.data;

import ca.stellardrift.permissionsex.subject.ImmutableSubjectData;
import ca.stellardrift.permissionsex.subject.SubjectRef;
import ca.stellardrift.permissionsex.util.Change;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * An automatically updating reference to the latest data for a certain subject.
 *
 * @since 2.0.0
 */
public class SubjectDataReference implements Consumer<ImmutableSubjectData>, SubjectRef {
    private final String identifier;
    private final SubjectDataCacheImpl cache;
    private final Set<Consumer<ImmutableSubjectData>> updateListeners;
    final AtomicReference<ImmutableSubjectData> data = new AtomicReference<>();
    private final boolean strongListeners;

    /**
     * Create a new reference to subject data.
     *
     * <p>Instances are accessible through a {@link SubjectDataCacheImpl} instance.</p>
     *
     * @param identifier The subject's identifier
     * @param cache The cache to get data from and listen for changes from
     * @param strongListeners Whether or not to hold strong references to listeners registered
     */
    SubjectDataReference(String identifier, SubjectDataCacheImpl cache, boolean strongListeners) {
        this.identifier = identifier;
        this.cache = cache;
        this.strongListeners = strongListeners;
        if (strongListeners) {
            updateListeners = Collections.newSetFromMap(new ConcurrentHashMap<>());
        } else {
            updateListeners = Collections.newSetFromMap(new MapMaker().weakKeys().concurrencyLevel(10).makeMap());
        }
    }

    @Override
    public ImmutableSubjectData get() {
        return this.data.get();
    }

    @Override
    public CompletableFuture<Change<ImmutableSubjectData>> update(UnaryOperator<ImmutableSubjectData> modifierFunc) {
        ImmutableSubjectData data, newData;
        do {
            data = get();

            newData = modifierFunc.apply(data);
            if (newData == data) {
                return CompletableFuture.completedFuture(Change.of(data, newData));
            }
        } while (!this.data.compareAndSet(data, newData));
        final ImmutableSubjectData finalData = data;
        return this.cache.set(this.identifier, newData).thenApply(finalNew -> Change.of(finalData, finalNew));
    }

    @Override
    public void accept(ImmutableSubjectData newData) {
        synchronized (data) {
            this.data.set(newData);
            this.updateListeners.forEach(cb -> cb.accept(newData));
        }
    }

    @Override
    public boolean holdsListenersStrongly() {
        return this.strongListeners;
    }

    @Override
    public void onUpdate(Consumer<ImmutableSubjectData> listener) {
        updateListeners.add(listener);
    }

    /**
     * Get the cache this subject is held in.
     *
     * @return The cache holding this data
     */
    public SubjectDataCacheImpl getCache() {
        return cache;
    }

    @Override
    public Map.Entry<String, String> getIdentifier() {
        return Maps.immutableEntry(getCache().getType(), this.identifier);
    }

    @Override
    public CompletableFuture<Boolean> isRegistered() {
        return getCache().isRegistered(this.identifier);
    }

    @Override
    public CompletableFuture<ImmutableSubjectData> remove() {
        return getCache().remove(this.identifier);
    }
}
