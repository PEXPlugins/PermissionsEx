/*
 * PermissionsEx - a permissions plugin for your server ecosystem
 * Copyright © 2021 zml [at] stellardrift [dot] ca and PermissionsEx contributors
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
package ca.stellardrift.permissionsex.bukkit;

import ca.stellardrift.permissionsex.context.ContextDefinition;
import ca.stellardrift.permissionsex.context.EnumContextDefinition;
import ca.stellardrift.permissionsex.context.SimpleContextDefinition;
import ca.stellardrift.permissionsex.impl.context.IpSetContextDefinition;
import ca.stellardrift.permissionsex.impl.util.IpSet;
import ca.stellardrift.permissionsex.impl.util.PCollections;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import static ca.stellardrift.permissionsex.context.ContextDefinitionProvider.GLOBAL_CONTEXT;

/**
 * Contexts available on the Bukkit platform.
 *
 * @since 2.0.0
 */
public final class BukkitContexts {

    /**
     * Get the context definition holding the world instance a user is currently in
     *
     * @return the context definition for key {@code world}
     */
    public static ContextDefinition<String> world() {
        return WorldDefinition.INSTANCE;
    }

    /**
     * Get the context definition holding the dimension type a user is currently in
     *
     * @return the context definition for key {@code dimension}
     */
    public static ContextDefinition<World.Environment> dimension() {
        return DimensionType.INSTANCE;
    }

    /**
     * Get the context definition holding the IP address a user is connecting from
     *
     * @return the context definition for key {@code remoteip}
     */
    public static ContextDefinition<IpSet> remoteIp() {
        return RemoteIp.INSTANCE;
    }

    /**
     * Get the context definition holding the virtual host a user has connected to.
     *
     * @return the context definition for key {@code localhost}
     */
    public static ContextDefinition<String> localHost() {
        return LocalHost.INSTANCE;
    }

    /**
     * Get the context definition holding the IP address a user has connected to.
     *
     * @return the context definition for key {@code localip}
     */
    public static ContextDefinition<IpSet> localIp() {
        return LocalIp.INSTANCE;
    }

    /**
     * Get the context definition holding the port a user has connected to.
     *
     * @return the context definition for key {@code localport}
     */
    public static ContextDefinition<Integer> localPort() {
        return LocalPort.INSTANCE;
    }

    static class WorldDefinition extends SimpleContextDefinition {
        static final WorldDefinition INSTANCE = new WorldDefinition();

        private WorldDefinition() {
            super("world");
        }

        @Override
        public boolean matches(final String ownVal, final String testVal) {
            return ownVal.equalsIgnoreCase(testVal);
        }

        @Override
        public void accumulateCurrentValues(final CalculatedSubject subject, final Consumer<String> consumer) {
            final @Nullable Object associated = subject.associatedObject();
            if (associated instanceof Player) {
                consumer.accept(((Player) associated).getWorld().getName());
            }
        }

        @Override
        public Set<String> suggestValues(final CalculatedSubject subject) {
            return PCollections.asSet(Bukkit.getWorlds(), World::getName);
        }
    }

    static final class DimensionType extends EnumContextDefinition<World.Environment> {
        static final DimensionType INSTANCE = new DimensionType();

        private DimensionType() {
            super("dimension", World.Environment.class);
        }

        @Override
        public void accumulateCurrentValues(final CalculatedSubject subject, final Consumer<World.Environment> consumer) {
            final @Nullable Object associated = subject.associatedObject();
            if (associated instanceof Player) {
                consumer.accept(((Player) associated).getWorld().getEnvironment());
            }
        }
    }

    static final class RemoteIp extends IpSetContextDefinition {
        static final RemoteIp INSTANCE = new RemoteIp();

        private RemoteIp() {
            super("remoteip");
        }

        @Override
        public void accumulateCurrentValues(final CalculatedSubject subject, final Consumer<IpSet> consumer) {
            final @Nullable Object associated = subject.associatedObject();
            if (associated instanceof Player) {
                final InetSocketAddress address = ((Player) associated).getAddress();
                if (address != null) {
                    consumer.accept(IpSet.only(address.getAddress()));
                }
            }
        }
    }

    static final class LocalHost extends SimpleContextDefinition {
        static final LocalHost INSTANCE = new LocalHost();

        private LocalHost() {
            super("localhost");
        }

        @Override
        public void accumulateCurrentValues(final CalculatedSubject subject, final Consumer<String> consumer) {
            final @Nullable String host = subject.transientData().get().segment(GLOBAL_CONTEXT).options().get("hostname");
            if (host != null) {
                consumer.accept(host);
            }
        }
    }

    static final class LocalIp extends IpSetContextDefinition {
        static final LocalIp INSTANCE = new LocalIp();

        private LocalIp() {
            super("localip");
        }

        @Override
        public void accumulateCurrentValues(final CalculatedSubject subject, final Consumer<IpSet> consumer) {
            // TODO: implement local IP setting
        }

    }

    static final class LocalPort extends ContextDefinition<Integer> {

        static final LocalPort INSTANCE = new LocalPort();

        private LocalPort() {
            super("localport");
        }

        @Override
        public String serialize(final Integer canonicalValue) {
            return canonicalValue.toString();
        }

        @Override
        public @Nullable Integer deserialize(final String userValue) {
            try {
                return Integer.parseInt(userValue);
            } catch (final NumberFormatException ex) {
                return null;
            }
        }

        @Override
        public boolean matches(final Integer ownVal, final Integer testVal) {
            return Objects.equals(ownVal, testVal);
        }

        @Override
        public void accumulateCurrentValues(final CalculatedSubject subject, final Consumer<Integer> consumer) {
            consumer.accept(Bukkit.getPort());
        }

    }

}
