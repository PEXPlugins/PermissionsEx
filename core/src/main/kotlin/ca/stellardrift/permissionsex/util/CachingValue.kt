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

@file:JvmName("CachingValues")
package ca.stellardrift.permissionsex.util

import kotlin.reflect.KProperty

class CachingValue<Value>(private val currentFun: () -> Long, private val maxDelta: Long, private val updateFunc: () -> Value) {
    @Volatile
    private var lastTime: Long = currentFun()
    @Volatile
    private var lastValue: Value = updateFunc()

    fun get(): Value {
        val now = currentFun()
        if ((now - lastTime) > maxDelta) {
            lastValue = updateFunc()
            lastTime = now
        }
        return lastValue
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): Value {
        return get()
    }

    fun refresh() {
        lastValue = updateFunc()
        lastTime = currentFun()
    }
}

/**
 * Create a value that is cached for a certain amount of time
 */
fun <Value> cachedByTime(maxDelta: Long, updateFunc: () -> Value): CachingValue<Value> {
    return CachingValue(System::currentTimeMillis, maxDelta, updateFunc)
}
