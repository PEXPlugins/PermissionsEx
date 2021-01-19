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
package ca.stellardrift.permissionsex.fabric;

import ca.stellardrift.permissionsex.context.ContextDefinition;
import ca.stellardrift.permissionsex.context.SimpleContextDefinition;
import ca.stellardrift.permissionsex.fabric.impl.FabricPermissionsExImpl;
import ca.stellardrift.permissionsex.fabric.impl.Functions;
import ca.stellardrift.permissionsex.fabric.impl.bridge.ClientConnectionBridge;
import ca.stellardrift.permissionsex.fabric.impl.context.CommandSourceContextDefinition;
import ca.stellardrift.permissionsex.fabric.impl.context.IdentifierContextDefinition;
import ca.stellardrift.permissionsex.impl.context.IpSetContextDefinition;
import ca.stellardrift.permissionsex.impl.util.IpSet;
import ca.stellardrift.permissionsex.impl.util.PCollections;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Context definitions used on the fabric platform.
 *
 * @since 2.0.0
 */
public final class FabricContexts {

    /**
     * A context definition for the world a subject is active in.
     *
     * <p>Values are the {@link World#getRegistryKey()} of worlds.</p>
     *
     * @return a world context definition
     * @since 2.0.0
     */
    public static ContextDefinition<Identifier> world() {
        return WorldDefinition.INSTANCE;
    }

    /**
     * A context definition for the dimension type of the world a subject is active in.
     *
     * <p>Values are the keys for {@link net.minecraft.world.dimension.DimensionType DimensionTypes}.</p>
     *
     * @return a dimension type context definition
     * @since 2.0.0
     */
    public static ContextDefinition<Identifier> dimension() {
        return DimensionDefinition.INSTANCE;
    }

    /**
     * A context definition for the IP a user is connecting from.
     *
     * <p>This context is not set for a user connecting to a local integrated server.</p>
     *
     * @return the remote IP context definition
     * @since 2.0.0
     */
    public static ContextDefinition<IpSet> remoteIp() {
        return RemoteIpDefinition.INSTANCE;
    }

    /**
     * A context definition for the IP a user is connecting to.
     *
     * <p>This context is not set for a user connecting to a local integrated server.</p>
     *
     * @return the local IP context definition
     * @since 2.0.0
     */
    public static ContextDefinition<IpSet> localIp() {
        return LocalIpDefinition.INSTANCE;
    }

    /**
     * A context definition for the host name a user has connected with.
     *
     * <p>This exposes the literal address provided by the client, for the purposes of virtual hosting.</p>
     *
     * @return a context for the requested host name
     * @since 2.0.0
     */
    public static ContextDefinition<String> localHost() {
        return LocalHostDefinition.INSTANCE;
    }

    /**
     * A context definition for the port a user has connected with.
     *
     * <p>This exposes the literal port provided by the client, for the purposes of virtual hosting.</p>
     *
     * @return a context for the requested local port
     * @since 2.0.0
     */
    public static ContextDefinition<Integer> localPort() {
        return LocalPortDefinition.INSTANCE;
    }

    /**
     * A context providing all currently executing {@link CommandFunction}s as contexts.
     *
     * <p>If a check is performed from a nested function call, both functions will appear as values.</p>
     *
     * @return a context type for the executing functions
     * @since 2.0.0
     */
    public static ContextDefinition<Identifier> function() {
        return Functions.Context.INSTANCE;
    }

    static final class WorldDefinition extends IdentifierContextDefinition implements CommandSourceContextDefinition<Identifier> {

        static final WorldDefinition INSTANCE = new WorldDefinition();

        private WorldDefinition() {
            super("world");
        }

        @Override
        public void accumulateCurrentValues(
            final CalculatedSubject subject,
            final Consumer<Identifier> consumer
        ) {
            final @Nullable Object associated = subject.associatedObject();
            if (associated instanceof ServerPlayerEntity) {
                consumer.accept(((ServerPlayerEntity) associated).getServerWorld().getRegistryKey().getValue());
            }
        }

        @Override
        public void accumulateCurrentValues(
            final ServerCommandSource source,
            final Consumer<Identifier> consumer
        ) {
            final @Nullable ServerWorld world = source.getWorld();
            if (world != null) {
                consumer.accept(world.getRegistryKey().getValue());
            }
        }

        @Override
        public Set<Identifier> suggestValues(final CalculatedSubject subject) {
            final @Nullable MinecraftServer server = FabricPermissionsExImpl.INSTANCE.server();
            return server == null ? PCollections.set() : PCollections.asSet(server.getWorlds(), it -> it.getRegistryKey().getValue());
        }

    }

    static final class DimensionDefinition extends IdentifierContextDefinition implements CommandSourceContextDefinition<Identifier> {

        private static final DimensionDefinition INSTANCE = new DimensionDefinition();

        private DimensionDefinition() {
            super("dimension");
        }

        @Override
        public void accumulateCurrentValues(final CalculatedSubject subject, final Consumer<Identifier> consumer) {
            final @Nullable Object associated = subject.associatedObject();
            if (associated instanceof ServerPlayerEntity) {
                final ServerPlayerEntity player = (ServerPlayerEntity) associated;
                final @Nullable Identifier dimension = player.getServerWorld().getRegistryManager()
                    .get(Registry.DIMENSION_TYPE_KEY).getId(player.getServerWorld().getDimension());
                if (dimension != null) {
                    consumer.accept(dimension);
                }
            }
        }

