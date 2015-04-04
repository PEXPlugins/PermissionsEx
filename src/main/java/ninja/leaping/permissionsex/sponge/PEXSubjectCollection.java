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

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.data.CalculatedSubject;
import ninja.leaping.permissionsex.data.SubjectCache;
import ninja.leaping.permissionsex.exception.PermissionsLoadingException;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.context.Context;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.util.command.CommandSource;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Subject collection
 */
public class PEXSubjectCollection implements SubjectCollection {
    private final String identifier;
    private final PermissionsExPlugin plugin;
    private volatile SubjectCache cache, transientCache;

    private final LoadingCache<String, PEXSubject> subjectCache = CacheBuilder.newBuilder().build(new CacheLoader<String, PEXSubject>() {
        @Override
        public PEXSubject load(String identifier) throws Exception {
            return new PEXSubject(identifier, PEXSubjectCollection.this);
        }
    });

    public PEXSubjectCollection(final String identifier, final PermissionsExPlugin plugin) throws ExecutionException, PermissionsLoadingException {
        this.identifier = identifier;
        this.plugin = plugin;
        updateCaches();
    }

    SubjectCache getCache() {
        return cache;
    }

    SubjectCache getTransientCache() {
        return transientCache;
    }

    void updateCaches() throws PermissionsLoadingException, ExecutionException {
        this.cache = plugin.getManager().getSubjects(identifier);
        this.transientCache = plugin.getManager().getTransientSubjects(identifier);
        for (Map.Entry<String, PEXSubject> subject : subjectCache.asMap().entrySet()) {
            subject.getValue().update(getCalculatedSubject(subject.getKey()), getCache(), getTransientCache());
        }
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    PermissionsExPlugin getPlugin() {
        return this.plugin;
    }

    @Override
    public PEXSubject get(String identifier) {
        try {
            return subjectCache.get(identifier);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public void uncache(String identifier) {
        subjectCache.invalidate(identifier);
        PermissionsEx manager = plugin.getManager();
        if (manager != null) {
            manager.uncache(getIdentifier(), identifier);
        }
    }

    @Override
    public boolean hasRegistered(String identifier) {
        return cache.isRegistered(identifier);
    }

    @Override
    public Iterable<Subject> getAllSubjects() {
        return Iterables.transform(cache.getAllIdentifiers(), new Function<String, Subject>() {
            @Nullable
            @Override
            public Subject apply(String s) {
                return get(s);
            }
        });
    }

    @Override
    public Map<Subject, Boolean> getAllWithPermission(String permission) {
        return getAllWithPermission(null, permission);
    }

    @Override
    public Map<Subject, Boolean> getAllWithPermission(Set<Context> contexts, String permission) {
        final ImmutableMap.Builder<Subject, Boolean> ret = ImmutableMap.builder();
        for (PEXSubject subject : subjectCache.asMap().values()) {
                Tristate permissionValue = subject.getPermissionValue(contexts == null ? subject.getActiveContexts() : contexts, permission);
                if (permissionValue != Tristate.UNDEFINED) {
                    ret.put(subject, permissionValue.asBoolean());
                }
        }
        return ret.build();
    }

    public Optional<CommandSource> getCommandSource(String identifier) {
        final Function<String, Optional<CommandSource>> provider = plugin.getCommandSourceProvider(getIdentifier());
        if (provider != null) {
            return provider.apply(identifier);
        } else {
            return Optional.absent();
        }
    }

    Iterable<PEXSubject> getActiveSubjects() {
        return subjectCache.asMap().values();
    }

    public CalculatedSubject getCalculatedSubject(String identifier) throws PermissionsLoadingException {
        return plugin.getManager().getCalculatedSubject(this.identifier, identifier);
    }
}
