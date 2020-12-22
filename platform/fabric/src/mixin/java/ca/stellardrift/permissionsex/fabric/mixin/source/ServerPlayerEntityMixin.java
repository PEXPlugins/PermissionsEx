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
package ca.stellardrift.permissionsex.fabric.mixin.source;

import ca.stellardrift.permissionsex.fabric.FabricPermissionsEx;
import ca.stellardrift.permissionsex.fabric.impl.PermissionCommandSourceBridge;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import ca.stellardrift.permissionsex.subject.SubjectType;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity implements PermissionCommandSourceBridge<UUID> {
    private final AtomicReference<CalculatedSubject> permSubject = new AtomicReference<>();

    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos,yaw, gameProfile);
    }

    @NotNull
    @Override
    public SubjectType<UUID> getPermType() {
        return FabricPermissionsEx.getUserSubjectType();
    }

    @NotNull
    @Override
    public UUID getPermIdentifier() {
        return getUuid();
    }

    @NotNull
    @Override
    public CalculatedSubject asCalculatedSubject() {
        CalculatedSubject ret = permSubject.get();
        if (ret != null) {
            return ret;
        }
        CalculatedSubject updated = FabricPermissionsEx.getEngine().subjects(getPermType()).get(getPermIdentifier()).join();
        permSubject.set(updated);
        return updated;
    }
}
