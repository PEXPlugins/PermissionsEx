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
package ca.stellardrift.permissionsex.impl.util;

import ca.stellardrift.permissionsex.impl.util.IpSet;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.*;

public class IpSetTest {
    private static final InetAddress TEST_ADDRESS;

    static {
        try {
            TEST_ADDRESS = InetAddress.getByAddress(new byte[] {84, 127, 48, 22});
        } catch (UnknownHostException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static InetAddress createAddress(final String addr) {
        try {
            return InetAddress.getByName(addr);
        } catch (final UnknownHostException ex) {
            fail(ex);
            throw new IllegalStateException(ex);
        }
    }

    @Test
    void testContainsSelf() {
        assertTrue(IpSet.fromAddrPrefix(TEST_ADDRESS, 32).contains(TEST_ADDRESS));
        assertTrue(IpSet.fromAddrPrefix(TEST_ADDRESS, 16).contains(TEST_ADDRESS));
    }

    @Test
    void testIPv4Addresses() {
        final IpSet subject = IpSet.fromCidr("10.42.0.0/16");
        assertTrue(subject.contains(createAddress("10.42.2.5")));
        assertFalse(subject.contains(createAddress("10.43.2.5")));
    }

    @Test
    void testIPv6Addresses() {
        final IpSet subject = IpSet.fromCidr("[fc00::]/8");
        assertTrue(subject.contains(createAddress("fcc0:c0b2:2a14:7afc:5216:1854:1a2f:2c13")));
        final IpSet emptyPrefix = IpSet.fromCidr("::/0");
        assertTrue(emptyPrefix.contains(createAddress("::dead:beef")));
    }

    @Test
    void testNonByteAlignedPrefixLengths() {
        final IpSet subject = IpSet.fromCidr("[2064:45:300::]/40");
        assertTrue(subject.contains(createAddress("2064:45:310::cafe")));
        assertFalse(subject.contains(createAddress("2064:45:410::cafe")));
    }

    @Test
    void testContainsOtherIpSet() {
        final InetAddress addr1 = createAddress("84.184.0.0");
        final InetAddress addr2 = createAddress("127.0.0.1");
        final InetAddress addr3 = createAddress("84.184.44.20");

        final IpSet addr116Set = IpSet.fromAddrPrefix(addr1, 16);

        assertTrue(addr116Set.contains(addr116Set));
        assertTrue(IpSet.fromAddrPrefix(addr3, 32).contains(addr3));

        assertFalse(addr116Set.contains(IpSet.fromAddrPrefix(addr2, 8)));
        assertFalse(addr116Set.contains(IpSet.fromAddrPrefix(addr2, 16)));
        assertFalse(addr116Set.contains(IpSet.fromAddrPrefix(addr2, 32)));

        assertTrue(addr116Set.contains(addr3));
        assertTrue(addr116Set.contains(IpSet.fromAddrPrefix(addr3, 16)));
        assertTrue(addr116Set.contains(IpSet.fromAddrPrefix(addr3, 32)));

        assertFalse(addr116Set.contains(IpSet.fromAddrPrefix(addr3, 8)));
        assertFalse(IpSet.fromAddrPrefix(addr3, 32).contains(IpSet.fromAddrPrefix(addr3, 16)));
    }

}
