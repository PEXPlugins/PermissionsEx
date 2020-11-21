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
package ca.stellardrift.permissionsex.sponge

import ca.stellardrift.permissionsex.subject.SubjectTypeDefinition
import java.lang.IllegalArgumentException
import java.util.UUID
import org.spongepowered.api.Game
import org.spongepowered.api.entity.living.player.server.ServerPlayer
import org.spongepowered.api.profile.GameProfileCache

/**
 * Metadata for user types
 */
internal class UserSubjectTypeDefinition(typeName: String, private val game: Game) : SubjectTypeDefinition<ServerPlayer>(typeName) {
    override fun isNameValid(name: String): Boolean {
        return try {
            UUID.fromString(name)
            true
        } catch (ex: IllegalArgumentException) {
            false
        }
    }

    override fun getAliasForName(name: String): String? {
        return try {
            UUID.fromString(name)
            null
        } catch (ex: IllegalArgumentException) {
            val player = game.server.getPlayer(name)
            if (player.isPresent) {
                player.get().uniqueId.toString()
            } else {
                val res: GameProfileCache = game.server.gameProfileManager.cache
                res.streamOfMatches(name)
                    .filter { it.name.isPresent && it.name.get().equals(name, ignoreCase = true) }
                    .findFirst()
                    .map { it.uniqueId.toString() }
                    .orElse(null)
            }
        }
    }

    override fun getAssociatedObject(identifier: String): ServerPlayer? {
        return try {
            game.server.getPlayer(UUID.fromString(identifier)).orElse(null)
        } catch (ex: IllegalArgumentException) {
            null
        }
    }
}
