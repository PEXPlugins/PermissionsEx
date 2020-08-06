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

package ca.stellardrift.permissionsex.velocity

import ca.stellardrift.permissionsex.context.ContextDefinition
import ca.stellardrift.permissionsex.context.SimpleContextDefinition
import ca.stellardrift.permissionsex.subject.CalculatedSubject
import ca.stellardrift.permissionsex.util.IpSet
import ca.stellardrift.permissionsex.util.IpSetContextDefinition
import ca.stellardrift.permissionsex.util.castMap
import ca.stellardrift.permissionsex.util.maxPrefixLength
import com.velocitypowered.api.proxy.Player

object RemoteIpContextDefinition : IpSetContextDefinition("remoteip") {
    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: (value: IpSet) -> Unit) {
        subject.associatedObject.castMap<Player> {
            consumer(IpSet.fromAddrPrefix(this.remoteAddress.address, this.remoteAddress.address.maxPrefixLength))
        }
    }
}

object LocalIpContextDefinition : IpSetContextDefinition("localip") {
    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: (value: IpSet) -> Unit) {
        subject.associatedObject.castMap<Player> {
            virtualHost.ifPresent {
                if (!it.isUnresolved) {
                    consumer(IpSet.fromAddrPrefix(it.address, it.address.maxPrefixLength))
                }
            }
        }
    }
}

object LocalHostContextDefinition : SimpleContextDefinition("localhost") {
    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: (value: String) -> Unit) {
        subject.associatedObject.castMap<Player> {
            virtualHost.ifPresent {
                consumer(it.hostString)
            }
        }
    }
}

object LocalPortContextDefinition : ContextDefinition<Int>("localport") {
    override fun serialize(userValue: Int): String = userValue.toString()
    override fun deserialize(canonicalValue: String): Int = canonicalValue.toInt()
    override fun matches(ownVal: Int, testVal: Int): Boolean = ownVal == testVal
    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: (value: Int) -> Unit) =
        subject.associatedObject.castMap<Player> {
            virtualHost.ifPresent { consumer(it.port) }
        }
}
