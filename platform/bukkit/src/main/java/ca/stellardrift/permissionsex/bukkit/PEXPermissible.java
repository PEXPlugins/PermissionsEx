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

import ca.stellardrift.permissionsex.PermissionsEngine;
import ca.stellardrift.permissionsex.context.ContextDefinitionProvider;
import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.impl.context.TimeContextDefinition;
import ca.stellardrift.permissionsex.impl.util.PCollections;
import ca.stellardrift.permissionsex.legacy.LegacyConversions;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import ca.stellardrift.permissionsex.util.NodeTree;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.permissions.PermissionRemovedExecutor;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.spongepowered.configurate.util.UnmodifiableCollections.immutableMapEntry;

public class PEXPermissible extends PermissibleBase {

    private final Player player;
    private final PermissionsExPlugin plugin;
    private final PermissionsEngine engine;
    @Nullable Permissible previousPermissible;
    private final Set<PEXPermissionAttachment> attachments = ConcurrentHashMap.newKeySet();
    private final CalculatedSubject pexSubject;

    PEXPermissible(final Player player, final PermissionsExPlugin plugin) {
        super(player);
        this.player = player;
        this.plugin = plugin;
        this.engine = plugin.engine();
        this.pexSubject = plugin.users().get(player.getUniqueId()).join();
    }

    PermissionsEngine manager() {
        return this.engine;
    }

    @Override
    public boolean isPermissionSet(final String name) {
        return getPermissionValue(pexSubject.activeContexts(), name.toLowerCase(Locale.ROOT)) != 0;
    }

    private int getPermissionValue(final Set<ContextValue<?>> contexts, final String permission) {
        int ret = getPermissionValue0(pexSubject.permissions(contexts), permission);
        if (ret == 0) {
            for (final Metapermission meta : METAPERMISSIONS) {
                final Matcher match = meta.matchAgainst.matcher(permission);
                if (match.matches() && meta.isMatch(match, pexSubject, contexts)) {
                    ret = 1;
                }
            }
        }

        /*
         * We may fall back to op value if no other data is set
         * This only takes into account the permission directly being checked having a default set to FALSE or NOT_OP, and not any parents.
         * I believe this is incorrect, but the real-world impacts are likely minor -zml
         */
        if (ret == 0 && this.plugin.manager().engine().config().getPlatformConfig().fallbackOp()) {
            final Permission perm = this.plugin.permissionList().get(permission);
            if (perm == null) {
                ret = this.isOp() ? 1 : 0;
            } else if (perm.getDefault() != PermissionDefault.FALSE) {
                ret = (this.isOp() ^ (perm.getDefault() == PermissionDefault.NOT_OP)) ? 1 : 0;
            }
        }

        if (engine.debugMode()) {
            engine.logger().info(Messages.SUPERPERMS_CHECK_NOTIFY.tr(permission, player.getName(), contexts, ret));
        }
        return ret;
    }

    private int getPermissionValue0(final NodeTree nodeTree, final String name) {
        int result = nodeTree.get(name);
        if (result != 0) {
            return result;
        }
        final @Nullable PermissionList list = this.plugin.permissionList();
        if (list != null) {
            for (final Map.Entry<String, Boolean> entry : list.getParents(name)) {
                result = getPermissionValue0(nodeTree, entry.getKey());
                if (!entry.getValue()) {
                    result = -result;
                }
                if (result != 0) {
                    return result;
                }
            }
        }
        return 0;
    }

    @Override
    public boolean isPermissionSet(final Permission perm) {
        return isPermissionSet(perm.getName());
    }

    @Override
    public boolean hasPermission(final String perm) {
        return getPermissionValue(pexSubject.activeContexts(), perm.toLowerCase(Locale.ROOT)) > 0;
    }

    @Override
    public boolean hasPermission(final Permission perm) {
        return hasPermission(perm.getName());
    }

    @Override
    public PermissionAttachment addAttachment(final Plugin plugin, final String name, final boolean value) {
        return super.addAttachment(plugin, name, value);
    }

    @Override
    public PermissionAttachment addAttachment(final Plugin plugin) {
        final PEXPermissionAttachment attach = new PEXPermissionAttachment(plugin, player, this);
        this.pexSubject.transientData()
            .update(ContextDefinitionProvider.GLOBAL_CONTEXT, it -> it.plusParent(attach))
            .thenRun(() -> this.attachments.add(attach));
        return attach;
    }

    boolean removeAttachmentInternal(final PEXPermissionAttachment attach) {
        this.pexSubject.transientData().update(
            ContextDefinitionProvider.GLOBAL_CONTEXT,
            it -> it.minusParent(attach)
        ).thenRun(() -> {
            final @Nullable PermissionRemovedExecutor callback = attach.getRemovalCallback();
            if (callback != null) {
                callback.attachmentRemoved(attach);
            }
        });
        return true;
    }

    @Override
    public void removeAttachment(final PermissionAttachment attachment) {
        if (!(attachment instanceof PEXPermissionAttachment)) {
            throw new IllegalArgumentException("Provided attachment was not a PEX attachment!");
        }

        removeAttachmentInternal((PEXPermissionAttachment) attachment);
        this.attachments.remove(attachment);
    }

