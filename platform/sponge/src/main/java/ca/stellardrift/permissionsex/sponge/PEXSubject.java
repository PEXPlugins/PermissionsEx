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

package ca.stellardrift.permissionsex.sponge;

import ca.stellardrift.permissionsex.PermissionsEx;
import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import ca.stellardrift.permissionsex.util.CachingValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectReference;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

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
    private final CachingValue<ActiveContextsHolder> activeContexts;

    private static class ActiveContextsHolder {
        private final Set<Context> spongeContexts;
        private final Set<ContextValue<?>> pexContexts;

        private ActiveContextsHolder(Set<Context> spongeContexts, Set<ContextValue<?>> pexContexts) {
            this.spongeContexts = spongeContexts;
            this.pexContexts = pexContexts;
        }

        public Set<Context> getSpongeContexts() {
            return spongeContexts;
        }

        public Set<ContextValue<?>> getPexContexts() {
            return pexContexts;
        }
    }

    public PEXSubject(CalculatedSubject baked, PEXSubjectCollection collection) {
        this.ref = (SubjectReference) baked.getIdentifier();
        this.collection = collection;
        this.baked = baked;
        this.data = new PEXSubjectData(baked.data(), collection.getPlugin());
        this.transientData = new PEXSubjectData(baked.transientData(), collection.getPlugin());
        this.activeContexts = collection.getPlugin().tickBasedCachingValue(1L, () -> {
            time().onGetActiveContexts().startTimingIfSync();
            try {
                Set<ContextValue<?>> pexContexts = baked.getActiveContexts();
                Set<Context> spongeContexts = PEXSubjectData.contextsPexToSponge(pexContexts);

                Set<Context> spongeContextsAccum = new HashSet<>();
                for (ContextCalculator<Subject> spongeCalc : this.collection.getPlugin().getContextCalculators()) {
                    spongeCalc.accumulateContexts(this, spongeContextsAccum);
                }

                spongeContexts.addAll(spongeContextsAccum);
                pexContexts.addAll(PEXSubjectData.contextsSpongeToPex(spongeContextsAccum, getManager()));

                return new ActiveContextsHolder(spongeContexts, pexContexts);
            } finally {
                time().onGetActiveContexts().stopTimingIfSync();
            }
        });
    }

    static CompletableFuture<PEXSubject> load(String identifier, PEXSubjectCollection collection) {
        return collection.getCalculatedSubject(identifier).thenApply(baked -> {
            return new PEXSubject(baked, collection);
        });
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

    public PermissionsEx<?> getManager() {
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
        final Object associated = this.baked.getAssociatedObject();
        return associated instanceof CommandSource ? Optional.of((CommandSource) associated) : Optional.empty();
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
            Objects.requireNonNull(contexts, "contexts");
            Objects.requireNonNull(key, "key");
            return baked.getOption(PEXSubjectData.contextsSpongeToPex(contexts, getManager()), key);
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
        return getPermissionValue(contexts, permission).asBoolean();
    }

    @Override
    public boolean hasPermission(String permission) {
        return getBaked().getPermission(getActivePexContexts(), permission) > 0;
    }

    @Override
    public Tristate getPermissionValue(Set<Context> contexts, String permission) {
        time().onGetPermission().startTimingIfSync();
        try {
            Preconditions.checkNotNull(contexts, "contexts");
            Preconditions.checkNotNull(permission, "permission");
            int ret = baked.getPermission(PEXSubjectData.contextsSpongeToPex(contexts, getManager()), permission);
            return ret == 0 ? Tristate.UNDEFINED : ret > 0 ? Tristate.TRUE : Tristate.FALSE;
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
        return activeContexts.get().getPexContexts();
    }

    @Override
    public Set<Context> getActiveContexts() {
        return activeContexts.get().getSpongeContexts();
    }

    @Override
    public List<SubjectReference> getParents(final Set<Context> contexts) {
        time().onGetParents().startTimingIfSync();
        try {
            Preconditions.checkNotNull(contexts, "contexts");
            return Lists.transform(baked.getParents(PEXSubjectData.contextsSpongeToPex(contexts, getManager())), input -> PEXSubjectReference.of(input, getContainingCollection().getPlugin()));
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

    @Override
    public int hashCode() {
        return Objects.hash(data, ref);
    }
}
