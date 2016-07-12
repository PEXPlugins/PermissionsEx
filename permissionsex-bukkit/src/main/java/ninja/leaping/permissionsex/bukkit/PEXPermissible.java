/**
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
package ninja.leaping.permissionsex.bukkit;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.data.SubjectRef;
import ninja.leaping.permissionsex.subject.CalculatedSubject;
import ninja.leaping.permissionsex.util.NodeTree;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.permissions.PermissionRemovedExecutor;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import static ninja.leaping.permissionsex.PermissionsEx.SUBJECTS_GROUP;
import static ninja.leaping.permissionsex.PermissionsEx.SUBJECTS_USER;
import static ninja.leaping.permissionsex.bukkit.BukkitTranslations.t;

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
                public boolean isMatch(Matcher result, CalculatedSubject subj, Set<Map.Entry<String, String>> contexts) {
                    return subj.getParents(contexts).contains(Maps.immutableEntry(SUBJECTS_GROUP, result.group("name")));
                }

                @Override
                public Iterator<String> getValues(CalculatedSubject subj, Set<Map.Entry<String, String>> contexts) {
                    return subj.getParents(contexts).stream()
                            .filter(ent -> ent.getType().equals(SUBJECTS_GROUP))
                            .flatMap(ent -> StreamSupport.<String>stream(Spliterators.spliterator(new String[]{"group." + ent.getIdentifier(), "groups." + ent.getIdentifier()}, Spliterator.IMMUTABLE | Spliterator.DISTINCT), false))
                            .iterator();
                }
            },
            new Metapermission(Pattern.compile("options\\.(?<key>.*)\\.(?<value>.*)")) {
                @Override
                public boolean isMatch(Matcher result, CalculatedSubject subj, Set<Map.Entry<String, String>> contexts) {
                    return subj.getOption(contexts, result.group("key")).map(val -> val.equals(result.group("value"))).orElse(false);
                }

                @Override
                public Iterator<String> getValues(CalculatedSubject subj, Set<Map.Entry<String, String>> contexts) {
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

        public abstract boolean isMatch(Matcher result, CalculatedSubject subj, Set<Map.Entry<String, String>> contexts);

        public abstract Iterator<String> getValues(CalculatedSubject subj, Set<Map.Entry<String, String>> contexts);
    }

    private static class SpecificOptionMetapermission extends Metapermission {
        private final String option;
        public SpecificOptionMetapermission(String option) {
            super(Pattern.compile(Pattern.quote(option) + "\\.(?<value>.+)"));
            this.option = option;
        }

        @Override
        public boolean isMatch(Matcher result, CalculatedSubject subj, Set<Map.Entry<String, String>> contexts) {
            return subj.getOption(contexts, option).map(val -> val.equals(result.group("value"))).orElse(false);
        }

        @Override
        public Iterator<String> getValues(CalculatedSubject subj, Set<Map.Entry<String, String>> contexts) {
            String ret = subj.getOptions(contexts).get(option);
            return ret == null ? Iterators.emptyIterator() : Iterators.singletonIterator(this.option + "." + ret);
        }
    }
    private final Player player;
    private final PermissionsExPlugin plugin;
    private PermissionsEx pex;
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

    public PermissionsEx getManager() {
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
    public boolean isPermissionSet(String name) {
        Preconditions.checkNotNull(name, "name");
        name = name.toLowerCase();
        return getPermissionValue(getActiveContexts(), name) != 0;
    }

    private int getPermissionValue(Set<Map.Entry<String, String>> contexts, String permission) {
        int ret = getPermissionValue0(subj.getPermissions(contexts), permission);

        if (ret == 0) {
            for (Metapermission mPerm : METAPERMISSIONS) {
                Matcher match = mPerm.matchAgainst.matcher(permission);
                if (match.matches() && mPerm.isMatch(match, subj, contexts)) {
                    ret = 1;
                }
            }
        }

        if (pex.hasDebugMode()) {
            pex.getLogger().info(t("Checked permission %s for player %s in contexts %s: %s", permission, player.getName(), contexts, ret));
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
        return getPermissionValue(getActiveContexts(), inName) > 0;
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
        this.subj.transientData().updateSegment(PermissionsEx.GLOBAL_CONTEXT, seg -> seg.withAddedParent(SubjectRef.of(PEXPermissionAttachment.ATTACHMENT_TYPE, attach.getIdentifier())))
                .thenRun(() -> this.attachments.add(attach));
        return attach;
    }

    public boolean removeAttachmentInternal(final PEXPermissionAttachment attach) {
        this.subj.transientData().updateSegment(PermissionsEx.GLOBAL_CONTEXT, seg -> seg.withRemovedParent(SubjectRef.of(PEXPermissionAttachment.ATTACHMENT_TYPE, attach.getIdentifier())))
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
        return addAttachment(plugin); // TODO: Implement timed permissions
    }

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        ImmutableSet.Builder<PermissionAttachmentInfo> ret = ImmutableSet.builder();
        final Set<Map.Entry<String, String>> activeContexts = getActiveContexts();
        ret.addAll(Iterables.transform(subj.getPermissions(activeContexts).asMap().entrySet(),
                input -> new PermissionAttachmentInfo(player, input.getKey(), null, input.getValue() > 0)));
        for (Metapermission mPerm : METAPERMISSIONS) {
            ret.addAll(Iterators.transform(mPerm.getValues(this.subj, activeContexts), input -> new PermissionAttachmentInfo(player, input, null, true)));
        }
        return ret.build();
    }

    public Set<Map.Entry<String, String>> getActiveContexts() {
        ImmutableSet.Builder<Map.Entry<String, String>> builder = ImmutableSet.builder();
        builder.add(Maps.immutableEntry("world", player.getWorld().getName()));
        builder.add(Maps.immutableEntry("dimension", player.getWorld().getEnvironment().name().toLowerCase()));
        for (String serverTag : plugin.getManager().getConfig().getServerTags()) {
            builder.add(Maps.immutableEntry(PermissionsExPlugin.SERVER_TAG_CONTEXT, serverTag));
        }
        return builder.build();
    }

    public void setPreviousPermissible(Permissible previousPermissible) {
        this.previousPermissible = previousPermissible;
    }

    public Permissible getPreviousPermissible() {
        return previousPermissible;
    }
}
