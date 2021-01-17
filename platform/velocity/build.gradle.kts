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

val velocityRunClasspath by configurations.creating {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class, Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category::class, Category.LIBRARY))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements::class, LibraryElements.JAR))
    }
}

repositories {
    ivy("https://versions.velocitypowered.com/download/") {
        patternLayout { artifact("[revision].[ext]") }
        metadataSources { artifact() }
        content { includeModule("com.velocitypowered", "velocity-proxy") }
    }
}

dependencies {
    val cloudVersion: String by project
    val velocityVersion: String by project
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

    annotationProcessor(shadow("com.velocitypowered:velocity-api:$velocityVersion")!!)
    velocityRunClasspath("com.velocitypowered:velocity-proxy:$velocityVersion")
}

pexPlatform {
    relocate(
        "cloud.commandframework",
        "com.github.benmanes.caffeine",
        "com.zaxxer.hikari",
        "io.leangen.geantyref",
        "org.antlr",
        "org.checkerframework",
        "org.jdbi",
        "org.spongepowered.configurate",
        "org.pcollections"
    )
}

val pluginJar = tasks.shadowJar.map { it.outputs }
val spongeRunFiles = velocityRunClasspath.asFileTree
val runVelocity by tasks.registering(JavaExec::class) {
    group = "pex"
    description = "Spin up a Velocity server environment"
    standardInput = System.`in`
    javaLauncher.set(pexPlatform.developmentRuntime())

    inputs.files(pluginJar)

    classpath(spongeRunFiles)
    workingDir = layout.projectDirectory.dir("run").asFile

    doFirst {
        // Prepare
        val modsDir = workingDir.resolve("plugins")
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
