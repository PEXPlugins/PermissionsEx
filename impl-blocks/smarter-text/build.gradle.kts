import ca.stellardrift.permissionsex.gradle.applyCommonSettings
import ca.stellardrift.permissionsex.gradle.setupPublication

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
 *
 */

applyCommonSettings()
setupPublication()

plugins {
    id("ca.stellardrift.localization")
}

localization {
    templateFile.set(rootProject.file("etc/messages-template.kt.tmpl"))
}

dependencies {
    api("net.kyori:text-api:3.0.3")
    api("net.kyori:text-feature-pagination:3.0.3")
    api(project(":permissionsex-core"))
}
