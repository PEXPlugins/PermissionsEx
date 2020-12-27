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

import ca.stellardrift.permissionsex.context.ContextDefinitionProvider;
import ca.stellardrift.permissionsex.context.ContextValue;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * The core subject data interface.
 *
 * <p>This class is an immutable holder for all of a single subject's data. No inherited data is
 * included, and no context calculations are performed when querying objects of this class.</p>
 *
 * <p>The global context is represented by an empty set
 * ({@link ContextDefinitionProvider#GLOBAL_CONTEXT}), not a null value.</p>
 *
 * @since 2.0.0
 */
public interface ImmutableSubjectData {

    /**
     * Get all non-empty segments associated with this subject.
     *
     * @return all segments
     * @since 2.0.0
     */
    Map<? extends Set<ContextValue<?>>, Segment> segments();

    /**
     * Apply a transformation to every segment contained in this subject.
     *
     * @param transformer action to apply
     * @return the modified subject data
     * @since 2.0.0
     */
    ImmutableSubjectData withSegments(final BiFunction<Set<ContextValue<?>>, Segment, Segment> transformer);

    /**
     * Apply a transformation to the segment at the provided contexts.
     *
     * <p>If no segment is present at {@code contexts}, an empty segment will be used as the
     * input value.</p>
     *
     * @param contexts the contexts to modify at
     * @param operation the operation to perform
     * @return an updated subject data
     * @since 2.0.0
     */
    ImmutableSubjectData withSegment(final Set<ContextValue<?>> contexts, final UnaryOperator<Segment> operation);

    /**
     * Return a map derived from all segments in this data, transformed by {@code mapper}.
     *
     * @param mapper the mapper
     * @param <V> the output value type
     * @return the map
     * @since 2.0.0
     */
    <V> Map<Set<ContextValue<?>>, V> mapSegmentValues(final Function<Segment, V> mapper);

    /**
     * Get a value derived from a single segment.
     *
     * @param contexts the contexts to query the segment from
     * @param mapper the transformation
     * @param <V> the output value type
     * @return the transformed value, or null if no segment was present at the context
     * @since 2.0.0
     */
    <V> @Nullable V mapSegment(final Set<ContextValue<?>> contexts, final Function<Segment, V> mapper);

    /**
     * Get a segment at the specified coordinates.
     *
     * @param contexts the context coordinates for the segment
     * @return the segment at the coordinates, or an empty segment if none is present
     * @since 2.0.0
     */
    Segment segment(final Set<ContextValue<?>> contexts);

    /**
     * Make an updated subject data with the segment applied at the specified context set.
     *
     * @param contexts contexts to set at
     * @param segment the segment to set
     * @return an updated subject data
     * @since 2.0.0
     */
    ImmutableSubjectData withSegment(final Set<ContextValue<?>> contexts, final Segment segment);

    /**
     * Gets the contexts with data set in this subject.
     *
     * @return An immutable set of all sets of contexts with data stored
     * @since 2.0.0
     */
    Set<? extends Set<ContextValue<?>>> activeContexts();

    /**
     * Create a new subject data instance, applying all data from {@code other}.
     *
     * <p>This will <em>add</em> to existing data, rather than overwriting.</p>
     *
     * @param other source to add from
     * @return a modified subject data
     * @since 2.0.0
     */
    default ImmutableSubjectData mergeFrom(final ImmutableSubjectData other) {
        ImmutableSubjectData output = this;
        for (final Map.Entry<? extends Set<ContextValue<?>>, Segment> entry : other.segments().entrySet()) {
            output = output.withSegment(entry.getKey(), output.segment(entry.getKey()).mergeFrom(entry.getValue()));
        }
        return output;
    }

}
