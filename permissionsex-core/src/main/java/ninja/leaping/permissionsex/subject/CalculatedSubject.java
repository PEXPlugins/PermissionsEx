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

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.data.Caching;
import ninja.leaping.permissionsex.data.ImmutableSubjectData;
import ninja.leaping.permissionsex.data.SubjectDataReference;
import ninja.leaping.permissionsex.data.SubjectRef;
import ninja.leaping.permissionsex.util.NodeTree;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.util.Map.Entry;

/**
 * This is a holder that maintains the current subject data state
 */
public class CalculatedSubject implements Caching<ImmutableSubjectData> {
    private final SubjectDataBaker baker;
    private final SubjectRef identifier;
    private final SubjectType type;
    private SubjectDataReference ref, transientRef;

    private final AsyncLoadingCache<Set<Entry<String, String>>, BakedSubjectData> data;

    CalculatedSubject(SubjectDataBaker baker, SubjectRef identifier, SubjectType type) {
        this.baker = Preconditions.checkNotNull(baker, "baker");
        this.identifier = Preconditions.checkNotNull(identifier, "identifier");
        this.type = Preconditions.checkNotNull(type, "type");
        this.data = Caffeine.newBuilder()
                .maximumSize(32)
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .buildAsync(((key, executor) -> this.baker.bake(CalculatedSubject.this, key)));
    }

    void initialize(SubjectDataReference persistentRef, SubjectDataReference transientRef) {
        this.ref = persistentRef;
        this.transientRef = transientRef;
    }

    public SubjectRef getIdentifier() {
        return identifier;
    }

    PermissionsEx getManager() {
        return this.type.getManager();
    }

    private BakedSubjectData getData(Set<Entry<String, String>> contexts) {
        Preconditions.checkNotNull(contexts, "contexts");
        return data.synchronous().get(ImmutableSet.copyOf(contexts));
    }

    public NodeTree getPermissions(Set<Map.Entry<String, String>> contexts) {
        return getData(contexts).getPermissions();
    }

    public Map<String, String> getOptions(Set<Map.Entry<String, String>> contexts) {
        return getData(contexts).getOptions();
    }

    public List<SubjectRef> getParents(Set<Map.Entry<String, String>> contexts) {
        List<SubjectRef> parents = getData(contexts).getParents();
        getManager().getNotifier().onParentCheck(getIdentifier(), contexts, parents);
        return parents;
    }

    private Set<Set<Map.Entry<String, String>>> getActiveContexts() {
        return data.synchronous().asMap().keySet();
    }

    public int getPermission(Set<Entry<String, String>> contexts, String permission) {
        int ret = getPermissions(contexts).get(Preconditions.checkNotNull(permission, "permission"));
        getManager().getNotifier().onPermissionCheck(getIdentifier(), contexts, permission, ret);
        return ret;
    }

    public Optional<String> getOption(Set<Entry<String, String>> contexts, String option) {
        String val = getOptions(contexts).get(Preconditions.checkNotNull(option, "option"));
        getManager().getNotifier().onOptionCheck(getIdentifier(), contexts, option, val);
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
        data.synchronous().invalidateAll();
        getManager().getActiveSubjectTypes().stream()
                .flatMap(type -> type.getActiveSubjects().stream())
                .filter(subj -> {
                    for (Set<Map.Entry<String, String>> ent : subj.getActiveContexts()) {
                        if (subj.getParents(ent).contains(this.identifier)) {
                            return true;
                        }
                    }
                    return false;
                })
                .forEach(subj -> subj.data.synchronous().invalidateAll());
    }

    public SubjectType getType() {
        return this.type;
    }
}
