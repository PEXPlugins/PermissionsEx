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

package ca.stellardrift.permissionsex.datastore.conversion

import ca.stellardrift.permissionsex.PermissionsEx
import ca.stellardrift.permissionsex.backend.AbstractDataStore
import ca.stellardrift.permissionsex.backend.ConversionResult
import ca.stellardrift.permissionsex.backend.DataStoreFactory
import ca.stellardrift.permissionsex.backend.Messages.LUCKPERMS_DESCRIPTION_FILE_COMBINED
import ca.stellardrift.permissionsex.backend.Messages.LUCKPERMS_DESCRIPTION_FILE_SEPARATE
import ca.stellardrift.permissionsex.backend.Messages.LUCKPERMS_NAME
import ca.stellardrift.permissionsex.backend.StoreProperties
import com.google.auto.service.AutoService
import net.kyori.adventure.text.Component
import org.pcollections.PVector
import org.pcollections.TreePVector
import java.nio.file.Files
import java.util.Locale

@AutoService(DataStoreFactory::class)
class LuckPermsConversionProvider : AbstractDataStore.Factory<LuckPermsFileDataStore, LuckPermsFileDataStore.Config>(
    "luckperms-file",
    LuckPermsFileDataStore.Config::class.java,
    ::LuckPermsFileDataStore
), DataStoreFactory.Convertable {
    private val formatNames = enumValues<ConfigFormat>()

    override fun friendlyName(): Component = LUCKPERMS_NAME()

    override fun listConversionOptions(pex: PermissionsEx<*>): PVector<ConversionResult> {
        val luckBaseDir = pex.baseDirectory.parent.resolve("LuckPerms")
        var result = TreePVector.empty<ConversionResult>()
        for (format in formatNames) {
            val configDir = luckBaseDir.resolve(format.storageDirName)
            if (Files.exists(configDir)) {
                if (Files.isDirectory(configDir.resolve("groups"))) {
                    result = result + ConversionResult.builder()
                        .store(make(format, false))
                        .description(LUCKPERMS_DESCRIPTION_FILE_SEPARATE(format.name))
                        .build()
                }

                if (Files.isRegularFile(configDir.resolve("groups.${format.extension}"))) {
                    result = result + ConversionResult.builder()
                        .store(make(format, true))
                        .description(LUCKPERMS_DESCRIPTION_FILE_COMBINED(format.name))
                        .build()
                }
            }
        }

        return result
    }

    fun make(format: ConfigFormat, combined: Boolean): LuckPermsFileDataStore {
        val config = LuckPermsFileDataStore.Config(format = format, combined = combined)
        return if (combined) {
            LuckPermsFileDataStore(StoreProperties.of("lp-${format.name.toLowerCase(Locale.ROOT)}-combined", config, this))
        } else {
            LuckPermsFileDataStore(StoreProperties.of("lp-${format.name.toLowerCase(Locale.ROOT)}", config, this))
        }
    }
}
