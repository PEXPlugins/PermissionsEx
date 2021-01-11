/*
 * PermissionsEx - a permissions plugin for your server ecosystem
 * Copyright © 2021 zml [at] stellardrift [dot] ca and PermissionsEx contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package ca.stellardrift.permissionsex.bukkit;

import ca.stellardrift.permissionsex.context.ContextDefinitionProvider;
import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.impl.util.PCollections;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import ca.stellardrift.permissionsex.subject.SubjectRef;
import ca.stellardrift.permissionsex.subject.SubjectType;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of the Vault {@link Permission} service.
 */
final class PEXVault extends Permission {
    final PermissionsExPlugin pex;

    PEXVault(final PermissionsExPlugin pex) {
        this.pex = pex;
        this.plugin = pex;
    }

    // -- Basic definitions -- //

    @Override
    public String getName() {
        return this.pex.getName();
    }

    @Override
    public boolean isEnabled() {
        return this.pex.isEnabled();
    }

    @Override
    public boolean hasSuperPermsCompat() {
        return true;
    }

    @Override
    public boolean hasGroupSupport() {
        return true;
    }

    @Override
    public String[] getGroups() {
        return this.pex.groups().allIdentifiers().toArray(String[]::new);
    }


    CalculatedSubject getGroup(final String name) {
        return this.pex.groups().get(name).join();
    }

    CalculatedSubject getUser(final OfflinePlayer player) {
        return this.pex.users().get(player.getUniqueId()).join();
    }

    Set<ContextValue<?>> contextsFrom(final @Nullable String world) {
        if (world == null) {
            return ContextDefinitionProvider.GLOBAL_CONTEXT;
        } else {
            return PCollections.set(BukkitContexts.world().createValue(world));
        }
    }

    /**
     * Get an active world from an offline player if possible
     *
     * @param player The offline player object
     * @return Maybe a world?
     */
    public @Nullable String getActiveWorld(final OfflinePlayer player) {
        final @Nullable Player ply = player.getPlayer();
        return ply == null ? null : ply.getWorld().getName();
    }

    /**
     * This will replace the world in the given subject's active contexts.
     *
     * <p>Unfortunately, PEX's context system does not map perfectly onto Vault's idea of per-world permissions,
     * so this may not perfectly match cases where the world being queried does not match an online player's
     * current world.</p>
     *
     * @param subject the subject to provide active contexts for
     * @param worldOverride the world to override with, if any
     * @return the appropriate context values
     */
    Set<ContextValue<?>> contextsFrom(final CalculatedSubject subject, final @Nullable String worldOverride) {
        final Set<ContextValue<?>> origContexts = subject.activeContexts();
        if (worldOverride == null) {
            return origContexts;
        }

        final @Nullable Object associated = subject.associatedObject();
        if (associated instanceof Player && ((Player) associated).getWorld().getName().equalsIgnoreCase(worldOverride)) {
            return origContexts;
        }

        origContexts.removeIf(it -> BukkitContexts.world().equals(it.definition()));
        origContexts.add(BukkitContexts.world().createValue(worldOverride));
        return origContexts;
    }

    // Implementation of Vault API //

    @Override
    public boolean groupHas(final @Nullable String world, final String name, final String permission) {
        requireNonNull(name, "name");

        requireNonNull(permission, "permission");
        final CalculatedSubject subject = getGroup(name);
        return subject.permission(contextsFrom(subject, world), permission) > 0;
    }

    @Override
    public boolean groupAdd(final @Nullable String world, final String name, final String permission) {
        requireNonNull(name, "name");
        requireNonNull(permission, "permission");

        return !getGroup(name).data()
            .update(contextsFrom(world), it -> it.withPermission(permission, 1)).isCancelled();
    }

    @Override
    public boolean groupRemove(final @Nullable String world, final String name, final String permission) {
        requireNonNull(name, "name");
        requireNonNull(permission, "permission");

        return !getGroup(name).data()
            .update(contextsFrom(world), it -> it.withPermission(permission, 0)).isCancelled();
    }

    @Override
    public boolean playerHas(final @Nullable String world, final OfflinePlayer player, final String permission) {
        requireNonNull(player, "player");
        requireNonNull(permission, "permission");

        final CalculatedSubject subj = getUser(player);
        final int value = subj.permission(contextsFrom(subj, world), permission);
        if (value > 0) {
            return true;
        } else if (value < 0) {
            return false;
        } else {
            return this.pex.manager().engine().config().getPlatformConfig().fallbackOp() && player.isOp();
        }
    }

    @Override
    public boolean playerAdd(final @Nullable String world, final OfflinePlayer player, final String permission) {
        requireNonNull(player, "player");
        requireNonNull(permission, "permission");

        return !getUser(player).data()
            .update(contextsFrom(world), it -> it.withPermission(permission, 1)).isCancelled();
    }

    @Override
    public boolean playerAddTransient(final OfflinePlayer player, final String permission) {
        return playerAddTransient(getActiveWorld(player), player, permission);
    }

