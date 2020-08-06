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

import ca.stellardrift.permissionsex.PermissionsEx.SUBJECTS_USER
import ca.stellardrift.permissionsex.context.ContextDefinition
import ca.stellardrift.permissionsex.context.SimpleContextDefinition
import ca.stellardrift.permissionsex.subject.CalculatedSubject
import ca.stellardrift.permissionsex.subject.SubjectTypeDefinition
import ca.stellardrift.permissionsex.util.IpSet
import ca.stellardrift.permissionsex.util.IpSetContextDefinition
import ca.stellardrift.permissionsex.util.castMap
import ca.stellardrift.permissionsex.util.maxPrefixLength
import java.util.Optional
import java.util.UUID
import net.md_5.bungee.api.connection.ProxiedPlayer

class UserSubjectTypeDefinition(private val plugin: PermissionsExPlugin) : SubjectTypeDefinition<ProxiedPlayer>(SUBJECTS_USER) {
    override fun isNameValid(name: String): Boolean {
        return try {
            UUID.fromString(name)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun getAliasForName(name: String): Optional<String> {
        return try {
            UUID.fromString(name)
            Optional.empty()
        } catch (e: Exception) {
            Optional.ofNullable(plugin.proxy.getPlayer(name)?.uniqueId?.toString())
        }
    }

    override fun getAssociatedObject(identifier: String): Optional<ProxiedPlayer> {
        return try {
            val id = UUID.fromString(identifier)
            Optional.ofNullable(plugin.proxy.getPlayer(id))
        } catch (e: IllegalArgumentException) {
            Optional.empty()
        }
    }
}

object RemoteIpContextDefinition : IpSetContextDefinition("remoteip") {

    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: (value: IpSet) -> Unit) {
        subject.associatedObject.castMap<ProxiedPlayer> {
            consumer(IpSet.fromAddrPrefix(address.address, address.address.maxPrefixLength))
        }
    }
}

object LocalHostContextDefinition : SimpleContextDefinition("localhost") {
    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: (value: String) -> Unit) {
        subject.associatedObject.castMap<ProxiedPlayer> {
            pendingConnection.virtualHost?.hostName?.apply(consumer)
        }
    }
}

object LocalIpContextDefinition : IpSetContextDefinition("localip") {
    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: (value: IpSet) -> Unit) {
        subject.associatedObject.castMap<ProxiedPlayer> {
            pendingConnection.virtualHost?.address?.run {
                IpSet.fromAddrPrefix(this, this.maxPrefixLength)
            }?.apply(consumer)
        }
    }
}

object LocalPortContextDefiniiton : ContextDefinition<Int>("localport") {
    override fun serialize(userValue: Int): String = userValue.toString()
    override fun deserialize(canonicalValue: String): Int = Integer.parseInt(canonicalValue)
    override fun matches(ownVal: Int, testVal: Int): Boolean = ownVal == testVal

    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: (value: Int) -> Unit) {
        subject.associatedObject.castMap<ProxiedPlayer> {
            pendingConnection.virtualHost?.port?.apply(consumer)
        }
    }
}
