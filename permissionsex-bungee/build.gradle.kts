import ca.stellardrift.permissionsex.gradle.Versions
import ca.stellardrift.permissionsex.gradle.applyCommonSettings
import ca.stellardrift.permissionsex.gradle.configurate
import ca.stellardrift.permissionsex.gradle.setupPublication
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

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
    id("com.github.johnrengelman.shadow")
}

applyCommonSettings()
setupPublication()

repositories {
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

dependencies {
    api(project(":permissionsex-core")) {
        exclude("com.google.code.gson")
        exclude("com.google.code.findbugs")
        exclude("com.google.guava")
        exclude("org.checkerframework", "checker-qual")
    }

    implementation(configurate("yaml")) {
        exclude("org.yaml", "snakeyaml")
    }
    implementation("org.slf4j:slf4j-jdk14:${Versions.SLF4J}")
    implementation(project(":impl-blocks:permissionsex-bungee-text")) { isTransitive = false }
    api(project(":impl-blocks:permissionsex-proxy-common")) { isTransitive = false }
    implementation(project(":impl-blocks:permissionsex-hikari-config"))

    shadow("net.md-5:bungeecord-api:1.14-SNAPSHOT")
}

tasks.processResources {
    expand("project" to project)
}

val relocateRoot = project.ext["pexRelocateRoot"]
val shadowJar by tasks.getting(ShadowJar::class) {
    minimize {
        exclude(dependency("com.github.ben-manes.caffeine:.*:.*"))
    }
    listOf("com.github.benmanes", "com.zaxxer", "com.typesafe",
        "com.sk89q.squirrelid", "ninja.leaping.configurate", "org.jetbrains.annotations",
        "org.slf4j", "org.json.simple", "org.antlr.v4.runtime").forEach {
        relocate(it, "$relocateRoot.$it")
    }
}

tasks.assemble {
    dependsOn(shadowJar)
}