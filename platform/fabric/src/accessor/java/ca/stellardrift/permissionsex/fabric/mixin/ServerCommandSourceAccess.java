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
package ca.stellardrift.permissionsex.fabric.mixin;

import com.mojang.brigadier.ResultConsumer;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ServerCommandSource.class)
public interface ServerCommandSourceAccess {
    @Accessor("level")
    int getLevel();

    @Accessor("output")
    CommandOutput accessor$output();

    @Invoker("<init>")
    static ServerCommandSource invoker$new(
            final CommandOutput output,
            final Vec3d pos,
            final Vec2f rot,
            final ServerWorld world,
            final int level,
            final String simpleName,
            final Text name,
            final MinecraftServer server,
            final @Nullable Entity entity,
            final boolean silent,
            final ResultConsumer<ServerCommandSource> consumer,
            final EntityAnchorArgumentType.EntityAnchor entityAnchor
    ) {
        throw new AssertionError();
    }
}
