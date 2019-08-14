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
package ninja.leaping.permissionsex.sponge;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.context.ContextValue;
import ninja.leaping.permissionsex.subject.CalculatedSubject;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectReference;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static ninja.leaping.permissionsex.sponge.PEXSubjectData.contextsPexToSponge;
import static ninja.leaping.permissionsex.sponge.PEXSubjectData.contextsSpongeToPex;

/**
 * Permissions subject implementation
 */
@NonnullByDefault
class PEXSubject implements Subject {
    private final PEXSubjectCollection collection;
    private final PEXSubjectData data;
    private final PEXSubjectData transientData;
    private volatile CalculatedSubject baked;
    private final SubjectReference ref;
    private final AtomicReference<ActiveContextsHolder> cachedContexts = new AtomicReference<>();

    private static class ActiveContextsHolder {
        private final int updateTicks;
        private final Set<Context> contexts;

        private ActiveContextsHolder(int updateTicks, Set<Context> contexts) {
            this.updateTicks = updateTicks;
            this.contexts = contexts;
        }

        public int getUpdateTicks() {
            return updateTicks;
        }

        public Set<Context> getContexts() {
            return contexts;
        }
    }

    public PEXSubject(CalculatedSubject baked, PEXSubjectCollection collection) {
        this.ref = (SubjectReference) baked.getIdentifier();
        this.collection = collection;
        this.baked = baked;
        this.data = new PEXSubjectData(baked.data(), collection.getPlugin());
        this.transientData = new PEXSubjectData(baked.transientData(), collection.getPlugin());
    }

    public static CompletableFuture<PEXSubject> load(String identifier, PEXSubjectCollection collection) {
        return collection.getCalculatedSubject(identifier).thenApply(baked -> new PEXSubject(baked, collection));
    }

    private Timings time() {
        return collection.getPlugin().getTimings();
    }

    @Override
    public String getIdentifier() {
        return this.ref.getSubjectIdentifier();
    }

    @Override
    public Optional<String> getFriendlyIdentifier() {
        return Optional.empty();
    }

    public PermissionsEx getManager() {
        return this.collection.getPlugin().getManager();
    }

    @Override
    public SubjectReference asSubjectReference() {
        return this.ref;
    }

    @Override
    public boolean isSubjectDataPersisted() {
        return true;
	}

    public CalculatedSubject getBaked() {
        return this.baked;
    }

    @Override
    public Optional<CommandSource> getCommandSource() {
        return this.baked.getAssociatedObject().filter(obj -> obj instanceof CommandSource).map(x -> (CommandSource) x);
    }

    @Override
    public PEXSubjectCollection getContainingCollection() {
        return this.collection;
    }

    @Override
    public PEXSubjectData getSubjectData() {
        return data;
    }

    @Override
    public PEXSubjectData getTransientSubjectData() {
        return transientData;
    }

    @Override
    public Optional<String> getOption(Set<Context> contexts, String key) {
        time().onGetOption().startTimingIfSync();
        try {
            Preconditions.checkNotNull(contexts, "contexts");
            Preconditions.checkNotNull(key, "key");
            return baked.getOption(contextsSpongeToPex(contexts, getManager()), key);
        } finally {
            time().onGetOption().stopTimingIfSync();
        }
    }

    @Override
    public Optional<String> getOption(String key) {
        return getBaked().getOption(getActivePexContexts(), key);
    }

    @Override
    public boolean hasPermission(Set<Context> contexts, String permission) {
        return getPermission(contexts, permission)> 0;
    }

    @Override
    public boolean hasPermission(String permission) {
        return getBaked().getPermission(getActivePexContexts(), permission) > 0;
    }

    @Override
    public Tristate getPermissionValue(Set<Context> contexts, String permission) {
        return Tristate.fromInt(getPermission(contexts, permission));
    }

