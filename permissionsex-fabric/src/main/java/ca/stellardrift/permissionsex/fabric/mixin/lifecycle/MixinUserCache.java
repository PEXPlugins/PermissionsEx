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

import ca.stellardrift.permissionsex.fabric.PEXUserCache;
import ca.stellardrift.permissionsex.util.MinecraftProfile;
import com.mojang.authlib.GameProfile;
import net.minecraft.util.UserCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(UserCache.class)
public abstract class MixinUserCache implements PEXUserCache {
    private ThreadLocal<Boolean> blockLookup = ThreadLocal.withInitial(() -> false);

    @Shadow
    public abstract GameProfile findByName(String name);

    @Inject(method = "findByName", at = @At(value = "JUMP", opcode = Opcodes.IFNULL, ordinal = 1),
            slice = @Slice(from=@At(value = "INVOKE", target = "java.util.Deque.remove", remap = false)), cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD)
    public void injectAfterCacheQuery(String name, CallbackInfoReturnable<GameProfile> ci, String str, Object obj, GameProfile unused) {
        if (obj == null && blockLookup.get()) {
            ci.setReturnValue(null);
        }
    }

    @Nullable
    @Override
    public MinecraftProfile findByUsername(@NotNull String username, boolean resolveOnline) {
        try {
            blockLookup.set(!resolveOnline);
            return (MinecraftProfile) findByName(username); // oh the magic of mixins
        } finally {
            blockLookup.set(false);
        }
    }
}
