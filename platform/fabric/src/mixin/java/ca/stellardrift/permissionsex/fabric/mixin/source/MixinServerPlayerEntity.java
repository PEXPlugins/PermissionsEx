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

import ca.stellardrift.permissionsex.PermissionsEx;
import ca.stellardrift.permissionsex.fabric.IPermissionCommandSource;
import ca.stellardrift.permissionsex.fabric.LocaleHolder;
import ca.stellardrift.permissionsex.fabric.PermissionsExMod;
import ca.stellardrift.permissionsex.fabric.UtilKt;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.ClientSettingsC2SPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity extends PlayerEntity implements LocaleHolder, IPermissionCommandSource {

    private Locale permissionsex$clientLocale = null;

    private final AtomicReference<CalculatedSubject> permSubject = new AtomicReference<>();

    public MixinServerPlayerEntity(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos,yaw, gameProfile);
    }

    @Nullable
    @Override
    public Locale getLocale() {
        return permissionsex$clientLocale;
    }

    @NotNull
    @Override
    public String getPermType() {
        return PermissionsEx.SUBJECTS_USER;
    }

    @NotNull
    @Override
    public String getPermIdentifier() {
        return getGameProfile().getId().toString();
    }

    @NotNull
    @Override
    public CalculatedSubject asCalculatedSubject() {
        CalculatedSubject ret = permSubject.get();
        if (ret != null) {
            return ret;
        }
        CalculatedSubject updated = PermissionsExMod.INSTANCE.getManager().getSubjects(getPermType()).get(getPermIdentifier()).join();
        permSubject.set(updated);
        return updated;
    }

    @Inject(method = "setClientSettings", at = @At("HEAD"))
    private void handleSetClientSettings(final ClientSettingsC2SPacket packet, final CallbackInfo ci) {
        final String language = ((AccessorClientSettingsC2SPacket) packet).getLanguage();
        if (language == null) {
            this.permissionsex$clientLocale = null;
        } else {
            this.permissionsex$clientLocale = UtilKt.asMCLocale(language);
        }
    }
}
