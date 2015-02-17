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

import ninja.leaping.permissionsex.backends.DataStore;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.context.Context;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Subject collection
 */
public class PEXSubjectCollection implements SubjectCollection {
    private final DataStore data;
    private final String type;

    public PEXSubjectCollection(DataStore data, String type) {
        this.data = data;
        this.type = type;
    }

    @Override
    public String getIdentifier() {
        return this.type;
    }

    @Override
    public Subject get(String identifier) {
        return null;
    }

    @Override
    public boolean hasRegistered(String identifier) {
        return data.isRegistered(type, identifier);
    }

    @Override
    public Iterable<Subject> getAllSubjects() {
        return Collections.emptyList(); //data.getAll(type);
    }

    @Override
    public Map<Subject, Boolean> getAllWithPermission(String permission) {
        return null;
    }

    @Override
    public Map<Subject, Boolean> getAllWithPermission(Set<Context> contexts, String permission) {
        return null;
    }
}
