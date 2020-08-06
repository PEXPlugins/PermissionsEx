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

package ca.stellardrift.permissionsex.sponge.rank;

import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectData;

import java.util.List;
import java.util.Set;

public interface RankLadder {
    /**
     * The name assigned to the rank-ladder. These names are case-insensitive
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
     * @return whether any action occurred.
     */
    boolean promote(Set<Context> contexts, SubjectData input);

    /**
     * Demote the given subject data on this rank ladder in the given context.
     *
     * If the subject is not currently on the rank ladder in this context, nothing will happen.
     * If the subject is currently at the bottom of this rank ladder, the subject will be removed from the rank ladder entirely.
     * If the subject has multiple memberships at various points in this rank ladder, all of them will be moved down by one step
     *
     * @param contexts The context combination to promote in
     * @param input The subject data to promote
     * @return whether any action occurred.
     */
    boolean demote(Set<Context> contexts, SubjectData input);

    /**
     * Get all ranks that are present on this ladder, in order from lowest rank to highest rank
     *
     * @return ladder ranks
     */
    List<Subject> getRanks();
}
