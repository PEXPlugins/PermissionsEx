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
package ca.stellardrift.permissionsex.fabric;

import net.minecraft.util.Identifier;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Permissions that replace Vanilla operator checks
 *
 * @since 2.0.0
 */
public final class MinecraftPermissions {

    private static final String BASE = "minecraft";

    // --- Bypass game limits ---
    public static final String BYPASS_WHITELIST = BASE + ".bypass.whitelist";
    public static final String BYPASS_SPAWN_PROTECTION = BASE + ".bypass.spawnprotection";
    public static final String BYPASS_PLAYER_LIMIT = BASE + ".bypass.playercount";
    public static final String BYPASS_MOVE_SPEED_PLAYER = BASE + ".bypass.movespeed.player";
    public static final String BYPASS_CHAT_SPAM = BASE + ".bypass.chatspeed";
    // minecraft.bypass.movespeed.vehicle.<namespace>.<type>
    public static final String BYPASS_MOVE_SPEED_VEHICLE = BASE + ".bypass.movespeed.vehicle";
    public static final String UPDATE_DIFFICULTY = BASE + ".updatedifficulty";

    // --- World interaction ---

    public static final String COMMAND_BLOCK_PLACE = BASE + ".commandblock.place";
    public static final String COMMAND_BLOCK_VIEW = BASE + ".commandblock.view";
    public static final String COMMAND_BLOCK_EDIT = BASE + ".commandblock.edit";
    public static final String COMMAND_BLOCK_BREAK = BASE + ".commandblock.break";
    public static final String JIGSAW_BLOCK_VIEW = BASE + ".jigsawblock.view";
    public static final String JIGSAW_BLOCK_EDIT = BASE + ".jigsawblock.edit";
    public static final String JIGSAW_BLOCK_BREAK = BASE + ".jigsawblock.break";
    public static final String STRUCTURE_BLOCK_VIEW = BASE + ".structureblock.view";
    public static final String STRUCTURE_BLOCK_EDIT = BASE + ".structureblock.edit";
    public static final String STRUCTURE_BLOCK_BREAK = BASE + ".structureblock.break";
    public static final String DEBUG_STICK_USE = BASE + ".debugstick.use";
    // minecraft.game.nbt.query.entity.<namespace>.<type>
    public static final String QUERY_ENTITY_NBT = BASE + ".nbt.query.entity";
    public static final String QUERY_BLOCK_NBT = BASE + ".nbt.query.block";
    public static final String LOAD_ENTITY_DATA = BASE + ".nbt.load.entity";
    public static final String LOAD_BLOCK_ITEM_DATA = BASE + ".nbt.load.block";

    // --- Command behaviors ---
    public static final String USE_SELECTOR = BASE + ".selector";
    public static final String BROADCAST_SEND = BASE + ".adminbroadcast.send";
    public static final String BROADCAST_RECEIVE = BASE + ".adminbroadcast.receive";

    public static String forCommand(final String command) {
        return BASE + ".command." + command;
    }

    public static String makeSpecific(final String parent, final @Nullable Identifier child) {
        if (child == null) {
            return parent;
        } else {
            return parent + '.' + child.getNamespace() + '.' + child.getPath();
        }
    }

    private MinecraftPermissions() {
    }

}
