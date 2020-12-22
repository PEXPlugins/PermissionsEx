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
package ca.stellardrift.permissionsex.bungee

import ca.stellardrift.permissionsex.context.ContextDefinition
import ca.stellardrift.permissionsex.context.SimpleContextDefinition
import ca.stellardrift.permissionsex.impl.context.IpSetContextDefinition
import ca.stellardrift.permissionsex.impl.util.IpSet
import ca.stellardrift.permissionsex.subject.CalculatedSubject
import java.net.InetSocketAddress
import java.util.function.Consumer
import net.md_5.bungee.api.connection.ProxiedPlayer

object RemoteIpContextDefinition : IpSetContextDefinition("remoteip") {

    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: Consumer<IpSet>) {
        (subject.associatedObject() as? ProxiedPlayer)?.apply {
            val address = socketAddress as? InetSocketAddress ?: return
            consumer.accept(IpSet.only(address.address))
        }
    }
}

object LocalHostContextDefinition : SimpleContextDefinition("localhost") {
    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: Consumer<String>) {
        (subject.associatedObject() as? ProxiedPlayer)?.apply {
            pendingConnection.virtualHost?.hostName?.apply(consumer::accept)
        }
    }
}

object LocalIpContextDefinition : IpSetContextDefinition("localip") {
    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: Consumer<IpSet>) {
        (subject.associatedObject() as? ProxiedPlayer)?.apply {
            pendingConnection.virtualHost?.address?.run {
                IpSet.only(this)
            }?.apply(consumer::accept)
        }
    }
}

object LocalPortContextDefiniiton : ContextDefinition<Int>("localport") {
    override fun serialize(userValue: Int): String = userValue.toString()
    override fun deserialize(userValue: String): Int = Integer.parseInt(userValue)
    override fun matches(ownVal: Int, testVal: Int): Boolean = ownVal == testVal

    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: Consumer<Int>) {
        (subject.associatedObject() as? ProxiedPlayer)?.apply {
            pendingConnection.virtualHost?.port?.apply(consumer::accept)
        }
    }
}