    @Override
    public int getPermission(Set<Context> contexts, String permission) {
        time().onGetPermission().startTimingIfSync();
        try {
            Preconditions.checkNotNull(contexts, "contexts");
            Preconditions.checkNotNull(permission, "permission");
            return baked.getPermission(contextsSpongeToPex(contexts, getManager()), permission);
        } finally {
            time().onGetPermission().stopTimingIfSync();
        }
    }

    @Override
    public boolean isChildOf(SubjectReference parent) {
        return getBaked().getParents().contains(PEXSubjectReference.of(parent, getContainingCollection().getPlugin()));
    }

    @Override
    public boolean isChildOf(Set<Context> contexts, SubjectReference parent) {
        Preconditions.checkNotNull(contexts, "contexts");
        Preconditions.checkNotNull(parent, "parent");
        return getParents(contexts).contains(parent);
    }

    @Override
    public List<SubjectReference> getParents() {
        return Lists.transform(getBaked().getParents(), input -> PEXSubjectReference.of(input, getContainingCollection().getPlugin()));
    }

    public Set<ContextValue<?>> getActivePexContexts() {
        time().onGetActiveContexts().startTimingIfSync();
        try {
            /*int ticks;
            ActiveContextsHolder holder, newHolder;
            do {
                ticks = Sponge.getGame().getServer().getRunningTimeTicks();
                holder = this.cachedContexts.get();
                if (holder != null && ticks == holder.getUpdateTicks()) {
                    return holder.getContexts();
                }*/
                Set<Context> spongeContexts = new HashSet<>();
                for (ContextCalculator<Subject> spongeCalc : this.collection.getPlugin().getContextCalculators()) {
                    spongeCalc.accumulateContexts(this, spongeContexts);
                }
                Set<ContextValue<?>> pexContexts = baked.getActiveContexts();
                pexContexts.addAll(contextsSpongeToPex(spongeContexts, getManager()));
                //newHolder = new ActiveContextsHolder(ticks, ImmutableSet.copyOf(contextsPexToSponge(getBaked().getActiveContexts())));
            /*} while (!this.cachedContexts.compareAndSet(holder, newHolder));
            return newHolder.getContexts();*/
            return pexContexts;
        } finally {
            time().onGetActiveContexts().stopTimingIfSync();
        }

    }

    @Override
    public Set<Context> getActiveContexts() {
        time().onGetActiveContexts().startTimingIfSync();
        try {
            int ticks;
            ActiveContextsHolder holder, newHolder;
            do {
                ticks = Sponge.getGame().getServer().getRunningTimeTicks();
                holder = this.cachedContexts.get();
                if (holder != null && ticks == holder.getUpdateTicks()) {
                    return holder.getContexts();
                }
                Set<Context> spongeContexts = contextsPexToSponge(getBaked().getActiveContexts());
                for (ContextCalculator<Subject> spongeCalc : this.collection.getPlugin().getContextCalculators()) {
                    spongeCalc.accumulateContexts(this, spongeContexts);
                }
                newHolder = new ActiveContextsHolder(ticks, ImmutableSet.copyOf(contextsPexToSponge(getBaked().getActiveContexts())));
            } while (!this.cachedContexts.compareAndSet(holder, newHolder));
            return newHolder.getContexts();
        } finally {
            time().onGetActiveContexts().stopTimingIfSync();
        }
    }

    @Override
    public List<SubjectReference> getParents(final Set<Context> contexts) {
        time().onGetParents().startTimingIfSync();
        try {
            Preconditions.checkNotNull(contexts, "contexts");
            return Lists.transform(baked.getParents(contextsSpongeToPex(contexts, getManager())), input -> PEXSubjectReference.of(input, getContainingCollection().getPlugin()));
        } finally {
            time().onGetParents().stopTimingIfSync();
        }
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof PEXSubject)) {
            return false;
        }

        PEXSubject otherSubj = (PEXSubject) other;

        return this.ref.equals(otherSubj.ref)
                && this.data.equals(otherSubj.data);
    }
}
