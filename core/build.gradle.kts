
import ca.stellardrift.build.common.adventure
import ca.stellardrift.build.common.configurate

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
    id("ca.stellardrift.opinionated.kotlin")
    id("ca.stellardrift.localization")
}

useAutoService()
useImmutables()
dependencies {
    val adventureVersion: String by project
    val configurateVersion: String by project
    val slf4jVersion: String by project

    api(project(":api"))

    api(platform(configurate("bom", configurateVersion)))
    api(configurate("gson"))
    api(configurate("hocon"))
    implementation(configurate("yaml"))
    implementation(configurate("extra-kotlin")) { isTransitive = false }
    implementation(kotlin("reflect"))
    implementation("com.github.ben-manes.caffeine:caffeine:2.8.6") {
        exclude("com.google.errorprone")
    }
    implementation("com.google.guava:guava:21.0")
    implementation(project(":impl-blocks:glob"))

    api(adventure("api", adventureVersion))
    implementation(adventure("text-serializer-plain", adventureVersion))
    implementation(adventure("text-serializer-legacy", adventureVersion))
    api("org.slf4j:slf4j-api:$slf4jVersion")

    testImplementation("org.slf4j:slf4j-jdk14:$slf4jVersion")
    testImplementation("org.mockito:mockito-core:3.6.0")
    testImplementation("com.h2database:h2:1.4.200")
    testImplementation("org.mariadb.jdbc:mariadb-java-client:2.7.1")
    testImplementation("org.postgresql:postgresql:42.2.18")
}
