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

import ca.stellardrift.permissionsex.PermissionsEngine;
import ca.stellardrift.permissionsex.fabric.impl.FabricPermissionsExImpl;
import ca.stellardrift.permissionsex.fabric.impl.FabricSubjectTypes;
import ca.stellardrift.permissionsex.fabric.impl.bridge.PermissionCommandSourceBridge;
import ca.stellardrift.permissionsex.fabric.impl.bridge.ServerCommandSourceBridge;
import ca.stellardrift.permissionsex.subject.SubjectRef;
import ca.stellardrift.permissionsex.subject.SubjectTypeCollection;
import com.mojang.authlib.GameProfile;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * An implementation of PermissionsEx in the Fabric environment.
 *
 * <p>Some functionality may be unavailable when there is no server running.</p>
 *
 * <p>This complements the {@link Permissions} api to introduce more advanced
 * functionality specific to PermissionsEx.</p>
 *
 * @since 2.0.0
 */
public final class FabricPermissionsEx {

    /**
     * Get the permissions engine for this game instance.
     *
     * @return the permissions engine
     * @since 2.0.0
     */
    public static PermissionsEngine engine() {
        return FabricPermissionsExImpl.INSTANCE.manager().engine();
    }

    // Subject types //

    /**
     * Get the collection of user subjects.
     *
     * <p>User subjects represent players who can be on the server.</p>
     *
     * @return the {@code user} subject collection
     * @since 2.0.0
     */
    public static SubjectTypeCollection<UUID> users() {
        return FabricPermissionsExImpl.INSTANCE.manager().users();
    }

    /**
     * Get the collection of group subjects.
     *
     * @return the {@code group} subject collection
     * @since 2.0.0
     */
    public static SubjectTypeCollection<String> groups() {
        return FabricPermissionsExImpl.INSTANCE.manager().groups();
    }

    /**
     * Get the collection of system subjects.
     *
     * <p>System subjects are the server console and RCON connections.</p>
     *
     * @return the {@code system} subject collection
     * @since 2.0.0
     */
    public static SubjectTypeCollection<String> system() {
        return engine().subjects(FabricSubjectTypes.SYSTEM);
    }

    /**
     * Get the collection of command block subjects.
     *
     * <p>Command blocks query the subject with their custom name, or {@code @} if no name is set.</p>
     *
     * @return the {@code command-block} subject collection
     * @since 2.0.0
     */
    public static SubjectTypeCollection<String> commandBlocks() {
        return engine().subjects(FabricSubjectTypes.COMMAND_BLOCK);
    }

    /**
     * Get the collection of function subjects.
     *
     * <p>These are currently only queried for the tick and load function tags.</p>
     *
     * @return the function subject type
     */
    @ApiStatus.Experimental
    public static SubjectTypeCollection<Identifier> functions() {
        return engine().subjects(FabricSubjectTypes.FUNCTION);
    }

    /**
     * Get whether a player has the permission [perm], or if the player does not have a defined
     * [SubjectRef] (which could be true for mod-provided fake players) whether the player has the
     * provided [fallbackOpLevel].
     *
     */
    public static boolean hasPermission(final PlayerEntity player, final String permission) {
        return hasPermission(
            player,
            permission,
            player instanceof ServerPlayerEntity ? ((ServerPlayerEntity) player).server.getOpPermissionLevel() : 2
        );
    }

    /**
     * Get whether a player has the permission [perm], or if the player does not have a defined
     * [SubjectRef] (which could be true for mod-provided fake players) whether the player has the
     * provided [fallbackOpLevel].
     *
     */
    public static boolean hasPermission(final PlayerEntity player, final String permission, final int fallbackOpLevel) {
        requireNonNull(player, "player");
        requireNonNull(permission, "permission");

        if (player instanceof PermissionCommandSourceBridge<?>) {
            return ((PermissionCommandSourceBridge<?>) player).hasPermission(permission);
        } else {
            return player.hasPermissionLevel(fallbackOpLevel);
        }
    }

    /**
     * Get whether this {@link GameProfile} has a permission.
     *
     * <p>If the game profile is incomplete (i.e. has no UUID set), no permission can be checked so this
     * will always return {@code false}.</p>
     */
    public static boolean hasPermission(final GameProfile profile, final String permission) {
        final UUID id = profile.getId();
        if (id == null) {
            FabricPermissionsExImpl.INSTANCE.logger().error(Messages.GAMEPROFILE_ERROR_INCOMPLETE.tr(profile.getName()));
            return false;
        }
        return users().get(id).join().hasPermission(permission);
    }

    /**
     * Get the subject override to be used for permission checks instead of delegating to the output.
     */
    public static @Nullable SubjectRef<?> subjectOverride(final ServerCommandSource source) {
        return ((ServerCommandSourceBridge) source).subjectOverride();
    }

    /**
     * Create a new command source with the applied subject override, or none if [subj] is `null`.
     */
    public static ServerCommandSource withSubjectOverride(final ServerCommandSource source, final @Nullable SubjectRef<?> override) {
        return ((ServerCommandSourceBridge) source).withSubjectOverride(override);
    }

    private FabricPermissionsEx() {
    }

}
