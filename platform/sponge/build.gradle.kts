
import ca.stellardrift.build.configurate.ConfigFormats
import ca.stellardrift.build.configurate.transformations.convertFormat
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
    id("pex-platform")
    id("ca.stellardrift.localization")
    id("ca.stellardrift.templating")
    id("ca.stellardrift.configurate-transformations")
}

val spongeRunClasspath by configurations.creating {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class, Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category::class, Category.LIBRARY))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements::class, LibraryElements.JAR))
    }
}

repositories {
    mavenLocal {
        content {
            includeGroup("org.spongepowered")
        }
    }
}

dependencies {
    val slf4jVersion: String by project

    api(project(":impl-blocks:minecraft")) {
        // Dependencies provided by spongeapi
        exclude(group = "com.google.guava", module = "guava")
        exclude(group = "com.github.ben-manes.caffeine", module = "caffeine")
        exclude(group = "org.spongepowered")
        exclude(group = "net.kyori")
    }

    testImplementation(compileOnly("org.spongepowered:spongeapi:8.0.0-SNAPSHOT")!!)
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.13.3") { isTransitive = false }

    testImplementation("org.slf4j:slf4j-jdk14:$slf4jVersion")
    testImplementation("org.mockito:mockito-core:3.7.0")

    spongeRunClasspath(project(project.path, configuration = "shadow"))
    spongeRunClasspath("org.spongepowered:spongevanilla:1.16.4-8.0.0-RC375:universal") { isTransitive = false }
}

pexPlatform {
    relocate(
        "cloud.commandframework",
        "kotlin",
        "kotlinx",
        "org.antlr",
        "org.jdbi",
        "org.jetbrains.annotations",
        "org.pcollections",
        "org.slf4j"
    )
    relocate("org.apache.logging.slf4j", keepElements = 2)
    excludeChecker()
}

val shadowJar by tasks.getting(ShadowJar::class) {
    dependencies {
        exclude(dependency("com.google.code.gson:gson:.*"))
        exclude(dependency("io.leangen:geantyref:.*"))
    }

    manifest.attributes(
        "Loader" to "java_plain" // declare as a Sponge plugin
    )
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("**/*.yml") {
        convertFormat(ConfigFormats.YAML, ConfigFormats.JSON)
        name = name.substringBeforeLast('.') + ".json"
    }
}

val pluginJar = shadowJar.outputs
val spongeRunFiles = spongeRunClasspath.asFileTree
val runSponge by tasks.registering(JavaExec::class) {
    group = "pex"
    description = "Spin up a SpongeVanilla API 8 server environment"
    standardInput = System.`in`
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(8)) })

    inputs.files(spongeRunClasspath)

    classpath(spongeRunFiles)
    mainClass.set("org.spongepowered.vanilla.installer.InstallerMain")
    workingDir = layout.projectDirectory.dir("run").asFile

    doFirst {
        // Prepare
        if (!workingDir.isDirectory) {
            workingDir.mkdirs()
        }
    }
}
