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

@file:JvmName("FabricDefinitions")
package ca.stellardrift.permissionsex.fabric

import ca.stellardrift.permissionsex.context.ContextDefinition
import ca.stellardrift.permissionsex.context.SimpleContextDefinition
import ca.stellardrift.permissionsex.subject.CalculatedSubject
import ca.stellardrift.permissionsex.subject.SubjectTypeDefinition
import ca.stellardrift.permissionsex.util.IpSet
import ca.stellardrift.permissionsex.util.IpSetContextDefinition
import ca.stellardrift.permissionsex.util.castMap
import ca.stellardrift.permissionsex.util.maxPrefixLength
import java.net.InetSocketAddress
import java.util.Optional
import java.util.UUID
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.world.dimension.DimensionType

const val SUBJECTS_SYSTEM = "system"
const val SUBJECTS_COMMAND_BLOCK = "commandblock"
const val IDENTIFIER_RCON = "rcon"

/**
 * An interface that can be implemented by context definitions that can draw from
 * a [ServerCommandSource] to get current context data.
 */
interface CommandSourceContextDefinition<T> {
    fun accumulateCurrentValues(source: ServerCommandSource, consumer: (value: T) -> Unit)
}

object WorldContextDefinition : SimpleContextDefinition("world"), CommandSourceContextDefinition<String> {
    override fun accumulateCurrentValues(source: ServerCommandSource, consumer: (value: String) -> Unit) {
        val world = source.world
        if (world != null) {
            consumer(world.levelProperties.levelName)
        }
    }

    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: (value: String) -> Unit) {
        subject.associatedObject.castMap<ServerPlayerEntity> {
            consumer(serverWorld.levelProperties.levelName)
        }
    }

    override fun suggestValues(subject: CalculatedSubject): Set<String> {
        return PermissionsExMod.server.worlds.map { it.levelProperties.levelName }.toSet()
    }
}

object DimensionContextDefinition : SimpleContextDefinition("dimension"), CommandSourceContextDefinition<String> {
    override fun accumulateCurrentValues(source: ServerCommandSource, consumer: (value: String) -> Unit) {
        val dimension = DimensionType.getId(source.world?.dimension?.type)?.path
        if (dimension != null) {
            consumer(dimension)
        }
    }

    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: (value: String) -> Unit) {
        subject.associatedObject.castMap<ServerPlayerEntity> {
            val key = DimensionType.getId(serverWorld.dimension.type)?.path
            if (key != null) {
                consumer(key)
            }
        }
    }

    override fun suggestValues(subject: CalculatedSubject): Set<String> {
        return DimensionType.getAll().mapNotNull { DimensionType.getId(it)?.path }.toSet()
    }
}

object RemoteIpContextDefinition : IpSetContextDefinition("remoteip"), CommandSourceContextDefinition<IpSet> {
    override fun accumulateCurrentValues(source: ServerCommandSource, consumer: (value: IpSet) -> Unit) {
        source.ifPlayer { (it.networkHandler.connection.address as? InetSocketAddress)
            ?.run { consumer(IpSet.fromAddrPrefix(address, address.maxPrefixLength)) }
        }
    }

    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: (value: IpSet) -> Unit) {
        subject.associatedObject.castMap<ServerPlayerEntity> { (networkHandler.connection.address as? InetSocketAddress)?.run {
            consumer(IpSet.fromAddrPrefix(address, address.maxPrefixLength))
        } }
    }
}

object LocalIpContextDefinition : IpSetContextDefinition("localip"), CommandSourceContextDefinition<IpSet> {
    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: (value: IpSet) -> Unit) =
        subject.associatedObject.castMap<ServerPlayerEntity> { accumulate(this, consumer) }

    override fun accumulateCurrentValues(source: ServerCommandSource, consumer: (value: IpSet) -> Unit) {
        source.ifPlayer { accumulate(it, consumer) }
    }

    private fun accumulate(ply: ServerPlayerEntity, consumer: (value: IpSet) -> Unit) =
        consumer((ply.networkHandler.connection as IVirtualHostHolder).virtualHost.address.run {
            IpSet.fromAddrPrefix(this, this.maxPrefixLength)
        })
}

object LocalHostContextDefinition : SimpleContextDefinition("localhost"), CommandSourceContextDefinition<String> {
    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: (value: String) -> Unit) =
        subject.associatedObject.castMap<ServerPlayerEntity> { accumulate(this, consumer) }
    override fun accumulateCurrentValues(source: ServerCommandSource, consumer: (value: String) -> Unit) {
        source.ifPlayer { accumulate(it, consumer) }
    }

    private fun accumulate(ply: ServerPlayerEntity, consumer: (value: String) -> Unit) =
        consumer((ply.networkHandler.connection as IVirtualHostHolder).virtualHost.hostString)
}

object LocalPortContextDefinition : ContextDefinition<Int>("localport"), CommandSourceContextDefinition<Int> {
    override fun serialize(userValue: Int): String = userValue.toString()
    override fun deserialize(canonicalValue: String): Int = canonicalValue.toInt()
    override fun matches(ownVal: Int, testVal: Int): Boolean = ownVal == testVal

    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: (value: Int) -> Unit) =
        subject.associatedObject.castMap<ServerPlayerEntity> { accumulate(this, consumer) }
    override fun accumulateCurrentValues(source: ServerCommandSource, consumer: (value: Int) -> Unit) {
        source.ifPlayer { accumulate(it, consumer) }
    }

    private fun accumulate(ply: ServerPlayerEntity, consumer: (value: Int) -> Unit) =
        consumer((ply.networkHandler.connection as IVirtualHostHolder).virtualHost.port)
}

class UserSubjectTypeDefinition : SubjectTypeDefinition<ServerPlayerEntity>("user") {
    override fun isNameValid(name: String): Boolean {
        return try {
            UUID.fromString(name)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    override fun getAliasForName(name: String): Optional<String> {
        return try {
            UUID.fromString(name)
            Optional.empty()
        } catch (e: IllegalArgumentException) {
            Optional.ofNullable(PermissionsExMod.server.userCache.findByName(name)?.id?.toString())
        }
    }

    override fun getAssociatedObject(identifier: String): Optional<ServerPlayerEntity> {
        return try {
            val uid = UUID.fromString(identifier)
            Optional.ofNullable(PermissionsExMod.server.playerManager.getPlayer(uid))
        } catch (e: IllegalArgumentException) {
            Optional.empty()
        }
    }
}
