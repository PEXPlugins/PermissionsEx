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

package ca.stellardrift.permissionsex.data;

import ca.stellardrift.permissionsex.context.ContextValue;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The core subject data interface. This class is an immutable holder for all of a single subject's data.
 *
 * No inherited data is included, and no context calculations are performed when querying objects of this class
 *
 * The global context is represented by an empty set, not a null value.
 */
public interface ImmutableSubjectData {
    /**
     * Query all options registered, as a map from context combination to map of key to value
     *
     * @return All options registered, or an empty map
     */
    Map<Set<ContextValue<?>>, Map<String, String>> getAllOptions();

    /**
     * Get options registered in a single context set.
     *
     * @param contexts The contexts to check in
     * @return The options, as an immutable map
     */
    Map<String, String> getOptions(Set<ContextValue<?>> contexts);

    /**
     * Return a new subject data object with updated option information
     *
     * @param contexts The contexts to set this option in
     * @param key The key of the option to set. Must be unique.
     * @param value The value to set attached to the key
     * @return An updated data object
     */
    ImmutableSubjectData setOption(Set<ContextValue<?>> contexts, String key, String value);

    /**
     * Return a new subject data object with an entirely new set of options
     *
     * @param contexts The contexts to set these options in
     * @param values The options to set
     * @return An updated data object
     */
    ImmutableSubjectData setOptions(Set<ContextValue<?>> contexts, Map<String, String> values);

    /**
     * Return a new subject data object with no options set in the given contexts
     *
     * @param contexts The contexts to set these options in
     * @return A new subject data object with no options information in the provided contexts.
     */
    ImmutableSubjectData clearOptions(Set<ContextValue<?>> contexts);

    /**
     * Return a new subject data object with no options set.
     *
     * @return A new subject data object with no options information.
     */
    ImmutableSubjectData clearOptions();

    /**
     * Get all permissions data in this subject
     *
     * @return an immutable map from context set to map of permission to value
     */
    Map<Set<ContextValue<?>>, Map<String, Integer>> getAllPermissions();

    /**
     * Get permissions data for a single context in this subject
     *
     * @param contexts The context set to get data fort
     * @return an immutable map from permission to value
     */
    Map<String, Integer> getPermissions(Set<ContextValue<?>> contexts);

    /**
     * Set a single permission to a specific value. Values greater than zero evaluate to true,
     * values less than zero evaluate to false, and equal to zero will unset the permission. Higher absolute values carry more weight.
     *
     * @param contexts The contexts to set this permission in
     * @param permission The permission to set
     * @param value The value. Zero to unset.
     * @return An updated subject data object
     */
    ImmutableSubjectData setPermission(Set<ContextValue<?>> contexts, String permission, int value);

    /**
     * Set all permissions in a single context set
     *
     * @param contexts The context set
     * @param values A map from permissions to their values
     * @return An updated subject data object
     */
    ImmutableSubjectData setPermissions(Set<ContextValue<?>> contexts, Map<String, Integer> values);

    /**
     * Return an updated subject data object with all permissions in every context set unset.
     *
     * @return The updated subject data object
     */
    ImmutableSubjectData clearPermissions();

    /**
     * Return an updated subject data object with all permissions in a given context set unset.
     *
     * @param contexts The context set to unset permissions in
     * @return The updated subject data object
     */
    ImmutableSubjectData clearPermissions(Set<ContextValue<?>> contexts);

    /**
     * Get an immutable map of parents in every context set. The map returned is from context set to a list of subject identifiers
     *
     * @return An immutable map of all parents
     */
    Map<Set<ContextValue<?>>, List<Map.Entry<String, String>>> getAllParents();

    /**
     * Get parents in a specific context
     * @param contexts The set of contexts to get parents in
     * @return An immutable list of parents. Empty list
     */
    List<Map.Entry<String, String>> getParents(Set<ContextValue<?>> contexts);

    /**
     * Add a single parent subject in a single context set
     *
     * @param contexts The context set to add a parent subject in
     * @param type The type of the parent subject being added
     * @param identifier The identifier of the parent subject being added
     * @return An updated subject data object
     */
    ImmutableSubjectData addParent(Set<ContextValue<?>> contexts, String type, String identifier);

    /**
     * Remove a single parent subject in a single context set
     *
     * @param contexts The context set to remove a parent subject in
     * @param type The type of the parent subject being removed
     * @param identifier The identifier of the parent subject being removed
     * @return An updated subject data object
     */
    ImmutableSubjectData removeParent(Set<ContextValue<?>> contexts, String type, String identifier);

    /**
     * Set the parents in a single context set to the provided list
     *
     * @param contexts The set of contexts to update the parents in
     * @param parents The parents that will be written in the given context
     * @return An updated subject data object
     */
    ImmutableSubjectData setParents(Set<ContextValue<?>> contexts, List<Map.Entry<String, String>> parents);

    /**
     * Remove all parents from this subject data object
     *
     * @return An updated subject data object
     */
    ImmutableSubjectData clearParents();

    /**
     * Remove every parent in the provided context set from this subject data object
     *
     * @param contexts The contexts to remove parents in
     * @return The updated subject data object
     */
    ImmutableSubjectData clearParents(Set<ContextValue<?>> contexts);

    /**
     * Get the fallback permissions value in a given context set. This is the value that will be returned for permissions
     * that do not match anything more specific
     *
     * @param contexts The context set to query the default value in
     * @return The default value in the given context set, or 0 if none is set.
     */
    int getDefaultValue(Set<ContextValue<?>> contexts);

    /**
     * Set the fallback permission value in a given context set
     *
     * @param contexts The context set to apply the given default value in
     * @param defaultValue The default value to apply. A default value of 0 is equivalent to unset
     * @return An updated subject data object
     */
    ImmutableSubjectData setDefaultValue(Set<ContextValue<?>> contexts, int defaultValue);

    /**
     * Get every default value set in this subject data entry
     *
     * @return A map from context set to default value
     */
    Map<Set<ContextValue<?>>, Integer> getAllDefaultValues();

    /**
     * Gets the contexts we have data for
     *
     * @return An immutable set of all sets of contexts with data stored
     */
    Set<Set<ContextValue<?>>> getActiveContexts();

}
