/*
 * PermissionsEx - a permissions plugin for your server ecosystem
 * Copyright © 2020 zml [at] stellardrift [dot] ca and PermissionsEx contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ca.stellardrift.permissionsex.bukkit

import ca.stellardrift.permissionsex.PermissionsEx.GLOBAL_CONTEXT
import ca.stellardrift.permissionsex.context.ContextDefinition
import ca.stellardrift.permissionsex.context.EnumContextDefinition
import ca.stellardrift.permissionsex.context.SimpleContextDefinition
import ca.stellardrift.permissionsex.subject.CalculatedSubject
import ca.stellardrift.permissionsex.util.IpSet
import ca.stellardrift.permissionsex.util.IpSetContextDefinition
import ca.stellardrift.permissionsex.util.maxPrefixLength
import java.util.function.Consumer
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.Player

object WorldContextDefinition : ContextDefinition<String>("world") {
    override fun serialize(canonicalValue: String): String = canonicalValue

    override fun deserialize(userValue: String): String = userValue
    override fun matches(ownVal: String, testVal: String): Boolean = ownVal.equals(testVal, ignoreCase = true)

    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: Consumer<String>) {
        (subject.associatedObject as? Player)?.also { consumer.accept(it.world.name) }
    }

    override fun suggestValues(subject: CalculatedSubject): Set<String> {
        return Bukkit.getWorlds().map(World::getName).toSet()
    }
}

object DimensionContextDefinition :
    EnumContextDefinition<World.Environment>("dimension", World.Environment::class.java) {
    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: Consumer<World.Environment>) {
        (subject.associatedObject as? Player)?.also { consumer.accept(it.world.environment) }
    }
}

object RemoteIpContextDefinition : IpSetContextDefinition("remoteip") {

    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: Consumer<IpSet>) {
        (subject.associatedObject as? Player)?.run {
            address?.address?.apply {
                consumer.accept(IpSet.fromAddrPrefix(this, this.maxPrefixLength))
            }
        }
    }
}

object LocalHostContextDefinition : SimpleContextDefinition("localhost") {
    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: Consumer<String>) {
        subject.transientData().get().getOptions(GLOBAL_CONTEXT)["hostname"]?.apply(consumer::accept)
    }
}

object LocalIpContextDefinition : IpSetContextDefinition("localip") {
    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: Consumer<IpSet>) {
        // TODO: Implement local IP setting
    }
}

object LocalPortContextDefinition : ContextDefinition<Int>("localport") {
    override fun serialize(userValue: Int): String = userValue.toString()
    override fun deserialize(userValue: String): Int = Integer.parseInt(userValue)
    override fun matches(ownVal: Int, testVal: Int): Boolean = ownVal == testVal

    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: Consumer<Int>) {
        consumer.accept(Bukkit.getPort())
    }
}
