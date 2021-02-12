
import ca.stellardrift.build.common.adventure
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
    id("ca.stellardrift.localization")
    id("ca.stellardrift.templating")
    id("pex-platform")
    id("ca.stellardrift.opinionated.kotlin")
    kotlin("kapt")
}

val spongeRunClasspath by configurations.creating {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class, Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category::class, Category.LIBRARY))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements::class, LibraryElements.JAR))
    }
}

dependencies {
    val adventurePlatformVersion: String by project
    val cloudVersion: String by project
    val slf4jVersion: String by project

    api(project(":impl-blocks:minecraft")) {
        exclude("com.google.guava", "guava")
        exclude("org.slf4j", "slf4j-api")
        exclude("com.github.ben-manes.caffeine", "caffeine")
    }

    implementation(adventure("platform-spongeapi", adventurePlatformVersion)) {
        exclude("com.google.code.gson")
    }
    implementation("cloud.commandframework:cloud-sponge7:$cloudVersion")

    testImplementation(kapt(shadow("org.spongepowered:spongeapi:7.3.0")!!)!!)

    testImplementation("org.slf4j:slf4j-jdk14:$slf4jVersion")
    testImplementation("org.mockito:mockito-core:3.7.7")

    spongeRunClasspath("org.spongepowered:spongevanilla:1.12.2-7.3.0") { isTransitive = false }
}

pexPlatform {
    relocate(
        "cloud.commandframework",
        "com.typesafe.config",
        "io.leangen.geantyref",
        "kotlin",
        "kotlinx",
        "net.kyori",
        "org.antlr",
        "org.checkerframework",
        "org.intellij",
        "org.jdbi",
        "org.jetbrains.annotations",
        "org.pcollections",
        "org.spongepowered.configurate"
    )
}

val shadowJar by tasks.getting(ShadowJar::class) {
    dependencies {
        exclude(dependency("org.yaml:snakeyaml:.*"))
        exclude(dependency("com.google.code.gson:gson:.*"))
    }
}

val pluginJar = shadowJar.outputs
val spongeRunFiles = spongeRunClasspath.asFileTree
val runSponge7 by tasks.registering(JavaExec::class) {
    group = "pex"
    description = "Spin up a SpongeVanilla server environment"
    standardInput = System.`in`
    // Sponge on 1.12 is stuck on Java 8 because of LaunchWrapper
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(8)) })

    inputs.files(spongeRunClasspath, pluginJar)

    classpath(spongeRunFiles)
    mainClass.set("org.spongepowered.server.launch.VersionCheckingMain")
    workingDir = layout.projectDirectory.dir("run").asFile

    doFirst {
        // Prepare
        val modsDir = workingDir.resolve("mods")
        if (!modsDir.isDirectory) {
            modsDir.mkdirs()
        }

        project.copy {
            into(modsDir.absolutePath)
            from(pluginJar) {
                rename { "${rootProject.name}-${project.name}.jar" }
            }
        }
    }
}
