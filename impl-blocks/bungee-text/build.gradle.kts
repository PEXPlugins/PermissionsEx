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

repositories {
    maven {
        name = "bungeecord-repo"
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

localization {
    templateFile.set(rootProject.file("etc/messages-template.kt.tmpl"))
}

dependencies {
    api(project(":permissionsex-core"))
    api("net.md-5:bungeecord-chat:1.14-SNAPSHOT")
    api(project(":impl-blocks:permissionsex-smarter-text")) { isTransitive = false }
}
