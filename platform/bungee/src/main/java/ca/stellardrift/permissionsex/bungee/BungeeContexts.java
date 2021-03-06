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
package ca.stellardrift.permissionsex.bungee;

import ca.stellardrift.permissionsex.context.ContextDefinition;
import ca.stellardrift.permissionsex.context.SimpleContextDefinition;
import ca.stellardrift.permissionsex.impl.context.IpSetContextDefinition;
import ca.stellardrift.permissionsex.impl.util.IpSet;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Context definitions available on the Bungee proxy.
 *
 * @since 2.0.0
 */
public final class BungeeContexts {
    /**
     * A context providing a value of the current IP a player is connecting from.
     *
     * @return the {@code remoteip} context definition
     * @since 2.0.0
     */
    public static ContextDefinition<IpSet> remoteIp() {
        return RemoteIp.INSTANCE;
    }

    /**
     * A context providing a value of the current IP a player is connecting to.
     *
     * @return the {@code localip} context definition
     * @since 2.0.0
     */
    public static ContextDefinition<IpSet> localIp() {
        return LocalIp.INSTANCE;
    }

    /**
     * A context providing a value of the current host name a player is connecting to.
     *
     * <p>This takes into account virtual hosts.</p>
     *
     * @return the {@code localhost} context definition
     * @since 2.0.0
     */
    public static ContextDefinition<String> localHost() {
        return LocalHost.INSTANCE;
    }


    /**
     * A context providing a value of the current host port a player is connecting to.
     *
     * <p>This takes into account virtual hosts.</p>
     *
     * @return the {@code localport} context definition
     * @since 2.0.0
     */
    public static ContextDefinition<Integer> localPort() {
        return LocalPort.INSTANCE;
    }

    static final class RemoteIp extends IpSetContextDefinition {
        private static final ContextDefinition<IpSet> INSTANCE = new RemoteIp();

        private RemoteIp() {
            super("remoteip");
        }

        @Override
        public void accumulateCurrentValues(final CalculatedSubject subject, final Consumer<IpSet> consumer) {
            final @Nullable Object associated = subject.associatedObject();
            if (associated instanceof ProxiedPlayer) {
                final SocketAddress address = ((ProxiedPlayer) associated).getSocketAddress();
                if (address instanceof InetSocketAddress) {
                    consumer.accept(IpSet.only(((InetSocketAddress) address).getAddress()));
                }
            }
        }
    }

    static final class LocalIp extends IpSetContextDefinition {
        private static final ContextDefinition<IpSet> INSTANCE = new LocalIp();

        private LocalIp() {
            super("localip");
        }

        @Override
        public void accumulateCurrentValues(final CalculatedSubject subject, final Consumer<IpSet> consumer) {
            final @Nullable Object associated = subject.associatedObject();
            if (associated instanceof ProxiedPlayer) {
                final @Nullable InetSocketAddress localAddress = ((ProxiedPlayer) associated).getPendingConnection().getVirtualHost();
                final @Nullable InetAddress address = localAddress == null ? null : localAddress.getAddress();
                if (address != null) {
                    consumer.accept(IpSet.only(address));
                }
            }
        }
    }

    static final class LocalHost extends SimpleContextDefinition {
        private static final ContextDefinition<String> INSTANCE = new LocalHost();

        private LocalHost() {
            super("localhost");
        }

        @Override
        public void accumulateCurrentValues(final CalculatedSubject subject, final Consumer<String> consumer) {
            final @Nullable Object associated = subject.associatedObject();
            if (associated instanceof ProxiedPlayer) {
                final @Nullable InetSocketAddress virtualHost = ((ProxiedPlayer) associated).getPendingConnection().getVirtualHost();
                if (virtualHost != null) {
                    consumer.accept(virtualHost.getHostName());
                }
            }
        }
    }

    static final class LocalPort extends ContextDefinition<Integer> {
        private static final ContextDefinition<Integer> INSTANCE = new LocalPort();

        private LocalPort() {
            super("localport");
        }

        @Override
        public String serialize(final Integer userValue) {
            return userValue.toString();
        }

        @Override
        public Integer deserialize(final String userValue) {
            return Integer.parseInt(userValue);
        }

        @Override
        public boolean matches(final Integer ownVal, final Integer testVal) {
            return Objects.equals(ownVal, testVal);
        }

        @Override
        public void accumulateCurrentValues(final CalculatedSubject subject, final Consumer<Integer> consumer) {
            final @Nullable Object associated = subject.associatedObject();
            if (associated instanceof ProxiedPlayer) {
                final @Nullable InetSocketAddress virtualHost = ((ProxiedPlayer) associated).getPendingConnection().getVirtualHost();
                if (virtualHost != null) {
                    consumer.accept(virtualHost.getPort());
                }
            }
        }
    }
}
