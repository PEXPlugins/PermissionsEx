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

package ca.stellardrift.permissionsex.fabric.mixin.lifecycle;

import ca.stellardrift.permissionsex.fabric.IVirtualHostHolder;
import ca.stellardrift.permissionsex.fabric.mixin.AccessorHandshakeC2SPacket;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.server.network.ServerHandshakeNetworkHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetSocketAddress;

@Mixin(ServerHandshakeNetworkHandler.class)
public class MixinServerHandshakeNetworkHandler {
    @Shadow @Final
    private ClientConnection connection;

    @Inject(method = "onHandshake", at = @At("HEAD"))
    public void applyVirtualHostToConnection(HandshakeC2SPacket handshakePacket, CallbackInfo ci) {
        IVirtualHostHolder conn = (IVirtualHostHolder) connection;
        AccessorHandshakeC2SPacket packet = (AccessorHandshakeC2SPacket) handshakePacket;
        InetSocketAddress addr = new InetSocketAddress(packet.address(), packet.port());
        conn.setVirtualHost(addr);
    }
}
