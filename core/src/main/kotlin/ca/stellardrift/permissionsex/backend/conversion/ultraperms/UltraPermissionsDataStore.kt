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

package ca.stellardrift.permissionsex.backend.conversion.ultraperms

import ca.stellardrift.permissionsex.BaseDirectoryScope
import ca.stellardrift.permissionsex.PermissionsEx
import ca.stellardrift.permissionsex.backend.conversion.ConversionProvider
import ca.stellardrift.permissionsex.backend.conversion.ConversionResult
import ca.stellardrift.permissionsex.util.unaryPlus
import java.net.URL
import java.nio.file.Files
import java.util.UUID
import net.kyori.text.Component

data class PermissionEntry(val holder: String, val permission: String, val positive: Boolean, val expiry: Long)
data class ServerEntry(val name: String, val lastHeartbeat: Long)
data class UserEntry(val name: String, val superadmin: Boolean, val groups: Map<String, Int>, val prefix: String, val suffix: String, val skull: URL)
data class GroupEntry(val name: String, val serverId: UUID, val priority: Int, val default: Boolean, val icon: String /* Material */)

class UltraPermissionsDataStore {
    companion object : ConversionProvider {
        override val name: Component
            get() = +"UltraPermissions"

        override fun listConversionOptions(pex: PermissionsEx<*>): List<ConversionResult> {
            val ultraPermsDir = pex.getBaseDirectory(BaseDirectoryScope.JAR).resolve("UltraPermissions")
            if (Files.exists(ultraPermsDir)) {
                // TODO: prepare an instance of an UltraPerms configuration
            }
            return listOf()
        }
    }
}
