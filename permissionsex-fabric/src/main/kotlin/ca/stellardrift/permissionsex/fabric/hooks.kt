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

@file:JvmName("PermissionsExHooks")
package ca.stellardrift.permissionsex.fabric

import ca.stellardrift.permissionsex.PermissionsEx.SUBJECTS_USER
import ca.stellardrift.permissionsex.context.ContextValue
import ca.stellardrift.permissionsex.subject.CalculatedSubject
import com.mojang.authlib.GameProfile
import com.mojang.brigadier.builder.ArgumentBuilder
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.Locale
import java.util.function.Predicate
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.util.Identifier

object RedirectTargets {
    const val SERVER_IS_CREATIVE_LEVEL_TWO_OP = "net.minecraft.server.network.ServerPlayerEntity.isCreativeLevelTwoOp()Z"
    const val IS_CREATIVE_LEVEL_TWO_OP = "net.minecraft.entity.player.PlayerEntity.isCreativeLevelTwoOp()Z"
    const val DEDICATED_PLAYER_MANAGER_IS_OP = "net.minecraft.server.dedicated.DedicatedPlayerManager.isOperator(Lcom/mojang/authlib/GameProfile;)Z"
    const val PLAYER_MANAGER_IS_OP = "net.minecraft.server.PlayerManager.isOperator(Lcom/mojang/authlib/GameProfile;)Z"
    const val OPERATOR_LIST_IS_EMPTY = "net.minecraft.server.OperatorList.isEmpty()Z"
    const val OPERATOR_LIST_CONTAINS = "net.minecraft.server.OperatorList.contains(Ljava/lang/Object;)Z"
    const val COMMAND_SOURCE_HAS_PERM_LEVEL = "net.minecraft.server.command.CommandSource.hasPermissionLevel(I)Z"
    const val SERVER_COMMAND_SOURCE_HAS_PERM_LEVEL = "net.minecraft.server.command.ServerCommandSource.hasPermissionLevel(I)Z"
    const val SERVER_PLAYER_ALLOWS_PERMISSION_LEVEL = "net.minecraft.server.network.ServerPlayerEntity.allowsPermissionLevel(I)Z"
    const val SERVER_NETWORK_HANDLER_IS_OWNER = "net.minecraft.server.network.ServerPlayNetworkHandler.isServerOwner()Z"
}

object MinecraftPermissions {
    const val BASE = "minecraft"

    // --- Bypass game limits ---
    const val BYPASS_WHITELIST = "$BASE.bypass.whitelist"
    const val BYPASS_SPAWN_PROTECTION = "$BASE.bypass.spawnprotection"
    const val BYPASS_PLAYER_LIMIT = "$BASE.bypass.playercount"
    const val BYPASS_MOVE_SPEED_PLAYER = "$BASE.bypass.movespeed.player"
    const val BYPASS_CHAT_SPAM = "$BASE.bypass.chatspeed"
    // minecraft.bypass.movespeed.vehicle.<namespace>.<type>
    const val BYPASS_MOVE_SPEED_VEHICLE = "$BASE.bypass.movespeed.vehicle"
    const val UPDATE_DIFFICULTY = "$BASE.updatedifficulty"

    // --- World interaction ---

    const val COMMAND_BLOCK_PLACE = "$BASE.commandblock.place"
    const val COMMAND_BLOCK_VIEW = "$BASE.commandblock.view"
    const val COMMAND_BLOCK_EDIT = "$BASE.commandblock.edit"
    const val COMMAND_BLOCK_BREAK = "$BASE.commandblock.break"
    const val JIGSAW_BLOCK_VIEW = "$BASE.jigsawblock.view"
    const val JIGSAW_BLOCK_EDIT = "$BASE.jigsawblock.edit"
    const val JIGSAW_BLOCK_BREAK = "$BASE.jigsawblock.break"
    const val STRUCTURE_BLOCK_VIEW = "$BASE.structureblock.view"
    const val STRUCTURE_BLOCK_EDIT = "$BASE.structureblock.edit"
    const val STRUCTURE_BLOCK_BREAK = "$BASE.structureblock.break"
    const val DEBUG_STICK_USE = "$BASE.debugstick.use"
    // minecraft.game.nbt.query.entity.<namespace>.<type>
    const val QUERY_ENTITY_NBT = "$BASE.nbt.query.entity"
    const val QUERY_BLOCK_NBT = "$BASE.nbt.query.block"
    const val LOAD_ENTITY_DATA = "$BASE.nbt.load.entity"
    const val LOAD_BLOCK_ITEM_DATA = "$BASE.nbt.load.block"

    // --- Command behaviors ---
    const val USE_SELECTOR = "$BASE.selector"
    const val BROADCAST_SEND = "$BASE.adminbroadcast.send"
    const val BROADCAST_RECEIVE = "$BASE.adminbroadcast.receive"

    @JvmStatic
    fun forCommand(command: String): String {
        return "$BASE.command.$command"
    }

    @JvmStatic
    fun makeSpecific(parent: String, child: Identifier?): String {
        return if (child == null) {
            parent
        } else {
            "$parent.${child.namespace}.${child.path}"
        }
    }
}

interface IVirtualHostHolder {
    /**
     * The hostname a client used to connect to this server
     * May be *unresolved* if the provided hostname could not be resolved
     */
    var virtualHost: InetSocketAddress
}

@JvmField
val LOCAL_HOST: InetSocketAddress = InetSocketAddress(InetAddress.getLocalHost(), 25565)

interface HandshakeC2SPacketAccess {
    val address: String
    val port: Int
}

interface IPermissionCommandSource {
    @JvmDefault
    fun hasPermission(perm: String): Boolean {
        return asCalculatedSubject().hasPermission(perm)
    }

    @JvmDefault
    fun asCalculatedSubject(): CalculatedSubject {
        return PermissionsExMod.manager.getSubjects(permType)[permIdentifier].join()
    }

    @JvmDefault
    val activeContexts: Set<ContextValue<*>> get() {
        return asCalculatedSubject().activeContexts
    }

    val permType: String
    val permIdentifier: String
}

interface IServerCommandSource {
    fun withPermissionOverride(override: IPermissionCommandSource?): ServerCommandSource
    fun getPermissionOverride(): IPermissionCommandSource?
}

fun <T : Any> commandPermissionCheck(permission: String): Predicate<T> {
    return Predicate {
        if (it is IPermissionCommandSource) {
            it.hasPermission(permission)
        } else {
            false
        }
    }
}

fun <T : ArgumentBuilder<ServerCommandSource, T>> T.requirePermission(permission: String): T {
    return requires(commandPermissionCheck(permission))
}

fun ServerCommandSource.hasPermission(perm: String): Boolean {
    return this.asCommander().hasPermission(perm)
}

@JvmOverloads
fun PlayerEntity.hasPermission(perm: String, fallbackOpLevel: Int = 2): Boolean {
    return if (this is IPermissionCommandSource) {
        hasPermission(perm)
    } else {
        allowsPermissionLevel(fallbackOpLevel)
    }
}

fun GameProfile.hasPermission(perm: String): Boolean {
    if (this.id == null) {
        PermissionsExMod.logger.error(Messages.GAMEPROFILE_ERROR_INCOMPLETE(this.name))
        return false
    }
    return PermissionsExMod.manager.getSubjects(SUBJECTS_USER)[this.id.toString()].join().hasPermission(perm)
}

internal interface LocaleHolder {
    val locale: Locale?
}
