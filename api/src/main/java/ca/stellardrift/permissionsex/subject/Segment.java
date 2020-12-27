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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Subject data set for a specific configuration of properties.
 *
 * <p>Segments are immutable, so any changes will produce a new object.</p>
 *
 * @since 2.0.0
 */
public interface Segment {

    int PERMISSION_UNSET = 0;

    /**
     * Get options registered in a single context set.
     *
     * @return the options, as an immutable map
     * @since 2.0.0
     */
    Map<String, String> options();

    /**
     * Return a new segment with updated option information.
     *
     * @param key the key of the option to set. Must be unique.
     * @param value the value to set attached to the key
     * @return an updated segment
     * @since 2.0.0
     */
    Segment withOption(final String key, final String value);

    /**
     * Return a new segment with an option removed, if it is present
     *
     * @param key the key of the option to remove
     * @return an updated segment, or the same segment if no such option was present
     * @since 2.0.0
     */
    Segment withoutOption(final String key);

    /**
     * Return a new segment with an entirely new set of options
     *
     * @param values the options to set
     * @return an updated segment
     * @since 2.0.0
     */
    Segment withOptions(final Map<String, String> values);

    /**
     * Return a new segment with all options unset.
     *
     * @return a new segment with no options information.
     * @since 2.0.0
     */
    default Segment withoutOptions() {
        return withOptions(Collections.emptyMap());
    }

    /**
     * Get permissions data for a single context in this subject.
     *
     * @return an immutable map from permission to value
     * @since 2.0.0
     */
    Map<String, Integer> permissions();

    /**
     * Set a single permission to a specific value.
     *
     * <p>Values greater than zero evaluate to true, values less than zero evaluate to false,
     * and equal to zero will unset the permission. Higher absolute values carry more weight.</p>
     *
     * @param permission the permission to set
     * @param value the value. Zero to unset.
     * @return an updated segment
     * @since 2.0.0
     */
    Segment withPermission(final String permission, final int value);

    /**
     * Set all permissions.
     *
     * @param values a map from permissions to their values
     * @return an updated segment object
     * @since 2.0.0
     */
    Segment withPermissions(Map<String, Integer> values);

    /**
     * Return a new segment with all permissions unset.
     *
     * @return the updated segment
     * @since 2.0.0
     */
    Segment withoutPermissions();

    /**
     * Get parents in this segment
     *
     * @return an immutable list of parents
     * @since 2.0.0
     */
    List<? extends SubjectRef<?>> parents();

    /**
     * Update a segment with an added parent.
     *
     * <p>This parent will be added at the beginning of the list of parents, meaning it will have
     * higher priority than any existing parents.</p>
     *
     * @param type the type of the parent subject being added
     * @param identifier the identifier of the parent subject being added
     * @return an updated segment
     * @since 2.0.0
     */
    default <I> Segment plusParent(SubjectType<I> type, I identifier) {
        return this.plusParent(SubjectRef.subject(type, identifier));
    }

    /**
     * Update a new segment with an added parent.
     *
     * <p>This parent will be added at the end of the list of parents, meaning it will have lower
     * priority than any existing parents.</p>
     *
     * @param subject a reference to the subject that should be added as parent.
     * @param <I> identifier type
     * @return an updated segment
     * @since 2.0.0
     */
    <I> Segment plusParent(SubjectRef<I> subject);

    /**
     * Update a new segment to remove the passed parent.
     *
     * @param type the type of the parent subject being removed
     * @param identifier the identifier of the parent subject being removed
     * @return an updated segment
     * @since 2.0.0
     */
    default <I> Segment minusParent(SubjectType<I> type, I identifier) {
        return this.minusParent(SubjectRef.subject(type, identifier));
    }

    /**
     * Remove a single parent subject in this segment.
     *
     * @param subject a reference to the subject that should be added as parent.
     * @param <I> identifier type
     * @return an updated segment
     * @since 2.0.0
     */
    <I> Segment minusParent(final SubjectRef<I> subject);

    /**
     * Update a new segment with the provided {@code parents}.
     *
     * @param parents the parents that will be written
     * @return an updated segment
     * @since 2.0.0
     */
    Segment withParents(List<SubjectRef<?>> parents);

    /**
     * Remove all parents from this segment.
     *
     * @return an updated segment
     * @since 2.0.0
     */
    Segment withoutParents();

    /**
     * Get the fallback permissions value for this segment.
     *
     * This is the value that will be returned for permissions that do not match anything
     * more specific.
     *
     * @return the default value in the given context set, or 0 if none is set.
     * @since 2.0.0
     */
    int fallbackPermission();

    /**
     * Update a new segment with a new fallback permission.
     *
     * @param defaultValue the default value to apply. A default value of 0 is equivalent to unset
     * @return an updated segment
     * @since 2.0.0
     */
    Segment withFallbackPermission(int defaultValue);

    /**
     * Return a new segment with no data set.
     *
     * @return the cleared segment
     * @since 2.0.0
     */
    Segment cleared();

    /**
     * Return {@code true} if this segment has no set data.
     *
     * @return if this sement is empty
     * @since 2.0.0
     */
    boolean empty();

    /**
     * Add all data from the segment {@code other} to this one.
     *
     * @param other the source segment
     * @return an updated segment
     * @since 2.0.0
     */
    default Segment mergeFrom(final Segment other) {
        if (other.empty()) {
            return this;
        } else if (this.empty()) {
            return other;
        }

        Segment output = this;
        final Map<String, Integer> permissions = other.permissions();
        final Map<String, String> options = other.options();
        final List<? extends SubjectRef<?>> parents = other.parents();

        if (!permissions.isEmpty()) {
            final Map<String, Integer> newPermissions = new HashMap<>(output.permissions());
            newPermissions.putAll(permissions);
            output = output.withPermissions(newPermissions);
        }

        if (!options.isEmpty()) {
            final Map<String, String> newOptions = new HashMap<>(output.options());
            newOptions.putAll(options);
            output = output.withOptions(newOptions);
        }

        if (!parents.isEmpty()) {
            final List<SubjectRef<?>> newParents = new ArrayList<>(output.parents());
            newParents.addAll(parents);
            output = output.withParents(newParents);
        }

        return output;
    }
}
