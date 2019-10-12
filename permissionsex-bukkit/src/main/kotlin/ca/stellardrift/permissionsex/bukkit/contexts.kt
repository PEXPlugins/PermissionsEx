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

package ca.stellardrift.permissionsex.bukkit

import ca.stellardrift.permissionsex.PermissionsEx.GLOBAL_CONTEXT
import ca.stellardrift.permissionsex.context.ContextDefinition
import ca.stellardrift.permissionsex.context.EnumContextDefinition
import ca.stellardrift.permissionsex.context.SimpleContextDefinition
import ca.stellardrift.permissionsex.subject.CalculatedSubject
import ca.stellardrift.permissionsex.util.IpSet
import ca.stellardrift.permissionsex.util.IpSetContextDefinition
import ca.stellardrift.permissionsex.util.castMap
import ca.stellardrift.permissionsex.util.maxPrefixLength
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.Player

object WorldContextDefinition : ContextDefinition<String>("world") {
    override fun serialize(userValue: String): String = userValue

    override fun deserialize(canonicalValue: String): String = canonicalValue
    override fun matches(ownVal: String, testVal: String): Boolean = ownVal.equals(testVal, ignoreCase = true)

    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: (value: String) -> Unit) {
        subject.associatedObject.castMap<Player> { consumer(world.name) }
    }

    override fun suggestValues(subject: CalculatedSubject): Set<String> {
        return Bukkit.getWorlds().map(World::getName).toSet()
    }
}

object DimensionContextDefinition :
    EnumContextDefinition<World.Environment>("dimension", World.Environment::class.java) {
    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: (value: World.Environment) -> Unit) {
        subject.associatedObject.castMap<Player> { consumer(world.environment) }
    }
}

object RemoteIpContextDefinition : IpSetContextDefinition("remoteip") {

    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: (value: IpSet) -> Unit) {
        subject.associatedObject.castMap<Player> {
            address?.address?.apply {
                consumer(IpSet.fromAddrPrefix(this, this.maxPrefixLength))
            }
        }
    }

}

object LocalHostContextDefinition : SimpleContextDefinition("localhost") {
    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: (value: String) -> Unit) {
        subject.transientData().get().getOptions(GLOBAL_CONTEXT)["hostname"]?.apply(consumer)
    }
}

object LocalIpContextDefinition : IpSetContextDefinition("localip") {
    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: (value: IpSet) -> Unit) {
    }
}

object LocalPortContextDefiniiton : ContextDefinition<Int>("localport") {
    override fun serialize(userValue: Int): String = userValue.toString()
    override fun deserialize(canonicalValue: String): Int = Integer.parseInt(canonicalValue)
    override fun matches(ownVal: Int, testVal: Int): Boolean = ownVal == testVal

    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: (value: Int) -> Unit) {
        consumer(Bukkit.getPort())
    }
}