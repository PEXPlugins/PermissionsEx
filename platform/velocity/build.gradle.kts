
import ca.stellardrift.build.common.velocity
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
    id("com.github.johnrengelman.shadow")
    kotlin("kapt")
    id("ca.stellardrift.localization")
    id("ca.stellardrift.templating")
}

setupPublication()

repositories {
    velocity()
}

dependencies {
    api(project(":core")) {
        exclude("org.slf4j", "slf4j-api")
        exclude("com.google.code.gson")
        exclude("com.google.guava")
        exclude("org.spongepowered", "configurate-yaml")
        exclude("net.kyori")
        // exclude("org.yaml", "snakeyaml")
    }
    api(project(":impl-blocks:proxy-common")) { isTransitive = false }
    implementation(project(":impl-blocks:hikari-config")) {
        exclude("org.slf4j", "slf4j-api")
    }
    implementation(project(":impl-blocks:profile-resolver")) { isTransitive = false }

    kapt(shadow("com.velocitypowered:velocity-api:1.0.8-SNAPSHOT")!!)
}

localization {
    templateFile.set(rootProject.file("etc/messages-template.kt.tmpl"))
}

val relocateRoot = project.ext["pexRelocateRoot"]
val shadowJar by tasks.getting(ShadowJar::class) {
    minimize {
        exclude(dependency("com.github.ben-manes.caffeine:.*:.*"))
    }
    listOf("com.zaxxer", "com.typesafe", "com.github.benmanes",
        "ninja.leaping.configurate", "org.jetbrains",
        "org.checkerframework", "org.antlr.v4").forEach {
        relocate(it, "$relocateRoot.$it")
    }
    exclude("org/checkerframework/**")

    manifest {
        attributes("Automatic-Module-Name" to project.name)
    }
}

tasks.assemble {
    dependsOn(shadowJar)
}
