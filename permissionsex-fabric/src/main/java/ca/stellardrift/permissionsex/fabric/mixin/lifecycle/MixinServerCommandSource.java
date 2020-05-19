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

import ca.stellardrift.permissionsex.commands.commander.Commander;
import ca.stellardrift.permissionsex.commands.commander.MessageFormatter;
import ca.stellardrift.permissionsex.commands.commander.Permission;
import ca.stellardrift.permissionsex.fabric.FabricMessageFormatter;
import ca.stellardrift.permissionsex.fabric.IPermissionCommandSource;
import ca.stellardrift.permissionsex.fabric.LocaleHolder;
import ca.stellardrift.permissionsex.util.PEXComponentRenderer;
import ca.stellardrift.text.fabric.ComponentCommandSource;
import com.google.common.collect.Maps;
import net.kyori.text.Component;
import net.kyori.text.format.TextColor;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Locale;
import java.util.Map;

@Mixin(ServerCommandSource.class)
public abstract class MixinServerCommandSource implements Commander {
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
    @Final
    private CommandOutput output;

    @Shadow
    @Final
    private boolean silent;

    @Shadow
    public abstract boolean hasPermissionLevel(int level);

    @Shadow @Final private int level;

    @NotNull
    @Override
    public String getName() {
        return simpleName;
    }

    @NotNull
    @Override
    public Locale getLocale() {
        return entity instanceof LocaleHolder ? ((LocaleHolder) entity).getLocale() : Locale.getDefault();
    }

    @Override
    public Map.Entry<String, String> getSubjectIdentifier() {
        if (this instanceof IPermissionCommandSource) {
            return Maps.immutableEntry(((IPermissionCommandSource) this).getPermType(), ((IPermissionCommandSource) this).getPermIdentifier());
        } else {
            return null;
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
    public boolean hasPermission(@NotNull Permission permission) {
        int ret = 0;
        if (this instanceof IPermissionCommandSource) {
            ret = ((IPermissionCommandSource) this).asCalculatedSubject().getPermission(permission.getValue());
        }

        if (ret == 0) { // op status
            ret = this.level;
        }

        if (ret == 0) { // permission def value
            ret = permission.getDefault();
        }

        return ret > 0;
    }

    @NotNull
    @Override
    public MessageFormatter getFormatter() {
        return fmt;
    }

    private void sendFeedback(Component text) {
        out().sendFeedback(PEXComponentRenderer.INSTANCE.render(text, getLocale()), false);
    }

    @Override
    public void msg(Component text) {
        sendFeedback(text.colorIfAbsent(TextColor.DARK_AQUA));
    }

    private ComponentCommandSource out() {
        return (ComponentCommandSource) this;
    }
}
