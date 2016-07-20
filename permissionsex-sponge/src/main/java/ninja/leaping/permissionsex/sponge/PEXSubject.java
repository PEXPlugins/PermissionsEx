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
import ninja.leaping.permissionsex.subject.CalculatedSubject;
import ninja.leaping.permissionsex.exception.PermissionsLoadingException;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import static ninja.leaping.permissionsex.sponge.PEXSubjectData.parSet;

/**
 * Permissions subject implementation
 */
@NonnullByDefault
class PEXSubject implements Subject {
    private final PEXSubjectCollection collection;
    private final PEXSubjectData data;
    private final PEXSubjectData transientData;
    private volatile CalculatedSubject baked;
    private final String identifier;
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

    public PEXSubject(String identifier, PEXSubjectCollection collection) throws ExecutionException, PermissionsLoadingException {
        this.identifier = identifier;
        this.collection = collection;
        this.baked = collection.getCalculatedSubject(identifier);
        this.data = new PEXSubjectData(baked.data(), collection.getPlugin());
        this.transientData = new PEXSubjectData(baked.transientData(), collection.getPlugin());
    }

    private Timings time() {
        return collection.getPlugin().getTimings();
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    private String identifyUser() {
        final Optional<CommandSource> source = getCommandSource();
        return getIdentifier() + (source.isPresent() ? "/" + source.get().getName() : "");
    }

    public CalculatedSubject getBaked() {
        return this.baked;
    }

    @Override
    public Optional<CommandSource> getCommandSource() {
        return getContainingCollection().getCommandSource(this.identifier);
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
            return baked.getOption(parSet(contexts), key);
        } finally {
            time().onGetOption().stopTimingIfSync();
        }
    }

    @Override
    public boolean hasPermission(Set<Context> contexts, String permission) {
        return getPermissionValue(contexts, permission).asBoolean();
    }

    @Override
    public Tristate getPermissionValue(Set<Context> contexts, String permission) {
        time().onGetPermission().startTimingIfSync();
        try {
            Preconditions.checkNotNull(contexts, "contexts");
            Preconditions.checkNotNull(permission, "permission");
            int ret = baked.getPermission(parSet(contexts), permission);
            return ret == 0 ? Tristate.UNDEFINED : ret > 0 ? Tristate.TRUE : Tristate.FALSE;
        } finally {
            time().onGetPermission().stopTimingIfSync();
        }
    }

    @Override
    public boolean isChildOf(Set<Context> contexts, Subject parent) {
        Preconditions.checkNotNull(contexts, "contexts");
        Preconditions.checkNotNull(parent, "parent");
        return getParents(contexts).contains(parent);
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
                Set<Context> set = new HashSet<>();
                for (ContextCalculator<Subject> calc : this.collection.getPlugin().getContextCalculators()) {
                    calc.accumulateContexts(this, set);
                }
                newHolder = new ActiveContextsHolder(ticks, ImmutableSet.copyOf(set));
            } while (!this.cachedContexts.compareAndSet(holder, newHolder));
            return newHolder.getContexts();
        } finally {
            time().onGetActiveContexts().stopTimingIfSync();
        }
    }

    @Override
    public List<Subject> getParents(final Set<Context> contexts) {
        time().onGetParents().startTimingIfSync();
        try {
            Preconditions.checkNotNull(contexts, "contexts");
            return Lists.transform(baked.getParents(parSet(contexts)), input -> collection.getPlugin().getSubjects(input.getKey()).get(input.getValue()));
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

        return this.identifier.equals(otherSubj.identifier)
                && this.data.equals(otherSubj.data);
    }
}
