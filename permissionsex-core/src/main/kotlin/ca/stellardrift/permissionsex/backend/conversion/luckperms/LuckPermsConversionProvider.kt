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

package ca.stellardrift.permissionsex.backend.conversion.luckperms

import ca.stellardrift.permissionsex.PermissionsEx
import ca.stellardrift.permissionsex.backend.Messages.LUCKPERMS_DESCRIPTION_FILE_COMBINED
import ca.stellardrift.permissionsex.backend.Messages.LUCKPERMS_DESCRIPTION_FILE_SEPARATE
import ca.stellardrift.permissionsex.backend.Messages.LUCKPERMS_NAME
import ca.stellardrift.permissionsex.backend.conversion.ConversionProvider
import ca.stellardrift.permissionsex.backend.conversion.ConversionResult
import java.nio.file.Files

object LuckPermsConversionProvider : ConversionProvider {
    private val formatNames = enumValues<ConfigFormat>()

    override val name = LUCKPERMS_NAME()

    override fun listConversionOptions(pex: PermissionsEx<*>): List<ConversionResult> {
        val luckBaseDir = pex.baseDirectory.parent.resolve("LuckPerms")
        val result = mutableListOf<ConversionResult>()
        for (format in formatNames) {
            val configDir = luckBaseDir.resolve(format.storageDirName)
            if (Files.exists(configDir)) {
                if (Files.isDirectory(configDir.resolve("groups"))) {
                    result += ConversionResult(LuckPermsFileDataStore("lp-${format.name.toLowerCase()}", format, false), LUCKPERMS_DESCRIPTION_FILE_SEPARATE(format.name))
                }

                if (Files.isRegularFile(configDir.resolve("groups.${format.extension}"))) {
                    result += ConversionResult(LuckPermsFileDataStore("lp-${format.name.toLowerCase()}-combined", format, true), LUCKPERMS_DESCRIPTION_FILE_COMBINED(format.name))
                }
            }
        }

        return result
    }
}