    void removeAllAttachments() {
        for (final PEXPermissionAttachment attachment : this.attachments) {
            removeAttachmentInternal(attachment);
        }
        this.attachments.clear();
    }

    // We don't need this currently? Guess could clear cache somehow, but automated should get it
    // right. well, except for people adding children to permissions -- that is just weird
    @Override
    public void recalculatePermissions() {
    }

    @Override
    public void clearPermissions() {
        // todo
    }

    @Override
    public @Nullable PermissionAttachment addAttachment(final Plugin plugin, final String name, final boolean value, final int ticks) {
        return super.addAttachment(plugin, name, value, ticks);
    }

    @Override
    public PermissionAttachment addAttachment(
        final Plugin plugin,
        final int ticks
    ) {
        final PEXPermissionAttachment attach = new PEXPermissionAttachment(this.plugin, player, this);
        this.pexSubject.transientData()
            .update(PCollections.set(
                TimeContextDefinition.BEFORE_TIME.createValue(ZonedDateTime.now(ZoneId.systemDefault())
                    .plus(ticks * 50L, ChronoUnit.MILLIS))
            ), it -> it.plusParent(attach))
            .thenRun(() -> this.attachments.add(attach));
        return attach;
    }

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        final Set<ContextValue<?>> activeContexts = pexSubject.activeContexts();
        final Stream<PermissionAttachmentInfo> direct = this.pexSubject.permissions(activeContexts).asMap().entrySet().stream()
            .map(ent -> new PermissionAttachmentInfo(player, ent.getKey(), null, ent.getValue() > 0));

        final Stream<PermissionAttachmentInfo> metapermissions = Arrays.stream(METAPERMISSIONS)
            .flatMap(mPerm -> mPerm.getValues(this.pexSubject, activeContexts))
            .map(value -> new PermissionAttachmentInfo(this.player, value, null, true));

        return Stream.concat(direct, metapermissions)
            .collect(Collectors.toSet());
    }

    // Metapermission system
    private static final Metapermission[] METAPERMISSIONS = {
        /*
         * | Permission                 | Usage
         |----------------------------|------
         | `group.<group>`            | Added for each group a user is in
         | `groups.<group>`           | same as above
         | `options.<option>.<value>` | Each option the user has
         | `prefix.<prefix>`          | User's prefix
         | `suffix.<suffix>`          | User's suffix
         */
        new Metapermission(Pattern.compile("groups?\\.(.+)")) {
            @Override
            boolean isMatch(final MatchResult result, final CalculatedSubject subj, final Set<ContextValue<?>> contexts) {
                // TODO: This needs to use a SubjectRef
                return subj.parents(contexts).contains(immutableMapEntry(PermissionsEngine.SUBJECTS_GROUP, result.group(1)));
            }

            @Override
            Stream<String> getValues(final CalculatedSubject subj, final Set<ContextValue<?>> contexts) {
                return subj.parents(contexts).stream()
                    .filter(ref -> ref.type().name().equals(LegacyConversions.SUBJECTS_GROUP))
                    .flatMap(ref -> Stream.of("group." + ref.serializedIdentifier(), "groups." + ref.serializedIdentifier()));
            }
        },
        new Metapermission(Pattern.compile("options\\.(.*)\\.(.*)")) {
            @Override
            boolean isMatch(final MatchResult result, final CalculatedSubject subject, final Set<ContextValue<?>> contexts) {
                return subject.option(contexts, result.group(1))
                    .map(it -> it.equals(result.group(2)))
                    .orElse(false);
            }

            @Override
            Stream<String> getValues(final CalculatedSubject subj, final Set<ContextValue<?>> contexts) {
                return subj.options(contexts).entrySet().stream()
                    .map(ent -> "options." + ent.getKey() + "." + ent.getValue());
            }
        },
        new SpecificOptionMetapermission("prefix"),
        new SpecificOptionMetapermission("suffix")
    };
    static abstract class Metapermission {

        /**
         * Pattern to match against
         */
        protected final Pattern matchAgainst;

        protected Metapermission(final Pattern matchAgainst) {
            this.matchAgainst = matchAgainst;
        }

        abstract boolean isMatch(final MatchResult result, final CalculatedSubject subj, final Set<ContextValue<?>> contexts);

        abstract Stream<String> getValues(final CalculatedSubject subj, final Set<ContextValue<?>> contexts);

    }

    static final class SpecificOptionMetapermission extends Metapermission {

        private final String option;

        SpecificOptionMetapermission(final String option) {
            super(Pattern.compile(Pattern.quote(option) + "\\.(.+)"));
            this.option = option;
        }

        @Override
        boolean isMatch(final MatchResult result, final CalculatedSubject subject, final Set<ContextValue<?>> contexts) {
            return subject.option(contexts, this.option)
                .map(it -> it.equals(result.group(1)))
                .orElse(false);
        }

        @Override
        Stream<String> getValues(final CalculatedSubject subj, final Set<ContextValue<?>> contexts) {
            final String ret = subj.options(contexts).get(this.option);
            return ret == null ? Stream.of() : Stream.of(this.option + "." + ret);
        }

    }

}
