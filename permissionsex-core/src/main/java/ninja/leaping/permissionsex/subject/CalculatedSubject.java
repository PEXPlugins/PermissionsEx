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
    private final Map.Entry<String, String> identifier;
    private final SubjectType type;
    private SubjectDataReference ref, transientRef;

    private final AsyncLoadingCache<Set<Entry<String, String>>, BakedSubjectData> data;

    CalculatedSubject(SubjectDataBaker baker, Map.Entry<String, String> identifier, SubjectType type) {
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

    /**
     * Get the identifier for this subject, as a map entry where the key is the identifier for this subject's type
     * and the value is the specific identifier for this subject.
     *
     * @return The identifier
     */
    public Map.Entry<String, String> getIdentifier() {
        return identifier;
    }

    /**
     * Get the subject type holding this calculated subject
     * @return The subject type
     */
    public SubjectType getType() {
        return this.type;
    }

    PermissionsEx getManager() {
        return this.type.getManager();
    }

    /**
     * Get the calculated data for a specific context set
     *
     * @param contexts The contexts to get data in. These will be processed for combinations
     * @return The baked subject data
     */
    private BakedSubjectData getData(Set<Entry<String, String>> contexts) {
        Preconditions.checkNotNull(contexts, "contexts");
        return data.synchronous().get(ImmutableSet.copyOf(contexts));
    }

    /**
     * Get the permissions tree for a certain set of contexts
     *
     * @param contexts The contexts to get permissions in
     * @return A node tree with the calculated permissions
     */
    public NodeTree getPermissions(Set<Map.Entry<String, String>> contexts) {
        return getData(contexts).getPermissions();
    }

    /**
     * Get all options for a certain set of contexts. Options are plain strings and therefore do not have wildcard handling.
     *
     * @param contexts The contexts to query
     * @return A map of option keys to values
     */
    public Map<String, String> getOptions(Set<Map.Entry<String, String>> contexts) {
        return getData(contexts).getOptions();
    }

    /**
     * Get a list of all parents inheriting from this subject
     * @param contexts The contexts to check
     * @return The list of parents that apply to this subject
     */
    public List<Map.Entry<String, String>> getParents(Set<Map.Entry<String, String>> contexts) {
        List<Map.Entry<String, String>> parents = getData(contexts).getParents();
        getManager().getNotifier().onParentCheck(getIdentifier(), contexts, parents);
        return parents;
    }

    /**
     * Contexts that have been queried recently enough to still be cached
     * @return
     */
    private Set<Set<Map.Entry<String, String>>> getActiveContexts() {
        return data.synchronous().asMap().keySet();
    }

    /**
     * Query a specific permission in a certain set of contexts.
     * This method takes into account context and wildcard inheritance calculations for any permission.
     *
     * Any checks made through this method will be logged by the {@link ninja.leaping.permissionsex.logging.PermissionCheckNotifier}
     * registered with the PEX engine.
     *
     * @param contexts The contexts to check in
     * @param permission The permission to query
     * @return The permission value. &lt;0 evaluates to false, 0 is undefined, and &gt;0 evaluates to true.
     */
    public int getPermission(Set<Entry<String, String>> contexts, String permission) {
        int ret = getPermissions(contexts).get(Preconditions.checkNotNull(permission, "permission"));
        getManager().getNotifier().onPermissionCheck(getIdentifier(), contexts, permission, ret);
        return ret;
    }

    /**
     * Get an option that may be present for a certain subject
     *
     * Any checks made through this method will be logged by the {@link ninja.leaping.permissionsex.logging.PermissionCheckNotifier}
     * registered with the PEX engine.
     *
     * @param contexts The contexts to check in
     * @param option The option to query
     * @return The option, if set
     */
    public Optional<String> getOption(Set<Entry<String, String>> contexts, String option) {
        String val = getOptions(contexts).get(Preconditions.checkNotNull(option, "option"));
        getManager().getNotifier().onOptionCheck(getIdentifier(), contexts, option, val);
        return Optional.ofNullable(val);
    }

    /**
     * Access this subject's persistent data
     *
     * @return A reference to the persistent data of this subject
     */
    public SubjectDataReference data() {
        return this.ref;
    }

    /**
     * Access this subject's transient data
     *
     * @return A reference to the transient data of this subject
     */
    public SubjectDataReference transientData() {
        return this.transientRef;
    }

    /**
     * Internal use only. Cache updating listener
     *
     * @param newData Updated subject data object. Ignored
     */
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

}
