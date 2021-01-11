
import ca.stellardrift.build.configurate.ConfigFormats
import ca.stellardrift.build.configurate.transformations.convertFormat
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.task.RemapJarTask
import org.jetbrains.kotlin.backend.common.atMostOne

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
    val cloudVersion: String by project

    shade(project(":impl-blocks:minecraft")) {
        exclude("com.google.guava")
        exclude("com.google.code.gson")
        exclude("org.spongepowered")
        exclude("net.kyori")
        exclude("org.jetbrains.kotlin")
    }

    shade(project(":impl-blocks:hikari-config"))
    shade("org.apache.logging.log4j:log4j-slf4j-impl:2.8.1") { isTransitive = false }
    shade("cloud.commandframework:cloud-brigadier:$cloudVersion")

    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:$minecraftVersion+build.7:v2")
    modImplementation("net.fabricmc:fabric-loader:0.11.0")
    modCompileOnly("com.sk89q.worldedit:worldedit-fabric-mc1.16.3:7.2.0") { isTransitive = false }
    modCompileOnly("com.sk89q.worldedit:worldedit-core:7.2.0") { isTransitive = false }

    modImplementation("net.fabricmc.fabric-api:fabric-api:0.29.3+1.17")
    modImplementation("net.fabricmc:fabric-language-kotlin:1.4.21+build.1")
    modImplementation(include("net.kyori:adventure-platform-fabric:$adventurePlatformVersion") {
        exclude("com.google.code.gson")
    })
    modImplementation(include("ca.stellardrift:confabricate:2.0.1") {
        exclude("com.google.code.gson")
    })
    modImplementation(include("me.lucko:fabric-permissions-api:0.1-SNAPSHOT")!!)

    runtimeOnly("net.minecraftforge:forgeflower:1.5.478.18")
}

tasks.withType(ProcessResources::class).configureEach {
    filesMatching("*.yml") {
        convertFormat(ConfigFormats.YAML, ConfigFormats.JSON)
        name = "${name.removeSuffix(".yml")}.json"
    }
}

tasks.withType(net.fabricmc.loom.task.AbstractRunTask::class).configureEach {
    // Midxin debug options
    jvmArgs(
        // "-Dmixin.debug.verbose=true",
        // "-Dmixin.debug.export=true",
        // "-Dmixin.debug.export.decompile.async=false",
        "-Dmixin.dumpTargetOnFailure=true",
        "-Dmixin.checks.interfaces=true",
        "-Dpermissionsex.debug.mixinaudit=true"
    )

    // Configure mixin agent
    jvmArgumentProviders += CommandLineArgumentProvider {
        // Resolve the Mixin configuration
        // Java agent: the jar file for mixin
        val mixinJar = configurations.runtimeClasspath.get().resolvedConfiguration
            .getFiles { it.name == "sponge-mixin" && it.group == "net.fabricmc" }
            .atMostOne()
        if (mixinJar != null) {
            listOf("-javaagent:$mixinJar")
        } else {
            emptyList()
        }
    }
}

pexPlatform {
    relocate(
        "cloud.commandframework",
        "com.github.benmanes.caffeine",
        "com.zaxxer.hikari",
        "org.antlr",
        "org.jdbi",
        "org.pcollections",
        "org.slf4j"
    )
    relocate("org.apache.logging.slf4j", keepElements = 2)
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
        exclude(dependency("io.leangen:geantyref:.*"))
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
