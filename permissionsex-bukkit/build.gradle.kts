
import ca.stellardrift.build.configurate
import ca.stellardrift.build.jitpack
import ca.stellardrift.build.kyoriText
import ca.stellardrift.build.spigot
import ca.stellardrift.permissionsex.gradle.Versions
import ca.stellardrift.permissionsex.gradle.setupPublication
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

import java.time.LocalDate
import java.time.ZoneOffset

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

val spigotVersion: String = "1.15.1-R0.1-SNAPSHOT"

plugins {
    id("com.github.johnrengelman.shadow")
    id("ca.stellardrift.localization")
}

setupPublication()

repositories {
    jitpack()
    spigot()
}

java {
    registerFeature("h2dbSupport") {
        usingSourceSet(sourceSets["main"])
    }
}

license {
    header = file("LICENSE_HEADER")
    ext["year"] = LocalDate.now(ZoneOffset.UTC).year
}

localization {
    templateFile.set(rootProject.file("etc/messages-template.kt.tmpl"))
}

tasks.processResources {
    expand("project" to project)
}

dependencies {
    api(project(":permissionsex-core")) {
        exclude(group="com.google.guava")
        exclude("org.yaml", "snakeyaml")
        exclude("com.google.code.gson", "gson")
    }

    implementation(configurate("yaml")) {
        exclude(group="com.google.guava")
        exclude("org.yaml", "snakeyaml")
    }
    implementation(kyoriText("adapter-bukkit", Versions.TEXT_ADAPTER)) {
        exclude("com.google.code.gson")
    }
    implementation(kyoriText("serializer-gson", Versions.TEXT)) {
        exclude("com.google.code.gson")
    }

    implementation("org.slf4j:slf4j-jdk14:${Versions.SLF4J}")
    implementation(project(":impl-blocks:permissionsex-hikari-config"))
    implementation(project(":impl-blocks:permissionsex-profile-resolver")) { isTransitive = false }

    // provided at runtime
    shadow("org.spigotmc:spigot-api:$spigotVersion")
    shadow("com.github.MilkBowl:VaultAPI:1.7")
    shadow("com.sk89q.worldguard:worldguard-bukkit:7.0.2-SNAPSHOT")
    shadow("com.h2database:h2:1.4.199")
}

val relocateRoot = project.ext["pexRelocateRoot"]
val shadowJar by tasks.getting(ShadowJar::class) {
    minimize {
        exclude(dependency("com.github.ben-manes.caffeine:.*:.*"))
    }
    listOf(
        "ninja.leaping.configurate",
        "com.zaxxer.hikari",
        "com.github.benmanes.caffeine",
        "com.google.errorprone",
        "com.typesafe",
        "org.jetbrains.annotations",
        "org.slf4j",
        "org.antlr",
        "net.kyori"
    ).forEach {
        relocate(it, "$relocateRoot.$it")
    }
    dependencies {
        exclude("org.yaml:snakeyaml")
    }
    exclude("org/checkerframework/**")
    manifest {
        attributes("Automatic-Module-Name" to project.name)
    }
}

tasks.assemble {
    dependsOn(shadowJar)
}
