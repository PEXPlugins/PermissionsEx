/*
 * PermissionsEx - a permissions plugin for your server ecosystem
 * Copyright © 2020 zml [at] stellardrift [dot] ca and PermissionsEx contributors
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

import ca.stellardrift.permissionsex.PermissionsEx;
import ca.stellardrift.permissionsex.context.BeforeTimeContextDefinition;
import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import ca.stellardrift.permissionsex.util.NodeTree;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import org.bukkit.entity.Player;
import org.bukkit.permissions.*;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import static ca.stellardrift.permissionsex.PermissionsEx.SUBJECTS_GROUP;
import static ca.stellardrift.permissionsex.PermissionsEx.SUBJECTS_USER;

/**
 * Implementation of Permissible using PEX for data
 */
public class PEXPermissible extends PermissibleBase {

    private static Metapermission[] METAPERMISSIONS = new Metapermission[] {
            /**
             * | Permission                 | Usage
             |----------------------------|------
             | `group.<group>`            | Added for each group a user is in
             | `groups.<group>`           | same as above
             | `options.<option>.<value>` | Each option the user has
             | `prefix.<prefix>`          | User's prefix
             | `suffix.<suffix>`          | User's suffix
             */
            new Metapermission(Pattern.compile("groups?\\.(?<name>.+)")) {
                @Override
                public boolean isMatch(Matcher result, CalculatedSubject subj, Set<ContextValue<?>> contexts) {
                    return subj.getParents(contexts).contains(Maps.immutableEntry(SUBJECTS_GROUP, result.group("name")));
                }

                @Override
                public Iterator<String> getValues(CalculatedSubject subj, Set<ContextValue<?>> contexts) {
                    return subj.getParents(contexts).stream()
                            .filter(ent -> ent.getKey().equals(SUBJECTS_GROUP))
                            .flatMap(ent -> StreamSupport.<String>stream(Spliterators.spliterator(new String[]{"group." + ent.getValue(), "groups." + ent.getValue()}, Spliterator.IMMUTABLE | Spliterator.DISTINCT), false))
                            .iterator();
                }
            },
            new Metapermission(Pattern.compile("options\\.(?<key>.*)\\.(?<value>.*)")) {
                @Override
                public boolean isMatch(Matcher result, CalculatedSubject subj, Set<ContextValue<?>> contexts) {
                    return subj.getOption(contexts, result.group("key")).map(val -> val.equals(result.group("value"))).orElse(false);
                }

                @Override
                public Iterator<String> getValues(CalculatedSubject subj, Set<ContextValue<?>> contexts) {
                    return Iterables.transform(subj.getOptions(contexts).entrySet(),
                            ent -> "options." + ent.getKey() + "." + ent.getValue())
                            .iterator();
                }
            },
            new SpecificOptionMetapermission("prefix"),
            new SpecificOptionMetapermission("suffix")
    };
    private abstract static class Metapermission {
        /**
         * Pattern to match against
         */
        private final Pattern matchAgainst;

        protected Metapermission(Pattern matchAgainst) {
            this.matchAgainst = matchAgainst;
        }

        public abstract boolean isMatch(Matcher result, CalculatedSubject subj, Set<ContextValue<?>> contexts);

        public abstract Iterator<String> getValues(CalculatedSubject subj, Set<ContextValue<?>> contexts);
    }

    private static class SpecificOptionMetapermission extends Metapermission {
        private final String option;
        public SpecificOptionMetapermission(String option) {
            super(Pattern.compile(Pattern.quote(option) + "\\.(?<value>.+)"));
            this.option = option;
        }

        @Override
        public boolean isMatch(Matcher result, CalculatedSubject subj, Set<ContextValue<?>> contexts) {
            return subj.getOption(contexts, option).map(val -> val.equals(result.group("value"))).orElse(false);
        }

        @Override
        public Iterator<String> getValues(CalculatedSubject subj, Set<ContextValue<?>> contexts) {
            String ret = subj.getOptions(contexts).get(option);
            return ret == null ? ImmutableSet.<String>of().iterator() : Iterators.singletonIterator(this.option + "." + ret);
        }
    }
    private final Player player;
    private final PermissionsExPlugin plugin;
    private PermissionsEx<BukkitConfiguration> pex;
    private CalculatedSubject subj;
    private Permissible previousPermissible;
    private final Set<PEXPermissionAttachment> attachments = new HashSet<>();

    public PEXPermissible(Player player, PermissionsExPlugin plugin) throws ExecutionException, InterruptedException {
        super(player);
        this.player = player;
        this.plugin = plugin;
        this.pex = plugin.getManager();
        this.subj = pex.getSubjects(SUBJECTS_USER).get(player.getUniqueId().toString()).get();
    }

    CalculatedSubject getPEXSubject() {
        return this.subj;
    }

    public PermissionsEx<BukkitConfiguration> getManager() {
        return this.pex;
    }

    @Override
    public boolean isOp() {
        return super.isOp(); // TODO: Implement op handling
    }

    @Override
    public void setOp(boolean value) {
        super.setOp(value);
    }

    @Override
    public boolean isPermissionSet(@NotNull String name) {
        Preconditions.checkNotNull(name, "name");
        name = name.toLowerCase();
        return getPermissionValue(subj.getActiveContexts(), name) != 0;
    }

