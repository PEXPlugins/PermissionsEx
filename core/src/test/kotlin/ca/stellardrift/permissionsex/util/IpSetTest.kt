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

import java.net.InetAddress
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private val testAddress = InetAddress.getByAddress(byteArrayOf(84, 127, 48, 22))
private fun createAddress(addr: String): InetAddress {
    return InetAddress.getByName(addr)
}
class IpSetTest {

    @Test
    fun `should contain itself`() {
        assertTrue(testAddress in IpSet.fromAddrPrefix(testAddress, 32))
        assertTrue(testAddress in IpSet.fromAddrPrefix(testAddress, 16))
    }

    @Test
    fun `should handle IPv4 addresses`() {
        val subject = IpSet.fromCidr("10.42.0.0/16")
        assertTrue(createAddress("10.42.2.5") in subject)
        assertFalse(createAddress("10.43.2.5") in subject)
    }

    @Test
    fun `test IPv6 addresses`() {
        val subject = IpSet.fromCidr("[fc00::]/8")
        assertTrue(createAddress("fcc0:c0b2:2a14:7afc:5216:1854:1a2f:2c13") in subject)
        val emptyPrefix = IpSet.fromCidr("::/0")
        assertTrue(createAddress("::dead:beef") in emptyPrefix)
    }

    @Test
    fun `test non byte-aligned prefix lengths`() {
        val subject = IpSet.fromCidr("[2064:45:300::]/40")
        assertTrue(createAddress("2064:45:310::cafe") in subject)
        assertFalse(createAddress("2064:45:410::cafe") in subject)
    }

    @Test
    fun `test contains for another IpSet`() {
        val addr1 = createAddress("84.184.0.0")
        val addr2 = createAddress("127.0.0.1")
        val addr3 = createAddress("84.184.44.20")

        val addr116Set = IpSet.fromAddrPrefix(addr1, 16)

        assertTrue(addr116Set in addr116Set)
        assertTrue(addr3 in IpSet.fromAddrPrefix(addr3, 32))

        assertFalse(IpSet.fromAddrPrefix(addr2, 8) in addr116Set)
        assertFalse(IpSet.fromAddrPrefix(addr2, 16) in addr116Set)
        assertFalse(IpSet.fromAddrPrefix(addr2, 32) in addr116Set)

        assertTrue(addr3 in addr116Set)
        assertTrue(IpSet.fromAddrPrefix(addr3, 16) in addr116Set)
        assertTrue(IpSet.fromAddrPrefix(addr3, 32) in addr116Set)

        assertFalse(IpSet.fromAddrPrefix(addr3, 8) in addr116Set)
        assertFalse(IpSet.fromAddrPrefix(addr3, 16) in IpSet.fromAddrPrefix(addr3, 32))
    }
}
