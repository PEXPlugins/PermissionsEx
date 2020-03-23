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
import ca.stellardrift.permissionsex.commands.commander.FixedTranslationComponentRenderer;
import ca.stellardrift.permissionsex.commands.commander.MessageFormatter;
import ca.stellardrift.permissionsex.fabric.*;
import ca.stellardrift.permissionsex.util.Translatable;
import com.google.common.collect.Maps;
import net.kyori.text.Component;
import net.kyori.text.ComponentBuilder;
import net.kyori.text.format.TextColor;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Mixin(ServerCommandSource.class)
public abstract class MixinServerCommandSource implements Commander<ComponentBuilder<?, ?>> {
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

    @Shadow
    public abstract void sendFeedback(Text text, boolean broadcastToOps);

    @Shadow
    public abstract void sendError(Text text);

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

    @NotNull
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

    @NotNull
    @Override
    public MessageFormatter<ComponentBuilder<?, ?>> getFormatter() {
        return fmt;
    }

    private void sendFeedback(Component text) {
        Component rendered = FixedTranslationComponentRenderer.INSTANCE.render(text, this);
        if (this.output.sendCommandFeedback() && !silent) {
            if (this.output instanceof ServerPlayerEntity) {
                TextAdapter.sendPlayerMessage(((ServerPlayerEntity) this.output), rendered);
            } else {
                sendFeedback(TextAdapter.toMcText(rendered), false);
            }
        }
    }

    private void sendError(Component text) {
        Component rendered = FixedTranslationComponentRenderer.INSTANCE.render(text, this);
        if (this.output.shouldTrackOutput() && !silent) {
            if (this.output instanceof ServerPlayerEntity) {
                TextAdapter.sendPlayerMessage(((ServerPlayerEntity) this.output), rendered);
            } else {
                sendError(TextAdapter.toMcText(rendered));
            }
        }
    }

    @Override
    public void msg(ComponentBuilder<?, ?> text) {
        sendFeedback(text.color(TextColor.DARK_AQUA).build());
    }

    @Override
    public void debug(ComponentBuilder<?, ?> text) {
        sendFeedback(text.color(TextColor.GRAY).build());
    }

    @Override
    public void error(ComponentBuilder<?, ?> text, Throwable err) {
        sendError(text.color(TextColor.RED).build());
    }

    @Override
    public void msgPaginated(@NotNull Translatable title, @Nullable Translatable header, @NotNull Iterable<? extends ComponentBuilder<?, ?>> text) {
        msg(getFormatter().combined("# ", getFormatter().tr(title), " #"));
        if (header != null) {
            msg(getFormatter().tr(header));
        }
        text.forEach(this::msg);

        msg(getFormatter().combined("#################################"));
    }

}
