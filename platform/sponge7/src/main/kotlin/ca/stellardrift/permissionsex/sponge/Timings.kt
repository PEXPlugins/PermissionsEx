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

package ca.stellardrift.permissionsex.sponge

import co.aikar.timings.Timing
import co.aikar.timings.Timings

class Timings(private val plugin: PermissionsExPlugin) {
    val getSubject: Timing = timing("getSubject")
    val getActiveContexts: Timing = timing("getActiveContexts")
    val getPermission: Timing = timing("getPermission")
    val getOption: Timing = timing("getOption")
    val getParents: Timing = timing("getParents")

    private fun timing(key: String): Timing {
        return Timings.of(plugin, key)
    }
}

inline fun <T> Timing.time(action: () -> T): T {
    startTimingIfSync()
    try {
        return action()
    } finally {
        stopTimingIfSync()
    }
}
