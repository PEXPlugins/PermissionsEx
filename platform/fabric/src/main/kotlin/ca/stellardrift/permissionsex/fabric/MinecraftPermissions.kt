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

import net.minecraft.util.Identifier

/**
 * Permissions used to replace Vanilla operator checks.
 */
object MinecraftPermissions {
    private const val BASE = "minecraft"

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
