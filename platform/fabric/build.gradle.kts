
import ca.stellardrift.build.configurate.ConfigFormats
import ca.stellardrift.build.configurate.transformations.convertFormat
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
    id("pex-platform")
    id("ca.stellardrift.opinionated.fabric")
    id("ca.stellardrift.localization")
}

val shade: Configuration by configurations.creating
configurations.implementation.get().extendsFrom(shade)

val minecraftVersion = "1.16.4"
dependencies {
    val adventurePlatformVersion: String by project

    shade(project(":impl-blocks:minecraft")) {
        exclude("com.google.guava")
        exclude("com.google.code.gson")
        exclude("org.spongepowered")
        exclude("net.kyori")
        exclude("org.jetbrains.kotlin")
    }

    shade(project(":impl-blocks:hikari-config"))
    shade("org.apache.logging.log4j:log4j-slf4j-impl:2.8.1") { isTransitive = false }

    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:$minecraftVersion+build.6:v2")
    modImplementation("net.fabricmc:fabric-loader:0.10.6+build.214")
    modCompileOnly("com.sk89q.worldedit:worldedit-fabric-mc1.16.3:7.2.0") { isTransitive = false }
    modCompileOnly("com.sk89q.worldedit:worldedit-core:7.2.0") { isTransitive = false }

    modImplementation("net.fabricmc.fabric-api:fabric-api:0.26.1+1.17")
    modImplementation("net.fabricmc:fabric-language-kotlin:1.4.0+build.1")
    modImplementation(include("net.kyori:adventure-platform-fabric:$adventurePlatformVersion") {
        exclude("com.google.code.gson")
    })
    modImplementation(include("ca.stellardrift:confabricate:2.0-SNAPSHOT+4.0.0") {
        exclude("com.google.code.gson")
    })
}

tasks.withType(ProcessResources::class).configureEach {
    filesMatching("*.yml") {
        convertFormat(ConfigFormats.YAML, ConfigFormats.JSON)
        name = "${name.removeSuffix(".yml")}.json"
    }
}

pexPlatform {
    relocate(
        "com.github.benmanes",
        "com.zaxxer",
        "org.antlr",
        "org.apache.logging.slf4j",
        "org.slf4j"
    )
}

val shadowJar by tasks.getting(ShadowJar::class) {
    configurations = listOf(shade)
    archiveClassifier.set("dev-all")
    from(sourceSets["accessor"].output)
    from(sourceSets["mixin"].output)

    dependencies {
        exclude(dependency("org.jetbrains.kotlin:.*:.*"))
        exclude(dependency("org.jetbrains.kotlinx:.*:.*"))
        exclude(dependency("org.jetbrains:annotations:.*"))
        exclude(dependency("org.checkerframework:checker-qual:.*"))
    }
}

val remapShadowJar = tasks.register<RemapJarTask>("remapShadowJar") {
    dependsOn(shadowJar)
    archiveClassifier.set("all")
    input.set(shadowJar.archiveFile)
    addNestedDependencies.set(true)
}

configurations {
    sequenceOf(shadowRuntimeElements, shadow).forEach {
        it.configure {
            outgoing {
                artifacts.clear()
                artifact(remapShadowJar)
            }
        }
    }
}
