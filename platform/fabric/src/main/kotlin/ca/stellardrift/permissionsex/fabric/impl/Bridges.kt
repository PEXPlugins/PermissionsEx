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
@file:JvmName("Bridges")
package ca.stellardrift.permissionsex.fabric.impl

import ca.stellardrift.permissionsex.context.ContextValue
import ca.stellardrift.permissionsex.subject.CalculatedSubject
import ca.stellardrift.permissionsex.subject.SubjectRef
import ca.stellardrift.permissionsex.subject.SubjectType
import java.net.InetAddress
import java.net.InetSocketAddress
import net.minecraft.server.command.ServerCommandSource

object RedirectTargets {
    const val SERVER_IS_CREATIVE_LEVEL_TWO_OP = "net.minecraft.server.network.ServerPlayerEntity.isCreativeLevelTwoOp()Z"
    const val IS_CREATIVE_LEVEL_TWO_OP = "net.minecraft.entity.player.PlayerEntity.isCreativeLevelTwoOp()Z"
    const val DEDICATED_PLAYER_MANAGER_IS_OP = "net.minecraft.server.dedicated.DedicatedPlayerManager.isOperator(Lcom/mojang/authlib/GameProfile;)Z"
    const val PLAYER_MANAGER_IS_OP = "net.minecraft.server.PlayerManager.isOperator(Lcom/mojang/authlib/GameProfile;)Z"
    const val OPERATOR_LIST_IS_EMPTY = "net.minecraft.server.OperatorList.isEmpty()Z"
    const val OPERATOR_LIST_CONTAINS = "net.minecraft.server.OperatorList.contains(Ljava/lang/Object;)Z"
    const val COMMAND_SOURCE_HAS_PERM_LEVEL = "net.minecraft.command.CommandSource.hasPermissionLevel(I)Z"
    const val SERVER_COMMAND_SOURCE_HAS_PERM_LEVEL = "net.minecraft.server.command.ServerCommandSource.hasPermissionLevel(I)Z"
    const val SERVER_PLAYER_HAS_PERMISSION_LEVEL = "net.minecraft.server.network.ServerPlayerEntity.hasPermissionLevel(I)Z"
    const val SERVER_NETWORK_HANDLER_IS_HOST = "net.minecraft.server.network.ServerPlayNetworkHandler.isHost()Z"
}

interface ClientConnectionBridge {
    /**
     * The hostname a client used to connect to this server
     * May be *unresolved* if the provided hostname could not be resolved
     */
    var virtualHost: InetSocketAddress
}

@JvmField
val LOCAL_HOST: InetSocketAddress = InetSocketAddress(InetAddress.getLocalHost(), 25565)

interface PermissionCommandSourceBridge<I> {
    @JvmDefault
    fun hasPermission(perm: String): Boolean {
        return asCalculatedSubject().hasPermission(perm)
    }

    @JvmDefault
    fun asCalculatedSubject(): CalculatedSubject {
        return FabricPermissionsExImpl.manager.subjects(permType)[permIdentifier].join()
    }

    @JvmDefault
    val activeContexts: Set<ContextValue<*>> get() {
        return asCalculatedSubject().activeContexts()
    }

    /**
     * Get a reference pointing to this subject.
     */
    @JvmDefault
    fun asReference(): SubjectRef<I> = SubjectRef.subject(this.permType, this.permIdentifier)

    val permType: SubjectType<I>
    val permIdentifier: I
}

interface ServerCommandSourceBridge {
    /**
     * Apply a permission subject override for a [ServerCommandSource].
     */
    fun withSubjectOverride(override: SubjectRef<*>?): ServerCommandSource

    /**
     * Set a new override on an existing [ServerCommandSource].
     *
     * Internal use only
     *
     * [withSubjectOverride] should probably be used instead.
     */
    fun subjectOverride(ref: SubjectRef<*>?)

    /**
     * Get the subject override for a [ServerCommandSource]
     */
    fun subjectOverride(): SubjectRef<*>?
}
