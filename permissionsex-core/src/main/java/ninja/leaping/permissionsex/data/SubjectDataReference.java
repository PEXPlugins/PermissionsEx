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
package ninja.leaping.permissionsex.data;

import com.google.common.collect.MapMaker;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

public class SubjectDataReference implements Caching<ImmutableSubjectData> {
    private final SubjectRef identifier;
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
    SubjectDataReference(SubjectRef identifier, SubjectCache cache, boolean strongListeners) {
        this.identifier = identifier;
        this.cache = cache;
        this.strongListeners = strongListeners;
        if (strongListeners) {
            updateListeners = Collections.newSetFromMap(new ConcurrentHashMap<>());
        } else {
            updateListeners = Collections.newSetFromMap(new MapMaker().weakKeys().concurrencyLevel(10).makeMap());
        }
    }

    public ImmutableSubjectData get() {
        return this.data.get();
    }

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
        return this.cache.set(this.identifier.getIdentifier(), newData).thenApply(finalNew -> new Change<>(finalData, finalNew));
    }

    @Override
    public void clearCache(ImmutableSubjectData newData) {
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

    public SubjectCache getCache() {
        return cache;
    }

    public SubjectRef getIdentifier() {
        return this.identifier;
    }
}
