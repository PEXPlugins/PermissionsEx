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
package ca.stellardrift.permissionsex.util;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

public final class IpSet implements Predicate<InetAddress> {
    public static final TypeSerializer<IpSet> SERIALIZER = new IpSetSerializer();
    private final InetAddress address;
    private final int prefixLength;

    /**
     * Create an IP set matching only one single address.
     *
     * @param address the address to match
     * @return a new ip set
     */
    public static IpSet only(final InetAddress address) {
        requireNonNull(address, "address");
        return new IpSet(address, maxPrefixLength(address));
    }

    /**
     * Resolve an ip set from address and prefix length
     * @param address the IP address
     * @param prefixLen the prefix length
     * @return a new ip set
     * @throws IllegalArgumentException if the prefix length is not valid for the provided address
     */
    public static IpSet fromAddrPrefix(final InetAddress address, final int prefixLen) {
        requireNonNull(address, "address");
        validatePrefixLength(address, prefixLen);
        return new IpSet(address, prefixLen);
    }

    /**
     * Given a CIDR range, construct an IP set.
     * @param spec the specification
     * @return a parsed ip set matching the range
     * @throws IllegalArgumentException if the syntax is not valid for CIDR, or the prefix length is invalid.
     */
    public static IpSet fromCidr(final String spec) {
        requireNonNull(spec, "spec");

        final @Nullable String addrString;
        final int prefixLen;
        final int slashIndex = spec.lastIndexOf("/");
        if (slashIndex == -1) {
            addrString = spec;
            prefixLen = addrString.startsWith("[") ? 128 : 32; // are we IPv6?
        } else {
            addrString = spec.substring(0, slashIndex);
            prefixLen = Integer.parseInt(spec.substring(slashIndex + 1));
        }
        final InetAddress addr;
        try {
            addr = InetAddress.getByName(addrString);
        } catch (final UnknownHostException ex) {
            throw new IllegalArgumentException(addrString + "does not contain a valid IP address");
        }
        return fromAddrPrefix(addr, prefixLen);
    }

    IpSet(final InetAddress address, final int prefixLength) {
        this.address = address;
        this.prefixLength = prefixLength;
    }

    // State validation

    static int maxPrefixLength(final InetAddress address) {
        if (address instanceof Inet4Address) {
            return 32;
        } else if (address instanceof Inet6Address) {
            return 128;
        } else {
            throw new IllegalArgumentException("Unknown IP address type " + address.getClass());
        }
    }

    static void validatePrefixLength(final InetAddress address, final int prefixLength) {
        if (prefixLength < 0) {
            throw new IllegalArgumentException("Minimum prefix length for an IP address is 0, but "
                    + prefixLength + " was provided.");
        }
        final int maxLength = maxPrefixLength(address);
        if (prefixLength > maxLength) {
            throw new IllegalArgumentException("Maximum prefix length for a "
                    + address.getClass().getSimpleName() + " is " + maxLength + ", but "
                    + prefixLength + " was provided");
        }
    }

    @Override
    public boolean test(final InetAddress address) {
        return this.contains(address);
    }

    public boolean contains(final InetAddress input) {
        final byte[] address = input.getAddress();
        final byte[] checkAddr = this.address.getAddress();
        if (address.length != checkAddr.length) {
            return false;
        }
        final byte completeSegments = (byte) (this.prefixLength >> 3);
        final byte overlap = (byte) (this.prefixLength & 7);
        for (int i = 0; i < completeSegments; ++i) {
            if (address[i] != checkAddr[i]) {
                return false;
            }
        }

        for (int i = 0; i < overlap; ++i) {
            if (((checkAddr[completeSegments + 1] >> (7 - i)) & 0x1)
                    != ((address[completeSegments + 1] >> (7 - i)) & 0x1)) {
                return false;
            }
        }
        return true;
    }

    public boolean contains(final IpSet other) {
        if (other.prefixLength < this.prefixLength) {
            return false;
        }

        return other.address.equals(this.address)
                || this.contains(other.address);
    }

    /**
     * Get the CIDR string representation of this IP set.
     *
     * @return the representation
     */
    @Override
    public String toString() {
        return this.address.getHostAddress() + "/" + this.prefixLength;
    }

    static class IpSetSerializer implements TypeSerializer<IpSet> {

        IpSetSerializer() {
        }

        @Override
        public IpSet deserialize(final Type type, final ConfigurationNode node) throws SerializationException {
            try {
                return IpSet.fromCidr(node.getString());
            } catch (final IllegalArgumentException ex) {
                throw new SerializationException(ex);
            }
        }

        @Override
        public void serialize(final Type type, final @Nullable IpSet obj, final ConfigurationNode node) throws SerializationException {
            if (obj == null) {
                node.raw(null);
                return;
            }

            node.set(obj.toString()); // to cidr
        }
    }

}
