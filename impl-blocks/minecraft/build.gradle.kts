import ca.stellardrift.build.common.adventure

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
plugins {
    id("pex-component")
    id("ca.stellardrift.localization")
}

useImmutables()
dependencies {
    val cloudVersion: String by project
    val immutablesVersion: String by project

    api(project(":api"))
    api(project(":core"))
    implementation(adventure("text-serializer-plain"))
    compileOnlyApi("org.immutables:gson:$immutablesVersion")
    implementation("io.projectreactor:reactor-core:3.4.0")
    runtimeOnly(project(":datastore:sql"))
    api("cloud.commandframework:cloud-core:$cloudVersion")
    implementation("cloud.commandframework:cloud-minecraft-extras:$cloudVersion")

    // Fixed version to line up with MC
    implementation("com.google.code.gson:gson:2.8.0")

    testRuntimeOnly("org.slf4j:slf4j-simple:1.7.30")
}
