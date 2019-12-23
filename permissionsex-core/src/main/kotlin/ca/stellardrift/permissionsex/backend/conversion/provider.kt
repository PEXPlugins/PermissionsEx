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

package ca.stellardrift.permissionsex.backend.conversion

import ca.stellardrift.permissionsex.PermissionsEx
import ca.stellardrift.permissionsex.backend.DataStore
import ca.stellardrift.permissionsex.backend.conversion.groupmanager.GroupManagerDataStore
import ca.stellardrift.permissionsex.backend.conversion.luckperms.LuckPermsConversionProvider
import ca.stellardrift.permissionsex.util.Translatable
import java.util.concurrent.ConcurrentHashMap

data class ConversionResult(val store: DataStore, val title: Translatable, val commandKey: String)

interface ConversionProvider {
    val name: Translatable
    fun listConversionOptions(pex: PermissionsEx<*>): List<ConversionResult>
}

object ConversionProviderRegistry {
    /**
     * Internal registry of providers
     */
    private val providers: MutableMap<String, ConversionProvider> = ConcurrentHashMap()

    /**
     * Get an immutable copy of the set of registered providers
     */
    val allProviders get() = providers.values.toSet()

    operator fun get(provider: String): ConversionProvider? {
        return providers[provider]
    }

    /**
     * Register a single provider
     */
    fun register(provider: ConversionProvider): Boolean {
        return providers.putIfAbsent(provider.name.untranslated, provider) == null
    }

    init {
        register(GroupManagerDataStore)
        register(LuckPermsConversionProvider)
    }
}
