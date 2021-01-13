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
@file:JvmName("FabricPermissionsEx")
package ca.stellardrift.permissionsex.fabric

import ca.stellardrift.permissionsex.PermissionsEngine
import ca.stellardrift.permissionsex.fabric.impl.FabricPermissionsExImpl
import ca.stellardrift.permissionsex.fabric.impl.PermissionCommandSourceBridge
import ca.stellardrift.permissionsex.fabric.impl.ServerCommandSourceBridge
import ca.stellardrift.permissionsex.fabric.impl.asCommander
import ca.stellardrift.permissionsex.subject.SubjectRef
import ca.stellardrift.permissionsex.subject.SubjectType
import com.google.common.collect.Maps
import com.mojang.authlib.GameProfile
import com.mojang.brigadier.builder.ArgumentBuilder
import java.util.UUID
import java.util.function.Predicate
import java.util.function.Supplier
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.util.Identifier
import net.minecraft.util.InvalidIdentifierException

// the basics //

val engine: PermissionsEngine get() = FabricPermissionsExImpl.manager

// Subject types

val userSubjectType: SubjectType<UUID> get() = FabricPermissionsExImpl.mcManager.users().type()
val groupSubjectType: SubjectType<String> get() = FabricPermissionsExImpl.mcManager.groups().type()

val systemSubjectType: SubjectType<String> = SubjectType.stringIdentBuilder("system")
    .fixedEntries(
        Maps.immutableEntry("Server", Supplier { FabricPermissionsExImpl.server }),
        Maps.immutableEntry(IDENTIFIER_RCON, Supplier { null })
    )
    .undefinedValues { true }
    .build()

// TODO: How can we represent permission level two for command blocks and functions
val commandBlockSubjectType: SubjectType<String> = SubjectType.stringIdentBuilder("command-block")
    .undefinedValues { true }
    .build()

val functionSubjectType: SubjectType<Identifier> = SubjectType.builder("function", Identifier::class.java)
    .serializedBy(Identifier::toString)
    .deserializedBy {
        try {
            Identifier(it)
        } catch (ex: InvalidIdentifierException) {
            throw ca.stellardrift.permissionsex.subject.InvalidIdentifierException(it)
        }
    }
    .undefinedValues { true }
    .build()

// Command convenience methods

/**
 * Create a predicate that will test if its source has a PermissionsEx permission.
 */
fun <T : Any> commandPermissionCheck(permission: String): Predicate<T> {
    return Predicate {
        if (it is PermissionCommandSourceBridge<*>) {
            it.hasPermission(permission)
        } else {
            false
        }
    }
}

fun <T : ArgumentBuilder<ServerCommandSource, T>> T.requirePermission(permission: String): T {
    return requires(commandPermissionCheck(permission))
}

// Subject permission check methods

/**
 * Check whether the receiver has a permission in its active contexts.
 */
fun ServerCommandSource.hasPermission(perm: String): Boolean {
    return this.asCommander().hasPermission(perm)
}

/**
 * Get whether a player has the permission [perm], or if the player does not have a defined
 * [SubjectRef] (which could be true for mod-provided fake players) whether the player has the
 * provided [fallbackOpLevel].
 *
 */
@JvmOverloads
fun PlayerEntity.hasPermission(perm: String, fallbackOpLevel: Int = 2): Boolean {
    return if (this is PermissionCommandSourceBridge<*>) {
        hasPermission(perm)
    } else {
        hasPermissionLevel(fallbackOpLevel)
    }
}

/**
 * Get whether this [GameProfile] has a permission.
 *
 * If the game profile is incomplete (i.e. has no UUID set), no permission can be checked so this
 * will always return `false`.
 */
fun GameProfile.hasPermission(perm: String): Boolean {
    if (this.id == null) {
        FabricPermissionsExImpl.logger().error(Messages.GAMEPROFILE_ERROR_INCOMPLETE.tr(this.name))
        return false
    }
    return FabricPermissionsExImpl.mcManager.users()[this.id].join().hasPermission(perm)
}

// Command subject overrides

/**
 * Get the subject override to be used for permission checks instead of delegating to the output.
 */
fun ServerCommandSource.subjectOverride(): SubjectRef<*>? {
    return (this as ServerCommandSourceBridge).subjectOverride()
}

/**
 * Create a new command source with the applied subject override, or none if [subj] is `null`.
 */
fun ServerCommandSource.withSubjectOverride(subj: SubjectRef<*>?): ServerCommandSource {
    return (this as ServerCommandSourceBridge).withSubjectOverride(subj)
}
