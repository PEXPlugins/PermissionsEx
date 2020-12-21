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
import ca.stellardrift.permissionsex.fabric.CommandSourceContextDefinition;
import ca.stellardrift.permissionsex.fabric.PermissionCommandSourceBridge;
import ca.stellardrift.permissionsex.fabric.PermissionsExMod;
import ca.stellardrift.permissionsex.fabric.ServerCommandSourceBridge;
import ca.stellardrift.permissionsex.fabric.UtilKt;
import ca.stellardrift.permissionsex.fabric.mixin.ServerCommandSourceAccess;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import ca.stellardrift.permissionsex.subject.SubjectRef;
import ca.stellardrift.permissionsex.subject.SubjectType;
import ca.stellardrift.permissionsex.util.CachingValue;
import com.google.common.collect.ImmutableSet;
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
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Mixin(ServerCommandSource.class)
public abstract class ServerCommandSourceMixin implements PermissionCommandSourceBridge<Object>, ServerCommandSourceBridge {

    @Shadow public abstract boolean hasPermissionLevel(int level);
    @Shadow public abstract MinecraftServer getMinecraftServer();

    @Shadow @Final private CommandOutput output;
    @Shadow @Final private MinecraftServer server;
    @Shadow @Final private String simpleName;
    @Shadow @Final private Vec3d position;
    @Shadow @Final private Vec2f rotation;
    @Shadow @Final private ServerWorld world;
    @Shadow @Final private int level;
    @Shadow @Final private Text name;
    @Shadow @Final @Nullable private Entity entity;
    @Shadow @Final private boolean silent;
    @Shadow @Final private ResultConsumer<ServerCommandSource> resultConsumer;
    @Shadow @Final private EntityAnchorArgumentType.EntityAnchor entityAnchor;

    private @MonotonicNonNull CachingValue<Set<ContextValue<?>>> pex$activeContexts;
    private @Nullable SubjectRef<?> pex$subjectOverride;

    @Inject(method = "<init>(Lnet/minecraft/server/command/CommandOutput;Lnet/minecraft/util/math/Vec3d;" +
            "Lnet/minecraft/util/math/Vec2f;Lnet/minecraft/server/world/ServerWorld;ILjava/lang/String;" +
            "Lnet/minecraft/text/Text;Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/entity/Entity;Z" +
            "Lcom/mojang/brigadier/ResultConsumer;Lnet/minecraft/command/argument/EntityAnchorArgumentType$EntityAnchor;)V", at = @At("RETURN"))
    protected void pex$applyActiveContexts(CommandOutput commandOutput_1, Vec3d vec3d_1, Vec2f vec2f_1,
                                           ServerWorld serverWorld_1, int int_1, String string_1, Text text_1,
                                           MinecraftServer minecraftServer_1, @Nullable Entity entity_1, boolean boolean_1,
                                           ResultConsumer<ServerCommandSource> resultConsumer_1,
                                           EntityAnchorArgumentType.EntityAnchor entityAnchorArgumentType$EntityAnchor_1,
                                           CallbackInfo ci) {
        final Supplier<Set<ContextValue<?>>> updater = () -> {
            if (!PermissionsExMod.INSTANCE.getAvailable()) {
                return ImmutableSet.of();
            }
            final Set<ContextValue<?>> accumulator = new HashSet<>();
            final CalculatedSubject subj = asCalculatedSubject();
            for (ContextDefinition<?> def : PermissionsExMod.INSTANCE.getManager().registeredContextTypes()) {
                pex$handleSingleCtx(subj, def, accumulator);
            }
            return ImmutableSet.copyOf(accumulator);
        };
        // TODO: This causes issues with function contexts, since the context may change multiple times in a single tick for a certain subject.
        if (this.server == null) {
            this.pex$activeContexts = CachingValue.timeBased(50L, updater);
        } else {
            this.pex$activeContexts = UtilKt.tickCachedValue(minecraftServer_1, 1L, updater);
        }
    }


    @SuppressWarnings("unchecked")
    private <T> void pex$handleSingleCtx(CalculatedSubject subj, ContextDefinition<T> definition, Set<ContextValue<?>> accumulator) {
        final Consumer<T> callback = key -> accumulator.add(definition.createValue(key));
        if (definition instanceof CommandSourceContextDefinition) {
            //noinspection ConstantConditions,unchecked // mixin target
            ((CommandSourceContextDefinition<T>) definition).accumulateCurrentValues((ServerCommandSource) (Object) this, callback);
        } else {
            definition.accumulateCurrentValues(subj, callback);
        }
    }

    @Inject(method = "*", at = @At("WITHER_MUTATOR"))
    private void pex$applySubjectOverride(final CallbackInfoReturnable<ServerCommandSource> cir) {
        ((ServerCommandSourceBridge) cir.getReturnValue()).subjectOverride(this.subjectOverride());
    }

    // PermissionCommandSourceBridge //

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public @NotNull SubjectType<Object> getPermType() {
        if (this.pex$subjectOverride != null) {
            return (SubjectType<Object>) this.pex$subjectOverride.type();
        } else if (this.output instanceof PermissionCommandSourceBridge<?>) {
            return ((PermissionCommandSourceBridge<Object>) this.output).getPermType();
        } else {
            return (SubjectType) PermissionsExMod.INSTANCE.getSystemSubjectType();
        }
    }

    @Override
    public @NotNull Object getPermIdentifier() {
        if (this.pex$subjectOverride != null) {
            return this.pex$subjectOverride.identifier();
        } else if (this.output instanceof PermissionCommandSourceBridge<?>) {
            return ((PermissionCommandSourceBridge<?>) this.output).getPermType();
        } else {
            return this.simpleName;
        }
    }

    @Override
    public boolean hasPermission(final @NotNull String perm) {
        return this.asCalculatedSubject().hasPermission(getActiveContexts(), perm);
    }

    @Override
    public @NotNull CalculatedSubject asCalculatedSubject() {
        return this.output instanceof PermissionCommandSourceBridge && this.pex$subjectOverride == null ? ((PermissionCommandSourceBridge<?>) this.output).asCalculatedSubject()
                : PermissionsExMod.INSTANCE.getManager().subjects(getPermType()).get(getPermIdentifier()).join();
    }

    @Override
    public @NotNull Set<ContextValue<?>> getActiveContexts() {
        return this.pex$activeContexts.get();
    }

    // ServerCommandSourceBridge //

    @Override
    public @NotNull ServerCommandSource withSubjectOverride(final @Nullable SubjectRef<?> override) {
        final ServerCommandSource out = ServerCommandSourceAccess.invoker$new(
                this.output,
                this.position,
                this.rotation,
                this.world,
                this.level,
                this.simpleName,
                this.name,
                this.server,
                this.entity,
                this.silent,
                this.resultConsumer,
                this.entityAnchor
        );

        ((ServerCommandSourceBridge) out).subjectOverride(override);
        return out;
    }

    @Override
    public void subjectOverride(final @Nullable SubjectRef<?> ref) {
        this.pex$subjectOverride = ref;
    }

    @Override
    public @Nullable SubjectRef<?> subjectOverride() {
        return this.pex$subjectOverride;
    }
}
