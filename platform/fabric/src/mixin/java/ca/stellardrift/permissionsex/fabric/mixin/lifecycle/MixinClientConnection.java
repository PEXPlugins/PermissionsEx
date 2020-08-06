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
import ca.stellardrift.permissionsex.fabric.PermissionsExHooks;
import io.netty.channel.Channel;
import net.minecraft.network.ClientConnection;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

@Mixin(ClientConnection.class)
public class MixinClientConnection implements IVirtualHostHolder {
    @Shadow
    private Channel channel;

    private InetSocketAddress virtualHost;

    @NotNull
    @Override
    public InetSocketAddress getVirtualHost() {
        if (virtualHost == null) {
            SocketAddress tempAddress = channel.localAddress();
            if (tempAddress instanceof InetSocketAddress) {
                return ((InetSocketAddress) tempAddress);
            } else {
                return PermissionsExHooks.LOCAL_HOST;
            }
        } else {
            return virtualHost;
        }
    }

    @Override
    public void setVirtualHost(@NotNull InetSocketAddress inetSocketAddress) {
        if (virtualHost != null) {
            throw new IllegalStateException("Virtual host can only be set once per connection!");
        }
        this.virtualHost = inetSocketAddress;
    }
}
