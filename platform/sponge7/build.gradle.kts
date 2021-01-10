
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
    kotlin("kapt")
}

dependencies {
    val adventurePlatformVersion: String by project
    val slf4jVersion: String by project

    api(project(":impl-blocks:minecraft")) {
        exclude("com.google.guava", "guava")
        exclude("org.slf4j", "slf4j-api")
        exclude("com.github.ben-manes.caffeine", "caffeine")
    }

    implementation(adventure("platform-spongeapi", adventurePlatformVersion)) {
        exclude("com.google.code.gson")
    }

    testImplementation(kapt(shadow("org.spongepowered:spongeapi:7.3.0")!!)!!)

    testImplementation("org.slf4j:slf4j-jdk14:$slf4jVersion")
    testImplementation("org.mockito:mockito-core:3.6.28")
}

pexPlatform {
    relocate(
        "cloud.commandframework",
        "io.leangen.geantyref",
        "kotlin",
        "kotlinx",
        "net.kyori",
        "org.antlr",
        "org.jdbi",
        "org.jetbrains.annotations",
        "org.spongepowered.configurate"
    )
}

val shadowJar by tasks.getting(ShadowJar::class) {
    dependencies {
        exclude(dependency("com.typesafe:config:.*"))
        exclude(dependency("org.yaml:snakeyaml:.*"))
        exclude(dependency("com.google.code.gson:gson:.*"))
    }
}