    @Override
    public boolean playerAddTransient(final Player player, final String permission) {
        return playerAddTransient(player.getWorld().getName(), player, permission);
    }

    @Override
    public boolean playerAddTransient(final @Nullable String worldName, final OfflinePlayer player, final String permission) {
        requireNonNull(player, "player");
        requireNonNull(permission, "permission");
        return !getUser(player).transientData()
            .update(contextsFrom(worldName), it -> it.withPermission(permission, 1)).isCancelled();
    }

    @Override
    public boolean playerRemoveTransient(final @Nullable String worldName, final OfflinePlayer player, final String permission) {
        requireNonNull(player, "player");
        requireNonNull(permission, "permission");
        return !getUser(player).transientData()
            .update(contextsFrom(worldName), it -> it.withPermission(permission, 0)).isCancelled();
    }

    @Override
    public boolean playerRemove(final @Nullable String world, final OfflinePlayer player, final String permission) {
        requireNonNull(player, "player");
        requireNonNull(permission, "permission");
        return !getUser(player).data()
            .update(contextsFrom(world), it -> it.withPermission(permission, 0)).isCancelled();
    }

    @Override
    public boolean playerRemoveTransient(final Player player, final String permission) {
        return playerRemoveTransient(player.getWorld().getName(), player, permission);
    }

    @Override
    public boolean playerRemoveTransient(final OfflinePlayer player, final String permission) {
        requireNonNull(player, "player");
        requireNonNull(permission, "permission");
        return playerRemoveTransient(getActiveWorld(player), player, permission);
    }

    @Override
    public boolean playerInGroup(final @Nullable String world, final OfflinePlayer player, final String group) {
        requireNonNull(player, "player");
        requireNonNull(group, "group");
        final CalculatedSubject subject = getUser(player);
        return subject.parents(contextsFrom(subject, world)).contains(SubjectRef.subject(this.pex.groups(), group));
    }

    @Override
    public boolean playerAddGroup(final @Nullable String world, final OfflinePlayer player, final String group) {
        requireNonNull(player, "player");
        requireNonNull(group, "group");
        return !getUser(player).data()
            .update(contextsFrom(world), it -> it.plusParent(this.pex.groups().type(), group)).isCancelled();
    }

    @Override
    public boolean playerRemoveGroup(final @Nullable String world, final OfflinePlayer player, final String group) {
        requireNonNull(player, "player");
        requireNonNull(group, "group");
        return !getUser(player).data()
            .update(contextsFrom(world), it -> it.minusParent(this.pex.groups().type(), group)).isCancelled();
    }

    @Override
    public String[] getPlayerGroups(final @Nullable String world, final OfflinePlayer player) {
        requireNonNull(player, "player");
        final CalculatedSubject subj = getUser(player);
        final SubjectType<String> groups = this.pex.groups().type();
        return subj.parents(contextsFrom(subj, world))
            .stream()
            .filter(ref -> ref.type().equals(groups))
            .map(SubjectRef::serializedIdentifier)
            .toArray(String[]::new);
    }

    @Override
    public @Nullable String getPrimaryGroup(final @Nullable String world, final OfflinePlayer player) {
        requireNonNull(player, "player");
        final String[] groups = getPlayerGroups(world, player);
        return groups.length > 0 ? groups[0] : null;
    }

    // -- Deprecated methods
    @SuppressWarnings("deprecation")
    private OfflinePlayer pFromName(final String name) {
        return this.pex.getServer().getOfflinePlayer(name);
    }

    @Override
    @Deprecated
    public boolean playerHas(final @Nullable String world, final String name, final String permission) {
        return playerHas(world, pFromName(name), permission);
    }

    @Override
    @Deprecated
    public boolean playerAdd(final @Nullable String world, final String name, final String permission) {
        return playerAdd(world, pFromName(name), permission);
    }

    @Override
    @Deprecated
    public boolean playerRemove(final @Nullable String world, final String name, final String permission) {
        return playerRemove(world, pFromName(name), permission);
    }

    @Override
    @Deprecated
    public boolean playerInGroup(final @Nullable String world, final String player, final String group) {
        return playerInGroup(world, pFromName(player), group);
    }

    @Override
    @Deprecated
    public boolean playerAddGroup(final @Nullable String world, final String player, final String group) {
        return playerAddGroup(world, pFromName(player), group);
    }

    @Override
    @Deprecated
    public boolean playerRemoveGroup(final @Nullable String world, final String player, final String group) {
        return playerRemoveGroup(world, pFromName(player), group);
    }

    @Override
    @Deprecated
    public String[] getPlayerGroups(final @Nullable String world, final String player) {
        return getPlayerGroups(world, pFromName(player));
    }

    @Override
    @Deprecated
    public @Nullable String getPrimaryGroup(final String world, final String player) {
        return getPrimaryGroup(world, pFromName(player));
    }

}
