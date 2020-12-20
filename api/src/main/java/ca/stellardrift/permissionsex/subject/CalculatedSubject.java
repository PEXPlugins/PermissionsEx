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
package ca.stellardrift.permissionsex.subject;

import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.logging.PermissionCheckNotifier;
import ca.stellardrift.permissionsex.util.NodeTree;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface CalculatedSubject {
    /**
     * Get the identifier for this subject, as a map entry where the key is the identifier for this
     * subject's type and the value is the specific identifier for this subject.
     *
     * @return The identifier
     */
    SubjectRef<?> getIdentifier();

    /**
     * Get the subject type holding this calculated subject.
     *
     * @return The subject type
     */
    SubjectTypeCollection<?> getType();

    /**
     * Get the permissions tree in this subject's active contexts.
     *
     * @return A node tree with the calculated permissions
     */
    default NodeTree getPermissions() {
        return getPermissions(getActiveContexts());
    }

    /**
     * Get the permissions tree for a certain set of contexts.
     *
     * @param contexts The contexts to get permissions in
     * @return A node tree with the calculated permissions
     */
    NodeTree getPermissions(Set<ContextValue<?>> contexts);

    /**
     * Get all options for this subject's active contexts. Options are plain strings and therefore
     * do not have wildcard handling.
     *
     * @return A map of option keys to values
     */
    default Map<String, String> getOptions() {
        return getOptions(getActiveContexts());
    }

    /**
     * Get all options for a certain set of contexts. Options are plain strings and therefore do not
     * have wildcard handling.
     *
     * @param contexts The contexts to query
     * @return A map of option keys to values
     */
    Map<String, String> getOptions(Set<ContextValue<?>> contexts);

    /**
     * Get a list of all parents inheriting from this subject in the active contexts.
     *
     * @return The list of parents that apply to this subject
     */
    default List<Map.Entry<String, String>> getParents() {
        return getParents(getActiveContexts());
    }

    /**
     * Get a list of all parents inheriting from this subject.
     *
     * @param contexts The contexts to check
     * @return The list of parents that apply to this subject
     */
    List<Map.Entry<String, String>> getParents(Set<ContextValue<?>> contexts);

    Set<ContextValue<?>> getActiveContexts();

    CompletableFuture<Set<ContextValue<?>>> getUsedContextValues();

    /**
     * Query a specific permission in this subject's active contexts
     * This method takes into account context and wildcard inheritance calculations for any permission.
     *
     * <p>Any checks made through this method will be logged by the {@link PermissionCheckNotifier}
     * registered with the PEX engine.</p>
     *
     * @param permission The permission to query
     * @return The permission value. &lt;0 evaluates to false, 0 is undefined, and &gt;0 evaluates to true.
     */
    default int getPermission(final String permission) {
        return getPermission(getActiveContexts(), permission);
    }

    /**
     * Query a specific permission in a certain set of contexts.
     * This method takes into account context and wildcard inheritance calculations for any permission.
     *
     * <p>Any checks made through this method will be logged by the {@link PermissionCheckNotifier}
     * registered with the PEX engine.</p>
     *
     * @param contexts The contexts to check in
     * @param permission The permission to query
     * @return The permission value. &lt;0 evaluates to false, 0 is undefined, and &gt;0 evaluates to true.
     */
    int getPermission(Set<ContextValue<?>> contexts, String permission);

    /**
     * Query whether this subject has a specific permission in this subject's active contexts
     * This method takes into account context and wildcard inheritance calculations for any permission.
     *
     * <p>Any checks made through this method will be logged by the {@link PermissionCheckNotifier}
     * registered with the PEX engine.</p>
     *
     * @param permission The permission to query
     * @return Whether the subject has a true permissions value
     */
    default boolean hasPermission(final String permission) {
        return hasPermission(getActiveContexts(), permission);
    }

    /**
     * Query whether this subject has a specific permission in the provided contexts
     * This method takes into account context and wildcard inheritance calculations for any permission.
     *
     * <p>Any checks made through this method will be logged by the {@link PermissionCheckNotifier}
     * registered with the PEX engine.</p>
     *
     * @param contexts The contexts to query this permission in
     * @param permission The permission to query
     * @return Whether the subject has a true permissions value
     */
    boolean hasPermission(Set<ContextValue<?>> contexts, String permission);

    /**
     * Get an option that may be present for a certain subject in the subject's active contexts
     *
     * <p>Any checks made through this method will be logged by the {@link PermissionCheckNotifier}
     * registered with the PEX engine.</p>
     *
     * @param option The option to query
     * @return The option, if set
     */
    default Optional<String> getOption(final String option) {
        return getOption(getActiveContexts(), option);
    }

    /**
     * Get an option that may be present for a certain subject
     *
     * <p>Any checks made through this method will be logged by the {@link PermissionCheckNotifier}
     * registered with the PEX engine.</p>
     *
     * @param contexts The contexts to check in
     * @param option The option to query
     * @return The option, if set
     */
    Optional<String> getOption(Set<ContextValue<?>> contexts, String option);

    /**
     * Get an option from this subject. The value will be returned as a {@link ConfigurationNode} to
     * allow easily accessing its data.
     *
     * <p>Any checks made through this method will be logged by the {@link PermissionCheckNotifier}
     * registered with the PEX engine.</p>
     *
     * @param option The option to query
     * @return The option, if set
     */
    default ConfigurationNode getOptionNode(final String option) {
        return getOptionNode(getActiveContexts(), option);
    }

    /**
     * Get an option from this subject. The value will be returned as a ConfigurationNode to allow easily accessing its data.
     *
     * Any checks made through this method will be logged by the {@link PermissionCheckNotifier}
     * registered with the PEX engine.
     *
     * @param contexts The contexts to check in
     * @param option The option to query
     * @return The option, if set
     */
    ConfigurationNode getOptionNode(Set<ContextValue<?>> contexts, String option);

    /**
     * Access this subject's persistent data.
     *
     * @return A reference to the persistent data of this subject
     */
    SubjectRef.ToData<?> data();

    /**
     * Access this subject's transient data.
     *
     * @return A reference to the transient data of this subject
     */
    SubjectRef.ToData<?> transientData();

    /**
     * Get a native object associated with this subject.
     *
     * <p>This object is provided by the {@link SubjectType} responsible for this
     * subject's type.</p>
     *
     * @return a native object
     */
    @Nullable Object getAssociatedObject();

    /**
     * Register a listener that will receive updates to this subject.
     *
     * A reference to the listener will be held, so when updates are no longer needed the listener should be unregistered
     *
     * @param listener The listener
     */
    void registerListener(Consumer<CalculatedSubject> listener);

    void unregisterListener(Consumer<CalculatedSubject> listener);
}
