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
import com.google.common.base.Supplier;
import ninja.leaping.permissionsex.data.SubjectCache;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.context.Context;
import org.spongepowered.api.util.command.CommandSource;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Subject collection
 */
public class PEXSubjectCollection implements SubjectCollection {
    private final PermissionsExPlugin plugin;
    private final SubjectCache cache;
    private volatile Function<String, Optional<CommandSource>> commandSourceProvider;

    public PEXSubjectCollection(PermissionsExPlugin plugin, SubjectCache cache) {
        this.plugin = plugin;
        this.cache = cache;
    }

    @Override
    public String getIdentifier() {
        return cache.getType();
    }

    @Override
    public Subject get(String identifier) {
        System.out.println("Getting subject for " + identifier);
        try {
            return new PEXSubject(identifier, new PEXOptionSubjectData(cache, identifier), plugin);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean hasRegistered(String identifier) {
        return cache.isRegistered(identifier);
    }

    @Override
    public Iterable<Subject> getAllSubjects() {
        return Collections.emptySet();
    }

    @Override
    public Map<Subject, Boolean> getAllWithPermission(String permission) {
        return null;
    }

    @Override
    public Map<Subject, Boolean> getAllWithPermission(Set<Context> contexts, String permission) {
        return null;
    }

    public Optional<CommandSource> getCommandSource(String identifier) {
        if (commandSourceProvider != null) {
            return commandSourceProvider.apply(identifier);
        } else {
            return Optional.absent();
        }
    }

    public void setCommandSourceProvider(Function<String, Optional<CommandSource>> provider) {
        this.commandSourceProvider = provider;
    }
}