        @Override
        public void accumulateCurrentValues(final ServerCommandSource source, final Consumer<Identifier> consumer) {
            final @Nullable ServerWorld world = source.getWorld();
            if (world != null) {
                final @Nullable Identifier dimension = source.getRegistryManager().get(Registry.DIMENSION_TYPE_KEY).getId(world.getDimension());
                if (dimension != null) {
                    consumer.accept(dimension);
                }
            }
        }

        @Override
        public Set<Identifier> suggestValues(final CalculatedSubject subject) {
            final @Nullable Object associated = subject.associatedObject();
            if (associated instanceof Entity) {
                final @Nullable MinecraftServer possible = ((Entity) associated).getServer();
                if (possible != null) {
                    return possible.getRegistryManager().get(Registry.DIMENSION_TYPE_KEY).getIds();
                }
            }
            return PCollections.set();
        }

    }

    static final class RemoteIpDefinition extends IpSetContextDefinition implements CommandSourceContextDefinition<IpSet> {

        private static final RemoteIpDefinition INSTANCE = new RemoteIpDefinition();

        private RemoteIpDefinition() {
            super("remoteip");
        }

        @Override
        public void accumulateCurrentValues(final CalculatedSubject subject, final Consumer<IpSet> consumer) {
            final @Nullable Object associated = subject.associatedObject();
            if (associated instanceof ServerPlayerEntity) {
                this.accumulate((ServerPlayerEntity) associated, consumer);
            }
        }

        @Override
        public void accumulateCurrentValues(final ServerCommandSource source, final Consumer<IpSet> consumer) {
            if (source.getEntity() instanceof ServerPlayerEntity) {
                this.accumulate((ServerPlayerEntity) source.getEntity(), consumer);
            }
        }

        private void accumulate(final ServerPlayerEntity player, final Consumer<IpSet> consumer) {
            final @Nullable SocketAddress address = player.networkHandler.connection.getAddress();
            if (address instanceof InetSocketAddress) {
                consumer.accept(IpSet.only(((InetSocketAddress) address).getAddress()));
            }
        }

    }

    static final class LocalIpDefinition extends IpSetContextDefinition implements CommandSourceContextDefinition<IpSet> {

        static final LocalIpDefinition INSTANCE = new LocalIpDefinition();

        private LocalIpDefinition() {
            super("localip");
        }

        @Override
        public void accumulateCurrentValues(final CalculatedSubject subject, final Consumer<IpSet> consumer) {
            final @Nullable Object associated = subject.associatedObject();
            if (associated instanceof ServerPlayerEntity) {
                this.accumulate((ServerPlayerEntity) associated, consumer);
            }
        }

        @Override
        public void accumulateCurrentValues(final ServerCommandSource source, final Consumer<IpSet> consumer) {
            if (source.getEntity() instanceof ServerPlayerEntity) {
                this.accumulate((ServerPlayerEntity) source.getEntity(), consumer);
            }
        }

        private void accumulate(final ServerPlayerEntity player, final Consumer<IpSet> consumer) {
            final @Nullable InetSocketAddress address = ((ClientConnectionBridge) player.networkHandler.connection).virtualHost();
            if (address != null) {
                consumer.accept(IpSet.only(address.getAddress()));
            }
        }

    }

    static final class LocalHostDefinition extends SimpleContextDefinition implements CommandSourceContextDefinition<String> {

        static final LocalHostDefinition INSTANCE = new LocalHostDefinition();

        private LocalHostDefinition() {
            super("localhost");
        }

        @Override
        public void accumulateCurrentValues(final CalculatedSubject subject, final Consumer<String> consumer) {
            final @Nullable Object associated = subject.associatedObject();
            if (associated instanceof ServerPlayerEntity) {
                this.accumulate((ServerPlayerEntity) associated, consumer);
            }
        }

        @Override
        public void accumulateCurrentValues(final ServerCommandSource source, final Consumer<String> consumer) {
            if (source.getEntity() instanceof ServerPlayerEntity) {
                this.accumulate((ServerPlayerEntity) source.getEntity(), consumer);
            }
        }

        private void accumulate(final ServerPlayerEntity player, final Consumer<String> consumer) {
            final @Nullable InetSocketAddress virtualHost = ((ClientConnectionBridge) player.networkHandler.connection).virtualHost();
            if (virtualHost != null) {
                consumer.accept(virtualHost.getHostString());
            }
        }

    }

    static final class LocalPortDefinition extends ContextDefinition<Integer> implements CommandSourceContextDefinition<Integer> {

        static final LocalPortDefinition INSTANCE = new LocalPortDefinition();

        private LocalPortDefinition() {
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
        public void accumulateCurrentValues(final CalculatedSubject subject, final Consumer<Integer> consumer) {
            final @Nullable Object associated = subject.associatedObject();
            if (associated instanceof ServerPlayerEntity) {
                this.accumulate((ServerPlayerEntity) associated, consumer);
            }
        }

        @Override
        public void accumulateCurrentValues(final ServerCommandSource source, final Consumer<Integer> consumer) {
            if (source.getEntity() instanceof ServerPlayerEntity) {
                this.accumulate((ServerPlayerEntity) source.getEntity(), consumer);
            }
        }

        private void accumulate(final ServerPlayerEntity player, final Consumer<Integer> consumer) {
            final @Nullable InetSocketAddress virtualHost = ((ClientConnectionBridge) player.networkHandler.connection).virtualHost();
            if (virtualHost != null) {
                consumer.accept(virtualHost.getPort());
            }
        }

    }

}
