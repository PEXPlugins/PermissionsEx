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

package ca.stellardrift.permissionsex.util

import ca.stellardrift.permissionsex.context.ContextDefinition
import com.google.common.reflect.TypeToken
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.objectmapping.ObjectMappingException
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer

class IpSet private constructor(private val addr: InetAddress, private val prefixLen: Int) {

    operator fun contains(input: InetAddress): Boolean {
        val address: ByteArray = input.address
        val checkAddr: ByteArray = addr.address
        if (address.size != checkAddr.size) {
            return false
        }
        val completeSegments = (prefixLen shr 3).toByte()
        val overlap = (prefixLen and 7).toByte()
        for (i in 0 until completeSegments) {
            if (address[i] != checkAddr[i]) {
                return false
            }
        }
        for (i in 0 until overlap) {
            if (checkAddr[completeSegments + 1].toInt().shr(7 - i) and 0x1 != address[completeSegments + 1].toInt().shr(7 - i) and 0x1) {
                return false
            }
        }
        return true
    }

    operator fun contains(other: IpSet): Boolean = when {
        other.prefixLen < this.prefixLen -> false
        other.addr == this.addr -> true
        else -> other.addr in this
    }

    override fun toString(): String {
        return addr.hostAddress + "/" + prefixLen
    }

    object IpSetSerializer : TypeSerializer<IpSet> {
        @Throws(
            ObjectMappingException::class
        )
        override fun deserialize(
            type: TypeToken<*>,
            value: ConfigurationNode
        ): IpSet {
            try {
                return fromCidr(value.string!!)
            } catch (e: IllegalArgumentException) {
                throw ObjectMappingException(e)
            }
        }

        @Throws(ObjectMappingException::class)
        override fun serialize(
            type: TypeToken<*>,
            obj: IpSet?,
            value: ConfigurationNode
        ) {
            value.value = obj.toString()
        }
    }

    companion object {
        @Throws(IllegalArgumentException::class)
        fun fromAddrPrefix(address: InetAddress, prefixLen: Int): IpSet {
            address.validatePrefixLength(prefixLen)
            return IpSet(address, prefixLen)
        }

        /**
         * Given a
         * @param spec
         * @return
         */
        @Throws(IllegalArgumentException::class)
        fun fromCidr(spec: String): IpSet {
            val addrString: String?
            val prefixLen: Int
            val slashIndex = spec.lastIndexOf("/")
            if (slashIndex == -1) {
                addrString = spec
                prefixLen = if (addrString.startsWith("[")) 128 else 32
            } else {
                addrString = spec.substring(0, slashIndex)
                prefixLen = Integer.parseInt(spec.substring(slashIndex + 1))
            }
            val addr: InetAddress
            try {
                addr = InetAddress.getByName(addrString)
            } catch (e: UnknownHostException) {
                throw IllegalArgumentException("$addrString does not contain a valid IP address")
            }
            return fromAddrPrefix(addr, prefixLen)
        }
    }
}

@Throws(IllegalArgumentException::class)
private fun InetAddress.validatePrefixLength(prefixLen: Int) {
    require(prefixLen >= 0) { "Minimum prefix length for an IP address is 0!" }
    val maxLen = maxPrefixLength
    require(prefixLen <= maxLen) { "Maximum prefix length for a " + this.javaClass.simpleName + " is " + maxLen }
}

/**
 * The maximum length a CIDR prefix applied to this IP address can be (aka the address' length in bits)
 */
val InetAddress.maxPrefixLength: Int get() =
    when (this) {
        is Inet4Address -> 32
        is Inet6Address -> 128
        else -> throw IllegalArgumentException("Unknown IP address type ${this::class}")
    }

abstract class IpSetContextDefinition(name: String) : ContextDefinition<IpSet>(name) {
    override fun serialize(userValue: IpSet): String = userValue.toString()
    override fun deserialize(canonicalValue: String): IpSet? {
        return try {
            IpSet.fromCidr(canonicalValue)
        } catch (ex: IllegalArgumentException) {
            null
        }
    }
    override fun matches(ownVal: IpSet, testVal: IpSet): Boolean = testVal in ownVal
}