    private int getPermissionValue(Set<ContextValue<?>> contexts, String permission) {
        int ret = getPermissionValue0(subj.getPermissions(contexts), permission);

        if (ret == 0) {
            for (Metapermission mPerm : METAPERMISSIONS) {
                Matcher match = mPerm.matchAgainst.matcher(permission);
                if (match.matches() && mPerm.isMatch(match, subj, contexts)) {
                    ret = 1;
                }
            }
        }

        /*
         * We may fall back to op value if no other data is set
         * This only takes into account the permission directly being checked having a default set to FALSE or NOT_OP, and not any parents.
         * I believe this is incorrect, but the real-world impacts are likely minor -zml
         */
        if (ret == 0 && getManager().getConfig().getPlatformConfig().shouldFallbackOp()) {
            Permission perm = plugin.getPermissionList().get(permission);
            if (perm == null) {
                ret = isOp() ? 1 : 0;

            } else if (perm.getDefault() != PermissionDefault.FALSE) {
                ret =  isOp() ^ perm.getDefault() == PermissionDefault.NOT_OP ? 1 : 0;
            }
        }

        if (pex.hasDebugMode()) {
            pex.getLogger().info(Messages.SUPERPERMS_CHECK_NOTIFY.toComponent(permission, player.getName(), contexts, ret));
        }
        return ret;
    }

    private int getPermissionValue0(NodeTree nodeTree, String name) {
        int val = nodeTree.get(name);
        if (val != 0) {
            return val;
        }

        for (Map.Entry<String, Boolean> ent : plugin.getPermissionList().getParents(name)) {
            val = getPermissionValue0(nodeTree, ent.getKey());
            if (!ent.getValue()) {
                val = -val;
            }
            if (val != 0) {
                return val;
            }
        }

        return 0;
    }

    @Override
    public boolean isPermissionSet(Permission perm) {
        Preconditions.checkNotNull(perm, "perm");
        return isPermissionSet(perm.getName());
    }

    @Override
    public boolean hasPermission(String inName) {
        Preconditions.checkNotNull(inName, "inName");
        inName = inName.toLowerCase();
        return getPermissionValue(subj.getActiveContexts(), inName) > 0;
    }

    @Override
    public boolean hasPermission(Permission perm) {
        Preconditions.checkNotNull(perm, "perm");
        return hasPermission(perm.getName());
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) {
        return super.addAttachment(plugin, name, value);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin) {
        final PEXPermissionAttachment attach = new PEXPermissionAttachment(plugin, player, this);
        this.subj.transientData().update(input -> input.addParent(PermissionsEx.GLOBAL_CONTEXT, PEXPermissionAttachment.ATTACHMENT_TYPE, attach.getIdentifier()))
                .thenRun(() -> this.attachments.add(attach));
        return attach;
    }

    public boolean removeAttachmentInternal(final PEXPermissionAttachment attach) {
        this.subj.transientData().update(input -> input.removeParent(PermissionsEx.GLOBAL_CONTEXT, PEXPermissionAttachment.ATTACHMENT_TYPE, attach.getIdentifier()))
                .thenRun(() -> {
                    PermissionRemovedExecutor exec = attach.getRemovalCallback();
                    if (exec != null) {
                        exec.attachmentRemoved(attach);
                    }
                });
        return true;
    }

    @Override
    public void removeAttachment(PermissionAttachment attachment) {
        if (!(attachment instanceof PEXPermissionAttachment)) {
            throw new IllegalArgumentException("Provided attachment was not a PEX attachment!");
        }
        removeAttachmentInternal(((PEXPermissionAttachment) attachment));
        this.attachments.remove(attachment);
    }

    void removeAllAttachments() {
        for (PEXPermissionAttachment attach : this.attachments) {
            removeAttachmentInternal(attach);
        }
        this.attachments.clear();
    }

    @Override
    public void recalculatePermissions() { // We don't need this currently? Guess could clear cache somehow, but automated should get it right. well, except for people adding children to permissions -- that is just weird
    }

    @Override
    public synchronized void clearPermissions() {
        // todo
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) {
        return super.addAttachment(plugin, name, value, ticks);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, int ticks) {
        final PEXPermissionAttachment attach = new PEXPermissionAttachment(plugin, player, this);
        this.subj.transientData().update(input -> input.addParent(ImmutableSet.of(BeforeTimeContextDefinition.INSTANCE.createValue(ZonedDateTime.now().plus(ticks * 50, ChronoUnit.MILLIS))), PEXPermissionAttachment.ATTACHMENT_TYPE, attach.getIdentifier()))
                .thenRun(() -> this.attachments.add(attach));
        return attach;
    }

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        ImmutableSet.Builder<PermissionAttachmentInfo> ret = ImmutableSet.builder();
        final Set<ContextValue<?>> activeContexts = subj.getActiveContexts();
        ret.addAll(Iterables.transform(subj.getPermissions(activeContexts).asMap().entrySet(),
                input -> new PermissionAttachmentInfo(player, input.getKey(), null, input.getValue() > 0)));
        for (Metapermission mPerm : METAPERMISSIONS) {
            ret.addAll(Iterators.transform(mPerm.getValues(this.subj, activeContexts), input -> new PermissionAttachmentInfo(player, input, null, true)));
        }
        return ret.build();
    }

    public void setPreviousPermissible(Permissible previousPermissible) {
        this.previousPermissible = previousPermissible;
    }

    public Permissible getPreviousPermissible() {
        return previousPermissible;
    }
}
