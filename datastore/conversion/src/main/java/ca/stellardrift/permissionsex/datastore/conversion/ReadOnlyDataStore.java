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
package ca.stellardrift.permissionsex.datastore.conversion;

import ca.stellardrift.permissionsex.context.ContextInheritance;
import ca.stellardrift.permissionsex.datastore.DataStore;
import ca.stellardrift.permissionsex.datastore.StoreProperties;
import ca.stellardrift.permissionsex.impl.backend.AbstractDataStore;
import ca.stellardrift.permissionsex.impl.util.Util;
import ca.stellardrift.permissionsex.rank.RankLadder;
import ca.stellardrift.permissionsex.subject.ImmutableSubjectData;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * A specialization of AbstractDataStore that handles backends for a global data store
 */
public abstract class ReadOnlyDataStore<T extends ReadOnlyDataStore<T, C>, C> extends AbstractDataStore<T, C> {

    protected ReadOnlyDataStore(final StoreProperties<C> props) {
        super(props);
    }

    public CompletableFuture<ImmutableSubjectData> setDataInternal(
            final String type,
            final String identifier,
            final @Nullable ImmutableSubjectData data
    ) {
        return readOnly();
    }

    public CompletableFuture<RankLadder> setRankLadderInternal(final String ladder, final @Nullable RankLadder newLadder) {
        return readOnly();
    }

    @Override
    public CompletableFuture<ContextInheritance> setContextInheritanceInternal(final ContextInheritance inheritance) {
        return readOnly();
    }

    private <V> CompletableFuture<V> readOnly() {
        return Util.failedFuture(new UnsupportedOperationException("The " + this.name() + " backend is read-only"));
    }

    @Override
    public <V> V performBulkOperationSync(final Function<DataStore, V> function) {
        return function.apply(this);
    }
}
