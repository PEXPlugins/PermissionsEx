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
 *
 */

package ca.stellardrift.permissionsex.fabric

import ca.stellardrift.permissionsex.context.SimpleContextDefinition
import ca.stellardrift.permissionsex.subject.CalculatedSubject
import ca.stellardrift.permissionsex.subject.SubjectTypeDefinition
import ca.stellardrift.permissionsex.util.Util
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.world.dimension.DimensionType
import java.util.Optional
import java.util.UUID

object WorldContextDefinition : SimpleContextDefinition("world") {
    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: (value: String) -> Unit) {
        Util.castOptional(subject.associatedObject, ServerPlayerEntity::class.java).ifPresent {
            consumer(it.serverWorld.levelProperties.levelName)
        }
    }
}

object DimensionContextDefinition : SimpleContextDefinition("dimension") {
    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: (value: String) -> Unit) {
        Util.castOptional(subject.associatedObject, ServerPlayerEntity::class.java).ifPresent {
            val key = DimensionType.getId(it.serverWorld.dimension.type)?.path
            if (key != null) {
                consumer(key)
            }
        }
    }
}

class UserSubjectTypeDefinition : SubjectTypeDefinition<ServerPlayerEntity>("user") {
    override fun isNameValid(name: String): Boolean {
        return try {
            UUID.fromString(name)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    override fun getAliasForName(name: String): Optional<String> {
        return try {
            UUID.fromString(name)
            Optional.empty()
        } catch (e: IllegalArgumentException) {
            Optional.ofNullable(PermissionsExMod.server.playerManager.getPlayer(name)?.uuid?.toString())
        }
    }

    override fun getAssociatedObject(identifier: String): Optional<ServerPlayerEntity> {
        return try {
            val uid = UUID.fromString(identifier)
            Optional.ofNullable(PermissionsExMod.server.playerManager.getPlayer(uid))
        } catch (e: IllegalArgumentException) {
            Optional.empty()
        }
    }

}
