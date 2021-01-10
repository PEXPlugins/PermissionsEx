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
    id("pex-platform")
    id("ca.stellardrift.localization")
    id("ca.stellardrift.templating")
}

dependencies {
    val cloudVersion: String by project
    api(project(":impl-blocks:minecraft")) {
        exclude("org.slf4j", "slf4j-api")
        exclude("com.google.code.gson")
        exclude("com.google.guava")
        // exclude("org.spongepowered", "configurate-*")
        exclude("net.kyori")
        exclude("org.yaml", "snakeyaml")
        exclude("com.typesafe")
    }
    api(project(":impl-blocks:proxy-common")) { isTransitive = false }
    implementation(project(":impl-blocks:hikari-config")) {
        exclude("org.slf4j", "slf4j-api")
    }
    implementation(project(":impl-blocks:minecraft")) { isTransitive = false }
    implementation("cloud.commandframework:cloud-velocity:$cloudVersion")

    annotationProcessor(shadow("com.velocitypowered:velocity-api:1.1.2")!!)
}

pexPlatform {
    relocate(
        "cloud.commandframework",
        "com.github.benmanes",
        "com.zaxxer",
        "io.leangen.geantyref",
        "org.antlr.v4",
        "org.checkerframework",
        "org.jetbrains",
        "org.spongepowered.configurate"
    )
}
