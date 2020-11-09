
import ca.stellardrift.build.common.adventure
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
    kotlin("kapt")
    id("ca.stellardrift.localization")
    id("ca.stellardrift.templating")
}

setupPublication()

repositories {
    maven("https://repo-new.spongepowered.org/repository/maven-public") {
        name = "spongeNew"
    }
}

dependencies {
    api(project(":core")) {
        exclude("com.google.guava", "guava")
        exclude("org.slf4j", "slf4j-api")
        exclude("com.github.ben-manes.caffeine", "caffeine")
    }

    implementation(adventure("platform-spongeapi", Versions.TEXT_ADAPTER)) {
        exclude("com.google.code.gson")
    }

    kapt(shadow("org.spongepowered:spongeapi:${Versions.SPONGE}")!!)
    testImplementation("org.spongepowered:spongeapi:${Versions.SPONGE}")

    testImplementation("org.slf4j:slf4j-jdk14:${Versions.SLF4J}")
    testImplementation("org.mockito:mockito-core:3.0.0")
}

localization {
    templateFile.set(rootProject.file("etc/messages-template.kt.tmpl"))
}

opinionated {
    useJUnit5()
}

val relocateRoot = project.ext["pexRelocateRoot"]
val shadowJar by tasks.getting(ShadowJar::class) {
    minimize()
    listOf(
        "org.antlr",
        "net.kyori",
        "org.jetbrains.annotations",
        "io.leangen.geantyref",
        "org.spongepowered.configurate"
    ).forEach {
        relocate(it, "$relocateRoot.$it")
    }
    exclude("org/checkerframework/**")
    exclude("**/module-info.class")

    dependencies {
        exclude(dependency("com.typesafe:config:.*"))
        exclude(dependency("org.yaml:snakeyaml:.*"))
        exclude(dependency("com.google.code.gson:gson:.*"))
    }

    manifest {
        attributes("Automatic-Module-Name" to project.name)
    }
}

tasks.assemble {
    dependsOn(shadowJar)
}
