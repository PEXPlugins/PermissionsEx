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

import ca.stellardrift.permissionsex.fabric.FabricMessageFormatter;
import ca.stellardrift.permissionsex.fabric.IPermissionCommandSource;
import ca.stellardrift.permissionsex.fabric.LocaleHolder;
import ca.stellardrift.permissionsex.util.Translatable;
import ca.stellardrift.permissionsex.util.command.Commander;
import ca.stellardrift.permissionsex.util.command.MessageFormatter;
import com.google.common.collect.Maps;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Mixin(ServerCommandSource.class)
public abstract class MixinServerCommandSource implements Commander<Text> {
    @SuppressWarnings("ConstantConditions")
    private final FabricMessageFormatter fmt = new FabricMessageFormatter((ServerCommandSource) (Object) this);

    @Shadow
    @Final
    private MinecraftServer minecraftServer;

    @Shadow
    @Final
    private Entity entity;

    @Shadow
    @Final
    private String simpleName;

    @Shadow
    public abstract boolean hasPermissionLevel(int level);

    @Shadow
    public abstract void sendFeedback(Text text, boolean broadcastToOps);

    @Shadow
    public abstract void sendError(Text text);

    @Override
    public String getName() {
        return simpleName;
    }

    @Override
    public Locale getLocale() {
        return entity instanceof LocaleHolder ? ((LocaleHolder) entity).getLocale() : Locale.getDefault();
    }

    @Override
    public Optional<Map.Entry<String, String>> getSubjectIdentifier() {
        if (this instanceof IPermissionCommandSource) {
            return Optional.of(Maps.immutableEntry(((IPermissionCommandSource) this).getPermType(), ((IPermissionCommandSource) this).getPermIdentifier()));
        } else {
            return Optional.empty();
        }
    }

    /*@Override // Implemented in the source ServerCommandSource mixin
    public boolean hasPermission(String permission) {
        if (this instanceof IPermissionCommandSource) {
            return ((IPermissionCommandSource) this).hasPermission(permission);
        } else {
            return hasPermissionLevel(minecraftServer.getOpPermissionLevel());
        }
    }*/

    @Override
    public MessageFormatter<Text> fmt() {
        return fmt;
    }

    @Override
    public void msg(Text text) {
        sendFeedback(text.formatted(Formatting.DARK_AQUA), false);
    }

    @Override
    public void debug(Text text) {
        sendFeedback(text.formatted(Formatting.GRAY), false);
    }

    @Override
    public void error(Text text) {
        sendError(text);
    }

    @Override
    public void msgPaginated(Translatable title, @Nullable Translatable header, Iterable<Text> text) {
        msg(fmt().combined("# ", fmt().tr(title), " #"));
        if (header != null) {
            msg(fmt().tr(header));
        }
        text.forEach(this::msg);

        msg(fmt().combined("#################################"));

    }
}
