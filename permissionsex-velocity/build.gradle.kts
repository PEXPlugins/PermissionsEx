import ca.stellardrift.permissionsex.gradle.applyCommonSettings
import ca.stellardrift.permissionsex.gradle.setupPublication
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

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
}

applyCommonSettings()
setupPublication()

repositories {
    maven {
        name = "velocity"
        url = uri("https://repo.velocitypowered.com/snapshots/")
    }
}

dependencies {
    api(project(":permissionsex-core")) {
        exclude("org.slf4j", "slf4j-api")
        exclude("com.google.code.gson")
        exclude("com.google.guava")
        exclude("org.spongepowered", "configurate-yaml")
        //exclude("org.yaml", "snakeyaml")
    }
    api(project(":impl-blocks:permissionsex-proxy-common")) { isTransitive = false }
    implementation(project(":impl-blocks:permissionsex-hikari-config")) {
        exclude("org.slf4j", "slf4j-api")
    }

    shadow("com.velocitypowered:velocity-api:1.0.0-SNAPSHOT")
    kapt("com.velocitypowered:velocity-api:1.0.0-SNAPSHOT")
}


// Expand tokens in source templates
val generatedSourceRoot = "$buildDir/generated/kotlin-templates"
val generateSource by tasks.registering(Copy::class) {
    from("src/main/kotlin-templates") {
        include("**/*.kt")
    }
    expand("project" to project)
    into(generatedSourceRoot)
}

sourceSets["main"].withConvention(KotlinSourceSet::class) {
    kotlin.srcDir(generatedSourceRoot)
}
tasks.compileKotlin {
    dependsOn(generateSource)
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
    dependencies {
        exclude("org.checkerframework:.*:.*")
    }
}

tasks.assemble {
    dependsOn(shadowJar)
}
