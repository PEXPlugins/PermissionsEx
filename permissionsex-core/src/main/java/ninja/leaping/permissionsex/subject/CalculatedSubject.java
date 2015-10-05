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
package ninja.leaping.permissionsex.subject;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.data.Caching;
import ninja.leaping.permissionsex.data.ImmutableSubjectData;
import ninja.leaping.permissionsex.data.SubjectDataReference;
import ninja.leaping.permissionsex.util.NodeTree;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static java.util.Map.Entry;

/**
 * This is a holder that maintains the current subject data state
 */
public class CalculatedSubject implements Caching<ImmutableSubjectData> {
    private final SubjectDataBaker baker;
    private final Map.Entry<String, String> identifier;
    private final PermissionsEx pex;
    private final SubjectDataReference ref, transientRef;

    private final LoadingCache<Set<Map.Entry<String, String>>, BakedSubjectData> data = CacheBuilder.newBuilder().maximumSize(5)
            .build(new CacheLoader<Set<Map.Entry<String, String>>, BakedSubjectData>() {
        @Override
        public BakedSubjectData load(Set<Map.Entry<String, String>> contexts) throws Exception {
            return baker.bake(CalculatedSubject.this, contexts);
        }
    });

    public CalculatedSubject(SubjectDataBaker baker, Map.Entry<String, String> identifier, PermissionsEx pex) throws ExecutionException {
        this.baker = Preconditions.checkNotNull(baker, "baker");
        this.identifier = Preconditions.checkNotNull(identifier, "identifier");
        this.pex = Preconditions.checkNotNull(pex, "pex");
        this.ref = SubjectDataReference.forSubject(identifier.getValue(), pex.getSubjects(identifier.getKey()));
        this.transientRef = SubjectDataReference.forSubject(identifier.getValue(), pex.getTransientSubjects(identifier.getKey()));
    }

    public Map.Entry<String, String> getIdentifier() {
        return identifier;
    }

    private String stringIdentifier() {
        return this.identifier.getKey() + " " + this.identifier.getValue();
    }

    PermissionsEx getManager() {
        return pex;
    }

    public NodeTree getPermissions(Set<Map.Entry<String, String>> contexts) {
        Preconditions.checkNotNull(contexts, "contexts");
        try {
            return data.get(contexts).getPermissions();
        } catch (ExecutionException e) {
            return NodeTree.of(Collections.<String, Integer>emptyMap());
        }
    }

    public Map<String, String> getOptions(Set<Map.Entry<String, String>> contexts) {
        Preconditions.checkNotNull(contexts, "contexts");
        try {
            return data.get(contexts).getOptions();
        } catch (ExecutionException e) {
            return ImmutableMap.of();
        }
    }

    public List<Map.Entry<String, String>> getParents(Set<Map.Entry<String, String>> contexts) {
        Preconditions.checkNotNull(contexts, "contexts");
        try {
            List<Map.Entry<String, String>> parents = data.get(contexts).getParents();
            if (pex.hasDebugMode()) {
                pex.getLogger().info("Parents checked in " + contexts + " for " +  stringIdentifier() + ": " + parents);
            }
            return parents;
        } catch (ExecutionException e) {
            return ImmutableList.of();
        }
    }

    private Set<Set<Map.Entry<String, String>>> getActiveContexts() {
        return data.asMap().keySet();
    }

    public int getPermission(Set<Entry<String, String>> contexts, String permission) {
        int ret = getPermissions(contexts).get(Preconditions.checkNotNull(permission, "permission"));
        if (pex.hasDebugMode()) {
            pex.getLogger().info("Permission " + permission + " checked in " + contexts + " for " + stringIdentifier() + ": " + ret);
        }
        return ret;
    }

    public Optional<String> getOption(Set<Entry<String, String>> contexts, String option) {
        String val = getOptions(contexts).get(Preconditions.checkNotNull(option, "option"));
        if (pex.hasDebugMode()) {
            pex.getLogger().info("Option " + option + " checked in " + contexts + " for " + stringIdentifier() + ": " + val);
        }
        return Optional.ofNullable(val);
    }

    public SubjectDataReference data() {
        return this.ref;
    }

    public SubjectDataReference transientData() {
        return this.transientRef;
    }

    @Override
    public void clearCache(ImmutableSubjectData newData) {
        data.invalidateAll();
        for (CalculatedSubject subject : pex.getActiveCalculatedSubjects()) {
            for (Set<Map.Entry<String, String>> ent : subject.getActiveContexts()) {
                if (subject.getParents(ent).contains(this.identifier)) {
                    subject.data.invalidateAll();
                    break;
                }
            }
        }
    }
}
