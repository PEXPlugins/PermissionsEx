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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.function.Function

class LuckPermsSqlDataStore : AbstractDataStore(FACTORY) {

    companion object {
        @JvmField
        val FACTORY = Factory("luckperms-sql", LuckPermsSqlDataStore::class.java)
    }

    override fun setDataInternal(type: String, identifier: String, data: ImmutableSubjectData?): Mono<ImmutableSubjectData> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getContextInheritanceInternal(): Mono<ContextInheritance> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun close() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAllIdentifiers(type: String): Flux<String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getRegisteredTypes(): Flux<String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDefinedContextKeys(): Flux<String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAll(): Flux<Pair<MutableMap.MutableEntry<String, String>, ImmutableSubjectData>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun initializeInternal() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDataInternal(type: String, identifier: String): Mono<ImmutableSubjectData> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setContextInheritanceInternal(contextInheritance: ContextInheritance): Mono<ContextInheritance> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Any?> performBulkOperationSync(function: Function<DataStore, T>): T {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isRegistered(type: String, identifier: String): Mono<Boolean> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAllRankLadders(): Flux<String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hasRankLadder(ladder: String): Mono<Boolean> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getRankLadderInternal(ladder: String): Mono<RankLadder> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setRankLadderInternal(ladder: String, newLadder: RankLadder?): Mono<RankLadder> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

