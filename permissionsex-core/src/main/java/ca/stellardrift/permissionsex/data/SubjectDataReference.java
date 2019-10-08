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

import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An automatically updating reference to the latest data for a certain subject.
 */
public class SubjectDataReference implements Consumer<ImmutableSubjectData> {
    private final String identifier;
    private final SubjectCache cache;
    private final Set<Consumer<ImmutableSubjectData>> updateListeners;
    final AtomicReference<ImmutableSubjectData> data = new AtomicReference<>();
    private final boolean strongListeners;

    /**
     * Create a new reference to subject data
     * Instances are accessible through a {@link SubjectCache} instance
     *
     * @param identifier The subject's identifier
     * @param cache The cache to get data from and listen for changes from
     * @param strongListeners Whether or not to hold strong references to listeners registered
     */
    SubjectDataReference(String identifier, SubjectCache cache, boolean strongListeners) {
        this.identifier = identifier;
        this.cache = cache;
        this.strongListeners = strongListeners;
        if (strongListeners) {
            updateListeners = Collections.newSetFromMap(new ConcurrentHashMap<>());
        } else {
            updateListeners = Collections.newSetFromMap(new MapMaker().weakKeys().concurrencyLevel(10).makeMap());
        }
    }

    /**
     * Get the current subject data
     *
     * @return The current data
     */
    public ImmutableSubjectData get() {
        return this.data.get();
    }

    /**
     * Update the contained data based on the result of a function
     *
     * @param modifierFunc The function that will be called to update the data
     * @return A future completing when data updates have been written to the data store
     */
    public CompletableFuture<Change<ImmutableSubjectData>> update(Function<ImmutableSubjectData, ImmutableSubjectData> modifierFunc) {
        ImmutableSubjectData data, newData;
        do {
            data = get();

            newData = modifierFunc.apply(data);
            if (newData == data) {
                return CompletableFuture.completedFuture(new Change<>(data, newData));
            }
        } while (!this.data.compareAndSet(data, newData));
        final ImmutableSubjectData finalData = data;
        return this.cache.set(this.identifier, newData).thenApply(finalNew -> new Change<>(finalData, finalNew));
    }

    @Override
    public void accept(ImmutableSubjectData newData) {
        synchronized (data) {
            this.data.set(newData);
            this.updateListeners.forEach(cb -> cb.accept(newData));
        }
    }

    /**
     * Get whether or not this reference will hold strong references to stored listeners.
     * If the return value  is false, registering a listener object with this reference will
     * not prevent it from being garbage collected, so the listener must be held somewhere
     * else for it to continue being called.
     * @return Whether or not listeners are held strongly.
     */
    public boolean holdsListenersStrongly() {
        return this.strongListeners;
    }

    /**
     * Register a listener to be called when an update is performed
     * @param listener The listener to register
     */
    public void onUpdate(Consumer<ImmutableSubjectData> listener) {
        updateListeners.add(listener);
    }

    /**
     * Get the cache this subject is held in
     * @return The cache holding this data
     */
    public SubjectCache getCache() {
        return cache;
    }

    /**
     * Get an identifier that can be used to refer to this subject
     * @return The subject's identifier
     */
    public Map.Entry<String, String> getIdentifier() {
        return Maps.immutableEntry(getCache().getType(), this.identifier);
    }
}
