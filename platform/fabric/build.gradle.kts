
import ca.stellardrift.build.transformations.ConfigFormats
import ca.stellardrift.build.transformations.convertFormat
import ca.stellardrift.permissionsex.gradle.Versions
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.task.RemapJarTask

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
    id("fabric-loom") version "0.5-SNAPSHOT"
    id("ca.stellardrift.opinionated.fabric") version "3.1"
    id("ca.stellardrift.configurate-transformations")
    id("com.github.johnrengelman.shadow")
    id("ca.stellardrift.localization")
}

val shade: Configuration by configurations.creating
configurations.implementation.get().extendsFrom(shade)

val minecraftVersion = "1.16.3"
dependencies {
    shade(project(":core")) {
        exclude("com.google.guava")
        exclude("com.google.code.gson")
        exclude("org.spongepowered")
        exclude("net.kyori")
        exclude("org.jetbrains.kotlin")
    }

    shade(project(":impl-blocks:hikari-config"))
    shade("org.apache.logging.log4j:log4j-slf4j-impl:2.8.1") { isTransitive = false }

    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:$minecraftVersion+build.17:v2")
    modImplementation("net.fabricmc:fabric-loader:0.10.0+build.208")
    modImplementation("com.sk89q.worldedit:worldedit-fabric-mc$minecraftVersion:7.2.0-SNAPSHOT") { isTransitive = false }
    modImplementation("com.sk89q.worldedit:worldedit-core:7.2.0-SNAPSHOT") { isTransitive = false }

    modImplementation("net.fabricmc.fabric-api:fabric-api:0.21.0+build.407-1.16")
    modImplementation("net.fabricmc:fabric-language-kotlin:1.4.0+build.1")
    modImplementation(include("net.kyori:adventure-platform-fabric:${Versions.TEXT_ADAPTER}") {
        exclude("com.google.code.gson")
    })
    modImplementation(include("ca.stellardrift:confabricate:1.3+3.7.1") {
        exclude("com.google.guava")
        exclude("com.google.code.gson")
    })
}

localization {
    templateFile.set(rootProject.file("etc/messages-template.kt.tmpl"))
}

tasks.withType(ProcessResources::class).configureEach {
    filesMatching("*.yml") {
        expand("project" to project)
        convertFormat(ConfigFormats.YAML, ConfigFormats.GSON)
        name = "${name.removeSuffix(".yml")}.json"
    }
}

val relocateRoot = project.ext["pexRelocateRoot"]
val shadowJar by tasks.getting(ShadowJar::class) {
    configurations = listOf(shade)
    minimize {
        exclude(dependency("com.github.ben-manes.caffeine:.*:.*"))
    }
    archiveClassifier.set("dev-all")
    from(sourceSets["accessor"].output)
    from(sourceSets["mixin"].output)

    dependencies {
        exclude(dependency("org.jetbrains.kotlin:.*:.*"))
        exclude(dependency("org.jetbrains.kotlinx:.*:.*"))
        exclude(dependency("org.jetbrains:annotations:.*"))
        exclude(dependency("org.checkerframework:checker-qual:.*"))
    }

    listOf("com.zaxxer", "com.github.benmanes", "org.slf4j",
        "org.antlr", "org.apache.logging.slf4j").forEach {
        relocate(it, "$relocateRoot.$it")
    }

    manifest {
        attributes("Automatic-Module-Name" to project.name)
    }
}

val remapShadowJar = tasks.register<RemapJarTask>("remapShadowJar") {
    dependsOn(shadowJar)
    archiveClassifier.set("all")
    input.set(shadowJar.archiveFile)
    addNestedDependencies.set(true)
}

tasks.assemble.configure {
    dependsOn(shadowJar)
}

tasks.build.configure {
    dependsOn(remapShadowJar)
}
