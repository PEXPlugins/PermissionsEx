
import ca.stellardrift.build.transformations.ConfigFormats
import ca.stellardrift.build.transformations.convertFormat
import ca.stellardrift.permissionsex.gradle.Versions
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
    id("ca.stellardrift.opinionated.kotlin")
    id("com.github.johnrengelman.shadow")
    id("ca.stellardrift.localization")
    id("ca.stellardrift.templating")
    id("ca.stellardrift.configurate-transformations")
}

setupPublication()

repositories {
    maven("https://repo-new.spongepowered.org/repository/maven-public") {
        name = "spongeNew"
    }
}

dependencies {
    api(project(":impl-blocks:minecraft")) {
        // Dependencies provided by spongeapi
        exclude(group = "com.google.guava", module = "guava")
        exclude(group = "com.github.ben-manes.caffeine", module = "caffeine")
        exclude(group = "org.spongepowered")
        exclude(group = "net.kyori")
    }

    testImplementation(compileOnly("org.spongepowered:spongeapi:8.0.0-SNAPSHOT")!!)
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.13.3") { isTransitive = false }

    testImplementation("org.slf4j:slf4j-jdk14:${Versions.SLF4J}")
    testImplementation("org.mockito:mockito-core:3.0.0")
}

localization {
    templateFile.set(rootProject.file("etc/messages-template.kt.tmpl"))
}

opinionated {
    useJUnit5()
    automaticModuleNames = true
}

val relocateRoot = project.ext["pexRelocateRoot"]
val shadowJar by tasks.getting(ShadowJar::class) {
    minimize()
    listOf(
        "org.antlr",
        "org.slf4j",
        "org.jetbrains.annotations",
        "org.apache.logging.slf4j",
        "kotlinx",
        "kotlin"
    ).forEach {
        relocate(it, "$relocateRoot.$it")
    }
    exclude("org/checkerframework/**")
    exclude("**/module-info.class")

    dependencies {
        exclude(dependency("com.google.code.gson:gson:.*"))
    }

    manifest {
        attributes(
            "Loader" to "java_plain" // declare as a Sponge plugin
        )
    }
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("**/*.yml") {
        expand("project" to project)
        convertFormat(ConfigFormats.YAML, ConfigFormats.GSON)
        name = name.substringBeforeLast('.') + ".json"
    }
}

tasks.assemble {
    dependsOn(shadowJar)
}
