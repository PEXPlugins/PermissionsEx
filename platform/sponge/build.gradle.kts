
import ca.stellardrift.build.common.sponge
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

repositories {
    sponge()
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
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.14.0") { isTransitive = false }

    testImplementation("org.slf4j:slf4j-jdk14:$slf4jVersion")
    testImplementation("org.mockito:mockito-core:3.0.0")
}

pexPlatform {
    relocate(
        "kotlin",
        "kotlinx",
        "org.antlr",
        "org.apache.logging.slf4j",
        "org.jetbrains.annotations",
        "org.slf4j"
    )
}

val shadowJar by tasks.getting(ShadowJar::class) {
    dependencies {
        exclude(dependency("com.google.code.gson:gson:.*"))
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
