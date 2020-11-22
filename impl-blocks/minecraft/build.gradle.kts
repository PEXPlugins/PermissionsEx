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
}

useImmutables()
dependencies {
    val immutablesVersion: String by project

    api(project(":api"))
    api(project(":core"))
    compileOnlyApi("org.immutables:gson:$immutablesVersion")

    // Fixed version to line up with MC
    implementation("com.google.code.gson:gson:2.8.6")

    testRuntimeOnly("org.slf4j:slf4j-simple:1.7.30")
}
