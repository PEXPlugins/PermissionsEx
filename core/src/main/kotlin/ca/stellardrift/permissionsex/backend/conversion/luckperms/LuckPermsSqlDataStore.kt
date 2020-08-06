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

package ca.stellardrift.permissionsex.backend.conversion.luckperms

import ca.stellardrift.permissionsex.backend.AbstractDataStore
import ca.stellardrift.permissionsex.backend.DataStore
import ca.stellardrift.permissionsex.data.ContextInheritance
import ca.stellardrift.permissionsex.data.ImmutableSubjectData
import ca.stellardrift.permissionsex.rank.RankLadder
import java.util.concurrent.CompletableFuture
import java.util.function.Function

class LuckPermsSqlDataStore(identifier: String) : AbstractDataStore<LuckPermsSqlDataStore>(identifier, FACTORY) {

    companion object {
        @JvmField
        val FACTORY = Factory("luckperms-sql", LuckPermsSqlDataStore::class.java, ::LuckPermsSqlDataStore)
    }

    override fun setDataInternal(type: String, identifier: String, data: ImmutableSubjectData?): CompletableFuture<ImmutableSubjectData> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun getContextInheritanceInternal(): CompletableFuture<ContextInheritance> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun close() {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun getAllIdentifiers(type: String): Set<String> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun getRegisteredTypes(): Set<String> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun getDefinedContextKeys(): CompletableFuture<Set<String>> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun getAll(): Iterable<Map.Entry<Map.Entry<String, String>, ImmutableSubjectData>> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun initializeInternal(): Boolean {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun getDataInternal(type: String, identifier: String): CompletableFuture<ImmutableSubjectData> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun setContextInheritanceInternal(contextInheritance: ContextInheritance): CompletableFuture<ContextInheritance> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Any?> performBulkOperationSync(function: Function<DataStore, T>): T {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun isRegistered(type: String, identifier: String): CompletableFuture<Boolean> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun getAllRankLadders(): Iterable<String> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun hasRankLadder(ladder: String): CompletableFuture<Boolean> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun getRankLadderInternal(ladder: String): CompletableFuture<RankLadder> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun setRankLadderInternal(ladder: String, newLadder: RankLadder?): CompletableFuture<RankLadder> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }
}
