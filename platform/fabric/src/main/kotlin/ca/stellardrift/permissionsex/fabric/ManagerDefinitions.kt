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
import ca.stellardrift.permissionsex.fabric.mixin.AccessorMinecraftServer
import ca.stellardrift.permissionsex.subject.CalculatedSubject
import ca.stellardrift.permissionsex.util.IpSet
import ca.stellardrift.permissionsex.util.IpSetContextDefinition
import ca.stellardrift.permissionsex.util.maxPrefixLength
import java.net.InetSocketAddress
import java.util.function.Consumer
import net.minecraft.entity.Entity
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier

const val SUBJECTS_SYSTEM = "system"
const val SUBJECTS_COMMAND_BLOCK = "commandblock"
const val IDENTIFIER_RCON = "rcon"

/**
 * An interface that can be implemented by context definitions that can draw from
 * a [ServerCommandSource] to get current context data.
 */
interface CommandSourceContextDefinition<T> {
    fun accumulateCurrentValues(source: ServerCommandSource, consumer: Consumer<T>)
}

abstract class IdentifierContextDefinition(name: String) : ContextDefinition<Identifier>(name) {
    override fun serialize(canonicalValue: Identifier): String {
        return canonicalValue.toString()
    }

    override fun deserialize(userValue: String): Identifier? {
        return Identifier.tryParse(userValue)
    }

    override fun matches(ownVal: Identifier, testVal: Identifier): Boolean {
        return ownVal == testVal
    }
}

object WorldContextDefinition : IdentifierContextDefinition("world"), CommandSourceContextDefinition<Identifier> {
    override fun accumulateCurrentValues(source: ServerCommandSource, consumer: Consumer<Identifier>) {
        val world = source.world
        if (world != null) {
            consumer.accept(world.registryKey.value)
        }
    }

    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: Consumer<Identifier>) {
        (subject.associatedObject as? ServerPlayerEntity)?.apply {
            consumer.accept(serverWorld.registryKey.value)
        }
    }

    override fun suggestValues(subject: CalculatedSubject): Set<Identifier> {
        return PermissionsExMod.server.worlds.map { it.registryKey.value }.toSet()
    }
}

object DimensionContextDefinition : IdentifierContextDefinition("dimension"), CommandSourceContextDefinition<Identifier> {
    override fun accumulateCurrentValues(source: ServerCommandSource, consumer: Consumer<Identifier>) {
        val dimension = source.world.registryManager.dimensionTypes.getId(source.world.dimension)
        if (dimension != null) {
            consumer.accept(dimension)
        }
    }

    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: Consumer<Identifier>) {
        (subject.associatedObject as?ServerPlayerEntity)?.apply {
            val key = world.registryManager.dimensionTypes.getId(world.dimension)
            if (key != null) {
                consumer.accept(key)
            }
        }
    }

    override fun suggestValues(subject: CalculatedSubject): Set<Identifier> {
        return (subject.associatedObject as? Entity)?.run {
            if (entityWorld is ServerWorld) {
                (entityWorld.server as? AccessorMinecraftServer)?.registryManager?.dimensionTypes?.ids
            } else {
                null
            }
        } ?: emptySet()
    }
}

object RemoteIpContextDefinition : IpSetContextDefinition("remoteip"), CommandSourceContextDefinition<IpSet> {
    override fun accumulateCurrentValues(source: ServerCommandSource, consumer: Consumer<IpSet>) {
        source.ifPlayer { (it.networkHandler.connection.address as? InetSocketAddress)
            ?.run { consumer.accept(IpSet.fromAddrPrefix(address, address.maxPrefixLength)) }
        }
    }

    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: Consumer<IpSet>) {
        ((subject.associatedObject as? ServerPlayerEntity)?.networkHandler?.connection?.address as? InetSocketAddress)?.run {
            consumer.accept(IpSet.fromAddrPrefix(address, address.maxPrefixLength))
        }
    }
}

object LocalIpContextDefinition : IpSetContextDefinition("localip"), CommandSourceContextDefinition<IpSet> {
    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: Consumer<IpSet>) {
        (subject.associatedObject as? ServerPlayerEntity)?.apply { accumulate(this, consumer) }
    }

    override fun accumulateCurrentValues(source: ServerCommandSource, consumer: Consumer<IpSet>) {
        source.ifPlayer { accumulate(it, consumer) }
    }

    private fun accumulate(ply: ServerPlayerEntity, consumer: Consumer<IpSet>) =
        consumer.accept((ply.networkHandler.connection as IVirtualHostHolder).virtualHost.address.run {
            IpSet.fromAddrPrefix(this, this.maxPrefixLength)
        })
}

object LocalHostContextDefinition : SimpleContextDefinition("localhost"), CommandSourceContextDefinition<String> {
    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: Consumer<String>) {
        (subject.associatedObject as? ServerPlayerEntity)?.apply { accumulate(this, consumer) }
    }

    override fun accumulateCurrentValues(source: ServerCommandSource, consumer: Consumer<String>) {
        source.ifPlayer { accumulate(it, consumer) }
    }

    private fun accumulate(ply: ServerPlayerEntity, consumer: Consumer<String>) =
        consumer.accept((ply.networkHandler.connection as IVirtualHostHolder).virtualHost.hostString)
}

object LocalPortContextDefinition : ContextDefinition<Int>("localport"), CommandSourceContextDefinition<Int> {
    override fun serialize(userValue: Int): String = userValue.toString()
    override fun deserialize(userValue: String): Int = userValue.toInt()
    override fun matches(ownVal: Int, testVal: Int): Boolean = ownVal == testVal

    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: Consumer<Int>) {
        (subject.associatedObject as? ServerPlayerEntity)?.apply { accumulate(this, consumer::accept) }
    }

    override fun accumulateCurrentValues(source: ServerCommandSource, consumer: Consumer<Int>) {
        source.ifPlayer { accumulate(it, consumer) }
    }

    private fun accumulate(ply: ServerPlayerEntity, consumer: Consumer<Int>) =
        consumer.accept((ply.networkHandler.connection as IVirtualHostHolder).virtualHost.port)
}
