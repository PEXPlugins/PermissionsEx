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

package ca.stellardrift.permissionsex.backend.conversion

import ca.stellardrift.permissionsex.backend.AbstractDataStore
import ca.stellardrift.permissionsex.backend.DataStore
import ca.stellardrift.permissionsex.context.ContextValue
import ca.stellardrift.permissionsex.data.ContextInheritance
import ca.stellardrift.permissionsex.data.ImmutableSubjectData
import ca.stellardrift.permissionsex.rank.RankLadder
import ca.stellardrift.permissionsex.util.Util
import java.util.concurrent.CompletableFuture
import java.util.function.Function

abstract class ReadOnlySubjectData : ImmutableSubjectData {
    override fun setOption(contexts: Set<ContextValue<*>>, key: String, value: String): ImmutableSubjectData {
        return this
    }

    override fun setOptions(contexts: Set<ContextValue<*>>, values: Map<String, String>): ImmutableSubjectData {
        return this
    }

    override fun clearOptions(contexts: Set<ContextValue<*>>): ImmutableSubjectData {
        return this
    }

    override fun clearOptions(): ImmutableSubjectData {
        return this
    }

    override fun setPermission(contexts: Set<ContextValue<*>>, permission: String, value: Int): ImmutableSubjectData {
        return this
    }

    override fun setPermissions(contexts: Set<ContextValue<*>>, values: Map<String, Int>): ImmutableSubjectData {
        return this
    }

    override fun clearPermissions(): ImmutableSubjectData {
        return this
    }

    override fun clearPermissions(contexts: Set<ContextValue<*>>): ImmutableSubjectData {
        return this
    }

    override fun addParent(contexts: Set<ContextValue<*>>, type: String, identifier: String): ImmutableSubjectData {
        return this
    }

    override fun removeParent(contexts: Set<ContextValue<*>>, type: String, identifier: String): ImmutableSubjectData {
        return this
    }

    override fun setParents(
        contexts: Set<ContextValue<*>>,
        parents: List<Map.Entry<String, String>>
    ): ImmutableSubjectData {
        return this
    }

    override fun clearParents(): ImmutableSubjectData {
        return this
    }

    override fun clearParents(contexts: Set<ContextValue<*>>): ImmutableSubjectData {
        return this
    }

    override fun setDefaultValue(contexts: Set<ContextValue<*>>, defaultValue: Int): ImmutableSubjectData {
        return this
    }
}

/**
 * A specialization of AbstractDataStore that handles backends for a global data store
 */
abstract class ReadOnlyDataStore<T : ReadOnlyDataStore<T>> protected constructor(identifier: String, factory: Factory<T>) : AbstractDataStore<T>(identifier, factory) {

    override fun setDataInternal(
        type: String,
        identifier: String,
        data: ImmutableSubjectData
    ): CompletableFuture<ImmutableSubjectData> {
        return Util.failedFuture(UnsupportedOperationException("The ${this::class.java.simpleName} backend is-read-only!"))
    }

    override fun setRankLadderInternal(ladder: String, newLadder: RankLadder): CompletableFuture<RankLadder> {
        return Util.failedFuture(UnsupportedOperationException("The ${this::class.java.simpleName} backend is-read-only!"))
    }

    override fun setContextInheritanceInternal(contextInheritance: ContextInheritance): CompletableFuture<ContextInheritance> {
        return Util.failedFuture(UnsupportedOperationException("The ${this::class.java.simpleName} backend is-read-only!"))
    }

    @Throws(Exception::class)
    override fun <T> performBulkOperationSync(function: Function<DataStore, T>): T {
        return function.apply(this)
    }
}
