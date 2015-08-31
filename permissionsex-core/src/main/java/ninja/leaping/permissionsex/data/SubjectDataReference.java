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

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

public class SubjectDataReference implements Caching<ImmutableSubjectData> {
    private final String identifier;
    private final SubjectCache cache;
    private final Caching<ImmutableSubjectData> childListener;
    private final AtomicReference<ImmutableSubjectData> data = new AtomicReference<>();

    public static SubjectDataReference forSubject(String identifier, SubjectCache holder) throws ExecutionException {
        return forSubject(identifier, holder, null);
    }
    public static SubjectDataReference forSubject(String identifier, SubjectCache holder, Caching<ImmutableSubjectData> listener) throws ExecutionException {
        final SubjectDataReference ref = new SubjectDataReference(identifier, holder, listener);
        ref.data.set(holder.getData(identifier, ref));
        return ref;
    }

    private SubjectDataReference(String identifier, SubjectCache cache, Caching<ImmutableSubjectData> childListener) {
        this.identifier = identifier;
        this.cache = cache;
        this.childListener = childListener;
    }

    public ImmutableSubjectData get() {
        return this.data.get();
    }

    public ListenableFuture<ImmutableSubjectData> set(ImmutableSubjectData newData) {
        return cache.set(this.identifier, newData);
    }

    public ListenableFuture<ImmutableSubjectData> update(Function<ImmutableSubjectData, ImmutableSubjectData> modifierFunc) {
        ImmutableSubjectData data, newData;
        do {
            data = get();

            newData = modifierFunc.apply(data);
            if (newData == data) {
                return Futures.immediateFuture(data);
            }
        } while (!this.data.compareAndSet(data, newData));
        return set(newData);
    }

    @Override
    public void clearCache(ImmutableSubjectData newData) {
        synchronized (data) {
            this.data.set(newData);
            if (this.childListener != null) {
                this.childListener.clearCache(newData);
            }
        }
    }
}
