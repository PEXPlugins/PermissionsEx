
import ca.stellardrift.permissionsex.gradle.Versions
import ca.stellardrift.permissionsex.gradle.applyCommonSettings
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

val spigotVersion: String = "1.14.4-R0.1-SNAPSHOT"

plugins {
    id("com.github.johnrengelman.shadow")
}

applyCommonSettings()
setupPublication()

repositories {
    maven {
        name = "spigot-repo"
        url = uri("https://hub.spigotmc.org/nexus/content/groups/public/")
    }
    maven {
        name = "vault-repo"
        url = uri("http://nexus.hc.to/content/repositories/pub_releases/")
    }
}

java {
    registerFeature("h2dbSupport") {
        usingSourceSet(sourceSets["main"])
    }
}

tasks.processResources {
    expand("project" to project)
}

dependencies {
    api(project(":permissionsex-core")) {
        exclude(group="com.google.guava")
        exclude("org.yaml", "snakeyaml")
        exclude("com.googlecode.json-simple", "json-simple")
        exclude("com.google.code.gson", "gson")
    }
    implementation("com.zaxxer:HikariCP:3.3.1")
    implementation("org.slf4j:slf4j-jdk14:${Versions.SLF4J}")

    // provided at runtime
    shadow("org.spigotmc:spigot-api:$spigotVersion")
    shadow("net.milkbowl.vault:VaultAPI:1.7")
    shadow("com.h2database:h2:1.4.199")
}

val relocateRoot = project.ext["pexRelocateRoot"]
val shadowJar by tasks.getting(ShadowJar::class) {
    //minimize()
    listOf(
        "ninja.leaping.configurate",
        "com.sk89q.squirrelid",
        "com.zaxxer.hikari",
        "com.github.benmanes.caffeine",
        "com.google.errorprone",
        "com.typesafe",
        "javax.annotation", // TODO: maybe we don't need to include this?
        "org.checkerframework",
        "org.jetbrains.annotations",
        "org.slf4j",
        "org.antlr"
    ).forEach {
        relocate(it, "$relocateRoot.$it")
    }
    dependencies {
        exclude("org.yaml:snakeyaml")
        exclude("com.google.code.findbugs:jsr305")
    }
}

tasks.assemble {
    dependsOn(shadowJar)
}
