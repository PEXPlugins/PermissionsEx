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
package ca.stellardrift.permissionsex.fabric.impl;

/**
 * Common targets for permission check redirects.
 */
public final class RedirectTargets {
    public static final String SERVER_IS_CREATIVE_LEVEL_TWO_OP = "net.minecraft.server.network.ServerPlayerEntity.isCreativeLevelTwoOp()Z";
    public static final String IS_CREATIVE_LEVEL_TWO_OP = "net.minecraft.entity.player.PlayerEntity.isCreativeLevelTwoOp()Z";
    public static final String DEDICATED_PLAYER_MANAGER_IS_OP = "net.minecraft.server.dedicated.DedicatedPlayerManager.isOperator(Lcom/mojang/authlib/GameProfile;)Z";
    public static final String PLAYER_MANAGER_IS_OP = "net.minecraft.server.PlayerManager.isOperator(Lcom/mojang/authlib/GameProfile;)Z";
    public static final String OPERATOR_LIST_IS_EMPTY = "net.minecraft.server.OperatorList.isEmpty()Z";
    public static final String OPERATOR_LIST_CONTAINS = "net.minecraft.server.OperatorList.contains(Ljava/lang/Object;)Z";
    public static final String COMMAND_SOURCE_HAS_PERM_LEVEL = "net.minecraft.command.CommandSource.hasPermissionLevel(I)Z";
    public static final String SERVER_COMMAND_SOURCE_HAS_PERM_LEVEL = "net.minecraft.server.command.ServerCommandSource.hasPermissionLevel(I)Z";
    public static final String SERVER_PLAYER_HAS_PERMISSION_LEVEL = "net.minecraft.server.network.ServerPlayerEntity.hasPermissionLevel(I)Z";
    public static final String SERVER_NETWORK_HANDLER_IS_HOST = "net.minecraft.server.network.ServerPlayNetworkHandler.isHost()Z";

    private RedirectTargets() {
    }

}
