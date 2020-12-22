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
package ca.stellardrift.permissionsex.datastore.conversion.luckperms

import ca.stellardrift.permissionsex.PermissionsEngine
import ca.stellardrift.permissionsex.datastore.ConversionResult
import ca.stellardrift.permissionsex.datastore.DataStoreFactory
import ca.stellardrift.permissionsex.datastore.StoreProperties
import ca.stellardrift.permissionsex.impl.PermissionsEx
import ca.stellardrift.permissionsex.impl.backend.AbstractDataStore
import com.google.auto.service.AutoService
import java.nio.file.Files
import java.util.Locale
import net.kyori.adventure.text.Component
import org.pcollections.PVector
import org.pcollections.TreePVector

@AutoService(DataStoreFactory::class)
class LuckPermsConversionProvider : AbstractDataStore.Factory<LuckPermsFileDataStore, LuckPermsFileDataStore.Config>(
    "luckperms-file",
    LuckPermsFileDataStore.Config::class.java,
    ::LuckPermsFileDataStore
), DataStoreFactory.Convertable {
    private val formatNames = enumValues<ConfigFormat>()

    override fun friendlyName(): Component = Messages.NAME.tr()

    override fun listConversionOptions(pex: PermissionsEngine): PVector<ConversionResult> {
        val luckBaseDir = (pex as PermissionsEx<*>).baseDirectory().parent.resolve("LuckPerms")
        var result = TreePVector.empty<ConversionResult>()
        for (format in formatNames) {
            val configDir = luckBaseDir.resolve(format.storageDirName)
            if (Files.exists(configDir)) {
                if (Files.isDirectory(configDir.resolve("groups"))) {
                    result = result + ConversionResult.builder()
                        .store(make(format, false))
                        .description(Messages.DESCRIPTION_FILE_SEPARATE.tr(format.name))
                        .build()
                }

                if (Files.isRegularFile(configDir.resolve("groups.${format.extension}"))) {
                    result = result + ConversionResult.builder()
                        .store(make(format, true))
                        .description(Messages.DESCRIPTION_FILE_COMBINED.tr(format.name))
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
