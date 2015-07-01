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
package ninja.leaping.permissionsex.util;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.sk89q.squirrelid.Profile;
import com.sk89q.squirrelid.cache.ProfileCache;
import ninja.leaping.permissionsex.data.ImmutableOptionSubjectData;
import ninja.leaping.permissionsex.data.SubjectCache;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * A SquirrelID profile cache that uses PEX's data to get the stored names of users
 */
public class PEXProfileCache implements ProfileCache  {
    private final SubjectCache subjects;

    public PEXProfileCache(SubjectCache subjects) {
        this.subjects = subjects;
    }

    @Override
    public void put(Profile profile) {
        final String identifier = profile.getUniqueId().toString();
        try {
            this.subjects.update(identifier, this.subjects.getData(identifier, null).setOption(ImmutableSet.<Map.Entry<String, String>>of(), "name", profile.getName()));
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void putAll(Iterable<Profile> iterable) {
        for (Profile prof : iterable) {
            put(prof);
        }
    }

    @Nullable
    @Override
    public Profile getIfPresent(UUID uuid) {
        try {
            final ImmutableOptionSubjectData data = subjects.getData(uuid.toString(), null);
            final String name = data.getOptions(ImmutableSet.<Map.Entry<String, String>>of()).get("name");
            if (name == null) {
                return null;
            }
            return new Profile(uuid, name);
        } catch (ExecutionException e) {
            return null;
        }
    }

    @Override
    public ImmutableMap<UUID, Profile> getAllPresent(Iterable<UUID> iterable) {
        return Maps.toMap(iterable, new Function<UUID, Profile>() {
            @Nullable
            @Override
            public Profile apply(@Nullable UUID input) {
                return getIfPresent(input);
            }
        });
    }
}
