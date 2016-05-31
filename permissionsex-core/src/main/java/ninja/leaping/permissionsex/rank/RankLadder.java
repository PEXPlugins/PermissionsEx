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
package ninja.leaping.permissionsex.rank;

import ninja.leaping.permissionsex.data.ImmutableSubjectData;
import ninja.leaping.permissionsex.data.SubjectRef;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a track of ranks along which a user can be promoted or demoted. These objects are immutable.
 */
public interface RankLadder {
    /**
     * The name assigned to the rank-ladder. These names are case-insensitive.
     *
     * @return The ladder's name
     */
    String getName();

    /**
     * Promote the given subject data on this rank ladder in the given context.
     *
     * If the subject is not currently on the rank ladder in this context, the subject will be placed on the lowest rank in this ladder.
     * If the subject is currently at the top of this rank ladder, nothing will happen.
     * If the subject has multiple memberships at various points in this rank ladder, all of them will be moved up by one step
     *
     * @param contexts The context combination to promote in
     * @param input The subject data to promote
     * @return The promoted data
     */
    ImmutableSubjectData promote(Set<Map.Entry<String, String>> contexts, ImmutableSubjectData input);

    /**
     * Demote the given subject data on this rank ladder in the given context.
     *
     * If the subject is not currently on the rank ladder in this context, nothing will happen.
     * If the subject is currently at the bottom of this rank ladder, the subject will be removed from the rank ladder entirely.
     * If the subject has multiple memberships at various points in this rank ladder, all of them will be moved down by one step
     *
     * @param contexts The context combination to promote in
     * @param input The subject data to promote
     * @return the demoted data
     */
    ImmutableSubjectData demote(Set<Map.Entry<String, String>> contexts, ImmutableSubjectData input);

    /**
     * Return if this subject is a member of any subjects that are part of this rank ladder within the given contexts.
     *
     * @param contexts The contexts to check in
     * @param subject The subject
     * @return Whether this ladder contains any of the direct parents of the subject in the given contexts
     */
    boolean isOnLadder(Set<Map.Entry<String, String>> contexts, ImmutableSubjectData subject);

    /**
     * Return a new rank ladder with the specified rank added at the highest point in the rank ladder.
     * If the rank is currently already in the rank ladder, it will be moved to the highest point.
     *
     * @param subject The rank to add
     * @return a rank ladder instance with the appropriate changes
     */
    RankLadder addRank(SubjectRef subject);


    /**
     * Return a new rank ladder with the specified rank added at a given point in the rank ladder.
     * If the rank is currently already in the rank ladder, it will be moved to the given index.
     *
     * @param subject The rank to add
     * @param index The point to add the rank at. Must be on the range [0, getRanks().size()]
     * @return a rank ladder instance with the appropriate changes
     */
    RankLadder addRankAt(SubjectRef subject, int index);

    /**
     * Get the index of this rank in the current ladder. This index is the index of the rank in {@link #getRanks()}.
     *
     * @param subject The rank to find the index of
     * @return The index of the rank, or -1 if the rank is not present.
     */
    int indexOfRank(SubjectRef subject);

    /**
     * Remove the given rank from this rank ladder.
     *
     * @param subject The rank to remove
     * @return A new ladder without the given rank, or this if the rank was not contained in this ladder
     */
    RankLadder removeRank(SubjectRef subject);

    /**
     * Provides a way to iterate through ranks currently active in this ladder.
     * @return A list of ranks present in the ladder. This list is immutable.
     */
    List<? extends SubjectRef> getRanks();
}
