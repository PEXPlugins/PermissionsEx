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

import ca.stellardrift.permissionsex.context.ContextDefinition;
import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.fabric.*;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import ca.stellardrift.permissionsex.util.CachingValue;
import ca.stellardrift.permissionsex.util.CachingValues;
import com.google.common.collect.ImmutableSet;
import com.mojang.brigadier.ResultConsumer;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;

@Mixin(ServerCommandSource.class)
public abstract class MixinServerCommandSource implements IPermissionCommandSource {

    @Shadow @Final
    private CommandOutput output;

    @Shadow @Final
    private String simpleName;

    @Shadow
    public abstract boolean hasPermissionLevel(int level);

    @Shadow
    public abstract MinecraftServer getMinecraftServer();

    @Shadow @Final private MinecraftServer server;
    private CachingValue<Set<ContextValue<?>>> activeContexts;

    @Inject(method = "<init>(Lnet/minecraft/server/command/CommandOutput;Lnet/minecraft/util/math/Vec3d;" +
            "Lnet/minecraft/util/math/Vec2f;Lnet/minecraft/server/world/ServerWorld;ILjava/lang/String;" +
            "Lnet/minecraft/text/Text;Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/entity/Entity;Z" +
            "Lcom/mojang/brigadier/ResultConsumer;Lnet/minecraft/command/argument/EntityAnchorArgumentType$EntityAnchor;)V", at = @At("RETURN"))
    protected void constructor(CommandOutput commandOutput_1, Vec3d vec3d_1, Vec2f vec2f_1,
                               ServerWorld serverWorld_1, int int_1, String string_1, Text text_1,
                               MinecraftServer minecraftServer_1, @Nullable Entity entity_1, boolean boolean_1,
                               ResultConsumer<ServerCommandSource> resultConsumer_1,
                               EntityAnchorArgumentType.EntityAnchor entityAnchorArgumentType$EntityAnchor_1,
                               CallbackInfo ci) {
        final Function0<Set<ContextValue<?>>> updater = () -> {
            if (!PermissionsExMod.INSTANCE.getAvailable()) {
                return ImmutableSet.of();
            }
            final Set<ContextValue<?>> accumulator = new HashSet<>();
            final CalculatedSubject subj = asCalculatedSubject();
            for (ContextDefinition<?> def : PermissionsExMod.INSTANCE.getManager().getRegisteredContextTypes()) {
                handleSingleCtx(subj, def, accumulator);
            }
            return ImmutableSet.copyOf(accumulator);
        };
        if (server == null) {
            activeContexts = CachingValues.cachedByTime(50L, updater);
        } else {
            activeContexts = UtilKt.tickCachedValue(minecraftServer_1, 1L, updater);
        }
    }


    @SuppressWarnings("unchecked")
    private <T> void handleSingleCtx(CalculatedSubject subj, ContextDefinition<T> definition, Set<ContextValue<?>> accumulator) {
        final Function1<T, Unit> callback = key -> {
            accumulator.add(definition.createValue(key));
           return null;
        };
        if (definition instanceof CommandSourceContextDefinition) {
            //noinspection ConstantConditions,unchecked // mixin target
            ((CommandSourceContextDefinition<T>) definition).accumulateCurrentValues((ServerCommandSource) (Object) this, callback);
        } else {
            definition.accumulateCurrentValues(subj, callback);
        }
    }

    @NotNull
    @Override
    public String getPermType() {
        return output instanceof IPermissionCommandSource ? ((IPermissionCommandSource) output).getPermType() : FabricDefinitions.SUBJECTS_SYSTEM;
    }

    @NotNull
    @Override
    public String getPermIdentifier() {
        return output instanceof IPermissionCommandSource ? ((IPermissionCommandSource) output).getPermIdentifier() : simpleName;
    }

    @Override
    public boolean hasPermission(@NotNull String perm) {
        return (output instanceof IPermissionCommandSource ? ((IPermissionCommandSource) output).asCalculatedSubject() : asCalculatedSubject()).hasPermission(getActiveContexts(), perm);
    }

    @NotNull
    @Override
    public CalculatedSubject asCalculatedSubject() {
        return output instanceof IPermissionCommandSource ? ((IPermissionCommandSource) output).asCalculatedSubject() : PermissionsExMod.INSTANCE.getManager().getSubjects(getPermType()).get(getPermIdentifier()).join();
    }

    @NotNull
    @Override
    public Set<ContextValue<?>> getActiveContexts() {
        return activeContexts.get();
    }
}
