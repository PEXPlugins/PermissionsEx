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

package ca.stellardrift.permissionsex

import ca.stellardrift.permissionsex.backend.DataStore
import ca.stellardrift.permissionsex.backend.memory.MemoryDataStore
import ca.stellardrift.permissionsex.config.EmptyPlatformConfiguration
import ca.stellardrift.permissionsex.config.PermissionsExConfiguration
import ca.stellardrift.permissionsex.exception.PermissionsException
import ca.stellardrift.permissionsex.util.unaryPlus

class EmptyTestConfiguration : PermissionsExConfiguration<EmptyPlatformConfiguration> {
    val defaultDataStore = MemoryDataStore("test")
    val _platformConfig = EmptyPlatformConfiguration()

    override fun getDataStore(name: String?): DataStore {
        if (name == defaultDataStore.name) {
            return defaultDataStore
        } else {
            throw PermissionsException(+"Unknown data store $name")
        }
    }

    override fun getDefaultDataStore(): DataStore {
        return this.defaultDataStore
    }

    override fun isDebugEnabled(): Boolean {
        return false
    }

    override fun getServerTags(): List<String> {
        return listOf()
    }

    override fun validate() {
    }

    override fun getPlatformConfig(): EmptyPlatformConfiguration {
        return _platformConfig
    }

    override fun reload(): PermissionsExConfiguration<EmptyPlatformConfiguration> {
        return this
    }
}
