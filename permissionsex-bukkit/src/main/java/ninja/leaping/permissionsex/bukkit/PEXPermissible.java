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

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.data.CalculatedSubject;
import ninja.leaping.permissionsex.data.ImmutableOptionSubjectData;
import ninja.leaping.permissionsex.data.SubjectCache;
import ninja.leaping.permissionsex.exception.PermissionsLoadingException;
import ninja.leaping.permissionsex.util.NodeTree;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.permissions.PermissionRemovedExecutor;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of Permissible using PEX for data
 */
public class PEXPermissible extends PermissibleBase {
    private final Player player;
    private final PermissionsExPlugin plugin;
    private PermissionsEx pex;
    private SubjectCache cache;
    private CalculatedSubject subj;
    private Permissible previousPermissible;
    private final Set<PEXPermissionAttachment> attachments = new HashSet<>();

    public PEXPermissible(Player player, PermissionsExPlugin pex) {
        super(player);
        this.player = player;
        this.plugin = pex;
        try {
            update(pex.getManager());
        } catch (PermissionsLoadingException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public PermissionsEx getManager() {
        return this.pex;
    }

    public void update(PermissionsEx newManager) throws PermissionsLoadingException {
        this.pex = newManager;
        this.cache = newManager.getTransientSubjects("user");
        this.subj = pex.getCalculatedSubject("user", player.getUniqueId().toString());
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
        pex.getLogger().info("Checked permission {} for player {} in contexts {}: {}", permission, player.getName(), contexts, ret);
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
        Futures.addCallback(this.cache.doUpdate(player.getUniqueId().toString(), new Function<ImmutableOptionSubjectData, ImmutableOptionSubjectData>() {
            @Nullable
            @Override
            public ImmutableOptionSubjectData apply(@Nullable ImmutableOptionSubjectData input) {
                return input.addParent(PermissionsExPlugin.GLOBAL_CONTEXT, PEXPermissionAttachment.ATTACHMENT_TYPE, attach.getIdentifier());
            }
        }), new FutureCallback<ImmutableOptionSubjectData>() {
            @Override
            public void onSuccess(@Nullable ImmutableOptionSubjectData result) {
                PEXPermissible.this.attachments.add(attach);
            }

            @Override
            public void onFailure(Throwable t) {

            }
        });
        return attach;
    }

    public boolean removeAttachmentInternal(final PEXPermissionAttachment attach) {
        Futures.addCallback(this.cache.doUpdate(player.getUniqueId().toString(), new Function<ImmutableOptionSubjectData, ImmutableOptionSubjectData>() {
            @Nullable
            @Override
            public ImmutableOptionSubjectData apply(@Nullable ImmutableOptionSubjectData input) {
                return input.removeParent(PermissionsExPlugin.GLOBAL_CONTEXT, PEXPermissionAttachment.ATTACHMENT_TYPE, attach.getIdentifier());
            }
        }), new FutureCallback<ImmutableOptionSubjectData>() {
            @Override
            public void onSuccess(@Nullable ImmutableOptionSubjectData result) {
                PermissionRemovedExecutor exec = attach.getRemovalCallback();
                if (exec != null) {
                    exec.attachmentRemoved(attach);
                }
            }

            @Override
            public void onFailure(Throwable t) {

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
        return ImmutableSet.copyOf(Iterables.transform(subj.getPermissions(getActiveContexts()).asMap().entrySet(), new Function<Map.Entry<String, Integer>, PermissionAttachmentInfo>() {
            @Nullable
            @Override
            public PermissionAttachmentInfo apply(Map.Entry<String, Integer> input) {
                return new PermissionAttachmentInfo(player, input.getKey(), null, input.getValue() > 0);
            }
        }));
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
