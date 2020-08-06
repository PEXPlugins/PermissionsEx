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

import ca.stellardrift.permissionsex.rank.RankLadder;

public interface RankingService {
    /**
     * Get a ladder by name. This will create a new ladder if none exists yet.
     *
     * @param ladder The name of the ladder to get. Case-insensitive.
     * @return the ladder
     */
    RankLadder getLadder(String ladder);

    /**
     * Return whether or not this ladder is present
     *
     * @param ladder The ladder to check. Case-insensitive
     * @return Whether this ladder is present
     */
    boolean hasLadder(String ladder);
}
