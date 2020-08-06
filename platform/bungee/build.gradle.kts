
import ca.stellardrift.build.common.configurate
import ca.stellardrift.build.common.kyoriText
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
    id("com.github.johnrengelman.shadow")
    id("ca.stellardrift.localization")
}

setupPublication()

repositories {
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

dependencies {
    api(project(":core")) {
        exclude("com.google.code.gson")
        exclude("com.google.guava")
        exclude("org.yaml", "snakeyaml")
    }

    implementation(configurate("yaml")) {
        exclude(group = "com.google.guava")
        exclude("org.yaml", "snakeyaml")
    }
    implementation(kyoriText("adapter-bungeecord", Versions.TEXT_ADAPTER)) {
        exclude("com.google.code.gson")
    }
    implementation(kyoriText("serializer-gson", Versions.TEXT)) { isTransitive = false }
    implementation("org.slf4j:slf4j-jdk14:${Versions.SLF4J}")
    implementation(project(":impl-blocks:profile-resolver")) { isTransitive = false }
    api(project(":impl-blocks:proxy-common")) { isTransitive = false }
    implementation(project(":impl-blocks:hikari-config"))

    shadow("net.md-5:bungeecord-api:1.14-SNAPSHOT")
}

tasks.processResources {
    expand("project" to project)
}

localization {
    templateFile.set(rootProject.file("etc/messages-template.kt.tmpl"))
}

val relocateRoot = project.ext["pexRelocateRoot"]
val shadowJar by tasks.getting(ShadowJar::class) {
    minimize {
        exclude(dependency("com.github.ben-manes.caffeine:.*:.*"))
    }
    listOf("com.github.benmanes", "com.zaxxer", "com.typesafe",
        "ninja.leaping.configurate", "org.jetbrains.annotations",
        "org.slf4j", "org.antlr.v4.runtime", "net.kyori").forEach {
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
